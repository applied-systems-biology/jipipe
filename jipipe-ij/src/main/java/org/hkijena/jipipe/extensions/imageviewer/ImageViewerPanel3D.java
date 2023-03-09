package org.hkijena.jipipe.extensions.imageviewer;

import com.google.common.eventbus.Subscribe;
import ij.IJ;
import ij.ImagePlus;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import ij.process.StackStatistics;
import ij3d.*;
import org.hkijena.jipipe.api.AbstractJIPipeRunnable;
import org.hkijena.jipipe.api.JIPipeRunnable;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageSliceIndex;
import org.hkijena.jipipe.extensions.imageviewer.plugins3d.CalibrationPlugin3D;
import org.hkijena.jipipe.extensions.imageviewer.plugins3d.LUTManagerPlugin3D;
import org.hkijena.jipipe.extensions.imageviewer.runs.RawImage2DExporterRun;
import org.hkijena.jipipe.extensions.imageviewer.utils.viewer3d.Image3DRenderType;
import org.hkijena.jipipe.extensions.imageviewer.utils.viewer3d.Image3DRendererSettings;
import org.hkijena.jipipe.extensions.imageviewer.utils.viewer3d.universe.CustomImage3DUniverse;
import org.hkijena.jipipe.extensions.imageviewer.utils.viewer3d.universe.CustomInteractiveBehavior;
import org.hkijena.jipipe.extensions.imageviewer.utils.viewer3d.SnapshotSettings;
import org.hkijena.jipipe.extensions.imageviewer.utils.viewer3d.StandardView;
import org.hkijena.jipipe.extensions.settings.FileChooserSettings;
import org.hkijena.jipipe.extensions.settings.ImageViewerUISettings;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.JIPipeWorkbenchAccess;
import org.hkijena.jipipe.ui.components.FormPanel;
import org.hkijena.jipipe.ui.components.icons.NewThrobberIcon;
import org.hkijena.jipipe.ui.components.markdown.MarkdownDocument;
import org.hkijena.jipipe.ui.components.tabs.DocumentTabPane;
import org.hkijena.jipipe.ui.parameters.ParameterPanel;
import org.hkijena.jipipe.ui.running.JIPipeRunExecuterUI;
import org.hkijena.jipipe.ui.running.JIPipeRunnerQueue;
import org.hkijena.jipipe.ui.running.JIPipeRunnerQueueUI;
import org.hkijena.jipipe.utils.AutoResizeSplitPane;
import org.hkijena.jipipe.utils.BufferedImageUtils;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.ui.CopyImageToClipboard;
import org.scijava.Disposable;
import org.scijava.java3d.Canvas3D;
import org.scijava.java3d.GraphicsConfigTemplate3D;
import org.scijava.java3d.View;
import org.scijava.vecmath.Color3f;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.*;
import java.util.List;

public class ImageViewerPanel3D extends JPanel implements JIPipeWorkbenchAccess, Disposable, UniverseListener {
    private final JIPipeImageViewer imageViewer;
    private final ImageViewerUISettings settings;
    private ImagePlus image;

    private JComponent currentContentPanel;
    private final JPanel rendererStatusPanel = new JPanel(new BorderLayout());
    private final JPanel dataStatusPanel = new JPanel(new BorderLayout());

    private final JPanel viewerPanel = new JPanel(new BorderLayout());
    private final JPanel viewerCanvasPanel = new JPanel(new BorderLayout());
    private CustomImage3DUniverse universe;
    private boolean active = false;

    private final JToolBar toolBar = new JToolBar();
    private final JToggleButton enableSideBarButton = new JToggleButton();
    private RendererStatus rendererStatus = RendererStatus.Uninitialized;

    private final DocumentTabPane tabPane = new DocumentTabPane();

    private UniverseInitializerRun universeInitializerRun;

    private final NewThrobberIcon initializationThrobberIcon = new NewThrobberIcon(this);
    private ImageLoaderRun imageLoaderRun;
    private List<Content> currentImageContents;

    private final Timer updateImageLaterTimer;

    private final Map<String, FormPanel> formPanels = new HashMap<>();
    private ImageStatistics imageStatistics;

    private final JIPipeRunnerQueue viewerRunnerQueue = new JIPipeRunnerQueue("3D viewer");

    private final Image3DRendererSettings image3DRendererSettings = new Image3DRendererSettings();

    private final JLabel renderInfoLabel = new JLabel("No image",
            UIUtils.getIconFromResources("devices/video-display.png"), JLabel.LEFT);

    public ImageViewerPanel3D(JIPipeImageViewer imageViewer) {
        this.imageViewer = imageViewer;
        this.settings = imageViewer.getSettings();
        this.updateImageLaterTimer = new Timer(1000, e -> updateImageNow());
        updateImageLaterTimer.setRepeats(false);
        initialize();
        updateSideBar();

        // Register events
        viewerRunnerQueue.getEventBus().register(this);
        image3DRendererSettings.addParameterChangeListener(e -> updateImageLater());
    }

    public Image3DRendererSettings getImage3DRendererSettings() {
        return image3DRendererSettings;
    }

    private void initialize() {
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
                new Insets(0,0,0,0),
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
                new Insets(0,0,0,0),
                0,
                0));
        viewerPanel.add(messagePanel, BorderLayout.NORTH);

        JToolBar contentStatusBar = new JToolBar();
        contentStatusBar.setFloatable(false);
        viewerPanel.add(contentStatusBar, BorderLayout.SOUTH);

        JIPipeRunnerQueueUI runnerQueueUI = new JIPipeRunnerQueueUI(getWorkbench(), viewerRunnerQueue);
        runnerQueueUI.makeFlat();
        contentStatusBar.add(runnerQueueUI);
        contentStatusBar.add(Box.createHorizontalGlue());
        contentStatusBar.add(new JLabel("Rotate", UIUtils.getIconFromResources("actions/input-mouse-click-left.png"), JLabel.LEFT));
        contentStatusBar.add(Box.createHorizontalStrut(8));
        contentStatusBar.add(UIUtils.createVerticalSeparator());
        contentStatusBar.add(Box.createHorizontalStrut(8));
        contentStatusBar.add(new JLabel("+",UIUtils.getIconFromResources("actions/tap-extract.png"), JLabel.LEFT));
        contentStatusBar.add(new JLabel("Move", UIUtils.getIconFromResources("actions/input-mouse-click-left.png"), JLabel.LEFT));
        contentStatusBar.add(Box.createHorizontalStrut(8));
        contentStatusBar.add(UIUtils.createVerticalSeparator());
        contentStatusBar.add(Box.createHorizontalStrut(8));
        contentStatusBar.add(new JLabel("Zoom", UIUtils.getIconFromResources("actions/input-mouse-click-middle.png"), JLabel.LEFT));

        contentStatusBar.add(Box.createHorizontalStrut(8));
        contentStatusBar.add(UIUtils.createVerticalSeparator());
        contentStatusBar.add(Box.createHorizontalStrut(8));
        contentStatusBar.add(renderInfoLabel);
        contentStatusBar.add(Box.createHorizontalStrut(8));

        JPopupMenu.setDefaultLightWeightPopupEnabled(false);

        initializeToolbar();
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
            }
            updateSideBar();
        });
        toolBar.add(enableSideBarButton);
    }

    private void initializeResetViewMenu() {
        JButton resetViewButton = new JButton("Reset view", UIUtils.getIconFromResources("actions/view-restore.png"));
        JPopupMenu resetViewMenu = UIUtils.addPopupMenuToComponent(resetViewButton);
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

    private void updateSideBar() {
        if (currentContentPanel != null) {
            remove(currentContentPanel);
        }
        if (enableSideBarButton.isSelected()) {
            JSplitPane splitPane = new AutoResizeSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                    viewerPanel,
                    tabPane, AutoResizeSplitPane.RATIO_3_TO_1);
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

        UIUtils.addPopupMenuToComponent(exportMenuButton, exportMenu);
        toolBar.add(exportMenuButton);
    }

    private void exportSnapshotToPNG() {
        if(universe != null) {
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

                if(ParameterPanel.showDialog(getWorkbench(), this, snapshotSettings, new MarkdownDocument(), "Create snapshot", ParameterPanel.WITH_SCROLLING)) {
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
        if(universe != null) {
            Canvas3D canvas = universe.getCanvas();

            SnapshotSettings snapshotSettings = new SnapshotSettings();
            snapshotSettings.setWidth(canvas.getWidth());
            snapshotSettings.setHeight(canvas.getHeight());

            if(ParameterPanel.showDialog(getWorkbench(), this, snapshotSettings, new MarkdownDocument(), "Create snapshot", ParameterPanel.WITH_SCROLLING)) {
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
            JIPipeRunExecuterUI.runInDialog(this, new RawImage2DExporterRun(getImage(), path));
        }
    }

    public JToolBar getToolBar() {
        return toolBar;
    }

    @Override
    public JIPipeWorkbench getWorkbench() {
        return imageViewer.getWorkbench();
    }

    public JIPipeImageViewer getImageViewer() {
        return imageViewer;
    }

    public ImagePlus getImage() {
        return image;
    }

    public void setImage(ImagePlus image) {
        this.image = image;
        this.imageStatistics = null;
        if(!active) {
            return;
        }
        if(imageLoaderRun != null) {
            viewerRunnerQueue.cancel(imageLoaderRun);
        }
        if(universe != null) {
            universe.setAutoAdjustView(true);
            universe.removeAllContents();
        }
        updateImageNow();
        for (JIPipeImageViewerPlugin3D plugin3D : getImageViewer().getPlugins3D()) {
            plugin3D.onImageChanged();
        }
        refreshFormPanel();
    }

    public void activate() {
        if(!active) {
            active = true;
            updateImageNow();
            for (JIPipeImageViewerPlugin3D plugin3D : getImageViewer().getPlugins3D()) {
                plugin3D.onImageChanged();
            }
            refreshFormPanel();
        }
    }

    public void updateImageLater() {
        updateImageLaterTimer.restart();
    }

    public void refreshFormPanel() {
        Map<String, Integer> scrollValues = new HashMap<>();
        for (Map.Entry<String, FormPanel> entry : formPanels.entrySet()) {
            scrollValues.put(entry.getKey(), entry.getValue().getScrollPane().getVerticalScrollBar().getValue());
            entry.getValue().clear();
        }
        for (JIPipeImageViewerPlugin3D plugin : imageViewer.getPlugins3D()) {
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
            plugin.initializeSettingsPanel(formPanel);
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

    private void updateImageNow() {
        updateImageLaterTimer.stop();
        if(!active)
            return;
        if(image == null && universe == null)
            return;
        if(universe == null) {
            initializeUniverse();
        }
        if(universe != null) {
            if (image != null) {
                viewerRunnerQueue.cancelIf(run -> run instanceof ImageLoaderRun);
                int resolutionFactor = image3DRendererSettings.getResolutionFactor(image);
                renderInfoLabel.setText( (int)image3DRendererSettings.getExpectedMemoryAllocationMegabytes(image) + "MB / " + resolutionFactor);
                imageLoaderRun = new ImageLoaderRun(this, image, image3DRendererSettings.getRenderType(), resolutionFactor);
                viewerRunnerQueue.enqueue(imageLoaderRun);
            }
        }
    }

    private void initializeUniverse() {
        if(universe == null && universeInitializerRun == null) {
            setRendererStatus(RendererStatus.Initializing);
            universeInitializerRun = new UniverseInitializerRun(this);
            viewerRunnerQueue.enqueue(universeInitializerRun);
        }
    }

    public static GraphicsConfiguration getBestConfigurationOnSameDevice(Window frame){

        GraphicsConfiguration gc = frame.getGraphicsConfiguration();
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice[] gs = ge.getScreenDevices();
        GraphicsConfiguration good = null;

        GraphicsConfigTemplate3D gct = new GraphicsConfigTemplate3D();

        for(GraphicsDevice gd: gs){

            if(gd==gc.getDevice()){
                good = gct.getBestConfiguration(gd.getConfigurations());
                if(good!=null)
                    break;
            }
        }

        return good;
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
        JLabel label = new JLabel("<html><strong>" + text + "</strong><br/>" + subText, icon, JLabel.LEFT );
        label.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(4,4,4,4),
                BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(UIManager.getColor("Button.borderColor"), 1, true),
                BorderFactory.createEmptyBorder(16,16,16,16))));
        return label;
    }

    public CustomImage3DUniverse getUniverse() {
        return universe;
    }

    @Override
    public void dispose() {
        setImage(null);
        updateImageLaterTimer.stop();
        viewerRunnerQueue.cancelAll();
        if(universe != null) {
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
            }
            catch (Throwable e) {
                e.printStackTrace();
            }
//        canvas3D.flush();
            universe = null;
        }
    }

    private void onUniverseInitialized(CustomImage3DUniverse universe, RendererStatus status) {
        if(universe != null) {
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
                updateImageNow();

            } catch (Throwable e) {
                e.printStackTrace();
                setRendererStatus(RendererStatus.ErrorGeneric);
            }
        }
        else {
            setRendererStatus(status);
        }
    }

    private void setupUniverseDefaults() {
        universe.addInteractiveBehavior(new CustomInteractiveBehavior(this, universe));
    }

    private void onImageContentReady(List<Content> contents) {
        if(universe != null) {
            if(currentImageContents != null) {
                for (Content currentImageContent : currentImageContents) {
                    universe.removeContent(currentImageContent.getName());
                }
            }
            currentImageContents = contents;

            // Create unique name because removal depends on the name?!
            for (Content content : contents) {
                content.setName(content.getName() + "-" + UUID.randomUUID());
                universe.addContent(content);
            }

            universe.fixWeirdRendering();
            updateLutAndThreshold();
            universe.setAutoAdjustView(false);

            for (JIPipeImageViewerPlugin3D plugin3D : getImageViewer().getPlugins3D()) {
                plugin3D.onImageContentReady(contents);
            }
        }
    }

    @Subscribe
    public void onViewerRunnerQueueFinished(JIPipeRunnable.FinishedEvent event)  {
        if(event.getRun() instanceof UniverseInitializerRun) {
            UniverseInitializerRun run = (UniverseInitializerRun) event.getRun();
            onUniverseInitialized((run).getUniverse(), (run).getStatus());
        }
        else if(event.getRun() instanceof ImageLoaderRun) {
            ImageLoaderRun run = (ImageLoaderRun) event.getRun();
            if(run.imageWasConvertedTo8Bit) {
                getImageViewer().setImage(run.imagePlus);
            }
            else {
                onImageContentReady((run).getContents());
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
        if(label != null) {
            dataStatusPanel.add(label);
        }
        dataStatusPanel.revalidate();
        dataStatusPanel.repaint();
    }

    public ImageStatistics getCurrentImageStats() {
        if(imageStatistics == null && image != null) {
            imageStatistics = new StackStatistics(image);
        }
        return imageStatistics;
    }

    public void updateLutAndThreshold() {
        if(currentImageContents != null) {
            CalibrationPlugin3D calibrationPlugin3D = getImageViewer().getPlugin(CalibrationPlugin3D.class);
            double min = 0;
            double max = 255;
            if(calibrationPlugin3D != null) {
                double[] minMax = calibrationPlugin3D.calculateCalibration();
                min = minMax[0];
                max = minMax[1];
            }

            LUTManagerPlugin3D lutManagerPlugin3D = getImageViewer().getPlugin(LUTManagerPlugin3D.class);
            if(lutManagerPlugin3D != null) {
            }

            for (Content currentImageContent : currentImageContents) {
                currentImageContent.setThreshold((int) min);
            }

        }
    }

    public enum RendererStatus {
        Uninitialized,
        Initializing,
        ErrorGeneric,
        Initialized
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
            }
            catch (Throwable e) {
                e.printStackTrace();
                status = RendererStatus.ErrorGeneric;
            }
        }
    }

    public static class ImageLoaderRun extends AbstractJIPipeRunnable {

        private final ImageViewerPanel3D viewerPanel3D;

        private ImagePlus imagePlus;

        private final Image3DRenderType renderType;

        private final int resolutionFactor;

        private boolean imageWasConvertedTo8Bit;

        private List<Content> contents = new ArrayList<>();

        public ImageLoaderRun(ImageViewerPanel3D viewerPanel3D, ImagePlus imagePlus, Image3DRenderType renderType, int resolutionFactor) {
            this.viewerPanel3D = viewerPanel3D;
            this.imagePlus = imagePlus;
            this.renderType = renderType;
            this.resolutionFactor = resolutionFactor;
        }

        @Override
        public String getTaskLabel() {
            return "Preprocessing image";
        }

        @Override
        public void run() {
            if(imagePlus.getType() != ImagePlus.COLOR_RGB && imagePlus.getType() !=  ImagePlus.GRAY8) {
                getProgressInfo().log("Converting image to 8-bit");
                imagePlus = ImageJUtils.convertToGreyscale8UIfNeeded(imagePlus);
                imageWasConvertedTo8Bit = true;
                return;
            }
            ImagePlus generatedImage = imagePlus;
            for (JIPipeImageViewerPlugin3D plugin3D : viewerPanel3D.getImageViewer().getPlugins3D()) {
                generatedImage = plugin3D.preprocess(generatedImage, getProgressInfo().resolveAndLog("Preprocessing with " + plugin3D.getClass().getName()));
            }
            if(generatedImage.getType() != ImagePlus.COLOR_RGB && generatedImage.getType() !=  ImagePlus.GRAY8) {
                getProgressInfo().log("Converting generated image to 8-bit");
                generatedImage = ImageJUtils.convertToGreyscale8UIfNeeded(generatedImage);
            }
            getProgressInfo().log("Converting into " + renderType);

            for (int c = 0; c < generatedImage.getNChannels(); c++) {
                Map<ImageSliceIndex, ImageProcessor> processorMap = new HashMap<>();
                int finalC = c;
                ImageJUtils.forEachIndexedZCTSlice(generatedImage, (ip, index) -> {
                    if(index.getC() == finalC) {
                        processorMap.put(new ImageSliceIndex(0, index.getZ(), index.getT()), ip);
                    }
                }, getProgressInfo().resolve("c=" + c));
                ImagePlus forChannel = ImageJUtils.mergeMappedSlices(processorMap);
                Content content = ContentCreator.createContent("Image-c" + finalC,
                        forChannel,
                        renderType.getNativeValue(),
                        resolutionFactor,
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

        public ImagePlus getImagePlus() {
            return imagePlus;
        }
    }
}
