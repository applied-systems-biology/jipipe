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

import com.google.common.primitives.Ints;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.ImageWindow;
import ij.gui.Roi;
import ij.measure.Calibration;
import ij.plugin.frame.RoiManager;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import ij.util.Tools;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.AVICompression;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.HyperstackDimension;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.SliceIndex;
import org.hkijena.jipipe.extensions.settings.FileChooserSettings;
import org.hkijena.jipipe.ui.components.ColorIcon;
import org.hkijena.jipipe.ui.components.DocumentChangeListener;
import org.hkijena.jipipe.ui.components.FormPanel;
import org.hkijena.jipipe.ui.components.PathEditor;
import org.hkijena.jipipe.ui.running.JIPipeRunExecuterUI;
import org.hkijena.jipipe.ui.theme.JIPipeUITheme;
import org.hkijena.jipipe.utils.BusyCursor;
import org.hkijena.jipipe.utils.ImageJCalibrationMode;
import org.hkijena.jipipe.utils.ImageJUtils;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.Adjustable;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class ImageViewerPanel extends JPanel {
    private final JButton zoomStatusButton = new JButton();
    private ImagePlus image;
    private ImageProcessor slice;
    private ImageStatistics statistics;
    private ImageViewerPanelCanvas canvas = new ImageViewerPanelCanvas();
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
    private ImageViewerPanelDisplayRangeControl displayRangeCalibrationControl;
    private JComboBox<ImageJCalibrationMode> calibrationModes;
    private List<ImageViewerLUTEditor> lutEditors = new ArrayList<>();
    private JToggleButton autoCalibrateButton = new JToggleButton("Keep auto-calibrating", UIUtils.getIconFromResources("actions/view-refresh.png"));
    private int rotation = 0;
    private ROIListData rois = new ROIListData();
    private JList<Roi> roiJList = new JList<>();
    private JLabel roiInfoLabel = new JLabel();
    private boolean roiSeeThroughZ = false;
    private boolean roiSeeThroughC = false;
    private boolean roiSeeThroughT = false;
    private boolean roiDrawOutline = true;
    private boolean roiFillOutline = false;
    private boolean roiDrawLabels = false;
    private boolean roiFilterList = false;
    private JMenuItem exportAllSlicesItem;
    private JMenuItem exportMovieItem;

    public ImageViewerPanel() {
        initialize();
        updateZoomStatus();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        scrollPane = new JScrollPane(canvas);
        canvas.setScrollPane(scrollPane);

        initializeCalibrationControls();

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

        // Setup ROI
        roiJList.setCellRenderer(new RoiListCellRenderer());
        roiJList.addListSelectionListener(e -> uploadSliceToCanvas());

        // Register slider events
        stackSlider.addAdjustmentListener(e -> refreshSlice());
        channelSlider.addAdjustmentListener(e -> refreshSlice());
        frameSlider.addAdjustmentListener(e -> refreshSlice());

        initializeAnimationControls();
    }

    public ImageJCalibrationMode getSelectedCalibration() {
        return (ImageJCalibrationMode) calibrationModes.getSelectedItem();
    }

    public void setSelectedCalibration(ImageJCalibrationMode mode) {
        calibrationModes.setSelectedItem(mode);
    }

    private void initializeCalibrationControls() {
        calibrationModes = new JComboBox<>();
        calibrationModes.setModel(new DefaultComboBoxModel<>(ImageJCalibrationMode.values()));
        calibrationModes.setSelectedItem(ImageJCalibrationMode.AutomaticImageJ);
        displayRangeCalibrationControl = new ImageViewerPanelDisplayRangeControl(this);
        calibrationModes.addActionListener(e -> {
            displayRangeCalibrationControl.applyCalibration(true);
        });
        autoCalibrateButton.addActionListener(e -> {
            if (autoCalibrateButton.isSelected()) {
                if (calibrationModes.getSelectedItem() != ImageJCalibrationMode.AutomaticImageJ) {
                    calibrationModes.setSelectedItem(ImageJCalibrationMode.AutomaticImageJ);
                } else {
                    displayRangeCalibrationControl.applyCalibration(true);
                }
            }
        });
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

    private void initializeToolbar() {
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);

        JButton openInImageJButton = new JButton("Open in ImageJ", UIUtils.getIconFromResources("apps/imagej.png"));
        openInImageJButton.addActionListener(e -> openInImageJ());

        addLeftToolbarButtons(toolBar);

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

        JButton rotateLeftButton = new JButton(UIUtils.getIconFromResources("actions/transform-rotate-left.png"));
        rotateLeftButton.setToolTipText("Rotate 90° to the left");
        rotateLeftButton.addActionListener(e -> rotateLeft());
        toolBar.add(rotateLeftButton);

        JButton rotateRightButton = new JButton(UIUtils.getIconFromResources("actions/transform-rotate.png"));
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
        if(getCanvas().getImage() == null) {
            JOptionPane.showMessageDialog(this, "No image loaded.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        Path targetFile = FileChooserSettings.saveFile(this, FileChooserSettings.KEY_DATA, "Export current slice", UIUtils.EXTENSION_FILTER_PNG, UIUtils.EXTENSION_FILTER_JPEG, UIUtils.EXTENSION_FILTER_BMP);
        if(targetFile != null) {
            String format = "PNG";
            if(UIUtils.EXTENSION_FILTER_BMP.accept(targetFile.toFile()))
                format = "BMP";
            else if(UIUtils.EXTENSION_FILTER_JPEG.accept(targetFile.toFile()))
                format = "JPEG";
            try {
                ImageIO.write(getCanvas().getImage(),format, targetFile.toFile());
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

        JComboBox<String> fileFormatEditor = new JComboBox<>(new String[] { "PNG", "JPEG", "BMP" } );
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
        if(response == JOptionPane.OK_OPTION) {
            FileChooserSettings.getInstance().setLastDataDirectory(exportPathEditor.getPath());
            Path targetPath = exportPathEditor.getPath();
            String format = fileFormatEditor.getSelectedItem() + "";
            String baseName = StringUtils.makeFilesystemCompatible(baseNameEditor.getText());
            ImageViewerStackExporterRun run = new ImageViewerStackExporterRun(this, targetPath, baseName, format);
            JIPipeRunExecuterUI.runInDialog(run);
        }
    }

    public void exportVideo() {
        FormPanel formPanel = new FormPanel(null, FormPanel.NONE);
        PathEditor exportPathEditor = new PathEditor(PathEditor.IOMode.Save, PathEditor.PathMode.FilesOnly);
        formPanel.addToForm(exportPathEditor, new JLabel("Exported file"), null);

        List<HyperstackDimension> availableDimensions = new ArrayList<>();
        if(image.getNFrames() > 1)
            availableDimensions.add(HyperstackDimension.Frame);
        if(image.getNSlices() > 1)
            availableDimensions.add(HyperstackDimension.Depth);
        if(image.getNChannels() > 1)
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
        if(response == JOptionPane.OK_OPTION) {
            FileChooserSettings.getInstance().setLastDataDirectory(exportPathEditor.getPath());
            ImageViewerVideoExporterRun run = new ImageViewerVideoExporterRun(
                    this,
                    exportPathEditor.getPath(),
                    getCurrentSlicePosition(),
                    (HyperstackDimension)dimensionEditor.getSelectedItem(),
                    animationTimer.getDelay(),
                    (AVICompression)compressionEditor.getSelectedItem(),
                    compressionQualityEditor.getValue());
            JIPipeRunExecuterUI.runInDialog(run);
        }
    }

    private void rotateLeft() {
        if(rotation == 0)
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

    protected void addLeftToolbarButtons(JToolBar toolBar) {
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
        this.lutEditors.clear();
        this.image = image;
        refreshSliders();
        refreshSlice();
        refreshImageInfo();
        refreshFormPanel();
        refreshMenus();
        for (ImageViewerLUTEditor lutEditor : lutEditors) {
            lutEditor.applyLUT();
        }
        displayRangeCalibrationControl.applyCalibration(true);
        updateROIJList();
        revalidate();
        repaint();
    }

    private void refreshMenus() {
        boolean hasMultipleSlices = image != null && image.getNDimensions() > 2;
        exportAllSlicesItem.setVisible(hasMultipleSlices);
        exportMovieItem.setVisible(hasMultipleSlices);
    }

    private void refreshFormPanel() {
        formPanel.clear();
        initializeCalibrationPanel();
        initializeLUTPanel();
        initializeROIPanel();
        if (image.getNChannels() > 1 || image.getNSlices() > 1 || image.getNFrames() > 1) {
            formPanel.addGroupHeader("Animation", UIUtils.getIconFromResources("actions/filmgrain.png"));
            formPanel.addToForm(animationSpeed, new JLabel("Speed (ms)"), null);
        }

        formPanel.addVerticalGlue();
    }

    private void initializeROIPanel() {
        FormPanel.GroupHeaderPanel headerPanel = formPanel.addGroupHeader("ROI", UIUtils.getIconFromResources("data-types/roi.png"));
        JButton importROIsButton = new JButton("Import", UIUtils.getIconFromResources("actions/document-import.png"));
        importROIsButton.setToolTipText("Imports ROIs from the ImageJ ROI manager");
        importROIsButton.addActionListener(e -> importROIs());
        headerPanel.addColumn(importROIsButton);

        JButton exportROIsButton = new JButton( UIUtils.getIconFromResources("actions/document-export.png"));
        exportROIsButton.setToolTipText("Exports ROIs to the ImageJ ROI manager");
        exportROIsButton.addActionListener(e -> exportROIs());
        headerPanel.addColumn(exportROIsButton);

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEtchedBorder());
        JToolBar listToolBar = new JToolBar();
        listToolBar.setFloatable(false);
        panel.add(listToolBar, BorderLayout.NORTH);

        listToolBar.add(roiInfoLabel);
        listToolBar.add(Box.createHorizontalGlue());

        {
            JToggleButton toggle = new JToggleButton(UIUtils.getIconFromResources("actions/eye.png"));
            toggle.setToolTipText("Show only visible ROI");
            toggle.setSelected(roiFilterList);
            toggle.addActionListener(e -> {roiFilterList = toggle.isSelected(); updateROIJList(); });
            listToolBar.add(toggle);
        }
        listToolBar.addSeparator();

        JButton selectAllButton = new JButton(UIUtils.getIconFromResources("actions/edit-select-all.png"));
        selectAllButton.setToolTipText("Select all");
        selectAllButton.addActionListener( e-> {
            roiJList.setSelectionInterval(0, roiJList.getModel().getSize() - 1);
        });
        listToolBar.add(selectAllButton);

        JButton deselectAllButton = new JButton(UIUtils.getIconFromResources("actions/edit-select-none.png"));
        deselectAllButton.setToolTipText("Clear selection");
        deselectAllButton.addActionListener(e -> {
            roiJList.clearSelection();
        });
        listToolBar.add(deselectAllButton);

        JButton invertSelectionButton = new JButton(UIUtils.getIconFromResources("actions/object-inverse.png"));
        invertSelectionButton.setToolTipText("Invert selection");
        invertSelectionButton.addActionListener(e -> {
            Set<Integer> selectedIndices = Arrays.stream(roiJList.getSelectedIndices()).boxed().collect(Collectors.toSet());
            roiJList.clearSelection();
            Set<Integer> newSelectedIndices = new HashSet<>();
            for (int i = 0; i < roiJList.getModel().getSize(); i++) {
                if(!selectedIndices.contains(i))
                    newSelectedIndices.add(i);
            }
            roiJList.setSelectedIndices(Ints.toArray(newSelectedIndices));
        });
        listToolBar.add(invertSelectionButton);

        listToolBar.addSeparator();

        JButton removeButton = new JButton(UIUtils.getIconFromResources("actions/delete.png"));
        removeButton.setToolTipText("Remove selected ROIs");
        removeButton.addActionListener(e -> removeSelectedROIs());
        listToolBar.add(removeButton);

        JScrollPane scrollPane = new JScrollPane(roiJList);
        panel.add(scrollPane, BorderLayout.CENTER);

        JToolBar viewToolBar = new JToolBar();
        viewToolBar.setFloatable(false);

        {
            JToggleButton toggle = new JToggleButton(UIUtils.getIconFromResources("actions/object-stroke.png"));
            toggle.setToolTipText("Draw outline");
            toggle.setSelected(roiDrawOutline);
            toggle.addActionListener(e -> {roiDrawOutline = toggle.isSelected(); uploadSliceToCanvas(); });
            viewToolBar.add(toggle);
        }
        {
            JToggleButton toggle = new JToggleButton(UIUtils.getIconFromResources("actions/object-fill.png"));
            toggle.setToolTipText("Fill outline");
            toggle.setSelected(roiFillOutline);
            toggle.addActionListener(e -> {roiFillOutline = toggle.isSelected(); uploadSliceToCanvas(); });
            viewToolBar.add(toggle);
        }
        {
            JToggleButton toggle = new JToggleButton(UIUtils.getIconFromResources("actions/edit-select-text.png"));
            toggle.setToolTipText("Draw labels");
            toggle.setSelected(roiDrawLabels);
            toggle.addActionListener(e -> {roiDrawLabels = toggle.isSelected(); uploadSliceToCanvas(); });
            viewToolBar.add(toggle);
        }

        viewToolBar.addSeparator();

        JButton editButton = new JButton(UIUtils.getIconFromResources("actions/edit.png"));
        JPopupMenu editMenu = new JPopupMenu();
        UIUtils.addReloadablePopupMenuToComponent(editButton, editMenu, () -> reloadEditRoiMenu(editMenu));
        viewToolBar.add(editButton);

        viewToolBar.add(Box.createHorizontalGlue());

        if(image.getNSlices() > 1) {
            JToggleButton toggle = new JToggleButton(UIUtils.getIconFromResources("actions/layer-flatten-z.png"));
            toggle.setToolTipText("Show all ROIs regardless of Z axis.");
            toggle.setSelected(roiSeeThroughZ);
            toggle.addActionListener(e -> {roiSeeThroughZ = toggle.isSelected(); uploadSliceToCanvas(); });
            viewToolBar.add(toggle);
        }
        if(image.getNFrames() > 1) {
            JToggleButton toggle = new JToggleButton(UIUtils.getIconFromResources("actions/layer-flatten-t.png"));
            toggle.setToolTipText("Show all ROIs regardless of time axis.");
            toggle.setSelected(roiSeeThroughT);
            toggle.addActionListener(e -> {roiSeeThroughT = toggle.isSelected(); uploadSliceToCanvas(); });
            viewToolBar.add(toggle);
        }
        if(image.getNChannels() > 1) {
            JToggleButton toggle = new JToggleButton(UIUtils.getIconFromResources("actions/layer-flatten-c.png"));
            toggle.setToolTipText("Show all ROIs regardless of channel axis.");
            toggle.setSelected(roiSeeThroughC);
            toggle.addActionListener(e -> {roiSeeThroughC = toggle.isSelected(); uploadSliceToCanvas(); });
            viewToolBar.add(toggle);
        }

        panel.add(viewToolBar, BorderLayout.SOUTH);

        formPanel.addWideToForm(panel, null);
    }

    public SliceIndex getCurrentSlicePosition() {
        return new SliceIndex(stackSlider.getValue() - 1, channelSlider.getValue() - 1, frameSlider.getValue() - 1);
    }

    private void reloadEditRoiMenu(JPopupMenu menu) {
        List<Roi> selectedRois = roiJList.getSelectedValuesList();
        menu.removeAll();
        if(selectedRois.isEmpty()) {
            JMenuItem noSelection = new JMenuItem("No ROI selected");
            noSelection.setEnabled(false);
            menu.add(noSelection);
            return;
        }

        Color currentStrokeColor = selectedRois.stream().map(Roi::getStrokeColor).filter(Objects::nonNull).findAny().orElse(Color.YELLOW);
        JMenuItem setLineColorItem = new JMenuItem("Set line color ...", new ColorIcon(16,16, currentStrokeColor));
        setLineColorItem.addActionListener(e -> {
            Color value = JColorChooser.showDialog(this, "Set line color", currentStrokeColor);
            if(value != null) {
                for (Roi roi : selectedRois) {
                    roi.setStrokeColor(value);
                }
                roiJList.repaint();
                uploadSliceToCanvas();
            }
        });
        menu.add(setLineColorItem);

        Color currentFillColor = selectedRois.stream().map(Roi::getFillColor).filter(Objects::nonNull).findAny().orElse(Color.RED);
        JMenuItem setFillColorItem = new JMenuItem("Set fill color ...", new ColorIcon(16,16, currentFillColor));
        setFillColorItem.addActionListener(e -> {
            Color value = JColorChooser.showDialog(this, "Set fill color", currentFillColor);
            if(value != null) {
                for (Roi roi : selectedRois) {
                    roi.setFillColor(value);
                }
                roiJList.repaint();
                uploadSliceToCanvas();
            }
        });
        menu.add(setFillColorItem);

        int currentStrokeThickness = Math.max(1, selectedRois.stream().map(Roi::getStrokeWidth).min(Comparator.naturalOrder()).get().intValue());
        JMenuItem setStrokeThicknessItem = new JMenuItem("Set line width ...", UIUtils.getIconFromResources("actions/transform-affect-stroke.png"));
        setStrokeThicknessItem.addActionListener(e -> {
            Integer value = UIUtils.getIntegerByDialog(this, "Set line width", "Please put the line width here:", currentStrokeThickness, 1, Integer.MAX_VALUE);
            if(value != null) {
                for (Roi roi : selectedRois) {
                    roi.setStrokeWidth(value);
                }
                roiJList.repaint();
                uploadSliceToCanvas();
            }
        });
        menu.add(setStrokeThicknessItem);

        String currentName =  selectedRois.stream().map(Roi::getName).filter(Objects::nonNull).findAny().orElse("");
        JMenuItem setNameItem = new JMenuItem("Set name ...", UIUtils.getIconFromResources("actions/tag.png"));
        setNameItem.addActionListener(e -> {
            String value = JOptionPane.showInputDialog(this, "Please set the name of the ROIs:", currentName);
            if(value != null) {
                for (Roi roi : selectedRois) {
                    roi.setName(value);
                }
                roiJList.repaint();
                uploadSliceToCanvas();
            }
        });
        menu.add(setNameItem);

        menu.addSeparator();

        int currentZPosition = Math.max(0, selectedRois.stream().map(Roi::getZPosition).min(Comparator.naturalOrder()).get());
        JMenuItem setZPositionItem = new JMenuItem("Set Z position ...", UIUtils.getIconFromResources("actions/mark-location.png"));
        setZPositionItem.addActionListener(e -> {
            Integer value = UIUtils.getIntegerByDialog(this, "Set Z position", "The first index is 1. Set it to zero to make the ROI appear on all Z planes.", currentZPosition, 0, Integer.MAX_VALUE);
            if(value != null) {
                for (Roi roi : selectedRois) {
                    roi.setPosition(roi.getCPosition(), value, roi.getTPosition());
                }
                roiJList.repaint();
                uploadSliceToCanvas();
            }
        });
        menu.add(setZPositionItem);

        int currentCPosition = Math.max(0, selectedRois.stream().map(Roi::getZPosition).min(Comparator.naturalOrder()).get());
        JMenuItem setCPositionItem = new JMenuItem("Set C position ...", UIUtils.getIconFromResources("actions/mark-location.png"));
        setCPositionItem.addActionListener(e -> {
            Integer value = UIUtils.getIntegerByDialog(this, "Set C position", "The first index is 1. Set it to zero to make the ROI appear on all channel planes.", currentCPosition, 0, Integer.MAX_VALUE);
            if(value != null) {
                for (Roi roi : selectedRois) {
                    roi.setPosition(value, roi.getZPosition(), roi.getTPosition());
                }
                roiJList.repaint();
                uploadSliceToCanvas();
            }
        });
        menu.add(setCPositionItem);

        int currentTPosition = Math.max(0, selectedRois.stream().map(Roi::getZPosition).min(Comparator.naturalOrder()).get());
        JMenuItem setTPositionItem = new JMenuItem("Set T position ...", UIUtils.getIconFromResources("actions/mark-location.png"));
        setTPositionItem.addActionListener(e -> {
            Integer value = UIUtils.getIntegerByDialog(this, "Set T position", "The first index is 1. Set it to zero to make the ROI appear on all frame planes.", currentTPosition, 0, Integer.MAX_VALUE);
            if(value != null) {
                for (Roi roi : selectedRois) {
                    roi.setPosition(roi.getCPosition(), roi.getZPosition(), value);
                }
                roiJList.repaint();
                uploadSliceToCanvas();
            }
        });
        menu.add(setTPositionItem);
    }

    public void removeSelectedROIs() {
        rois.removeAll(roiJList.getSelectedValuesList());
        updateROIJList();
    }

    public void exportROIs() {
        rois.addToRoiManager(RoiManager.getRoiManager());
    }

    public void importROIs() {
        for (Roi roi : RoiManager.getRoiManager().getRoisAsArray()) {
            rois.add((Roi) roi.clone());
        }
        updateROIJList();
    }

    private void updateROIJList() {
        DefaultListModel<Roi> model = new DefaultListModel<>();
        int[] selectedIndices = roiJList.getSelectedIndices();
        SliceIndex currentIndex = new SliceIndex(stackSlider.getValue() - 1, channelSlider.getValue() - 1, frameSlider.getValue() - 1);
        for (Roi roi : rois) {
            if(roiFilterList && !ROIListData.isVisibleIn(roi, currentIndex, roiSeeThroughZ, roiSeeThroughC, roiSeeThroughT))
                continue;
            model.addElement(roi);
        }
        roiJList.setModel(model);
        roiJList.setSelectedIndices(selectedIndices);
        roiInfoLabel.setText(rois.size() + " ROI");
        uploadSliceToCanvas();
    }

    private void initializeLUTPanel() {
        if (image.getType() == ImagePlus.COLOR_256 || image.getType() == ImagePlus.COLOR_RGB) {
            FormPanel.GroupHeaderPanel headerPanel = formPanel.addGroupHeader("LUT", UIUtils.getIconFromResources("actions/color-gradient.png"));
            JButton toRGBButton = new JButton("Split channels", UIUtils.getIconFromResources("actions/channelmixer.png"));
            headerPanel.add(toRGBButton);
            toRGBButton.addActionListener(e -> splitChannels());
        } else {
            while (lutEditors.size() < image.getNChannels()) {
                ImageViewerLUTEditor editor = new ImageViewerLUTEditor(this, lutEditors.size());
                editor.loadLUTFromImage();
                lutEditors.add(editor);
            }
            FormPanel.GroupHeaderPanel headerPanel = formPanel.addGroupHeader("LUT", UIUtils.getIconFromResources("actions/color-gradient.png"));
            if (image.getNChannels() == 3) {
                JButton toRGBButton = new JButton("Convert to RGB", UIUtils.getIconFromResources("actions/colors-rgb.png"));
                headerPanel.add(toRGBButton);
                toRGBButton.addActionListener(e -> convertImageToRGB());
            }
            for (int channel = 0; channel < image.getNChannels(); channel++) {
                ImageViewerLUTEditor editor = lutEditors.get(channel);
                JTextField channelNameEditor = new JTextField(editor.getChannelName());
                channelNameEditor.setOpaque(false);
                channelNameEditor.setBorder(null);
                channelNameEditor.getDocument().addDocumentListener(new DocumentChangeListener() {
                    @Override
                    public void changed(DocumentEvent documentEvent) {
                        editor.setChannelName(channelNameEditor.getText());
                    }
                });
                formPanel.addToForm(editor, channelNameEditor, null);
            }
        }
    }

    private void splitChannels() {
        if (image != null) {
            try (BusyCursor cursor = new BusyCursor(this)) {
                setImage(ImageJUtils.rgbToChannels(image));
            }
        }
    }

    private void convertImageToRGB() {
        if (image != null) {
            try (BusyCursor cursor = new BusyCursor(this)) {
                setImage(ImageJUtils.channelsToRGB(image));
            }
        }
    }

    private void initializeCalibrationPanel() {
        FormPanel.GroupHeaderPanel headerPanel = formPanel.addGroupHeader("Display range", UIUtils.getIconFromResources("actions/contrast.png"));
        headerPanel.addColumn(autoCalibrateButton);
        formPanel.addToForm(calibrationModes, new JLabel("Calibration type"), null);
        formPanel.addWideToForm(displayRangeCalibrationControl, null);
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
        if(rotation != 0) {
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
            image.setPosition(channel, stack, frame);
            if (autoCalibrateButton.isSelected()) {
                displayRangeCalibrationControl.applyCalibration(false);
            }
            if(roiFilterList) {
                updateROIJList();
            }
            this.slice = image.getProcessor();
            this.statistics = image.getStatistics();
            uploadSliceToCanvas();
            displayRangeCalibrationControl.updateSliders();
        }
    }

    public ImageProcessor generateSlice(int z, int c, int t, boolean withRoi, boolean withRotation) {
        image.setPosition(c + 1, z + 1, t + 1);
        ImageProcessor processor = image.getProcessor();
        if(withRoi && !(processor instanceof ColorProcessor) && !rois.isEmpty()) {
            processor = new ColorProcessor(processor.getBufferedImage());
            rois.draw(processor, new SliceIndex(z, c, t),
                    roiSeeThroughZ,
                    roiSeeThroughC,
                    roiSeeThroughT,
                    roiDrawOutline,
                    roiFillOutline,
                    roiDrawLabels,
                    1,
                    Color.RED,
                    Color.YELLOW,
                    roiJList.getSelectedValuesList());
        }
        if(withRotation && rotation != 0) {
            if(rotation == 90)
                processor = processor.rotateRight();
            else if(rotation == 180)
                processor = processor.rotateRight().rotateRight();
            else if(rotation == 270)
                processor = processor.rotateLeft();
            else
                throw new UnsupportedOperationException("Unknown rotation: " + rotation);
        }
        return processor;
    }

    public void uploadSliceToCanvas() {
        ImageProcessor processor = generateSlice(stackSlider.getValue() - 1,
                channelSlider.getValue() - 1,
                frameSlider.getValue() - 1,
                true,
                true);
        canvas.setImage(processor.getBufferedImage());
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

    public void disableAutoCalibration() {
        autoCalibrateButton.setSelected(false);
    }

    public void importROIs(ROIListData rois) {
        for (Roi roi : rois) {
            this.rois.add((Roi) roi.clone());
        }
        updateROIJList();
        uploadSliceToCanvas();
    }

    public static void main(String[] args) {
        JIPipeUITheme.ModernLight.install();
        ImagePlus image = IJ.openImage("/data/Mitochondria/data/Mic13 SNAP Deconv.lif - WT_Hela_Mic13_SNAP_Series011_10_cmle_converted.tif");
//        ImagePlus image = IJ.openImage("/home/rgerst/dots.png");
        JFrame frame = new JFrame();
        ImageViewerPanel panel = new ImageViewerPanel();
        panel.setImage(image);
        frame.setContentPane(panel);
        frame.pack();
        frame.setSize(1280, 1024);
        frame.setVisible(true);
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
        dataDisplay.setImage(image);
        JFrame frame = new JFrame(title);
        frame.setIconImage(UIUtils.getIcon128FromResources("jipipe.png").getImage());
        frame.setContentPane(dataDisplay);
        frame.pack();
        frame.setSize(1024, 768);
        frame.setVisible(true);
        SwingUtilities.invokeLater(dataDisplay::fitImageToScreen);
        return dataDisplay;
    }

}
