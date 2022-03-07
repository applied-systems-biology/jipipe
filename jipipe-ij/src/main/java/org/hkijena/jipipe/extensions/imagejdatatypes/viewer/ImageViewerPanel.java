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
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import ij.util.Tools;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.AVICompression;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.HyperstackDimension;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageSliceIndex;
import org.hkijena.jipipe.extensions.imagejdatatypes.viewer.plugins.*;
import org.hkijena.jipipe.extensions.imagejdatatypes.viewer.plugins.maskdrawer.MeasurementDrawerPlugin;
import org.hkijena.jipipe.extensions.imagejdatatypes.viewer.plugins.maskdrawer.MeasurementPlugin;
import org.hkijena.jipipe.extensions.settings.FileChooserSettings;
import org.hkijena.jipipe.extensions.settings.ImageViewerUISettings;
import org.hkijena.jipipe.ui.components.FormPanel;
import org.hkijena.jipipe.ui.components.PathEditor;
import org.hkijena.jipipe.ui.components.tabs.DocumentTabPane;
import org.hkijena.jipipe.ui.running.JIPipeRunExecuterUI;
import org.hkijena.jipipe.utils.*;
import org.hkijena.jipipe.utils.ui.CopyImageToClipboard;

import javax.imageio.ImageIO;
import javax.swing.Timer;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.*;

public class ImageViewerPanel extends JPanel {
    private static final Set<ImageViewerPanel> OPEN_PANELS = new HashSet<>();
    private static ImageViewerPanel ACTIVE_PANEL = null;
    private final JButton zoomStatusButton = new JButton();
    private final ImageViewerUISettings settings;
    private ImagePlus image;
    private ImageProcessor currentSlice;
    private ImageViewerPanelCanvas canvas;
    private Map<ImageSliceIndex, ImageStatistics> statisticsMap = new HashMap<>();
    private final JLabel stackSliderLabel = new JLabel("Slice (Z)");
    private final JLabel channelSliderLabel = new JLabel("Channel (C)");
    private final JLabel frameSliderLabel = new JLabel("Frame (T)");
    private final JScrollBar stackSlider = new JScrollBar(Adjustable.HORIZONTAL, 1, 1, 1, 100);
    private final JScrollBar channelSlider = new JScrollBar(Adjustable.HORIZONTAL, 1, 1, 1, 100);
    private final JScrollBar frameSlider = new JScrollBar(Adjustable.HORIZONTAL, 1, 1, 1, 100);
    private final JToggleButton animationStackToggle = new JToggleButton(UIUtils.getIconFromResources("actions/player_start.png"));
    private final JToggleButton animationChannelToggle = new JToggleButton(UIUtils.getIconFromResources("actions/player_start.png"));
    private final JToggleButton animationFrameToggle = new JToggleButton(UIUtils.getIconFromResources("actions/player_start.png"));
    private FormPanel bottomPanel;
    private long lastTimeZoomed;
    private final JLabel imageInfoLabel = new JLabel();
    private JScrollPane scrollPane;
    private final JSpinner animationSpeedControl = new JSpinner(new SpinnerNumberModel(250, 5, 10000, 1));
    private int rotation = 0;
    private JMenuItem exportAllSlicesItem;
    private JMenuItem exportMovieItem;
    private final JToolBar toolBar = new JToolBar();
    private List<ImageViewerPanelPlugin> plugins = new ArrayList<>();
    private JButton rotateLeftButton;
    private final Timer animationTimer = new Timer(250, e -> animateNextSlice());
    private JButton rotateRightButton;
    private final JToggleButton enableSideBarButton = new JToggleButton();
    private Component currentContentPanel;
    private final DocumentTabPane tabPane = new DocumentTabPane();
    private final Map<String, FormPanel> formPanels = new HashMap<>();
    private boolean isUpdatingSliders = false;
    public ImageViewerPanel() {
        if (JIPipe.getInstance() != null) {
            settings = ImageViewerUISettings.getInstance();
        } else {
            settings = null;
        }
        initialize();
        updateZoomStatus();
    }

    public static ImageViewerPanel getActiveViewerPanel() {
        return ACTIVE_PANEL;
    }

    public static Set<ImageViewerPanel> getOpenViewerPanels() {
        return OPEN_PANELS;
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
        List<ImageViewerPanelPlugin> pluginList = new ArrayList<>();
        pluginList.add(new CalibrationPlugin(dataDisplay));
        pluginList.add(new PixelInfoPlugin(dataDisplay));
        pluginList.add(new LUTManagerPlugin(dataDisplay));
        pluginList.add(new ROIManagerPlugin(dataDisplay));
        pluginList.add(new AnimationSpeedPlugin(dataDisplay));
        pluginList.add(new MeasurementDrawerPlugin(dataDisplay));
        pluginList.add(new MeasurementPlugin(dataDisplay));
        dataDisplay.setPlugins(pluginList);
        dataDisplay.setImage(image);
        ImageViewerWindow window = new ImageViewerWindow(dataDisplay);
        window.setTitle(title);
        window.setVisible(true);
        return dataDisplay;
    }

    public ImageViewerUISettings getSettings() {
        return settings;
    }

    public <T extends ImageViewerPanelPlugin> T getPlugin(Class<T> klass) {
        for (ImageViewerPanelPlugin plugin : plugins) {
            if (klass.isAssignableFrom(plugin.getClass()))
                return (T) plugin;
        }
        return null;
    }

    public List<ImageViewerPanelPlugin> getPlugins() {
        return plugins;
    }

    public void setPlugins(List<ImageViewerPanelPlugin> plugins) {
        this.plugins = plugins;
    }

    public <T extends ImageViewerPanelPlugin> T findPlugin(Class<T> klass) {
        for (ImageViewerPanelPlugin plugin : plugins) {
            if (klass.isAssignableFrom(plugin.getClass())) {
                return (T) plugin;
            }
        }
        return null;
    }

    public void setRotationEnabled(boolean enabled) {
        rotateLeftButton.setVisible(enabled);
        rotateRightButton.setVisible(enabled);
        if (!enabled) {
            rotation = 0;
            refreshImageInfo();
            refreshSlice();
        }
    }

    public void setAsActiveViewerPanel() {
        ACTIVE_PANEL = this;
    }

    public void addToOpenPanels() {
        OPEN_PANELS.add(this);
    }

    public void dispose() {
        if (ACTIVE_PANEL == this) {
            ACTIVE_PANEL = null;
        }
        OPEN_PANELS.remove(this);
        setImage(null);
    }

    private void initialize() {

        // Load default animation speed
        if (settings != null) {
            animationSpeedControl.getModel().setValue(settings.getDefaultAnimationSpeed());
        }

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

        bottomPanel = new FormPanel(null, FormPanel.NONE);
        add(bottomPanel, BorderLayout.SOUTH);

        // Register slider events
        stackSlider.addAdjustmentListener(e -> {
            if (!isUpdatingSliders)
                refreshSlice();
        });
        channelSlider.addAdjustmentListener(e -> {
            if (!isUpdatingSliders)
                refreshSlice();
        });
        frameSlider.addAdjustmentListener(e -> {
            if (!isUpdatingSliders)
                refreshSlice();
        });

        initializeAnimationControls();
        updateSideBar();
    }

    private void initializeAnimationControls() {
        animationTimer.setRepeats(true);
        animationSpeedControl.addChangeListener(e -> {
            int delay = ((SpinnerNumberModel) animationSpeedControl.getModel()).getNumber().intValue();
            if (settings != null) {
                settings.setDefaultAnimationSpeed(delay);
            }
            stopAnimations();
            animationTimer.setDelay(delay);
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

        JMenuItem saveRawImageItem = new JMenuItem("Export raw image to *.tif", UIUtils.getIconFromResources("actions/save.png"));
        saveRawImageItem.addActionListener(e -> saveRawImage());
        exportMenu.add(saveRawImageItem);

        JMenuItem exportCurrentSliceItem = new JMenuItem("Export snapshot of current slice", UIUtils.getIconFromResources("actions/viewimage.png"));
        exportCurrentSliceItem.addActionListener(e -> exportCurrentSliceToPNG());
        exportMenu.add(exportCurrentSliceItem);

        exportAllSlicesItem = new JMenuItem("Export snapshot of all slices", UIUtils.getIconFromResources("actions/qlipper.png"));
        exportAllSlicesItem.addActionListener(e -> exportAllSlicesToPNG());
        exportMenu.add(exportAllSlicesItem);

        exportMovieItem = new JMenuItem("Export movie", UIUtils.getIconFromResources("actions/filmgrain.png"));
        exportMovieItem.addActionListener(e -> exportVideo());
        exportMenu.add(exportMovieItem);

        exportMenu.addSeparator();

        JMenuItem copyCurrentSliceItem = new JMenuItem("Copy snapshot of current slice", UIUtils.getIconFromResources("actions/edit-copy.png"));
        copyCurrentSliceItem.addActionListener(e -> copyCurrentSliceToClipboard());
        exportMenu.add(copyCurrentSliceItem);

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

        toolBar.addSeparator();

        enableSideBarButton.setIcon(UIUtils.getIconFromResources("actions/sidebar.png"));
        enableSideBarButton.setToolTipText("Show side bar with additional tools");
        if (settings != null) {
            enableSideBarButton.setSelected(settings.isShowSideBar());
        } else {
            enableSideBarButton.setSelected(true);
        }
        enableSideBarButton.addActionListener(e -> {
            if (settings != null) {
                settings.setShowSideBar(enableSideBarButton.isSelected());
            }
            updateSideBar();
        });
        toolBar.add(enableSideBarButton);

        add(toolBar, BorderLayout.NORTH);
    }

    private void saveRawImage() {
        Path path = FileChooserSettings.saveFile(this, FileChooserSettings.LastDirectoryKey.Data, "Save as *.tif", UIUtils.EXTENSION_FILTER_TIFF);
        if (path != null) {
            JIPipeRunExecuterUI.runInDialog(this, new ImageViewerPanelRawImageExporterRun(getImage(), path));
        }
    }

    private void updateSideBar() {
        if (currentContentPanel != null) {
            remove(currentContentPanel);
        }
        if (enableSideBarButton.isSelected()) {
            JSplitPane splitPane = new AutoResizeSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                    scrollPane,
                    tabPane, AutoResizeSplitPane.RATIO_3_TO_1);
            add(splitPane, BorderLayout.CENTER);
            currentContentPanel = splitPane;
        } else {
            add(scrollPane, BorderLayout.CENTER);
            currentContentPanel = scrollPane;
        }
        revalidate();
        repaint();
    }

    public void exportCurrentSliceToPNG() {
        if (getCanvas().getImage() == null) {
            JOptionPane.showMessageDialog(this, "No image loaded.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        Path targetFile = FileChooserSettings.saveFile(this, FileChooserSettings.LastDirectoryKey.Data, "Export current slice", UIUtils.EXTENSION_FILTER_PNG, UIUtils.EXTENSION_FILTER_JPEG, UIUtils.EXTENSION_FILTER_BMP);
        if (targetFile != null) {
            String format = "PNG";
            if (UIUtils.EXTENSION_FILTER_BMP.accept(targetFile.toFile()))
                format = "BMP";
            else if (UIUtils.EXTENSION_FILTER_JPEG.accept(targetFile.toFile()))
                format = "JPEG";
            try {
                BufferedImage image = ImageJUtils.copyBufferedImage(getCanvas().getImage());
                for (ImageViewerPanelPlugin plugin : getPlugins()) {
                    plugin.postprocessDrawForExport(image, getCurrentSliceIndex());
                }
                ImageIO.write(image, format, targetFile.toFile());
            } catch (IOException e) {
                IJ.handleException(e);
            }
        }
    }

    public void copyCurrentSliceToClipboard() {
        if (getCanvas().getImage() == null) {
            JOptionPane.showMessageDialog(this, "No image loaded.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        BufferedImage image = ImageJUtils.copyBufferedImageToARGB(getCanvas().getImage());
        for (ImageViewerPanelPlugin plugin : getPlugins()) {
            plugin.postprocessDrawForExport(image, getCurrentSliceIndex());
        }
        CopyImageToClipboard copyImageToClipboard = new CopyImageToClipboard();
        copyImageToClipboard.copyImage(image);
    }

    public void exportAllSlicesToPNG() {
        FormPanel formPanel = new FormPanel(null, FormPanel.NONE);
        PathEditor exportPathEditor = new PathEditor(PathIOMode.Open, PathType.DirectoriesOnly);
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
        Path path = FileChooserSettings.saveFile(this, FileChooserSettings.LastDirectoryKey.Data, "Export video", UIUtils.EXTENSION_FILTER_AVI);
        if (path == null) {
            return;
        }

        FormPanel formPanel = new FormPanel(null, FormPanel.NONE);
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
            ImageViewerVideoExporterRun run = new ImageViewerVideoExporterRun(
                    this,
                    path,
                    getCurrentSliceIndex(),
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
            ImagePlus duplicate = ImageJUtils.duplicate(image);
            duplicate.setTitle(title);
            duplicate.show();
        }
    }

    private void refreshSliders() {
        try {
            isUpdatingSliders = true;
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
        } finally {
            isUpdatingSliders = false;
        }
    }

    public ImagePlus getImage() {
        return image;
    }

    public void setImage(ImagePlus image) {
        this.image = image;
        this.currentSlice = null;
        this.statisticsMap.clear();
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
        Map<String, Integer> scrollValues = new HashMap<>();
        for (Map.Entry<String, FormPanel> entry : formPanels.entrySet()) {
            scrollValues.put(entry.getKey(), entry.getValue().getScrollPane().getVerticalScrollBar().getValue());
            entry.getValue().clear();
        }
        for (ImageViewerPanelPlugin plugin : plugins) {
            FormPanel formPanel = formPanels.getOrDefault(plugin.getCategory(), null);
            if (formPanel == null) {
                formPanel = new FormPanel(null, FormPanel.WITH_SCROLLING);
                formPanels.put(plugin.getCategory(), formPanel);
                FormPanel finalFormPanel = formPanel;
                tabPane.registerSingletonTab(plugin.getCategory(),
                        plugin.getCategory(),
                        plugin.getCategoryIcon(),
                        () -> finalFormPanel,
                        DocumentTabPane.CloseMode.withoutCloseButton,
                        DocumentTabPane.SingletonTabMode.Present);
            }
            plugin.createPalettePanel(formPanel);
        }
        for (Map.Entry<String, FormPanel> entry : formPanels.entrySet()) {
            if (!entry.getValue().isHasVerticalGlue()) {
                entry.getValue().addVerticalGlue();
            }
            SwingUtilities.invokeLater(() -> {
                entry.getValue().getScrollPane().getVerticalScrollBar().setValue(scrollValues.getOrDefault(entry.getKey(), 0));
            });
        }
    }

    public JSpinner getAnimationSpeedControl() {
        return animationSpeedControl;
    }

    /**
     * Returns the currently viewed slice position
     *
     * @return the slice position. Zero-based indices
     */
    public ImageSliceIndex getCurrentSliceIndex() {
        return new ImageSliceIndex(channelSlider.getValue() - 1, stackSlider.getValue() - 1, frameSlider.getValue() - 1);
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
            this.currentSlice = image.getProcessor();
            for (ImageViewerPanelPlugin plugin : plugins) {
//                System.out.println(plugin + ": " + image.getDisplayRangeMin() + ", " + image.getDisplayRangeMax());
                plugin.onSliceChanged(true);
//                System.out.println(plugin + "(A): " + image.getDisplayRangeMin() + ", " + image.getDisplayRangeMax());
            }
            uploadSliceToCanvas();
        }
    }

    public ImageProcessor generateSlice(int c, int z, int t, boolean withRotation, boolean withPostprocessing) {
        image.setPosition(c + 1, z + 1, t + 1);
        for (ImageViewerPanelPlugin plugin : plugins) {
            plugin.beforeDraw(c, z, t);
        }
//        System.out.println(Arrays.stream(image.getLuts()).map(Object::toString).collect(Collectors.joining(" ")));
        ImageProcessor processor = image.getProcessor().duplicate();
        for (ImageViewerPanelPlugin plugin : plugins) {
            processor = plugin.draw(c, z, t, processor);
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
        if (withPostprocessing) {
            BufferedImage image = processor.getBufferedImage();
            for (ImageViewerPanelPlugin plugin : getPlugins()) {
                plugin.postprocessDrawForExport(image, new ImageSliceIndex(c, z, t));
            }
            processor = new ColorProcessor(image);
        }
        return processor;
    }

    public void uploadSliceToCanvas() {
        if (image != null) {
            ImageProcessor processor = generateSlice(channelSlider.getValue() - 1, stackSlider.getValue() - 1,
                    frameSlider.getValue() - 1,
                    true, false);
            if (processor == null) {
                canvas.setImage(null);
                return;
            }
//            System.out.println("tg " + processor.getMin() + ", " + processor.getMax());
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

    public ImageProcessor getCurrentSlice() {
        return currentSlice;
    }

    public ImageStatistics getCurrentSliceStats() {
        if(getImage() != null && getCurrentSliceIndex() != null) {
            return getSliceStats(getCurrentSliceIndex());
        }
        else {
            return null;
        }
    }

    public ImageStatistics getSliceStats(ImageSliceIndex sliceIndex) {
        if(getImage() != null) {
            ImageStatistics stats = statisticsMap.getOrDefault(sliceIndex, null);
            if(stats == null) {
                ImageProcessor processor = ImageJUtils.getSliceZero(image, sliceIndex);
                stats = processor.getStats();
                statisticsMap.put(sliceIndex, stats);
            }
            return stats;
        }
        return null;
    }

}
