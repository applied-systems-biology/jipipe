package org.hkijena.jipipe.extensions.imageviewer;

import ij.ImagePlus;
import ij3d.*;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.settings.ImageViewerUISettings;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.JIPipeWorkbenchAccess;
import org.hkijena.jipipe.ui.components.icons.NewThrobberIcon;
import org.hkijena.jipipe.ui.components.tabs.DocumentTabPane;
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
    private Image3DUniverse universe;
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

        JPanel messagePanel = new JPanel();
        messagePanel.setLayout(new BoxLayout(messagePanel, BoxLayout.Y_AXIS));
        messagePanel.add(rendererStatusPanel);
        messagePanel.add(dataStatusPanel);
        viewerPanel.add(messagePanel, BorderLayout.NORTH);

        JXStatusBar contentStatusBar = new JXStatusBar();
        contentStatusLabel.setText("Waiting for the 3D viewer ...");
        viewerPanel.add(contentStatusBar, BorderLayout.SOUTH);
        contentStatusBar.add(contentStatusLabel);

        initializeToolbar();
    }

    private void initializeToolbar() {
        toolBar.setFloatable(false);

        toolBar.add(Box.createHorizontalGlue());

        initializeExportMenu();
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

    private void onUniverseInitialized(Image3DUniverse universe, RendererStatus status) {
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

    public static class UniverseInitializer extends SwingWorker<Image3DUniverse, Object> {

        private final ImageViewerPanel3D panel3D;
        private RendererStatus status = RendererStatus.ErrorGeneric;

        public UniverseInitializer(ImageViewerPanel3D panel3D) {
            this.panel3D = panel3D;
        }

        @Override
        protected Image3DUniverse doInBackground() throws Exception {
            try {
                return new Image3DUniverse();
            }
            catch (Throwable e) {
                status = RendererStatus.ErrorGeneric;
            }
            return null;
        }

        @Override
        protected void done() {
            Image3DUniverse universe = null;
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
            return ContentCreator.createContent("Image", asRGB, RenderType.Volume.nativeValue, 8, 0, new Color3f(1, 1, 1), 0, new boolean[]{true, true, true});
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
