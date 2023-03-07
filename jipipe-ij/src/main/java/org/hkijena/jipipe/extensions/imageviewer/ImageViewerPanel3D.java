package org.hkijena.jipipe.extensions.imageviewer;

import ij.ImagePlus;
import ij3d.*;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.imageviewer.runs.RawImage2DExporterRun;
import org.hkijena.jipipe.extensions.imageviewer.utils.viewer3d.CustomImage3DUniverse;
import org.hkijena.jipipe.extensions.imageviewer.utils.viewer3d.CustomInteractiveBehavior;
import org.hkijena.jipipe.extensions.imageviewer.utils.viewer3d.StandardView;
import org.hkijena.jipipe.extensions.settings.FileChooserSettings;
import org.hkijena.jipipe.extensions.settings.ImageViewerUISettings;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.JIPipeWorkbenchAccess;
import org.hkijena.jipipe.ui.components.icons.NewThrobberIcon;
import org.hkijena.jipipe.ui.components.tabs.DocumentTabPane;
import org.hkijena.jipipe.ui.running.JIPipeRunExecuterUI;
import org.hkijena.jipipe.utils.AutoResizeSplitPane;
import org.hkijena.jipipe.utils.UIUtils;
import org.jdesktop.swingx.JXStatusBar;
import org.scijava.Disposable;
import org.scijava.java3d.Canvas3D;
import org.scijava.java3d.GraphicsConfigTemplate3D;
import org.scijava.java3d.View;
import org.scijava.vecmath.Color3f;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.concurrent.ExecutionException;

public class ImageViewerPanel3D extends JPanel implements JIPipeWorkbenchAccess, Disposable, UniverseListener {
    private final ImageViewerPanel imageViewerPanel;
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

    private UniverseInitializer universeInitializer;

    private final NewThrobberIcon initializationThrobberIcon = new NewThrobberIcon(this);
    private ImageLoader imageLoader;

    private final NewThrobberIcon contentStatusLabelThrobberIcon = new NewThrobberIcon(this);

    private final JLabel contentStatusLabel = new JLabel();

    public ImageViewerPanel3D(ImageViewerPanel imageViewerPanel) {
        this.imageViewerPanel = imageViewerPanel;
        this.settings = imageViewerPanel.getSettings();
        initialize();
        updateSideBar();
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

        JXStatusBar contentStatusBar = new JXStatusBar();
        contentStatusLabel.setText("Waiting for the 3D viewer ...");
        viewerPanel.add(contentStatusBar, BorderLayout.SOUTH);
        contentStatusBar.add(contentStatusLabel);

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

//        exportMenu.addSeparator();
//
//        exportMenu.add(exportDisplayedScaleToggle);
//
//        JMenuItem exportCurrentSliceItem = new JMenuItem("Export snapshot of current slice", UIUtils.getIconFromResources("actions/viewimage.png"));
//        exportCurrentSliceItem.addActionListener(e -> exportCurrentSliceToPNG());
//        exportMenu.add(exportCurrentSliceItem);
//
//        exportAllSlicesItem = new JMenuItem("Export snapshot of all slices", UIUtils.getIconFromResources("actions/qlipper.png"));
//        exportAllSlicesItem.addActionListener(e -> exportAllSlicesToPNG());
//        exportMenu.add(exportAllSlicesItem);
//
//        exportMovieItem = new JMenuItem("Export movie", UIUtils.getIconFromResources("actions/filmgrain.png"));
//        exportMovieItem.addActionListener(e -> exportVideo());
//        exportMenu.add(exportMovieItem);
//
//        exportMenu.addSeparator();
//
//        JMenuItem copyCurrentSliceItem = new JMenuItem("Copy snapshot of current slice", UIUtils.getIconFromResources("actions/edit-copy.png"));
//        copyCurrentSliceItem.addActionListener(e -> copyCurrentSliceToClipboard());
//        exportMenu.add(copyCurrentSliceItem);

        UIUtils.addPopupMenuToComponent(exportMenuButton, exportMenu);
        toolBar.add(exportMenuButton);
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
        return imageViewerPanel.getWorkbench();
    }

    public ImageViewerPanel getImageViewerPanel() {
        return imageViewerPanel;
    }

    public ImagePlus getImage() {
        return image;
    }

    public void setImage(ImagePlus image) {
        this.image = image;
        updateImage();
    }

    public void activate() {
        if(!active) {
            active = true;
            updateImage();
        }
    }

    private void updateImage() {
        if(!active)
            return;
        if(image == null && universe == null)
            return;
        if(universe == null) {
            initializeUniverse();
        }
        if(universe != null) {
            universe.removeAllContents();
            if (image != null) {
                if(imageLoader != null) {
                    imageLoader.cancel(true);
                }
                imageLoader = new ImageLoader(this, image);
                imageLoader.execute();
                updateContentStatus();
            }
        }
    }

    private void initializeUniverse() {
        if(universe == null && universeInitializer == null) {
            setRendererStatus(RendererStatus.Initializing);
            universeInitializer = new UniverseInitializer(this);
            universeInitializer.execute();
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

    public void updateContentStatus() {
        if(imageLoader != null && !imageLoader.isDone()) {
            contentStatusLabel.setIcon(contentStatusLabelThrobberIcon);
            contentStatusLabel.setText("Preparing image to be displayed in 3D ...");
            contentStatusLabelThrobberIcon.start();
        }
        else {
            contentStatusLabel.setIcon(UIUtils.getIconFromResources("actions/check-circle.png"));
            contentStatusLabel.setText("Ready");
            contentStatusLabelThrobberIcon.stop();
        }
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
                updateImage();

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

    private void onContentReady(Content content) {
        if(universe != null) {
            universe.addContent(content);
        }
        updateContentStatus();
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

    }

    public enum RendererStatus {
        Uninitialized,
        Initializing,
        ErrorGeneric,
        Initialized
    }

    public enum RenderType {
        Volume(0),
        OrthoSlice(1),
        Surface(2),
        SurfacePlot2D(3),
        MultiOrthoSlices(4);

        private final int nativeValue;

        RenderType(int nativeValue) {

            this.nativeValue = nativeValue;
        }

        public int getNativeValue() {
            return nativeValue;
        }
    }

    public static class UniverseInitializer extends SwingWorker<CustomImage3DUniverse, Object> {

        private final ImageViewerPanel3D panel3D;
        private RendererStatus status = RendererStatus.ErrorGeneric;

        public UniverseInitializer(ImageViewerPanel3D panel3D) {
            this.panel3D = panel3D;
        }

        @Override
        protected CustomImage3DUniverse doInBackground() throws Exception {
            try {
                return new CustomImage3DUniverse();
            }
            catch (Throwable e) {
                status = RendererStatus.ErrorGeneric;
            }
            return null;
        }

        @Override
        protected void done() {
            CustomImage3DUniverse universe = null;
            try {
                universe = get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
            panel3D.onUniverseInitialized(universe, status);
        }
    }

    public static class ImageLoader extends SwingWorker<Content, Object> {

        private final ImageViewerPanel3D panel3D;

        private final ImagePlus imagePlus;

        public ImageLoader(ImageViewerPanel3D panel3D, ImagePlus imagePlus) {
            this.panel3D = panel3D;
            this.imagePlus = imagePlus;
        }

        @Override
        protected Content doInBackground() throws Exception {
            ImagePlus asRGB = ImageJUtils.renderToRGBWithLUTIfNeeded(imagePlus, new JIPipeProgressInfo());
            return ContentCreator.createContent("Image", asRGB, RenderType.Volume.nativeValue, 1, 0, new Color3f(1, 1, 1), 0, new boolean[]{true, true, true});
        }

        @Override
        protected void done() {
            if(!isCancelled()) {
                try {
                    Content content = get();
                    panel3D.onContentReady(content);
                } catch (InterruptedException | ExecutionException e) {
                }
            }
        }
    }
}
