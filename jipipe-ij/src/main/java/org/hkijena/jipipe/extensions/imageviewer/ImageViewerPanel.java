package org.hkijena.jipipe.extensions.imageviewer;

import ij.ImagePlus;
import ij.gui.Roi;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.extensions.imageviewer.plugins2d.*;
import org.hkijena.jipipe.extensions.imageviewer.plugins2d.maskdrawer.MeasurementDrawerPlugin2D;
import org.hkijena.jipipe.extensions.imageviewer.plugins2d.roimanager.ROIManagerPlugin2D;
import org.hkijena.jipipe.extensions.settings.ImageViewerUISettings;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.JIPipeWorkbenchAccess;
import org.hkijena.jipipe.ui.cache.JIPipeCacheDataViewerWindow;
import org.hkijena.jipipe.utils.ReflectionUtils;
import org.hkijena.jipipe.utils.UIUtils;
import org.scijava.Disposable;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

public class ImageViewerPanel extends JPanel implements JIPipeWorkbenchAccess, Disposable {

    private static final Set<ImageViewerPanel> OPEN_PANELS = new HashSet<>();
    public static final List<Class<? extends ImageViewerPanelPlugin>> DEFAULT_PLUGINS = Arrays.asList(CalibrationPlugin2D.class,
            PixelInfoPlugin2D.class,
            LUTManagerPlugin2D.class,
            ROIManagerPlugin2D.class,
            AnimationSpeedPlugin2D.class,
            MeasurementDrawerPlugin2D.class,
            AnnotationInfoPlugin2D.class);
    private static ImageViewerPanel ACTIVE_PANEL = null;
    private final JIPipeWorkbench workbench;
    private final ImageViewerUISettings settings;
    private final Map<Class<?>, Object> contextObjects;
    private final JToolBar toolBar = new JToolBar();

    private final JPanel toolBarDynamicContent = new JPanel(new BorderLayout());

    private final JPanel dynamicContent = new JPanel(new BorderLayout());
    private final ImageViewerPanel2D imageViewerPanel2D;

    private final List<ImageViewerPanelPlugin> plugins = new ArrayList<>();

    private final List<ImageViewerPanelPlugin2D> plugins2D = new ArrayList<>();

    private final Map<Class<? extends ImageViewerPanelPlugin>, ImageViewerPanelPlugin> pluginMap = new HashMap<>();

    private ImagePlus image;

    private final JButton switchModeButton = new JButton();

    /**
     * Initializes a new image viewer
     *
     * @param workbench the workbench. Use {@link org.hkijena.jipipe.ui.JIPipeDummyWorkbench} if you do not have access to one.
     */
    public ImageViewerPanel(JIPipeWorkbench workbench, List<Class<? extends ImageViewerPanelPlugin>> pluginTypes, Map<Class<?>, Object> contextObjects) {
        this.workbench = workbench;
        this.contextObjects = contextObjects;
        if (JIPipe.getInstance() != null) {
            settings = ImageViewerUISettings.getInstance();
        } else {
            settings = null;
        }
        imageViewerPanel2D = new ImageViewerPanel2D(this);
        initializePlugins(pluginTypes);
        initialize();
        switchTo2D();
    }

    private void initializePlugins(List<Class<? extends ImageViewerPanelPlugin>> pluginTypes) {
        for (Class<? extends ImageViewerPanelPlugin> pluginType : pluginTypes) {
            Object plugin = ReflectionUtils.newInstance(pluginType, this);
            if(plugin instanceof ImageViewerPanelPlugin2D) {
                ImageViewerPanelPlugin2D plugin2D = (ImageViewerPanelPlugin2D) plugin;
                plugins.add(plugin2D);
                plugins2D.add(plugin2D);
                pluginMap.put(pluginType, plugin2D);
            }
        }
    }

    public static ImageViewerPanel createForCacheViewer(JIPipeCacheDataViewerWindow cacheDataViewerWindow) {
       return createForCacheViewer(cacheDataViewerWindow, Collections.emptyList());
    }

    public ImageViewerPanel2D getImageViewerPanel2D() {
        return imageViewerPanel2D;
    }

    public static ImageViewerPanel createForCacheViewer(JIPipeCacheDataViewerWindow cacheDataViewerWindow, List<Class<? extends ImageViewerPanelPlugin>> additionalPlugins) {
        Map<Class<?>, Object> contextObjects = new HashMap<>();
        ArrayList<Class<? extends ImageViewerPanelPlugin>> plugins = new ArrayList<>(DEFAULT_PLUGINS);
        plugins.addAll(additionalPlugins);
        contextObjects.put(JIPipeCacheDataViewerWindow.class, cacheDataViewerWindow);
        return new ImageViewerPanel(cacheDataViewerWindow.getWorkbench(),
                plugins,
                contextObjects);
    }

    private void initialize() {
        toolBar.setFloatable(false);
        setLayout(new BorderLayout());
        add(toolBar, BorderLayout.NORTH);

        // Switcher
        toolBar.add(switchModeButton);
        JPopupMenu switchModeMenu = UIUtils.addPopupMenuToComponent(switchModeButton);
        switchModeMenu.add(UIUtils.createMenuItem("Switch to 2D", "Display the image in 2D", UIUtils.getIconFromResources("data-types/imgplus-2d.png"), this::switchTo2D));
        switchModeMenu.add(UIUtils.createMenuItem("Switch to 3D", "Display the image in 3D", UIUtils.getIconFromResources("data-types/imgplus-3d.png"), this::switchTo3D));

        // Dynamic content
        add(dynamicContent, BorderLayout.CENTER);

        // Dynamic toolbar
        toolBarDynamicContent.setLayout(new BoxLayout(toolBarDynamicContent, BoxLayout.X_AXIS));
        toolBar.add(toolBarDynamicContent);

    }

    private void switchTo3D() {
        switchModeButton.setText("Display");
        switchModeButton.setIcon(UIUtils.getIconFromResources("data-types/imgplus-3d.png"));

        toolBarDynamicContent.removeAll();
        dynamicContent.removeAll();

        revalidate();
        repaint();
    }

    public void switchTo2D() {
        switchModeButton.setText("Display");
        switchModeButton.setIcon(UIUtils.getIconFromResources("data-types/imgplus-2d.png"));

        toolBarDynamicContent.removeAll();
        dynamicContent.removeAll();

        toolBarDynamicContent.add(imageViewerPanel2D.getToolBar(), BorderLayout.CENTER);
        dynamicContent.add(imageViewerPanel2D, BorderLayout.CENTER);

        revalidate();
        repaint();
    }

    public static ImageViewerPanel getActiveViewerPanel() {
        return ACTIVE_PANEL;
    }

    public static Set<ImageViewerPanel> getOpenViewerPanels() {
        return OPEN_PANELS;
    }

    public List<ImageViewerPanelPlugin> getPlugins() {
        return Collections.unmodifiableList(plugins);
    }

    public List<ImageViewerPanelPlugin2D> getPlugins2D() {
        return Collections.unmodifiableList(plugins2D);
    }

    @Override
    public JIPipeWorkbench getWorkbench() {
        return workbench;
    }

    public void setAsActiveViewerPanel() {
        ACTIVE_PANEL = this;
    }

    public void addToOpenPanels() {
        OPEN_PANELS.add(this);
    }

    /**
     * Opens the image in a new frame
     *
     * @param workbench the workbench
     * @param image     the image
     * @param title     the title
     * @return the panel
     */
    public static ImageViewerPanel showImage(JIPipeWorkbench workbench, ImagePlus image, String title) {
        ImageViewerPanel dataDisplay = new ImageViewerPanel(workbench,
                Arrays.asList(CalibrationPlugin2D.class,
                        PixelInfoPlugin2D.class,
                        LUTManagerPlugin2D.class,
                        ROIManagerPlugin2D.class,
                        AnimationSpeedPlugin2D.class,
                        MeasurementDrawerPlugin2D.class),
                Collections.emptyMap());
        dataDisplay.setImage(image);
        ImageViewerWindow window = new ImageViewerWindow(dataDisplay);
        window.setTitle(title);
        window.setVisible(true);
        return dataDisplay;
    }

    public void fitImageToScreen() {
        imageViewerPanel2D.fitImageToScreen();
    }

    @Override
    public void dispose() {
        if (ACTIVE_PANEL == this) {
            ACTIVE_PANEL = null;
        }
        OPEN_PANELS.remove(this);
        if(contextObjects != null) {
            contextObjects.clear();
        }
        image = null;
        imageViewerPanel2D.dispose();
    }

    public void setImage(ImagePlus image) {
        this.image = image;
        imageViewerPanel2D.setImage(image);
    }

    public ImagePlus getImage() {
        return image;
    }

    public ImageViewerPanel2D getViewerPanel2D() {
        return imageViewerPanel2D;
    }

    public <T> T getContextObject(Class<T> klass) {
        return (T)contextObjects.getOrDefault(klass, null);
    }

    public void setError(String errorMessage) {
        if(errorMessage != null) {
            JLabel errorLabel2D = new JLabel(errorMessage, UIUtils.getIconFromResources("emblems/no-data.png"), JLabel.LEFT);
            imageViewerPanel2D.getCanvas().setError(errorLabel2D);
        }
        else {
            imageViewerPanel2D.getCanvas().setError(null);
        }
    }

    public void clearRoi2D() {
        ROIManagerPlugin2D plugin = getPlugin(ROIManagerPlugin2D.class);
        if(plugin != null) {
            plugin.clearROIs(false);
        }
    }

    public void clearRoi3D() {
    }

    public JToolBar getToolBar() {
        return toolBar;
    }

    public <T> T getPlugin(Class<T> klass) {
        return (T) pluginMap.getOrDefault(klass, null);
    }

    public void addRoi2D(Collection<Roi> rois) {
        ROIManagerPlugin2D plugin = getPlugin(ROIManagerPlugin2D.class);
        if(plugin != null) {
            ROIListData roiListData = new ROIListData();
            roiListData.addAll(rois);
            plugin.importROIs(roiListData, false);
        }
    }

    public void addRoi2d(ROIListData rois) {
        ROIManagerPlugin2D plugin = getPlugin(ROIManagerPlugin2D.class);
        if(plugin != null) {
            plugin.importROIs(rois, false);
        }
    }
}
