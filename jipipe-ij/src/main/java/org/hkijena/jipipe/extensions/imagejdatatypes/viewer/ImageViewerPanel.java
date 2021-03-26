/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.extensions.imagejdatatypes.viewer;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.ImageWindow;
import ij.measure.Calibration;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import ij.util.Tools;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.AVICompression;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.HyperstackDimension;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageSliceIndex;
import org.hkijena.jipipe.extensions.imagejdatatypes.viewer.plugins.*;
import org.hkijena.jipipe.extensions.settings.FileChooserSettings;
import org.hkijena.jipipe.ui.components.FormPanel;
import org.hkijena.jipipe.ui.components.PathEditor;
import org.hkijena.jipipe.ui.running.JIPipeRunExecuterUI;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ImageViewerPanel extends JPanel {
    private final JButton zoomStatusButton = new JButton();
    private ImagePlus image;
    private ImageProcessor slice;
    private ImageStatistics statistics;
    private ImageViewerPanelCanvas canvas;
    private JLabel stackSliderLabel = new JLabel("Slice (Z)");
    private JLabel channelSliderLabel = new JLabel("Channel (C)");
    private JLabel frameSliderLabel = new JLabel("Frame (T)");
    private JScrollBar stackSlider = new JScrollBar(Adjustable.HORIZONTAL, 1, 1, 1, 100);
    private JScrollBar channelSlider = new JScrollBar(Adjustable.HORIZONTAL, 1, 1, 1, 100);
    private JScrollBar frameSlider = new JScrollBar(Adjustable.HORIZONTAL, 1, 1, 1, 100);
    private JToggleButton animationStackToggle = new JToggleButton(UIUtils.getIconFromResources("actions/player_start.png"));
    private JToggleButton animationChannelToggle = new JToggleButton(UIUtils.getIconFromResources("actions/player_start.png"));
    private JToggleButton animationFrameToggle = new JToggleButton(UIUtils.getIconFromResources("actions/player_start.png"));
    private FormPanel bottomPanel;
    private long lastTimeZoomed;
    private JLabel imageInfoLabel = new JLabel();
    private JScrollPane scrollPane;
    private FormPanel formPanel = new FormPanel(null, FormPanel.WITH_SCROLLING);
    private JSpinner animationSpeed = new JSpinner(new SpinnerNumberModel(250, 5, 10000, 1));
    private Timer animationTimer = new Timer(250, e -> animateNextSlice());
    private int rotation = 0;
    private JMenuItem exportAllSlicesItem;
    private JMenuItem exportMovieItem;
    private JToolBar toolBar = new JToolBar();
    private List<ImageViewerPanelPlugin> plugins = new ArrayList<>();
    private JButton rotateLeftButton;
    private JButton rotateRightButton;

    public ImageViewerPanel() {
        initialize();
        updateZoomStatus();
    }

    /**
     * Opens the image in a new frame
     *
     * @param image the image
     * @param title the title
     * @return the panel
     */
    public static ImageViewerPanel showImage(ImagePlus image, String title) {
        ImageViewerPanel dataDisplay = new ImageViewerPanel();
        dataDisplay.setPlugins(Arrays.asList(new CalibrationPlugin(dataDisplay),
                new PixelInfoPlugin(dataDisplay),
                new LUTManagerPlugin(dataDisplay),
                new ROIManagerPlugin(dataDisplay)));
        dataDisplay.setImage(image);
        ImageViewerWindow window = new ImageViewerWindow(dataDisplay);
        window.setTitle(title);
        window.setVisible(true);
        return dataDisplay;
    }

    public List<ImageViewerPanelPlugin> getPlugins() {
        return plugins;
    }

    public <T extends ImageViewerPanelPlugin> T findPlugin(Class<T> klass) {
        for (ImageViewerPanelPlugin plugin : plugins) {
            if(klass.isAssignableFrom(plugin.getClass())) {
                return (T)plugin;
            }
        }
        return null;
    }

    public void setPlugins(List<ImageViewerPanelPlugin> plugins) {
        this.plugins = plugins;
    }

    public void setRotationEnabled(boolean enabled) {
        rotateLeftButton.setVisible(enabled);
        rotateRightButton.setVisible(enabled);
        if(!enabled) {
            rotation = 0;
            refreshImageInfo();
            refreshSlice();
        }
    }

    private void initialize() {
        setLayout(new BorderLayout());
        canvas = new ImageViewerPanelCanvas(this);
        scrollPane = new JScrollPane(canvas);
        canvas.setScrollPane(scrollPane);
        initializeToolbar();
        canvas.addMouseWheelListener(e -> {
            if (e.isControlDown()) {
                if (e.getWheelRotation() < 0) {
                    increaseZoom();
                } else {
                    decreaseZoom();
                }
            } else {
                getParent().dispatchEvent(e);
            }
        });
        canvas.getEventBus().register(this);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                scrollPane,
                formPanel);
        splitPane.setDividerSize(3);
        splitPane.setResizeWeight(0.66);
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                super.componentResized(e);
                splitPane.setDividerLocation(0.66);
            }
        });
        add(splitPane, BorderLayout.CENTER);

        bottomPanel = new FormPanel(null, FormPanel.NONE);
        add(bottomPanel, BorderLayout.SOUTH);

        // Register slider events
        stackSlider.addAdjustmentListener(e -> refreshSlice());
        channelSlider.addAdjustmentListener(e -> refreshSlice());
        frameSlider.addAdjustmentListener(e -> refreshSlice());

        initializeAnimationControls();
    }

    private void initializeAnimationControls() {
        animationTimer.setRepeats(true);
        animationSpeed.addChangeListener(e -> {
            stopAnimations();
            animationTimer.setDelay(((SpinnerNumberModel) animationSpeed.getModel()).getNumber().intValue());
        });
        animationFrameToggle.addActionListener(e -> {
            if (animationFrameToggle.isSelected()) {
                animationChannelToggle.setSelected(false);
                animationStackToggle.setSelected(false);
                animationTimer.start();
            }
        });
        animationChannelToggle.addActionListener(e -> {
            if (animationChannelToggle.isSelected()) {
                animationFrameToggle.setSelected(false);
                animationStackToggle.setSelected(false);
                animationTimer.start();
            }
        });
        animationStackToggle.addActionListener(e -> {
            if (animationStackToggle.isSelected()) {
                animationChannelToggle.setSelected(false);
                animationFrameToggle.setSelected(false);
                animationTimer.start();
            }
        });
    }

    private void stopAnimations() {
        animationTimer.stop();
        animationFrameToggle.setSelected(false);
        animationChannelToggle.setSelected(false);
        animationStackToggle.setSelected(false);
    }

    private void addSliderToForm(JScrollBar slider, JLabel label, JToggleButton animation) {
        UIUtils.makeFlat25x25(animation);
        JPanel descriptionPanel = new JPanel(new BorderLayout());
        descriptionPanel.add(animation, BorderLayout.WEST);
        descriptionPanel.add(label, BorderLayout.CENTER);
        bottomPanel.addToForm(slider, descriptionPanel, null);
    }

    public JToolBar getToolBar() {
        return toolBar;
    }

    private void initializeToolbar() {
        toolBar.setFloatable(false);

        JButton openInImageJButton = new JButton("Open in ImageJ", UIUtils.getIconFromResources("apps/imagej.png"));
        openInImageJButton.addActionListener(e -> openInImageJ());

        toolBar.add(openInImageJButton);
        toolBar.add(Box.createHorizontalStrut(8));
        toolBar.add(imageInfoLabel);

        toolBar.add(Box.createHorizontalGlue());

        JButton exportMenuButton = new JButton(UIUtils.getIconFromResources("actions/camera.png"));
        exportMenuButton.setToolTipText("Export currently displayed image");
        JPopupMenu exportMenu = new JPopupMenu();

        JMenuItem exportCurrentSliceItem = new JMenuItem("Snapshot of current slice", UIUtils.getIconFromResources("actions/viewimage.png"));
        exportCurrentSliceItem.addActionListener(e -> exportCurrentSliceToPNG());
        exportMenu.add(exportCurrentSliceItem);

        exportAllSlicesItem = new JMenuItem("Snapshot of all slices", UIUtils.getIconFromResources("actions/qlipper.png"));
        exportAllSlicesItem.addActionListener(e -> exportAllSlicesToPNG());
        exportMenu.add(exportAllSlicesItem);

        exportMovieItem = new JMenuItem("Movie", UIUtils.getIconFromResources("actions/filmgrain.png"));
        exportMovieItem.addActionListener(e -> exportVideo());
        exportMenu.add(exportMovieItem);

        UIUtils.addPopupMenuToComponent(exportMenuButton, exportMenu);
        toolBar.add(exportMenuButton);
        toolBar.addSeparator();

        rotateLeftButton = new JButton(UIUtils.getIconFromResources("actions/transform-rotate-left.png"));
        rotateLeftButton.setToolTipText("Rotate 90° to the left");
        rotateLeftButton.addActionListener(e -> rotateLeft());
        toolBar.add(rotateLeftButton);

        rotateRightButton = new JButton(UIUtils.getIconFromResources("actions/transform-rotate.png"));
        rotateRightButton.setToolTipText("Rotate 90° to the right");
        rotateRightButton.addActionListener(e -> rotateRight());
        toolBar.add(rotateRightButton);

        JButton centerImageButton = new JButton("Center image", UIUtils.getIconFromResources("actions/zoom-center-page.png"));
        centerImageButton.addActionListener(e -> canvas.centerImage());
        toolBar.add(centerImageButton);

        JButton fitImageButton = new JButton("Fit image", UIUtils.getIconFromResources("actions/zoom-select-fit.png"));
        fitImageButton.addActionListener(e -> fitImageToScreen());
        toolBar.add(fitImageButton);

        JButton zoomOutButton = new JButton(UIUtils.getIconFromResources("actions/zoom-out.png"));
        UIUtils.makeFlat25x25(zoomOutButton);
        zoomOutButton.addActionListener(e -> decreaseZoom());
        toolBar.add(zoomOutButton);

        UIUtils.makeBorderlessWithoutMargin(zoomStatusButton);
        JPopupMenu zoomMenu = UIUtils.addPopupMenuToComponent(zoomStatusButton);
        for (double zoom = 0.5; zoom <= 2; zoom += 0.25) {
            JMenuItem changeZoomItem = new JMenuItem((int) (zoom * 100) + "%", UIUtils.getIconFromResources("actions/zoom.png"));
            double finalZoom = zoom;
            changeZoomItem.addActionListener(e -> {
                canvas.setZoom(finalZoom);
                updateZoomStatus();
            });
            zoomMenu.add(changeZoomItem);
        }
        zoomMenu.addSeparator();
        JMenuItem changeZoomToItem = new JMenuItem("Set zoom value ...");
        changeZoomToItem.addActionListener(e -> {
            String zoomInput = JOptionPane.showInputDialog(this, "Please enter a new zoom value (in %)", (int) (canvas.getZoom() * 100) + "%");
            if (!StringUtils.isNullOrEmpty(zoomInput)) {
                zoomInput = zoomInput.replace("%", "");
                try {
                    int percentage = Integer.parseInt(zoomInput);
                    canvas.setZoom(percentage / 100.0);
                    updateZoomStatus();
                } catch (NumberFormatException ex) {
                    ex.printStackTrace();
                }
            }
        });
        zoomMenu.add(changeZoomToItem);
        toolBar.add(zoomStatusButton);

        JButton zoomInButton = new JButton(UIUtils.getIconFromResources("actions/zoom-in.png"));
        UIUtils.makeFlat25x25(zoomInButton);
        zoomInButton.addActionListener(e -> increaseZoom());
        toolBar.add(zoomInButton);

        add(toolBar, BorderLayout.NORTH);
    }

    public void exportCurrentSliceToPNG() {
        if (getCanvas().getImage() == null) {
            JOptionPane.showMessageDialog(this, "No image loaded.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        Path targetFile = FileChooserSettings.saveFile(this, FileChooserSettings.KEY_DATA, "Export current slice", UIUtils.EXTENSION_FILTER_PNG, UIUtils.EXTENSION_FILTER_JPEG, UIUtils.EXTENSION_FILTER_BMP);
        if (targetFile != null) {
            String format = "PNG";
            if (UIUtils.EXTENSION_FILTER_BMP.accept(targetFile.toFile()))
                format = "BMP";
            else if (UIUtils.EXTENSION_FILTER_JPEG.accept(targetFile.toFile()))
                format = "JPEG";
            try {
                ImageIO.write(getCanvas().getImage(), format, targetFile.toFile());
            } catch (IOException e) {
                IJ.handleException(e);
            }
        }
    }

    public void exportAllSlicesToPNG() {
        FormPanel formPanel = new FormPanel(null, FormPanel.NONE);
        PathEditor exportPathEditor = new PathEditor(PathEditor.IOMode.Open, PathEditor.PathMode.DirectoriesOnly);
        exportPathEditor.setPath(FileChooserSettings.getInstance().getLastDataDirectory());
        formPanel.addToForm(exportPathEditor, new JLabel("Target directory"), null);

        JComboBox<String> fileFormatEditor = new JComboBox<>(new String[]{"PNG", "JPEG", "BMP"});
        fileFormatEditor.setSelectedItem("BMP");
        formPanel.addToForm(fileFormatEditor, new JLabel("File format"), null);

        JTextField baseNameEditor = new JTextField();
        formPanel.addToForm(baseNameEditor, new JLabel("File name prefix"), null);

        int response = JOptionPane.showOptionDialog(this,
                formPanel,
                "Export all slices to folder",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE,
                null,
                null,
                null);
        if (response == JOptionPane.OK_OPTION) {
            FileChooserSettings.getInstance().setLastDataDirectory(exportPathEditor.getPath());
            Path targetPath = exportPathEditor.getPath();
            String format = fileFormatEditor.getSelectedItem() + "";
            String baseName = StringUtils.makeFilesystemCompatible(baseNameEditor.getText());
            ImageViewerStackExporterRun run = new ImageViewerStackExporterRun(this, targetPath, baseName, format);
            JIPipeRunExecuterUI.runInDialog(this, run);
        }
    }

    public void exportVideo() {
        FormPanel formPanel = new FormPanel(null, FormPanel.NONE);
        PathEditor exportPathEditor = new PathEditor(PathEditor.IOMode.Save, PathEditor.PathMode.FilesOnly);
        exportPathEditor.setPath(FileChooserSettings.getInstance().getLastDataDirectory());
        formPanel.addToForm(exportPathEditor, new JLabel("Exported file"), null);

        List<HyperstackDimension> availableDimensions = new ArrayList<>();
        if (image.getNFrames() > 1)
            availableDimensions.add(HyperstackDimension.Frame);
        if (image.getNSlices() > 1)
            availableDimensions.add(HyperstackDimension.Depth);
        if (image.getNChannels() > 1)
            availableDimensions.add(HyperstackDimension.Channel);

        JComboBox<HyperstackDimension> dimensionEditor = new JComboBox<>(availableDimensions.toArray(new HyperstackDimension[0]));
        dimensionEditor.setSelectedItem(availableDimensions.get(0));
        formPanel.addToForm(dimensionEditor, new JLabel("Animated dimension"), null);

        JComboBox<AVICompression> compressionEditor = new JComboBox<>(AVICompression.values());
        compressionEditor.setSelectedItem(AVICompression.JPEG);
        formPanel.addToForm(compressionEditor, new JLabel("Compression"), null);

        JSlider compressionQualityEditor = new JSlider(0, 100, 100);
        formPanel.addToForm(compressionQualityEditor, new JLabel("Quality"), null);

        int response = JOptionPane.showOptionDialog(this,
                formPanel,
                "Export video",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE,
                null,
                null,
                null);
        if (response == JOptionPane.OK_OPTION) {
            FileChooserSettings.getInstance().setLastDataDirectory(exportPathEditor.getPath());
            ImageViewerVideoExporterRun run = new ImageViewerVideoExporterRun(
                    this,
                    exportPathEditor.getPath(),
                    getCurrentSlicePosition(),
                    (HyperstackDimension) dimensionEditor.getSelectedItem(),
                    animationTimer.getDelay(),
                    (AVICompression) compressionEditor.getSelectedItem(),
                    compressionQualityEditor.getValue());
            JIPipeRunExecuterUI.runInDialog(this, run);
        }
    }

    private void rotateLeft() {
        if (rotation == 0)
            rotation = 270;
        else
            rotation -= 90;
        refreshImageInfo();
        refreshSlice();
    }

    public void rotateRight() {
        rotation = (rotation + 90) % 360;
        refreshImageInfo();
        refreshSlice();
    }

    private void updateZoomStatus() {
        zoomStatusButton.setText((int) Math.round(canvas.getZoom() * 100) + "%");
    }

    private void increaseZoom() {
        long diff = System.currentTimeMillis() - lastTimeZoomed;
        double x = Math.min(250, diff);
        double fac = 0.2 - 0.15 * (x / 250);
        lastTimeZoomed = System.currentTimeMillis();
        canvas.setZoom(canvas.getZoom() + fac);
        updateZoomStatus();
    }

    private void decreaseZoom() {
        long diff = System.currentTimeMillis() - lastTimeZoomed;
        double x = Math.min(250, diff);
        double fac = 0.2 - 0.15 * (x / 250);
        lastTimeZoomed = System.currentTimeMillis();
        canvas.setZoom(canvas.getZoom() - fac);
        updateZoomStatus();
    }

    private void openInImageJ() {
        if (image != null) {
            String title = image.getTitle();
            ImagePlus duplicate = image.duplicate();
            duplicate.setTitle(title);
            duplicate.show();
        }
    }

    private void refreshSliders() {
        if (image != null) {
            bottomPanel.setVisible(true);
            bottomPanel.clear();

            if (image.getNChannels() > 1)
                addSliderToForm(channelSlider, channelSliderLabel, animationChannelToggle);
            if (image.getNSlices() > 1)
                addSliderToForm(stackSlider, stackSliderLabel, animationStackToggle);
            if (image.getNFrames() > 1)
                addSliderToForm(frameSlider, frameSliderLabel, animationFrameToggle);

            stackSlider.setMinimum(1);
            stackSlider.setMaximum(image.getNSlices() + 1);
            channelSlider.setMinimum(1);
            channelSlider.setMaximum(image.getNChannels() + 1);
            frameSlider.setMinimum(1);
            frameSlider.setMaximum(image.getNFrames() + 1);
        } else {
            bottomPanel.setVisible(false);
        }
    }

    public ImagePlus getImage() {
        return image;
    }

    public void setImage(ImagePlus image) {
        this.image = image;
        if (image != null) {
            this.statistics = image.getStatistics();
        } else {
            this.statistics = null;
        }
        refreshSliders();
        refreshSlice();
        refreshImageInfo();
        refreshFormPanel();
        refreshMenus();
        for (ImageViewerPanelPlugin plugin : plugins) {
            plugin.onImageChanged();
        }
        revalidate();
        repaint();
    }

    private void refreshMenus() {
        boolean hasMultipleSlices = image != null && image.getNDimensions() > 2;
        exportAllSlicesItem.setVisible(hasMultipleSlices);
        exportMovieItem.setVisible(hasMultipleSlices);
    }

    public void refreshFormPanel() {
        int scrollValue = formPanel.getScrollPane().getVerticalScrollBar().getValue();
        formPanel.clear();
        for (ImageViewerPanelPlugin plugin : plugins) {
            plugin.createPalettePanel(formPanel);
        }
        if (image != null && (image.getNChannels() > 1 || image.getNSlices() > 1 || image.getNFrames() > 1)) {
            formPanel.addGroupHeader("Animation", UIUtils.getIconFromResources("actions/filmgrain.png"));
            formPanel.addToForm(animationSpeed, new JLabel("Speed (ms)"), null);
        }
        formPanel.addVerticalGlue();
        SwingUtilities.invokeLater(() -> {
            formPanel.getScrollPane().getVerticalScrollBar().setValue(scrollValue);
        });
    }

    /**
     * Returns the currently viewed slice position
     * @return the slice position. Zero-based indices
     */
    public ImageSliceIndex getCurrentSlicePosition() {
        return new ImageSliceIndex(stackSlider.getValue() - 1, channelSlider.getValue() - 1, frameSlider.getValue() - 1);
    }

    public void refreshImageInfo() {
        String s = "";
        if (image == null) {
            imageInfoLabel.setText("");
            return;
        }
        int type = image.getType();
        Calibration cal = image.getCalibration();
        if (cal.scaled()) {
            boolean unitsMatch = cal.getXUnit().equals(cal.getYUnit());
            double cwidth = image.getWidth() * cal.pixelWidth;
            double cheight = image.getHeight() * cal.pixelHeight;
            int digits = Tools.getDecimalPlaces(cwidth, cheight);
            if (digits > 2) digits = 2;
            if (unitsMatch) {
                s += IJ.d2s(cwidth, digits) + "x" + IJ.d2s(cheight, digits)
                        + " " + cal.getUnits() + " (" + image.getWidth() + "x" + image.getHeight() + "); ";
            } else {
                s += (cwidth) + " " + cal.getXUnit() + " x "
                        + (cheight) + " " + cal.getYUnit()
                        + " (" + image.getWidth() + "x" + image.getHeight() + "); ";
            }
        } else
            s += image.getWidth() + "x" + image.getHeight() + " pixels; ";
        switch (type) {
            case ImagePlus.GRAY8:
            case ImagePlus.COLOR_256:
                s += "8-bit";
                break;
            case ImagePlus.GRAY16:
                s += "16-bit";
                break;
            case ImagePlus.GRAY32:
                s += "32-bit";
                break;
            case ImagePlus.COLOR_RGB:
                s += "RGB";
                break;
        }
        if (image.isInvertedLut())
            s += " (inverting LUT)";
        s += "; " + ImageWindow.getImageSize(image);
        if (rotation != 0) {
            s += " (Rotated " + rotation + "°)";
        }
        imageInfoLabel.setText(s);
    }

    private void animateNextSlice() {
        if (!isDisplayable()) {
            stopAnimations();
            return;
        }
        if (animationStackToggle.isSelected()) {
            int newIndex = (image.getZ() % image.getNSlices()) + 1;
            stackSlider.setValue(newIndex);
        } else if (animationChannelToggle.isSelected()) {
            int newIndex = (image.getC() % image.getNChannels()) + 1;
            channelSlider.setValue(newIndex);
        } else if (animationFrameToggle.isSelected()) {
            int newIndex = (image.getT() % image.getNFrames()) + 1;
            frameSlider.setValue(newIndex);
        } else {
            stopAnimations();
        }
    }

    public void refreshSlice() {
        if (image != null) {
            int stack = Math.max(1, Math.min(image.getNSlices(), stackSlider.getValue()));
            int frame = Math.max(1, Math.min(image.getNFrames(), frameSlider.getValue()));
            int channel = Math.max(1, Math.min(image.getNChannels(), channelSlider.getValue()));
            stackSliderLabel.setText(String.format("Slice (Z) %d/%d", stack, image.getNSlices()));
            frameSliderLabel.setText(String.format("Frame (T) %d/%d", frame, image.getNFrames()));
            channelSliderLabel.setText(String.format("Channel (C) %d/%d", channel, image.getNChannels()));
//            System.out.println("bps: " + image.getDisplayRangeMin() + ", " + image.getDisplayRangeMax());
            image.setPosition(channel, stack, frame);
            this.slice = image.getProcessor();
            this.statistics = image.getStatistics();
            for (ImageViewerPanelPlugin plugin : plugins) {
//                System.out.println(plugin + ": " + image.getDisplayRangeMin() + ", " + image.getDisplayRangeMax());
                plugin.onSliceChanged();
//                System.out.println(plugin + "(A): " + image.getDisplayRangeMin() + ", " + image.getDisplayRangeMax());
            }
            uploadSliceToCanvas();
        }
    }

    public ImageProcessor generateSlice(int z, int c, int t, boolean withRotation) {
        image.setPosition(c + 1, z + 1, t + 1);
        for (ImageViewerPanelPlugin plugin : plugins) {
            plugin.beforeDraw(z, c, t);
        }
//        System.out.println(Arrays.stream(image.getLuts()).map(Object::toString).collect(Collectors.joining(" ")));
        ImageProcessor processor = image.getProcessor();
        for (ImageViewerPanelPlugin plugin : plugins) {
            processor = plugin.draw(z, c, t, processor);
        }
        if (withRotation && rotation != 0) {
            if (rotation == 90)
                processor = processor.rotateRight();
            else if (rotation == 180)
                processor = processor.rotateRight().rotateRight();
            else if (rotation == 270)
                processor = processor.rotateLeft();
            else
                throw new UnsupportedOperationException("Unknown rotation: " + rotation);
        }
        return processor;
    }

    public void uploadSliceToCanvas() {
        if (image != null) {
            ImageProcessor processor = generateSlice(stackSlider.getValue() - 1,
                    channelSlider.getValue() - 1,
                    frameSlider.getValue() - 1,
                    true);
            if (processor == null) {
                canvas.setImage(null);
                return;
            }
            canvas.setImage(processor.getBufferedImage());
        } else {
            canvas.setImage(null);
        }
    }

    public ImageViewerPanelCanvas getCanvas() {
        return canvas;
    }

    public void fitImageToScreen() {
        if (image != null) {
            double zoomx = scrollPane.getViewport().getWidth() / (1.0 * image.getWidth());
            double zoomy = scrollPane.getViewport().getHeight() / (1.0 * image.getHeight());
            canvas.setZoom(Math.min(zoomx, zoomy));
            canvas.setContentXY(0, 0);
            updateZoomStatus();
        }
    }

    public ImageProcessor getSlice() {
        return slice;
    }

    public ImageStatistics getStatistics() {
        return statistics;
    }

}
