/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.plugins.imageviewer.legacy;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.ImageCanvas;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import org.apache.commons.lang3.math.NumberUtils;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeWorkbench;
import org.hkijena.jipipe.api.run.JIPipeRunnableQueue;
import org.hkijena.jipipe.desktop.JIPipeDesktop;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbenchAccess;
import org.hkijena.jipipe.desktop.app.running.queue.JIPipeDesktopCompactRunnableQueueButton;
import org.hkijena.jipipe.desktop.app.running.JIPipeDesktopRunExecuteUI;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopFormPanel;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopPathEditorComponent;
import org.hkijena.jipipe.desktop.commons.components.ribbon.JIPipeDesktopCheckBoxRibbonAction;
import org.hkijena.jipipe.desktop.commons.components.ribbon.JIPipeDesktopRibbon;
import org.hkijena.jipipe.desktop.commons.components.ribbon.JIPipeDesktopSmallButtonRibbonAction;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.plugins.imagejdatatypes.display.OpenInImageJ3DViewerDataDisplayOperation;
import org.hkijena.jipipe.plugins.imagejdatatypes.display.OpenInImageJDataDisplayOperation;
import org.hkijena.jipipe.plugins.imagejdatatypes.display.OpenInNapariDataDisplayOperation;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.*;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.blending.ImageBlendLayer;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.dimensions.HyperstackDimension;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.dimensions.ImageSliceIndex;
import org.hkijena.jipipe.plugins.imageviewer.legacy.api.JIPipeDesktopLegacyImageViewerPlugin2D;
import org.hkijena.jipipe.plugins.imageviewer.legacy.runs.RawImage2DExporterRun;
import org.hkijena.jipipe.plugins.imageviewer.legacy.runs.Stack2DExporterRun;
import org.hkijena.jipipe.plugins.imageviewer.legacy.runs.Stack2DRendererRun;
import org.hkijena.jipipe.plugins.imageviewer.legacy.runs.Video2DExporterRun;
import org.hkijena.jipipe.plugins.imageviewer.settings.LegacyImageViewer2DUIApplicationSettings;
import org.hkijena.jipipe.plugins.imageviewer.utils.viewer2d.ImageViewer2DSliceStatistics;
import org.hkijena.jipipe.plugins.imageviewer.utils.viewer2d.ImageViewerPanelCanvas2D;
import org.hkijena.jipipe.plugins.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.plugins.settings.JIPipeFileChooserApplicationSettings;
import org.hkijena.jipipe.utils.*;
import org.hkijena.jipipe.utils.ui.CopyImageToClipboard;
import org.hkijena.jipipe.utils.ui.JIPipeDesktopDockPanel;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.List;

public class JIPipeDesktopLegacyImageViewerPanel2D extends JPanel implements JIPipeDesktopWorkbenchAccess {

    public static final String DOCK_PANEL_PREFIX = "LEGACY_IMAGE_VIEWER2D_";
    private final JIPipeDesktopLegacyImageViewer imageViewer;
    private final JIPipeDesktopSmallButtonRibbonAction zoomStatusButton = new JIPipeDesktopSmallButtonRibbonAction("100%", "The status of the zoom",
            UIUtils.getIconFromResources("actions/transform-move-vertical.png"));
    private final LegacyImageViewer2DUIApplicationSettings settings;
    private final JLabel stackSliderLabel = new JLabel("Slice (Z)");
    private final JLabel channelSliderLabel = new JLabel("Channel (C)");
    private final JLabel frameSliderLabel = new JLabel("Frame (T)");
    //    private final Adjustable stackSlider = new JScrollBar(Adjustable.HORIZONTAL, 1, 1, 1, 100);
    private final JSlider stackSlider = new JSlider(1, 100, 1);
    private final JSlider channelSlider = new JSlider(1, 100, 1);
    private final JSlider frameSlider = new JSlider(1, 100, 1);
    private final JToggleButton animationStackToggle = new JToggleButton(UIUtils.getIconFromResources("actions/play.png"));
    private final JToggleButton animationChannelToggle = new JToggleButton(UIUtils.getIconFromResources("actions/play.png"));
    private final JToggleButton animationFrameToggle = new JToggleButton(UIUtils.getIconFromResources("actions/play.png"));
    private final JSpinner animationFPSControl = new JSpinner(new SpinnerNumberModel(24, 0.01, 1000, 0.1));
    private final JIPipeDesktopWorkbench workbench;
    private final JIPipeDesktopCheckBoxRibbonAction exportDisplayedScaleToggle = new JIPipeDesktopCheckBoxRibbonAction("Export as displayed", "If enabled, " +
            "the snapshot is exported exactly as displayed (including the size)", true, cb -> {
    });
    private final Map<ImageSliceIndex, ImageViewer2DSliceStatistics> statisticsMap = new HashMap<>();
    private final JPanel viewerPanel = new JPanel(new BorderLayout());
    private final JIPipeRunnableQueue viewerRunnerQueue = new JIPipeRunnableQueue("Image Viewer 2D");
    private final List<CompositeLayer> orderedCompositeBlendLayers = new ArrayList<>();
    private final Map<Integer, CompositeLayer> compositeBlendLayers = new HashMap<>();
    private ImagePlusData image;
    private ImageCanvas zoomedDummyCanvas;
    private ImageCanvas exportDummyCanvas;
    private ImageProcessor currentSlice;
    private ImageViewerPanelCanvas2D canvas;
    private JIPipeDesktopFormPanel bottomPanel;
    private long lastTimeZoomed;
    private boolean isUpdatingSliders = false;
    private JScrollPane canvasScrollPane;
    private boolean composite;
    private JIPipeDesktopDockPanel currentDockPanel;

    /**
     * Initializes a new image viewer
     *
     * @param imageViewer the viewer
     */
    public JIPipeDesktopLegacyImageViewerPanel2D(JIPipeDesktopLegacyImageViewer imageViewer) {
        this.imageViewer = imageViewer;
        this.workbench = imageViewer.getDesktopWorkbench();
        if (JIPipe.getInstance() != null) {
            settings = LegacyImageViewer2DUIApplicationSettings.getInstance();
        } else {
            settings = new LegacyImageViewer2DUIApplicationSettings();
        }
        exportDisplayedScaleToggle.setState(settings.isExportAsDisplayed());
        initialize();
        updateZoomStatus();
    }

    private static void incrementSlider(JSlider slider) {
        int value = slider.getValue();
        int maximum = slider.getMaximum();
        int newIndex = ((value) % maximum) + 1;
        slider.setValue(newIndex);
    }

    private static void decrementSlider(JSlider slider) {
        int value = slider.getValue();
        int maximum = slider.getMaximum();
        int newIndex = value - 1;
        if (newIndex < 1)
            newIndex += maximum;
        slider.setValue(newIndex);
    }

    public JIPipeDesktopWorkbench getDesktopWorkbench() {
        return workbench;
    }

    public LegacyImageViewer2DUIApplicationSettings getSettings() {
        return settings;
    }

    public List<CompositeLayer> getOrderedCompositeBlendLayers() {
        return Collections.unmodifiableList(orderedCompositeBlendLayers);
    }

    public Map<Integer, CompositeLayer> getCompositeBlendLayers() {
        return Collections.unmodifiableMap(compositeBlendLayers);
    }    private final Timer animationTimer = new Timer(250, e -> animateNextSlice());

    public void dispose() {
        animationTimer.stop();
        try {
            setImage(null);
        } catch (Exception | Error e) {
            e.printStackTrace();
        }
        try {
            uploadSliceToCanvas();
        } catch (Exception | Error e) {
            e.printStackTrace();
        }
    }

    public JIPipeRunnableQueue getViewerRunnerQueue() {
        return viewerRunnerQueue;
    }

    private void initialize() {

        // Load default animation speed
        if (settings != null) {
            double fps = settings.getDefaultAnimationFPS();
            animationFPSControl.getModel().setValue(fps);
            animationTimer.setDelay(Math.max(1, (int) (1000.0 / fps)));
        }

        setLayout(new BorderLayout());

        canvas = new ImageViewerPanelCanvas2D(this);
        canvasScrollPane = new JScrollPane(canvas);
        canvas.setScrollPane(canvasScrollPane);
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
//        canvas.getEventBus().register(this);

        // Viewer panel, status bar
        viewerPanel.add(canvasScrollPane, BorderLayout.CENTER);

        // Bottom panel (sliders)
        bottomPanel = new JIPipeDesktopFormPanel(null, JIPipeDesktopFormPanel.NONE);
        add(bottomPanel, BorderLayout.SOUTH);

        // Register slider events
        stackSlider.addChangeListener(e -> {
            if (!isUpdatingSliders)
                refreshSlice();
        });
        channelSlider.addChangeListener(e -> {
            if (!isUpdatingSliders)
                refreshSlice();
        });
        frameSlider.addChangeListener(e -> {
            if (!isUpdatingSliders)
                refreshSlice();
        });

        // Export
        exportDisplayedScaleToggle.addActionListener(e -> {
            if (settings != null) {
                settings.setExportAsDisplayed(exportDisplayedScaleToggle.getState());
                if (!JIPipe.NO_SETTINGS_AUTOSAVE) {
                    JIPipe.getSettings().save();
                }
            }
        });

        initializeAnimationControls();
        add(viewerPanel, BorderLayout.CENTER);

        createZoomMenu(zoomStatusButton.getButton());
    }

    public boolean isComposite() {
        return composite;
    }

    public void setComposite(boolean composite) {
        this.composite = composite;
        if (composite) {
            channelSlider.setValue(1);
        }
        refreshSliders();
        uploadSliceToCanvas();
    }

    private void initializeAnimationControls() {
        animationTimer.setRepeats(true);
        animationFPSControl.addChangeListener(e -> {
            double fps = ((SpinnerNumberModel) animationFPSControl.getModel()).getNumber().doubleValue();
            if (settings != null) {
                settings.setDefaultAnimationFPS(fps);
                if (!JIPipe.NO_SETTINGS_AUTOSAVE) {
                    JIPipe.getSettings().save();
                }
            }
            stopAnimations();
            animationTimer.setDelay(Math.max(1, (int) (1000.0 / fps)));
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

    private void addSliderToForm(JSlider slider, JLabel label, JToggleButton animation, String name, String labelFormat) {

        // configure slider
        slider.setMajorTickSpacing(10);
        slider.setMinorTickSpacing(1);
        slider.setPaintTicks(false);

        // fix label glitch
        {
            String maxFormat = String.format(labelFormat, slider.getMaximum(), slider.getMaximum());
            int stringWidth = label.getFontMetrics(label.getFont()).stringWidth(maxFormat);
            int bufferedSw = (int) (stringWidth + stringWidth * 0.2);
            label.setMinimumSize(new Dimension(bufferedSw, 16));
            label.setPreferredSize(new Dimension(bufferedSw, 16));
        }


        animation.setToolTipText("Toggle animation");
        UIUtils.makeButtonFlat25x25(animation);
        JPanel descriptionPanel = new JPanel();
        descriptionPanel.setLayout(new BoxLayout(descriptionPanel, BoxLayout.X_AXIS));

        JButton editButton = new JButton(UIUtils.getIconFromResources("actions/go-jump.png"));
        editButton.setToolTipText("Jump to slice");
        UIUtils.makeButtonFlat25x25(editButton);
        editButton.addActionListener(e -> {
            String input = JOptionPane.showInputDialog(this,
                    "Please input a new value for " + name + " (" + slider.getMinimum() + "-" + slider.getMaximum() + ")",
                    slider.getValue());
            if (!StringUtils.isNullOrEmpty(input)) {
                Integer index = NumberUtils.createInteger(input);
                index = Math.min(slider.getMaximum(), Math.max(slider.getMinimum(), index));
                slider.setValue(index);
            }
        });
        descriptionPanel.add(editButton);
        descriptionPanel.add(animation);
        descriptionPanel.add(label);

        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.setOpaque(false);
        contentPanel.add(slider, BorderLayout.CENTER);

        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.X_AXIS));
        contentPanel.add(rightPanel, BorderLayout.EAST);

        JButton lastFrame = new JButton(UIUtils.getIconFromResources("actions/caret-left.png"));
        UIUtils.makeButtonFlat25x25(lastFrame);
        lastFrame.setToolTipText("Go one slice back");
        lastFrame.addActionListener(e -> {
            decrementSlider(slider);
        });
        rightPanel.add(lastFrame);

        JButton nextFrame = new JButton(UIUtils.getIconFromResources("actions/caret-right.png"));
        UIUtils.makeButtonFlat25x25(nextFrame);
        nextFrame.setToolTipText("Go one slice forward");
        nextFrame.addActionListener(e -> {
            incrementSlider(slider);
        });
        rightPanel.add(nextFrame);

        slider.addMouseWheelListener(e -> {
            if (e.getWheelRotation() < 0) {
                incrementSlider(slider);
            } else {
                decrementSlider(slider);
            }
        });

        bottomPanel.addToForm(contentPanel, descriptionPanel, null);
    }


//    public void setRotationEnabled(boolean enabled) {
//        rotateLeftButton.setVisible(enabled);
//        rotateRightButton.setVisible(enabled);
//        if (!enabled) {
//            rotation = 0;
//            refreshImageInfo();
//            refreshSlice();
//        }
//    }

    public void buildRibbon(JIPipeDesktopRibbon ribbon) {
        buildViewRibbon(ribbon);
        buildExportRibbon(ribbon);
        buildAnimationRibbon(ribbon);

        // Build plugin ribbons
        for (JIPipeDesktopLegacyImageViewerPlugin2D plugin : imageViewer.getPlugins2D()) {
            plugin.buildRibbon(ribbon);
        }
    }

    private void buildAnimationRibbon(JIPipeDesktopRibbon ribbon) {
        JIPipeDesktopRibbon.Task animationTask = ribbon.getOrCreateTask("Animation");
        JIPipeDesktopRibbon.Band exportBand = animationTask.getOrCreateBand("Export");
        exportBand.addLargeButton("To movie", "Exports the all slices as image file", UIUtils.getIcon32FromResources("actions/filmgrain.png"), this::exportVideo);
        exportBand.addComponent(UIUtils.boxHorizontal(new JLabel("FPS"), Box.createHorizontalStrut(8), animationFPSControl), 1, new Insets(2, 2, 2, 2));
    }

    private void buildViewRibbon(JIPipeDesktopRibbon ribbon) {
        JIPipeDesktopRibbon.Task generalTask = ribbon.getOrCreateTask("General");
        JIPipeDesktopRibbon.Band viewBand = generalTask.getOrCreateBand("View");
        viewBand.addLargeButton("Fit", "Fits the image into the view", UIUtils.getIcon32FromResources("actions/zoom-select-fit.png"), this::fitImageToScreen);
        viewBand.addLargeButton("Center", "Centers the image into the view", UIUtils.getIcon32FromResources("actions/zoom-center-page.png"), () -> canvas.centerImage());

        // Zoom buttons
        viewBand.addSmallButton("Zoom in", "Increases the zoom", UIUtils.getIconFromResources("actions/magnifying-glass-plus.png"), this::increaseZoom);
        viewBand.add(zoomStatusButton);
        viewBand.addSmallButton("Zoom out", "Decreases the zoom", UIUtils.getIconFromResources("actions/magnifying-glass-minus.png"), this::decreaseZoom);
    }

    private void createZoomMenu(JButton button) {
        JPopupMenu zoomMenu = UIUtils.addPopupMenuToButton(button);
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
    }

    private void buildExportRibbon(JIPipeDesktopRibbon ribbon) {
        JIPipeDesktopRibbon.Task exportTask = ribbon.getOrCreateTask("Export");
        JIPipeDesktopRibbon.Band imageBand = exportTask.getOrCreateBand("Image");
        JIPipeDesktopRibbon.Band snapshotBand = exportTask.getOrCreateBand("Snapshot");

        imageBand.addLargeMenuButton("ImageJ", "Open the image in ImageJ", UIUtils.getIcon32FromResources("apps/imagej2.png"),
                UIUtils.createMenuItem("Raw image", "Use the raw image", UIUtils.getIconFromResources("actions/image.png"), this::exportRawImageToImageJ),
                UIUtils.createMenuItem("Rendered image", "Render the image (as displayed) and export that to ImageJ", UIUtils.getIconFromResources("actions/image.png"), this::exportRenderedImageToImageJ));
        imageBand.addLargeMenuButton("3D viewer", "Opens the image in the ImageJ 3D viewer", UIUtils.getIcon32FromResources("apps/imagej2.png"),
                UIUtils.createMenuItem("Raw image", "Use the raw image", UIUtils.getIconFromResources("actions/image.png"), this::exportRawImageToImageJ3DViewer),
                UIUtils.createMenuItem("Rendered image", "Render the image (as displayed) and export that to ImageJ", UIUtils.getIconFromResources("actions/image.png"), this::exportRenderedImageToImageJ3DViewer));
        imageBand.addLargeMenuButton("Napari", "Opens the image in Napari", UIUtils.getIcon32FromResources("apps/napari.png"),
                UIUtils.createMenuItem("Raw image", "Use the raw image", UIUtils.getIconFromResources("actions/image.png"), this::exportToRawImageNapari),
                UIUtils.createMenuItem("Rendered image", "Render the image (as displayed) and export that to Napari", UIUtils.getIconFromResources("actions/image.png"), this::exportRenderedImageToNapari));

        imageBand.addLargeMenuButton("Save TIFF", "Exports the image as *.tif", UIUtils.getIcon32FromResources("actions/filesave.png"),
                UIUtils.createMenuItem("Raw image", "Save the raw image", UIUtils.getIconFromResources("actions/filesave.png"), this::saveRawImageToTiff),
                UIUtils.createMenuItem("Rendered image", "Save the rendered image (as displayed)", UIUtils.getIconFromResources("actions/filesave.png"), this::saveRenderedImageToTiff));
        snapshotBand.addLargeButton("To clipboard", "Exports a snapshot of the current slice to the clipboard", UIUtils.getIcon32FromResources("actions/edit-copy.png"), this::copyCurrentSliceToClipboard);
        snapshotBand.addSmallButton("To PNG (current)", "Exports the current slice as image file", UIUtils.getIconFromResources("actions/viewimage.png"), this::exportCurrentSliceToPNG);
        snapshotBand.addSmallButton("To PNG (all)", "Exports the all slices as image file", UIUtils.getIconFromResources("actions/qlipper.png"), this::exportAllSlicesToPNG);
        snapshotBand.addSmallButton("To movie", "Exports the all slices as image file", UIUtils.getIconFromResources("actions/filmgrain.png"), this::exportVideo);
        snapshotBand.add(exportDisplayedScaleToggle);
    }

    private void exportRenderedImageToNapari() {
        viewerRunnerQueue.runInDialog(getDesktopWorkbench(), this, new Stack2DRendererRun(getImageViewer(), "Export rendered image to Napari", 1, (renderedImage) -> {
            SwingUtilities.invokeLater(() -> {
                ImagePlusData data = new ImagePlusData(renderedImage);
                data.setOverlays(getImageViewer().getDataOverlays());
                new OpenInNapariDataDisplayOperation().display(data, getDisplayName(), getDesktopWorkbench(), null);
            });
        }));
    }

    private void exportToRawImageNapari() {
        ImagePlusData data = new ImagePlusData(getImagePlus());
        data.setOverlays(getImageViewer().getDataOverlays());
        new OpenInNapariDataDisplayOperation().display(data, getDisplayName(), getDesktopWorkbench(), null);
    }

    private void saveRenderedImageToTiff() {
        Path path = JIPipeDesktop.saveFile(this, workbench, JIPipeFileChooserApplicationSettings.LastDirectoryKey.Data, "Save as *.tif", HTMLText.EMPTY, UIUtils.EXTENSION_FILTER_TIFF);
        if (path != null) {
            viewerRunnerQueue.runInDialog(getDesktopWorkbench(), this, new Stack2DRendererRun(getImageViewer(), "Export rendered image", 1, (renderedImage) -> {
                IJ.saveAs(renderedImage, "TIFF", path.toString());
            }));
        }
    }

    private void exportRawImageToImageJ3DViewer() {
        ImagePlusData data = new ImagePlusData(getImagePlus());
        data.setOverlays(getImageViewer().getDataOverlays());
        new OpenInImageJ3DViewerDataDisplayOperation().display(data, getDisplayName(), getDesktopWorkbench(), null);
    }

    private void exportRenderedImageToImageJ3DViewer() {
        viewerRunnerQueue.runInDialog(getDesktopWorkbench(), this, new Stack2DRendererRun(getImageViewer(), "Export rendered image to 3D Image Viewer", 1, (renderedImage) -> {
            SwingUtilities.invokeLater(() -> {
                ImagePlusData data = new ImagePlusData(renderedImage);
                data.setOverlays(getImageViewer().getDataOverlays());
                new OpenInImageJ3DViewerDataDisplayOperation().display(data, getDisplayName(), getDesktopWorkbench(), null);
            });
        }));
    }

    private void exportRenderedImageToImageJ() {
        viewerRunnerQueue.runInDialog(getDesktopWorkbench(), this, new Stack2DRendererRun(getImageViewer(), "Export rendered image to 3D Image Viewer", 1, (renderedImage) -> {
            SwingUtilities.invokeLater(() -> {
                ImagePlusData data = new ImagePlusData(renderedImage);
                data.setOverlays(getImageViewer().getDataOverlays());
                new OpenInImageJDataDisplayOperation().display(data, getDisplayName(), getDesktopWorkbench(), null);
            });
        }));
    }

    private void exportRawImageToImageJ() {
        ImagePlusData data = new ImagePlusData(getImagePlus());
        data.setOverlays(getImageViewer().getDataOverlays());
        new OpenInImageJDataDisplayOperation().display(data, getDisplayName(), getDesktopWorkbench(), null);
    }

    public String getDisplayName() {
        return getImageViewer().getDataViewer() != null ? getImageViewer().getDataViewer().getDataViewerWindow().getDisplayName() : "Image";
    }

    public void buildDock(JIPipeDesktopDockPanel dockPanel) {
        this.currentDockPanel = dockPanel;
        for (JIPipeDesktopLegacyImageViewerPlugin2D plugin : imageViewer.getPlugins2D()) {
            plugin.buildDock(dockPanel);
        }
    }

    public void buildStatusBar(JToolBar statusBar) {
        UIUtils.addToStatusBarWithSeparator(statusBar, new JIPipeDesktopCompactRunnableQueueButton(getDesktopWorkbench(), viewerRunnerQueue, "actions/tool_imageeffects.png"));
        for (JIPipeDesktopLegacyImageViewerPlugin2D plugin : imageViewer.getPlugins2D()) {
            plugin.buildStatusBar(statusBar);
        }
    }

    private void saveRawImageToTiff() {
        Path path = JIPipeDesktop.saveFile(this, workbench, JIPipeFileChooserApplicationSettings.LastDirectoryKey.Data, "Save as *.tif", HTMLText.EMPTY, UIUtils.EXTENSION_FILTER_TIFF);
        if (path != null) {
            JIPipeDesktopRunExecuteUI.runInDialog(workbench, this, new RawImage2DExporterRun(getImagePlus(), path), viewerRunnerQueue);
        }
    }

    /**
     * Returns the magnification that export/render methods should apply
     *
     * @return the magnification
     */
    public double getExportedMagnification() {
        return exportDisplayedScaleToggle.getState() ? canvas.getZoom() : 1.0;
    }

    public void exportCurrentSliceToPNG() {
        if (getCanvas().getImage() == null) {
            JOptionPane.showMessageDialog(this, "No image loaded.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        Path targetFile = JIPipeDesktop.saveFile(this, workbench, JIPipeFileChooserApplicationSettings.LastDirectoryKey.Data, "Export current slice", HTMLText.EMPTY, UIUtils.EXTENSION_FILTER_PNG, UIUtils.EXTENSION_FILTER_JPEG, UIUtils.EXTENSION_FILTER_BMP);
        if (targetFile != null) {
            String format = "PNG";
            if (UIUtils.EXTENSION_FILTER_BMP.accept(targetFile.toFile()))
                format = "BMP";
            else if (UIUtils.EXTENSION_FILTER_JPEG.accept(targetFile.toFile()))
                format = "JPEG";
            try {
                ImageProcessor processor = generateSlice(getCurrentSliceIndex().getC(), getCurrentSliceIndex().getZ(), getCurrentSliceIndex().getT(), getExportedMagnification(), true);
                BufferedImage image = BufferedImageUtils.copyBufferedImageToARGB(processor.getBufferedImage());
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
//        BufferedImage image = ImageJUtils.copyBufferedImageToARGB(getCanvas().getImage());
//        for (ImageViewerPanelPlugin plugin : getPlugins()) {
//            plugin.postprocessDrawForExport(image, getCurrentSliceIndex(), getExportedMagnification());
//        }
        ImageProcessor processor = generateSlice(getCurrentSliceIndex().getC(), getCurrentSliceIndex().getZ(), getCurrentSliceIndex().getT(), getExportedMagnification(), true);
        BufferedImage image = BufferedImageUtils.copyBufferedImageToARGB(processor.getBufferedImage());
        CopyImageToClipboard copyImageToClipboard = new CopyImageToClipboard();
        copyImageToClipboard.copyImage(image);
    }

    public void exportAllSlicesToPNG() {
        JIPipeDesktopFormPanel formPanel = new JIPipeDesktopFormPanel(null, JIPipeDesktopFormPanel.NONE);
        JIPipeDesktopPathEditorComponent exportPathEditor = new JIPipeDesktopPathEditorComponent(getDesktopWorkbench(), PathIOMode.Open, PathType.DirectoriesOnly);
        exportPathEditor.setPath(JIPipeFileChooserApplicationSettings.getInstance().getLastDataDirectory());
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
            JIPipeFileChooserApplicationSettings.getInstance().setLastDataDirectory(exportPathEditor.getPath());
            if (!JIPipe.NO_SETTINGS_AUTOSAVE) {
                JIPipe.getSettings().save();
            }
            Path targetPath = exportPathEditor.getPath();
            String format = fileFormatEditor.getSelectedItem() + "";
            String baseName = StringUtils.makeFilesystemCompatible(baseNameEditor.getText());
            Stack2DExporterRun run = new Stack2DExporterRun(imageViewer, targetPath, baseName, format);
            JIPipeDesktopRunExecuteUI.runInDialog(workbench, this, run, viewerRunnerQueue);
        }
    }

    public void exportVideo() {
        Path path = JIPipeDesktop.saveFile(this, workbench, JIPipeFileChooserApplicationSettings.LastDirectoryKey.Data, "Export video", HTMLText.EMPTY, UIUtils.EXTENSION_FILTER_AVI);
        if (path == null) {
            return;
        }

        JIPipeDesktopFormPanel formPanel = new JIPipeDesktopFormPanel(null, JIPipeDesktopFormPanel.NONE);
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
        compressionEditor.setSelectedItem(AVICompression.None);
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
            Video2DExporterRun run = new Video2DExporterRun(
                    imageViewer,
                    path,
                    getCurrentSliceIndex(),
                    (HyperstackDimension) dimensionEditor.getSelectedItem(),
                    animationTimer.getDelay(),
                    (AVICompression) compressionEditor.getSelectedItem(),
                    compressionQualityEditor.getValue());
            JIPipeDesktopRunExecuteUI.runInDialog(workbench, this, run, viewerRunnerQueue);
        }
    }

    private void updateZoomStatus() {
        zoomStatusButton.getButton().setText((int) Math.round(canvas.getZoom() * 100) + "%");
    }

    /**
     * A dummy {@link ImageCanvas} that is needed by some visualization algorithms for magnification
     * It is updated by {@link ImageViewerPanelCanvas2D}
     * Please do not make any changes to the display properties here, as the image viewer has its own canvas
     *
     * @return the dummy canvas
     */
    public ImageCanvas getZoomedDummyCanvas() {
        return zoomedDummyCanvas;
    }

    /**
     * A dummy {@link ImageCanvas} that is needed by some visualization algorithms for magnification
     * Its magnification should be permanently 1.0
     *
     * @return the dummy canvas
     */
    public ImageCanvas getExportDummyCanvas() {
        return exportDummyCanvas;
    }

    private double calculateZoomModifier(int fac) {
        // Calculate the dynamic speed factor based on how fast the user scrolls
        // 0 = slow, 1 = fastest
        long diff = System.currentTimeMillis() - lastTimeZoomed;
        lastTimeZoomed = System.currentTimeMillis();
        final double speedFactor = 1 - Math.min(250.0, diff) / 250;

        // Multiplicative zoom
        double targetScreenSizeModifier2 = 1.0 + settings.getZoomBaseSpeed() + speedFactor * settings.getZoomDynamicSpeed();
        if (fac < 0) {
            targetScreenSizeModifier2 = 1.0 / targetScreenSizeModifier2;
        }

        return targetScreenSizeModifier2;
    }

    private void increaseZoom() {
        double modifier = calculateZoomModifier(1);
        canvas.setZoom(canvas.getZoom() * modifier);
        updateZoomStatus();
    }

    private void decreaseZoom() {
        double modifier = calculateZoomModifier(-1);
        canvas.setZoom(canvas.getZoom() * modifier);
        updateZoomStatus();
    }

    private void refreshSliders() {
        try {
            isUpdatingSliders = true;
            if (image != null) {
                bottomPanel.setVisible(true);
                bottomPanel.clear();

                if (image.getNChannels() > 1 && !composite)
                    addSliderToForm(channelSlider, channelSliderLabel, animationChannelToggle, "Channel", "Channel (C) %d/%d");
                if (image.getNSlices() > 1)
                    addSliderToForm(stackSlider, stackSliderLabel, animationStackToggle, "Slice", "Slice (Z) %d/%d");
                if (image.getNFrames() > 1)
                    addSliderToForm(frameSlider, frameSliderLabel, animationFrameToggle, "Frame", "Frame (T) %d/%d");

                stackSlider.setMinimum(1);
                stackSlider.setMaximum(image.getNSlices());
                channelSlider.setMinimum(1);
                channelSlider.setMaximum(image.getNChannels());
                frameSlider.setMinimum(1);
                frameSlider.setMaximum(image.getNFrames());
            } else {
                bottomPanel.setVisible(false);
            }
        } finally {
            isUpdatingSliders = false;
        }
    }

    public ImagePlus getImagePlus() {
        return imageViewer.getImagePlus();
    }

    public void refreshFormPanel() {
        Map<String, Integer> scrollValues = new HashMap<>();
        for (Map.Entry<String, JIPipeDesktopDockPanel.Panel> entry : currentDockPanel.getPanels().entrySet()) {
            if (entry.getKey().startsWith(DOCK_PANEL_PREFIX)) {
                JIPipeDesktopFormPanel formPanel = entry.getValue().getComponent(JIPipeDesktopFormPanel.class);
                scrollValues.put(entry.getKey(), formPanel.getScrollPane().getVerticalScrollBar().getValue());
                formPanel.clear();
            }
        }

        List<JIPipeDesktopLegacyImageViewerPlugin2D> plugins2D = imageViewer.getPlugins2D();
        for (int i = 0; i < plugins2D.size(); i++) {
            JIPipeDesktopLegacyImageViewerPlugin2D plugin = plugins2D.get(i);
            if (plugin.isBuildingCustomPanel()) {
                currentDockPanel.addDockPanel("CUSTOM__" + DOCK_PANEL_PREFIX + plugin.getPanelName(),
                        plugin.getPanelName(),
                        plugin.getPanelIcon(),
                        plugin.getPanelLocation(),
                        false,
                        i,
                        plugin.buildCustomPanel());
            } else {
                JIPipeDesktopFormPanel formPanel = currentDockPanel.getPanelComponent(DOCK_PANEL_PREFIX + plugin.getPanelName(), JIPipeDesktopFormPanel.class);
                if (formPanel == null) {
                    formPanel = new JIPipeDesktopFormPanel(null, JIPipeDesktopFormPanel.WITH_SCROLLING);
                    currentDockPanel.addDockPanel(DOCK_PANEL_PREFIX + plugin.getPanelName(),
                            plugin.getPanelName(),
                            plugin.getPanelIcon(),
                            plugin.getPanelLocation(),
                            false,
                            i,
                            formPanel);
                }
                plugin.buildPanel(formPanel);
            }
        }
        for (Map.Entry<String, JIPipeDesktopDockPanel.Panel> entry : currentDockPanel.getPanels().entrySet()) {
            if (entry.getKey().startsWith(DOCK_PANEL_PREFIX)) {
                JIPipeDesktopFormPanel formPanel = entry.getValue().getComponent(JIPipeDesktopFormPanel.class);
                if (!formPanel.isHasVerticalGlue()) {
                    formPanel.addVerticalGlue();
                }
                SwingUtilities.invokeLater(() -> {
                    formPanel.getScrollPane().getVerticalScrollBar().setValue(scrollValues.getOrDefault(entry.getKey(), 0));
                });
            }
        }
    }

    public JSpinner getAnimationFPSControl() {
        return animationFPSControl;
    }

    /**
     * Returns the currently viewed slice position
     *
     * @return the slice position. Zero-based indices
     */
    public ImageSliceIndex getCurrentSliceIndex() {
        return new ImageSliceIndex(channelSlider.getValue() - 1, stackSlider.getValue() - 1, frameSlider.getValue() - 1);
    }

    public JIPipeDesktopLegacyImageViewer getImageViewer() {
        return imageViewer;
    }

    private void animateNextSlice() {
        if (!isDisplayable()) {
            stopAnimations();
            return;
        }
        if (animationStackToggle.isSelected()) {
            int newIndex = (image.getImage().getZ() % image.getNSlices()) + 1;
            stackSlider.setValue(newIndex);
        } else if (animationChannelToggle.isSelected()) {
            int newIndex = (image.getImage().getC() % image.getNChannels()) + 1;
            channelSlider.setValue(newIndex);
        } else if (animationFrameToggle.isSelected()) {
            int newIndex = (image.getImage().getT() % image.getNFrames()) + 1;
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
            image.getImage().setPosition(channel, stack, frame);
            this.currentSlice = image.getImage().getProcessor();
            for (JIPipeDesktopLegacyImageViewerPlugin2D plugin : imageViewer.getPlugins2D()) {
//                System.out.println(plugin + ": " + image.getDisplayRangeMin() + ", " + image.getDisplayRangeMax());
                plugin.onSliceChanged(true);
//                System.out.println(plugin + "(A): " + image.getDisplayRangeMin() + ", " + image.getDisplayRangeMax());
            }
            uploadSliceToCanvas();
        }
    }

    /**
     * Generates a slice
     *
     * @param c                  the channel location
     * @param z                  the depth location
     * @param t                  the frame location
     * @param magnification      the magnification
     * @param withPostprocessing if postprocessing (export postprocessing) should be applied
     * @return the new slice
     */
    public ImageProcessor generateSlice(int c, int z, int t, double magnification, boolean withPostprocessing) {
        if (composite && image.getNChannels() > 1) {

            ColorProcessor bottom = new ColorProcessor(image.getWidth(), image.getHeight());

            // Pre-generate processors for all channels
            for (int i = 0; i < image.getNChannels(); i++) {
                CompositeLayer layer = orderedCompositeBlendLayers.get(i);
                int cc = layer.getChannel();

                image.getImage().setPosition(cc + 1, z + 1, t + 1);
                for (JIPipeDesktopLegacyImageViewerPlugin2D plugin : imageViewer.getPlugins2D()) {
                    plugin.beforeDraw(cc, z, t);
                }
                ImageProcessor processor = image.getImage().getProcessor().duplicate();
                for (JIPipeDesktopLegacyImageViewerPlugin2D plugin : imageViewer.getPlugins2D()) {
                    processor = plugin.draw(cc, z, t, processor);
                }
                if (magnification != 1.0) {
                    processor.setInterpolationMethod(ImageProcessor.NONE);
                    processor = processor.resize((int) (magnification * image.getWidth()), (int) (magnification * image.getHeight()), false);
                }
                if (withPostprocessing) {
                    BufferedImage image = BufferedImageUtils.copyBufferedImageToARGB(processor.getBufferedImage());
                    for (JIPipeDesktopLegacyImageViewerPlugin2D plugin : imageViewer.getPlugins2D()) {
                        plugin.postprocessDrawForExport(image, new ImageSliceIndex(cc, z, t), magnification);
                    }
                    processor = new ColorProcessor(image);
                }
                if (!(processor instanceof ColorProcessor)) {
                    BufferedImage image = BufferedImageUtils.copyBufferedImageToARGB(processor.getBufferedImage());
                    processor = new ColorProcessor(image);
                }

                layer.getBlendMode().blend(bottom, (ColorProcessor) processor, layer.getOpacity());
            }

            return bottom;
        } else {
            image.getImage().setPosition(c + 1, z + 1, t + 1);
            for (JIPipeDesktopLegacyImageViewerPlugin2D plugin : imageViewer.getPlugins2D()) {
                plugin.beforeDraw(c, z, t);
            }
            ImageProcessor processor = image.getImage().getProcessor().duplicate();
            for (JIPipeDesktopLegacyImageViewerPlugin2D plugin : imageViewer.getPlugins2D()) {
                processor = plugin.draw(c, z, t, processor);
            }
            if (magnification != 1.0) {
                processor.setInterpolationMethod(ImageProcessor.NONE);
                processor = processor.resize((int) (magnification * image.getWidth()), (int) (magnification * image.getHeight()), false);
            }
            if (withPostprocessing) {
                BufferedImage image = BufferedImageUtils.copyBufferedImageToARGB(processor.getBufferedImage());
                for (JIPipeDesktopLegacyImageViewerPlugin2D plugin : imageViewer.getPlugins2D()) {
                    plugin.postprocessDrawForExport(image, new ImageSliceIndex(c, z, t), magnification);
                }
                processor = new ColorProcessor(image);
            }
            return processor;
        }
    }

    public void uploadSliceToCanvas() {
        if (image != null) {
            ImageProcessor processor = generateSlice(channelSlider.getValue() - 1, stackSlider.getValue() - 1,
                    frameSlider.getValue() - 1,
                    1.0, false);
            if (processor == null) {
                canvas.setImage(null, null);
                return;
            }
//            System.out.println("tg " + processor.getMin() + ", " + processor.getMax());
            canvas.setImage(processor.getBufferedImage(), getCurrentSliceIndex());
        } else {
            canvas.setImage(null, null);
        }
    }

    public ImageViewerPanelCanvas2D getCanvas() {
        return canvas;
    }

    public void fitImageToScreen() {
        if (image != null) {
            double zoomx = canvasScrollPane.getViewport().getWidth() / (1.0 * image.getWidth());
            double zoomy = canvasScrollPane.getViewport().getHeight() / (1.0 * image.getHeight());
            canvas.setZoom(Math.min(zoomx, zoomy));
            canvas.centerImage();
            updateZoomStatus();
        }
    }

    public ImagePlusData getImage() {
        return image;
    }

    public void setImage(ImagePlusData image) {
        boolean doFit = this.image == null;

        this.image = image;
        compositeBlendLayers.clear();
        orderedCompositeBlendLayers.clear();
        if (image != null) {
            ImagePlus imagePlus = image.getImage();
            this.zoomedDummyCanvas = new ImageCanvas(imagePlus);
            this.zoomedDummyCanvas.setMagnification(getCanvas().getZoom());
            this.exportDummyCanvas = new ImageCanvas(imagePlus);

            // Generate blend layers
            for (int c = 0; c < imagePlus.getNChannels(); c++) {
                CompositeLayer layer = new CompositeLayer(c);
                orderedCompositeBlendLayers.add(layer);
                compositeBlendLayers.put(c, layer);
            }

        } else {
            this.zoomedDummyCanvas = null;
            this.exportDummyCanvas = null;
        }
        this.currentSlice = null;
        this.statisticsMap.clear();
        for (JIPipeDesktopLegacyImageViewerPlugin2D plugin : imageViewer.getPlugins2D()) {
            try {
                plugin.onImageChanged();
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
        refreshSliders();
        refreshSlice();
        refreshFormPanel();
        revalidate();
        repaint();
        uploadSliceToCanvas();

        if (doFit) {
            UIUtils.invokeMuchLater(250, () -> {
                fitImageToScreen();
            });
        }
    }

    public ImageProcessor getCurrentSlice() {
        return currentSlice;
    }

    public ImageViewer2DSliceStatistics getCurrentSliceStats() {
        if (getImagePlus() != null && getCurrentSliceIndex() != null) {
            return getSliceStats(getCurrentSliceIndex());
        } else {
            return null;
        }
    }

    public ImageViewer2DSliceStatistics getSliceStats(ImageSliceIndex sliceIndex) {
        if (getImagePlus() != null) {
            ImageViewer2DSliceStatistics stats = statisticsMap.getOrDefault(sliceIndex, null);
            if (stats == null) {
                ImageProcessor processor = ImageJUtils.getSliceZeroSafe(image.getImage(), sliceIndex);
                stats = new ImageViewer2DSliceStatistics(processor);
                statisticsMap.put(sliceIndex, stats);
            }
            return stats;
        }
        return null;
    }

    public void moveCompositePriorityUp(int targetChannel) {
        CompositeLayer layer = compositeBlendLayers.getOrDefault(targetChannel, null);
        if (layer != null) {
            int oldIndex = orderedCompositeBlendLayers.indexOf(layer);
            if (oldIndex > 0) {
                int newIndex = oldIndex - 1;
                CompositeLayer backup = orderedCompositeBlendLayers.get(newIndex);
                orderedCompositeBlendLayers.set(newIndex, layer);
                orderedCompositeBlendLayers.set(oldIndex, backup);
                refreshFormPanel();
                uploadSliceToCanvas();
            }
        }
    }

    public void moveCompositePriorityDown(int targetChannel) {
        CompositeLayer layer = compositeBlendLayers.getOrDefault(targetChannel, null);
        if (layer != null) {
            int oldIndex = orderedCompositeBlendLayers.indexOf(layer);
            if (oldIndex >= 0 && oldIndex < orderedCompositeBlendLayers.size() - 1) {
                int newIndex = oldIndex + 1;
                CompositeLayer backup = orderedCompositeBlendLayers.get(newIndex);
                orderedCompositeBlendLayers.set(newIndex, layer);
                orderedCompositeBlendLayers.set(oldIndex, backup);
                refreshFormPanel();
                uploadSliceToCanvas();
            }
        }
    }

    @Override
    public JIPipeWorkbench getWorkbench() {
        return workbench;
    }

    public static class CompositeLayer extends ImageBlendLayer {
        private final int channel;

        public CompositeLayer(int channel) {
            this.channel = channel;
        }

        public CompositeLayer(CompositeLayer other) {
            super(other);
            this.channel = other.channel;
        }

        public int getChannel() {
            return channel;
        }
    }




}
