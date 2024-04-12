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

package org.hkijena.jipipe.plugins.imageviewer;

import ij.IJ;
import ij.ImagePlus;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import ij.process.LUT;
import ij.process.StackStatistics;
import ij3d.Content;
import ij3d.ContentCreator;
import ij3d.UniverseListener;
import org.apache.commons.lang3.math.NumberUtils;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.AbstractJIPipeRunnable;
import org.hkijena.jipipe.api.JIPipeWorkbench;
import org.hkijena.jipipe.api.run.JIPipeRunnable;
import org.hkijena.jipipe.api.run.JIPipeRunnableQueue;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbenchAccess;
import org.hkijena.jipipe.desktop.app.running.JIPipeDesktopRunExecuterUI;
import org.hkijena.jipipe.desktop.app.running.JIPipeDesktopRunnableQueueButton;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopFormPanel;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopParameterPanel;
import org.hkijena.jipipe.desktop.commons.components.icons.NewThrobberIcon;
import org.hkijena.jipipe.desktop.commons.components.tabs.JIPipeDesktopTabPane;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageSliceIndex;
import org.hkijena.jipipe.plugins.imageviewer.plugins3d.CalibrationPlugin3D;
import org.hkijena.jipipe.plugins.imageviewer.plugins3d.LUTManagerPlugin3D;
import org.hkijena.jipipe.plugins.imageviewer.plugins3d.OpacityManagerPlugin3D;
import org.hkijena.jipipe.plugins.imageviewer.runs.RawImage2DExporterRun;
import org.hkijena.jipipe.plugins.imageviewer.settings.ImageViewer3DUISettings;
import org.hkijena.jipipe.plugins.imageviewer.utils.viewer3d.Image3DRenderType;
import org.hkijena.jipipe.plugins.imageviewer.utils.viewer3d.Image3DRendererSettings;
import org.hkijena.jipipe.plugins.imageviewer.utils.viewer3d.SnapshotSettings;
import org.hkijena.jipipe.plugins.imageviewer.utils.viewer3d.StandardView;
import org.hkijena.jipipe.plugins.imageviewer.utils.viewer3d.universe.CustomImage3DUniverse;
import org.hkijena.jipipe.plugins.imageviewer.utils.viewer3d.universe.CustomInteractiveBehavior;
import org.hkijena.jipipe.plugins.parameters.library.markup.MarkdownText;
import org.hkijena.jipipe.plugins.settings.FileChooserSettings;
import org.hkijena.jipipe.utils.AutoResizeSplitPane;
import org.hkijena.jipipe.utils.BufferedImageUtils;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.ui.CopyImageToClipboard;
import org.scijava.Disposable;
import org.scijava.java3d.Canvas3D;
import org.scijava.java3d.GraphicsConfigTemplate3D;
import org.scijava.java3d.View;
import org.scijava.vecmath.Color3f;

import javax.imageio.ImageIO;
import javax.swing.Timer;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.List;
import java.util.*;

public class ImageViewerPanel3D extends JPanel implements JIPipeDesktopWorkbenchAccess, Disposable, UniverseListener, ComponentListener, JIPipeRunnable.FinishedEventListener {
    private final JIPipeImageViewer imageViewer;
    private final ImageViewer3DUISettings settings;
    private final JPanel rendererStatusPanel = new JPanel(new BorderLayout());
    private final JPanel dataStatusPanel = new JPanel(new BorderLayout());
    private final JPanel viewerPanel = new JPanel(new BorderLayout());
    private final JPanel viewerCanvasPanel = new JPanel(new BorderLayout());
    private final JToolBar toolBar = new JToolBar();
    private final JToggleButton enableSideBarButton = new JToggleButton();
    private final JIPipeDesktopTabPane tabPane = new JIPipeDesktopTabPane(true, JIPipeDesktopTabPane.TabPlacement.Right);
    private final NewThrobberIcon initializationThrobberIcon = new NewThrobberIcon(this);
    private final Timer rebuildImageLaterTimer;
    private final Map<String, JIPipeDesktopFormPanel> formPanels = new HashMap<>();
    private final JIPipeRunnableQueue viewerRunnerQueue = new JIPipeRunnableQueue("3D viewer");
    private final Image3DRendererSettings image3DRendererSettings = new Image3DRendererSettings();
    private final JLabel renderInfoLabel = new JLabel("No image",
            UIUtils.getIconFromResources("devices/video-display.png"), JLabel.LEFT);
    private final JSlider frameSlider = new JSlider(1, 100, 1);
    private final JToggleButton animationFrameToggle = new JToggleButton(UIUtils.getIconFromResources("actions/play.png"));
    private final JSpinner animationFPSControl = new JSpinner(new SpinnerNumberModel(24, 0.01, 1000, 0.1));
    private final JLabel frameSliderLabel = new JLabel("Frame (T)");
    private ImagePlusData image;
    private JComponent currentContentPanel;
    private CustomImage3DUniverse universe;
    private boolean active = false;
    private RendererStatus rendererStatus = RendererStatus.Uninitialized;
    private UniverseInitializerRun universeInitializerRun;
    private ImageLoaderRun imageLoaderRun;
    private List<Content> currentImageContents;
    private int currentImageContentsResamplingFactor;
    private ImageStatistics imageStatistics;
    private boolean isUpdatingSliders = false;
    private JIPipeDesktopFormPanel bottomPanel;
    private UpdateLutAndCalibrationRun currentUpdateCalibrationRun;
    public ImageViewerPanel3D(JIPipeImageViewer imageViewer) {
        this.imageViewer = imageViewer;
        if (JIPipe.getInstance() != null) {
            settings = ImageViewer3DUISettings.getInstance();
        } else {
            settings = new ImageViewer3DUISettings();
        }
        this.image3DRendererSettings.copyFrom(settings.getRendererSettings());
        this.rebuildImageLaterTimer = new Timer(1000, e -> rebuildImageNow());
        rebuildImageLaterTimer.setRepeats(false);
        initialize();
        updateSideBar();

        // Register events
        viewerRunnerQueue.setSilent(true);
        viewerRunnerQueue.getFinishedEventEmitter().subscribeWeak(this);
        image3DRendererSettings.getParameterChangedEventEmitter().subscribeLambda((emitter, event) -> rebuildImageLater());
        addComponentListener(this);
    }    private final Timer animationTimer = new Timer(250, e -> animateNextSlice());

    public static GraphicsConfiguration getBestConfigurationOnSameDevice(Window frame) {

        GraphicsConfiguration gc = frame.getGraphicsConfiguration();
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice[] gs = ge.getScreenDevices();
        GraphicsConfiguration good = null;

        GraphicsConfigTemplate3D gct = new GraphicsConfigTemplate3D();

        for (GraphicsDevice gd : gs) {

            if (gd == gc.getDevice()) {
                good = gct.getBestConfiguration(gd.getConfigurations());
                if (good != null)
                    break;
            }
        }

        return good;
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

    public int getCurrentImageContentsResamplingFactor() {
        return currentImageContentsResamplingFactor;
    }

    public ImageViewer3DUISettings getSettings() {
        return settings;
    }

    public Image3DRendererSettings getImage3DRendererSettings() {
        return image3DRendererSettings;
    }

    private void initialize() {

        // Load default animation speed
        if (settings != null) {
            double fps = settings.getDefaultAnimationFPS();
            animationFPSControl.getModel().setValue(fps);
            animationTimer.setDelay(Math.max(1, (int) (1000.0 / fps)));
        }

        setLayout(new BorderLayout());
        viewerPanel.add(viewerCanvasPanel, BorderLayout.CENTER);

        JPanel messagePanel = new JPanel(new GridBagLayout());
        messagePanel.add(rendererStatusPanel, new GridBagConstraints(0,
                1,
                1,
                1,
                1,
                0,
                GridBagConstraints.NORTHWEST,
                GridBagConstraints.HORIZONTAL,
                new Insets(0, 0, 0, 0),
                0,
                0));
        messagePanel.add(dataStatusPanel, new GridBagConstraints(0,
                2,
                1,
                1,
                1,
                0,
                GridBagConstraints.NORTHWEST,
                GridBagConstraints.HORIZONTAL,
                new Insets(0, 0, 0, 0),
                0,
                0));
        viewerPanel.add(messagePanel, BorderLayout.NORTH);

        JToolBar statusBar = new JToolBar();
        statusBar.setFloatable(false);
        viewerPanel.add(statusBar, BorderLayout.SOUTH);

        JIPipeDesktopRunnableQueueButton runnerQueueUI = new JIPipeDesktopRunnableQueueButton(getDesktopWorkbench(), viewerRunnerQueue);
        runnerQueueUI.makeFlat();
        statusBar.add(runnerQueueUI);
        statusBar.add(Box.createHorizontalGlue());
        statusBar.add(new JLabel("Rotate", UIUtils.getIconFromResources("actions/input-mouse-click-left.png"), JLabel.LEFT));
        statusBar.add(Box.createHorizontalStrut(8));
        statusBar.add(UIUtils.createVerticalSeparator());
        statusBar.add(Box.createHorizontalStrut(8));
        statusBar.add(new JLabel("+", UIUtils.getIconFromResources("actions/tap-extract.png"), JLabel.LEFT));
        statusBar.add(new JLabel("Move", UIUtils.getIconFromResources("actions/input-mouse-click-left.png"), JLabel.LEFT));
        statusBar.add(Box.createHorizontalStrut(8));
        statusBar.add(UIUtils.createVerticalSeparator());
        statusBar.add(Box.createHorizontalStrut(8));
        statusBar.add(new JLabel("Zoom", UIUtils.getIconFromResources("actions/input-mouse-click-middle.png"), JLabel.LEFT));

        statusBar.add(Box.createHorizontalStrut(8));
        statusBar.add(UIUtils.createVerticalSeparator());
        statusBar.add(Box.createHorizontalStrut(8));
        statusBar.add(renderInfoLabel);
        statusBar.add(Box.createHorizontalStrut(8));

        JPopupMenu.setDefaultLightWeightPopupEnabled(false);

        bottomPanel = new JIPipeDesktopFormPanel(null, JIPipeDesktopFormPanel.NONE);
        add(bottomPanel, BorderLayout.SOUTH);

        initializeToolbar();

        initializeAnimationControls();
    }

    private void refreshSliders() {
        try {
            isUpdatingSliders = true;
            if (image != null) {
                bottomPanel.setVisible(true);
                bottomPanel.clear();

                if (image.getNFrames() > 1)
                    addSliderToForm(frameSlider, frameSliderLabel, animationFrameToggle, "Frame", "Frame (T) %d/%d");

                frameSlider.setMinimum(1);
                frameSlider.setMaximum(image.getNFrames());
            } else {
                bottomPanel.setVisible(false);
            }
        } finally {
            isUpdatingSliders = false;
        }
    }

    private void initializeToolbar() {
        toolBar.setFloatable(false);

        toolBar.add(Box.createHorizontalGlue());
        initializeExportMenu();
        toolBar.addSeparator();
        initializeResetViewMenu();
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
                if (!JIPipe.NO_SETTINGS_AUTOSAVE) {
                    JIPipe.getSettings().saveApplicationSettings();
                }
            }
            updateSideBar();
        });
        toolBar.add(enableSideBarButton);
    }

    private void initializeResetViewMenu() {
        JButton resetViewButton = new JButton("Reset view", UIUtils.getIconFromResources("actions/view-restore.png"));
        JPopupMenu resetViewMenu = UIUtils.addPopupMenuToButton(resetViewButton);
        resetViewMenu.add(UIUtils.createMenuItem("Top",
                "Display the image from the top",
                UIUtils.getIconFromResources("actions/3d-side-top.png"),
                () -> universe.getCustomInteractiveViewPlatformTransformer().resetView(StandardView.Top)));
        resetViewMenu.add(UIUtils.createMenuItem("Bottom",
                "Display the image from the bottom",
                UIUtils.getIconFromResources("actions/3d-side-bottom.png"),
                () -> universe.getCustomInteractiveViewPlatformTransformer().resetView(StandardView.Bottom)));
        resetViewMenu.add(UIUtils.createMenuItem("North",
                "Display the image from the north side",
                UIUtils.getIconFromResources("actions/3d-side-north.png"),
                () -> universe.getCustomInteractiveViewPlatformTransformer().resetView(StandardView.North)));
        resetViewMenu.add(UIUtils.createMenuItem("South",
                "Display the image from the south side",
                UIUtils.getIconFromResources("actions/3d-side-south.png"),
                () -> universe.getCustomInteractiveViewPlatformTransformer().resetView(StandardView.South)));
        resetViewMenu.add(UIUtils.createMenuItem("East",
                "Display the image from the east side",
                UIUtils.getIconFromResources("actions/3d-side-east.png"),
                () -> universe.getCustomInteractiveViewPlatformTransformer().resetView(StandardView.East)));
        resetViewMenu.add(UIUtils.createMenuItem("West",
                "Display the image from the west side",
                UIUtils.getIconFromResources("actions/3d-side-west.png"),
                () -> universe.getCustomInteractiveViewPlatformTransformer().resetView(StandardView.West)));
        toolBar.add(resetViewButton);
    }

    public List<Content> getCurrentImageContents() {
        return currentImageContents != null ? Collections.unmodifiableList(currentImageContents) : null;
    }

    private void updateSideBar() {
        if (currentContentPanel != null) {
            remove(currentContentPanel);
        }
        if (enableSideBarButton.isSelected()) {
            JSplitPane splitPane = new AutoResizeSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                    viewerPanel,
                    tabPane, new AutoResizeSplitPane.DynamicSidebarRatio(450, false));
            add(splitPane, BorderLayout.CENTER);
            currentContentPanel = splitPane;
        } else {
            add(viewerPanel, BorderLayout.CENTER);
            currentContentPanel = viewerPanel;
        }
        revalidate();
        repaint();
    }

    private void initializeExportMenu() {
        JButton exportMenuButton = new JButton(UIUtils.getIconFromResources("actions/camera.png"));
        exportMenuButton.setToolTipText("Export currently displayed image");
        JPopupMenu exportMenu = new JPopupMenu();

        JMenuItem saveRawImageItem = new JMenuItem("Export raw image to *.tif", UIUtils.getIconFromResources("actions/save.png"));
        saveRawImageItem.addActionListener(e -> saveRawImage());
        exportMenu.add(saveRawImageItem);

        exportMenu.addSeparator();
//
//        exportMenu.add(exportDisplayedScaleToggle);
//
        JMenuItem exportCurrentSliceItem = new JMenuItem("Export snapshot to file", UIUtils.getIconFromResources("actions/viewimage.png"));
        exportCurrentSliceItem.addActionListener(e -> exportSnapshotToPNG());
        exportMenu.add(exportCurrentSliceItem);

//        exportAllSlicesItem = new JMenuItem("Export snapshot of all slices", UIUtils.getIconFromResources("actions/qlipper.png"));
//        exportAllSlicesItem.addActionListener(e -> exportAllSlicesToPNG());
//        exportMenu.add(exportAllSlicesItem);
//
//        exportMovieItem = new JMenuItem("Export movie", UIUtils.getIconFromResources("actions/filmgrain.png"));
//        exportMovieItem.addActionListener(e -> exportVideo());
//        exportMenu.add(exportMovieItem);

        exportMenu.addSeparator();
//
        JMenuItem copyCurrentSliceItem = new JMenuItem("Copy snapshot to clipboard", UIUtils.getIconFromResources("actions/edit-copy.png"));
        copyCurrentSliceItem.addActionListener(e -> copySnapshotToClipboard());
        exportMenu.add(copyCurrentSliceItem);

        UIUtils.addPopupMenuToButton(exportMenuButton, exportMenu);
        toolBar.add(exportMenuButton);
    }

    private void exportSnapshotToPNG() {
        if (universe != null) {
            Path targetFile = FileChooserSettings.saveFile(this, FileChooserSettings.LastDirectoryKey.Data, "Export current slice", UIUtils.EXTENSION_FILTER_PNG, UIUtils.EXTENSION_FILTER_JPEG, UIUtils.EXTENSION_FILTER_BMP);
            if (targetFile != null) {
                String format = "PNG";
                if (UIUtils.EXTENSION_FILTER_BMP.accept(targetFile.toFile()))
                    format = "BMP";
                else if (UIUtils.EXTENSION_FILTER_JPEG.accept(targetFile.toFile()))
                    format = "JPEG";

                Canvas3D canvas = universe.getCanvas();

                SnapshotSettings snapshotSettings = new SnapshotSettings();
                snapshotSettings.setWidth(canvas.getWidth());
                snapshotSettings.setHeight(canvas.getHeight());

                if (JIPipeDesktopParameterPanel.showDialog(getDesktopWorkbench(), this, snapshotSettings, new MarkdownText(), "Create snapshot", JIPipeDesktopParameterPanel.WITH_SCROLLING)) {
                    ImagePlus imagePlus = universe.takeSnapshot(snapshotSettings.getWidth(), snapshotSettings.getHeight());
                    BufferedImage image = BufferedImageUtils.copyBufferedImageToARGB(imagePlus.getBufferedImage());
                    try {
                        ImageIO.write(image, format, targetFile.toFile());
                    } catch (IOException e) {
                        IJ.handleException(e);
                    }
                }
            }
        }
    }

    private void copySnapshotToClipboard() {
        if (universe != null) {
            Canvas3D canvas = universe.getCanvas();

            SnapshotSettings snapshotSettings = new SnapshotSettings();
            snapshotSettings.setWidth(canvas.getWidth());
            snapshotSettings.setHeight(canvas.getHeight());

            if (JIPipeDesktopParameterPanel.showDialog(getDesktopWorkbench(), this, snapshotSettings, new MarkdownText(), "Create snapshot", JIPipeDesktopParameterPanel.WITH_SCROLLING)) {
                ImagePlus imagePlus = universe.takeSnapshot(snapshotSettings.getWidth(), snapshotSettings.getHeight());
                BufferedImage image = BufferedImageUtils.copyBufferedImageToARGB(imagePlus.getBufferedImage());
                CopyImageToClipboard copyImageToClipboard = new CopyImageToClipboard();
                copyImageToClipboard.copyImage(image);
            }

        }
    }

    private void saveRawImage() {
        Path path = FileChooserSettings.saveFile(this, FileChooserSettings.LastDirectoryKey.Data, "Save as *.tif", UIUtils.EXTENSION_FILTER_TIFF);
        if (path != null) {
            JIPipeDesktopRunExecuterUI.runInDialog(getDesktopWorkbench(), this, new RawImage2DExporterRun(getImagePlus(), path));
        }
    }

    public JToolBar getToolBar() {
        return toolBar;
    }

    public JIPipeDesktopWorkbench getDesktopWorkbench() {
        return imageViewer.getDesktopWorkbench();
    }

    @Override
    public JIPipeWorkbench getWorkbench() {
        return imageViewer.getWorkbench();
    }

    public JIPipeImageViewer getImageViewer() {
        return imageViewer;
    }

    public ImagePlus getImagePlus() {
        return imageViewer.getImagePlus();
    }

    public ImagePlusData getImage() {
        return image;
    }

    public void setImage(ImagePlusData image) {
        this.image = image;
        this.imageStatistics = null;

        refreshSliders();

        if (!active) {
            return;
        }
        viewerRunnerQueue.cancelIf(run -> !(run instanceof UniverseInitializerRun));
        if (universe != null) {
            universe.setAutoAdjustView(true);
            universe.removeAllContents();
        }
        rebuildImageNow();
        for (JIPipeImageViewerPlugin3D plugin3D : getImageViewer().getPlugins3D()) {
            plugin3D.onImageChanged();
        }
        refreshFormPanel();
    }

    public JIPipeRunnableQueue getViewerRunnerQueue() {
        return viewerRunnerQueue;
    }

    public void activate() {
        if (!active) {
            active = true;
            rebuildImageNow();
            for (JIPipeImageViewerPlugin3D plugin3D : getImageViewer().getPlugins3D()) {
                plugin3D.onViewerActivated();
            }
            for (JIPipeImageViewerPlugin3D plugin3D : getImageViewer().getPlugins3D()) {
                plugin3D.onImageChanged();
            }
            refreshFormPanel();
        }
    }

    public JSpinner getAnimationFPSControl() {
        return animationFPSControl;
    }

    public void rebuildImageLater() {
        rebuildImageLaterTimer.restart();
    }

    public void refreshFormPanel() {
        Map<String, Integer> scrollValues = new HashMap<>();
        for (Map.Entry<String, JIPipeDesktopFormPanel> entry : formPanels.entrySet()) {
            scrollValues.put(entry.getKey(), entry.getValue().getScrollPane().getVerticalScrollBar().getValue());
            entry.getValue().clear();
        }
        for (JIPipeImageViewerPlugin3D plugin : imageViewer.getPlugins3D()) {
            JIPipeDesktopFormPanel formPanel = formPanels.getOrDefault(plugin.getCategory(), null);
            if (formPanel == null) {
                formPanel = new JIPipeDesktopFormPanel(null, JIPipeDesktopFormPanel.WITH_SCROLLING);
                formPanels.put(plugin.getCategory(), formPanel);
                JIPipeDesktopFormPanel finalFormPanel = formPanel;
                tabPane.registerSingletonTab(plugin.getCategory(),
                        plugin.getCategory(),
                        plugin.getCategoryIcon(),
                        () -> finalFormPanel,
                        JIPipeDesktopTabPane.CloseMode.withoutCloseButton,
                        JIPipeDesktopTabPane.SingletonTabMode.Present);
            }
            plugin.initializeSettingsPanel(formPanel);
        }
        for (Map.Entry<String, JIPipeDesktopFormPanel> entry : formPanels.entrySet()) {
            if (!entry.getValue().isHasVerticalGlue()) {
                entry.getValue().addVerticalGlue();
            }
            SwingUtilities.invokeLater(() -> {
                entry.getValue().getScrollPane().getVerticalScrollBar().setValue(scrollValues.getOrDefault(entry.getKey(), 0));
            });
        }
    }

    private void rebuildImageNow() {
        rebuildImageLaterTimer.stop();
        if (!active)
            return;
        if (image == null && universe == null)
            return;
        if (universe == null) {
            initializeUniverse();
        }
        if (universe != null) {
            if (image != null) {
                viewerRunnerQueue.cancelIf(run -> run instanceof ImageLoaderRun);
                int resolutionFactor = image3DRendererSettings.getResamplingFactor(image.getImage());
                renderInfoLabel.setText((int) Math.ceil(image3DRendererSettings.getExpectedMemoryAllocationMegabytes(image.getImage())) + "MB / " + resolutionFactor);
                imageLoaderRun = new ImageLoaderRun(this, image, image3DRendererSettings.getRenderType(), resolutionFactor);
                viewerRunnerQueue.enqueue(imageLoaderRun);
            }
        }
    }

    private void initializeUniverse() {
        if (universe == null && universeInitializerRun == null) {
            setRendererStatus(RendererStatus.Initializing);
            universeInitializerRun = new UniverseInitializerRun(this);
            viewerRunnerQueue.enqueue(universeInitializerRun);
        }
    }

    public RendererStatus getRendererStatus() {
        return rendererStatus;
    }

    public void setRendererStatus(RendererStatus rendererStatus) {
        this.rendererStatus = rendererStatus;
        rendererStatusPanel.removeAll();
        initializationThrobberIcon.stop();
        switch (rendererStatus) {
            case Uninitialized: {
                // Do nothing
            }
            break;
            case Initializing: {
                rendererStatusPanel.add(createMessageLabel("Initializing renderer ...", "Please wait a moment until the 3D viewer is ready", initializationThrobberIcon), BorderLayout.CENTER);
                initializationThrobberIcon.start();
            }
            break;
            case ErrorGeneric: {
                rendererStatusPanel.add(createMessageLabel("Could not initialize 3D viewer!", "Something went wrong while loading the 3D viewer. Please check the log for more information.",
                        UIUtils.getIconFromResources("emblems/emblem-rabbitvcs-conflicted.png")), BorderLayout.CENTER);
            }
            break;
            case Initialized: {
                // Do nothing
            }
            break;
        }
        rendererStatusPanel.revalidate();
        rendererStatusPanel.repaint();
    }

    private JLabel createMessageLabel(String text, String subText, Icon icon) {
        JLabel label = new JLabel("<html><strong>" + text + "</strong><br/>" + subText, icon, JLabel.LEFT);
        label.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4),
                BorderFactory.createCompoundBorder(UIUtils.createControlBorder(),
                        BorderFactory.createEmptyBorder(16, 16, 16, 16))));
        return label;
    }

    public CustomImage3DUniverse getUniverse() {
        return universe;
    }

    @Override
    public void dispose() {
        stopAnimations();
        setImage(null);
        rebuildImageLaterTimer.stop();
        viewerRunnerQueue.cancelAll();
        imageLoaderRun = null;
        universeInitializerRun = null;
        if (universe != null) {
            universe.removeUniverseListener(this);

//        // Must remove the listener so this instance can be garbage
//        // collected and removed from the Canvas3D, overcomming the limit
//        // of 32 total Canvas3D instances.
//        try {
//            final Method m =
//                    SimpleUniverse.class.getMethod("removeRenderingErrorListener",
//                            RenderingErrorListener.class);
//            m.invoke(universe, new Object[] { error_listener });
//        }
//        catch (final Exception ex) {
//            System.out.println("Could NOT remove the RenderingErrorListener!");
//            ex.printStackTrace();
//        }
//        imp_updater.quit();

            try {
                universe.cleanup();
            } catch (Throwable e) {
                e.printStackTrace();
            }
//        canvas3D.flush();
            universe = null;
        }
    }

    private void onUniverseInitialized(CustomImage3DUniverse universe, RendererStatus status) {
        if (universe != null) {
            this.universe = universe;
            universe.addUniverseListener(this);
            Canvas3D canvas3D = universe.getCanvas();

            try {
                GraphicsConfiguration graphicsConfiguration = getBestConfigurationOnSameDevice(SwingUtilities.getWindowAncestor(this));

                // Workaround for https://bugs.java.com/view_bug.do?bug_id=8003398
                {
                    Method method = Component.class.getDeclaredMethod("setGraphicsConfiguration", GraphicsConfiguration.class);
                    method.setAccessible(true);
                    method.invoke(canvas3D, graphicsConfiguration);
                }

                viewerCanvasPanel.add(canvas3D, BorderLayout.CENTER);
                universe.ui.setHandTool();

                setupUniverseDefaults();

                revalidate();
                repaint();

                setRendererStatus(RendererStatus.Initialized);
                rebuildImageNow();

                for (JIPipeImageViewerPlugin3D plugin3D : getImageViewer().getPlugins3D()) {
                    plugin3D.onViewerUniverseReady();
                }

            } catch (Throwable e) {
                e.printStackTrace();
                setRendererStatus(RendererStatus.ErrorGeneric);
            }
        } else {
            setRendererStatus(status);
        }
    }

    private void setupUniverseDefaults() {
        universe.addInteractiveBehavior(new CustomInteractiveBehavior(this, universe));
    }

    private void onImageContentReady(List<Content> contents, int resamplingFactor) {
        if (universe != null) {
            if (currentImageContents != null) {
                for (Content currentImageContent : currentImageContents) {
                    universe.removeContent(currentImageContent.getName());
                }
            }
            currentImageContents = contents;
            currentImageContentsResamplingFactor = resamplingFactor;

            // Create unique name because removal depends on the name?!
            for (Content content : contents) {
                universe.addContent(content);
            }

            universe.fixWeirdRendering();
            scheduleUpdateLutAndCalibration();
            universe.setAutoAdjustView(false);

            for (JIPipeImageViewerPlugin3D plugin3D : getImageViewer().getPlugins3D()) {
                plugin3D.onImageContentReady(contents);
            }
        }
    }

    @Override
    public void onRunnableFinished(JIPipeRunnable.FinishedEvent event) {
        if (event.getRun() instanceof UniverseInitializerRun) {
            UniverseInitializerRun run = (UniverseInitializerRun) event.getRun();
            onUniverseInitialized((run).getUniverse(), (run).getStatus());
        } else if (event.getRun() instanceof ImageLoaderRun) {
            ImageLoaderRun run = (ImageLoaderRun) event.getRun();
            if (run.imageWasConvertedTo8Bit) {
                getImageViewer().setImageData(run.imageData);
            } else {
                onImageContentReady((run).getContents(), run.getResamplingFactor());
            }
        }
    }

    @Override
    public void transformationStarted(View view) {

    }

    @Override
    public void transformationUpdated(View view) {

    }

    @Override
    public void transformationFinished(View view) {

    }

    @Override
    public void contentAdded(Content c) {

    }

    @Override
    public void contentRemoved(Content c) {

    }

    @Override
    public void contentChanged(Content c) {

    }

    @Override
    public void contentSelected(Content c) {

    }

    @Override
    public void canvasResized() {

    }

    @Override
    public void universeClosed() {

    }

    public void showDataError(JLabel label) {
        dataStatusPanel.removeAll();
        if (label != null) {
            dataStatusPanel.add(label);
        }
        dataStatusPanel.revalidate();
        dataStatusPanel.repaint();
    }

    public ImageStatistics getCurrentImageStats() {
        if (imageStatistics == null && image != null) {
            imageStatistics = new StackStatistics(image.getImage());
        }
        return imageStatistics;
    }

    private void initializeAnimationControls() {
        frameSlider.addChangeListener(e -> {
            if (!isUpdatingSliders)
                setAnimationFrameFromSlider();
        });
        animationTimer.setRepeats(true);
        animationFPSControl.addChangeListener(e -> {
            double fps = ((SpinnerNumberModel) animationFPSControl.getModel()).getNumber().doubleValue();
            if (settings != null) {
                settings.setDefaultAnimationFPS(fps);
                if (!JIPipe.NO_SETTINGS_AUTOSAVE) {
                    JIPipe.getSettings().saveApplicationSettings();
                }
            }
            stopAnimations();
            animationTimer.setDelay(Math.max(1, (int) (1000.0 / fps)));
        });
        animationFrameToggle.addActionListener(e -> {
            if (animationFrameToggle.isSelected()) {
                animationTimer.start();
            }
        });
    }

    private void setAnimationFrameFromSlider() {
        if (universe != null && image != null && currentImageContents != null) {
            universe.showTimepoint(frameSlider.getValue() - 1);
        }
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
        UIUtils.makeFlat25x25(animation);
        JPanel descriptionPanel = new JPanel();
        descriptionPanel.setLayout(new BoxLayout(descriptionPanel, BoxLayout.X_AXIS));

        JButton editButton = new JButton(UIUtils.getIconFromResources("actions/go-jump.png"));
        editButton.setToolTipText("Jump to slice");
        UIUtils.makeFlat25x25(editButton);
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
        UIUtils.makeFlat25x25(lastFrame);
        lastFrame.setToolTipText("Go one slice back");
        lastFrame.addActionListener(e -> {
            decrementSlider(slider);
        });
        rightPanel.add(lastFrame);

        JButton nextFrame = new JButton(UIUtils.getIconFromResources("actions/caret-right.png"));
        UIUtils.makeFlat25x25(nextFrame);
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

    private void stopAnimations() {
        animationTimer.stop();
        animationFrameToggle.setSelected(false);
    }

    private void animateNextSlice() {
        if (!isDisplayable()) {
            stopAnimations();
            return;
        }
        if (animationFrameToggle.isSelected()) {
            int currentFrame = frameSlider.getValue();
            int newIndex = (currentFrame % image.getNFrames()) + 1;
            frameSlider.setValue(newIndex);
        } else {
            stopAnimations();
        }
    }

    public void scheduleUpdateLutAndCalibration() {
        if (currentUpdateCalibrationRun != null) {
            viewerRunnerQueue.cancel(currentUpdateCalibrationRun);
            currentUpdateCalibrationRun = null;
        }
        currentUpdateCalibrationRun = new UpdateLutAndCalibrationRun(getImageViewer(), currentImageContents);
        viewerRunnerQueue.enqueue(currentUpdateCalibrationRun);
    }

    public void updateLutAndCalibrationNow() {
        if (currentUpdateCalibrationRun != null) {
            viewerRunnerQueue.cancel(currentUpdateCalibrationRun);
            currentUpdateCalibrationRun = null;
        }
        UpdateLutAndCalibrationRun run = new UpdateLutAndCalibrationRun(getImageViewer(), currentImageContents);
        run.run();
    }

    @Override
    public void componentResized(ComponentEvent e) {
        if (universe != null) {
            universe.fixBlankCanvasLater();
        }
    }

    @Override
    public void componentMoved(ComponentEvent e) {

    }

    @Override
    public void componentShown(ComponentEvent e) {

    }

    @Override
    public void componentHidden(ComponentEvent e) {

    }

    public enum RendererStatus {
        Uninitialized,
        Initializing,
        ErrorGeneric,
        Initialized
    }

    public static class UpdateLutAndCalibrationRun extends AbstractJIPipeRunnable {

        private final JIPipeImageViewer imageViewer;

        private final List<Content> currentImageContents;

        public UpdateLutAndCalibrationRun(JIPipeImageViewer imageViewer, List<Content> currentImageContents) {
            this.imageViewer = imageViewer;
            this.currentImageContents = currentImageContents;
        }

        @Override
        public String getTaskLabel() {
            return "Update LUT and calibration";
        }

        @Override
        public void run() {
            if (currentImageContents != null) {
                CalibrationPlugin3D calibrationPlugin3D = imageViewer.getPlugin(CalibrationPlugin3D.class);
                int min = 0;
                int max = 255;
                if (calibrationPlugin3D != null) {
                    double[] minMax = calibrationPlugin3D.calculateCalibration();
                    min = (int) minMax[0];
                    max = (int) minMax[1];
                }

                if (max < min) {
                    int x = max;
                    max = min;
                    min = x;
                }

                LUTManagerPlugin3D lutManagerPlugin3D = imageViewer.getPlugin(LUTManagerPlugin3D.class);
                OpacityManagerPlugin3D opacityManagerPlugin3D = imageViewer.getPlugin(OpacityManagerPlugin3D.class);

                for (int channel = 0; channel < currentImageContents.size(); channel++) {
                    if (getProgressInfo().isCancelled())
                        return;
                    Content currentImageContent = currentImageContents.get(channel);
                    LUT lut;
                    LUT alphaLut;
                    if (lutManagerPlugin3D != null && channel < lutManagerPlugin3D.getLutEditors().size()) {
                        lut = lutManagerPlugin3D.getLutEditors().get(channel).getLUT();
                    } else {
                        lut = LUT.createLutFromColor(Color.WHITE);
                    }
                    if (opacityManagerPlugin3D != null && channel < opacityManagerPlugin3D.getAlphaLutEditors().size()) {
                        alphaLut = opacityManagerPlugin3D.getAlphaLutEditors().get(channel).getLUT();
                    } else {
                        alphaLut = LUT.createLutFromColor(Color.WHITE);
                    }
                    byte[] reds = new byte[256];
                    byte[] greens = new byte[256];
                    byte[] blues = new byte[256];
                    byte[] alphas = new byte[256];
                    lut.getReds(reds);
                    lut.getGreens(greens);
                    lut.getBlues(blues);
                    alphaLut.getReds(alphas);
                    int[] newReds = new int[256];
                    int[] newGreens = new int[256];
                    int[] newBlues = new int[256];
                    int[] newAlphas = new int[256];

//                int maxAlpha = 0;
//                for (byte alpha : alphas) {
//                    int alpha_ = Byte.toUnsignedInt(alpha);
//                    maxAlpha = Math.max(alpha_, maxAlpha);
//                }
//                if(maxAlpha == 0) {
//                    maxAlpha = 1;
//                }

                    for (int i = 0; i < 256; i++) {
                        if (i >= min) {
                            int normIndex = Math.max(0, Math.min(255, (int) (255.0 * i / (max - min))));
                            newAlphas[i] = Byte.toUnsignedInt(alphas[normIndex]);
                            newReds[i] = Byte.toUnsignedInt(reds[normIndex]);
                            newGreens[i] = Byte.toUnsignedInt(greens[normIndex]);
                            newBlues[i] = Byte.toUnsignedInt(blues[normIndex]);
                        }
                    }
                    currentImageContent.setThreshold(min);
                    currentImageContent.setLUT(newReds, newGreens, newBlues, newAlphas);
                }

            }
        }
    }

    public static class UniverseInitializerRun extends AbstractJIPipeRunnable {

        private final ImageViewerPanel3D panel3D;
        private RendererStatus status = RendererStatus.ErrorGeneric;

        private CustomImage3DUniverse universe;

        public UniverseInitializerRun(ImageViewerPanel3D panel3D) {
            this.panel3D = panel3D;
        }

        @Override
        public String getTaskLabel() {
            return "Initializing 3D viewer";
        }

        public RendererStatus getStatus() {
            return status;
        }

        public CustomImage3DUniverse getUniverse() {
            return universe;
        }

        @Override
        public void run() {
            try {
                universe = new CustomImage3DUniverse();
                status = RendererStatus.Initialized;
            } catch (Throwable e) {
                e.printStackTrace();
                status = RendererStatus.ErrorGeneric;
            }
        }
    }

    public static class ImageLoaderRun extends AbstractJIPipeRunnable {

        private final ImageViewerPanel3D viewerPanel3D;
        private final Image3DRenderType renderType;
        private final int resamplingFactor;
        private final List<Content> contents = new ArrayList<>();
        private ImagePlusData imageData;
        private boolean imageWasConvertedTo8Bit;

        public ImageLoaderRun(ImageViewerPanel3D viewerPanel3D, ImagePlusData imageData, Image3DRenderType renderType, int resamplingFactor) {
            this.viewerPanel3D = viewerPanel3D;
            this.imageData = imageData;
            this.renderType = renderType;
            this.resamplingFactor = resamplingFactor;
        }

        @Override
        public String getTaskLabel() {
            return "Preprocessing image";
        }

        @Override
        public void run() {
            ImagePlus imagePlus = imageData.getImage();
            if (imagePlus.getType() != ImagePlus.COLOR_RGB && imagePlus.getType() != ImagePlus.GRAY8) {
                LUT[] channelLUT = ImageJUtils.getChannelLUT(imagePlus);
                getProgressInfo().log("Converting image to 8-bit");
                imagePlus = ImageJUtils.convertToGreyscale8UIfNeeded(imagePlus);
                ImagePlusData newImageData = new ImagePlusData(imagePlus);
                newImageData.copyMetadata(imageData);
                ImageJUtils.setChannelLUT(newImageData.getImage(), channelLUT);

                imageData = newImageData;
                imageWasConvertedTo8Bit = true;
                return;
            }
            ImagePlus generatedImage = imagePlus;
            for (JIPipeImageViewerPlugin3D plugin3D : viewerPanel3D.getImageViewer().getPlugins3D()) {
                generatedImage = plugin3D.preprocess(generatedImage, getProgressInfo().resolveAndLog("Preprocessing with " + plugin3D.getClass().getName()));
            }
            if (generatedImage.getType() != ImagePlus.COLOR_RGB && generatedImage.getType() != ImagePlus.GRAY8) {
                getProgressInfo().log("Converting generated image to 8-bit");
                generatedImage = ImageJUtils.convertToGreyscale8UIfNeeded(generatedImage);
            }
            getProgressInfo().log("Converting into " + renderType);

            for (int c = 0; c < generatedImage.getNChannels(); c++) {
                if (getProgressInfo().isCancelled())
                    return;
                Map<ImageSliceIndex, ImageProcessor> processorMap = new HashMap<>();
                int finalC = c;
                ImageJUtils.forEachIndexedZCTSlice(generatedImage, (ip, index) -> {
                    if (index.getC() == finalC) {
                        processorMap.put(new ImageSliceIndex(0, index.getZ(), index.getT()), ip);
                    }
                }, getProgressInfo().resolve("c=" + c));
                ImagePlus forChannel = ImageJUtils.mergeMappedSlices(processorMap);
                forChannel.copyScale(imagePlus);

                if (forChannel.getType() == ImagePlus.GRAY8) {
                    ImageStatistics statistics = new StackStatistics(forChannel);

                    if (statistics.max == 0) {
                        // Workaround: viewer does not work with empty images. Convert to RGB
                        forChannel = ImageJUtils.convertToColorRGBIfNeeded(forChannel);
                    }
                }
                Content content = ContentCreator.createContent("Image-Channel-" + finalC + "-" + UUID.randomUUID(),
                        forChannel,
                        renderType.getNativeValue(),
                        resamplingFactor,
                        0,
                        new Color3f(1, 1, 1),
                        0,
                        new boolean[]{true, true, true});
                contents.add(content);
            }
        }

        public List<Content> getContents() {
            return contents;
        }

        public boolean isImageWasConvertedTo8Bit() {
            return imageWasConvertedTo8Bit;
        }

        public ImagePlusData getImageData() {
            return imageData;
        }

        public int getResamplingFactor() {
            return resamplingFactor;
        }
    }




}
