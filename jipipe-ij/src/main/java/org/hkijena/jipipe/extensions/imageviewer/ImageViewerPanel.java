package org.hkijena.jipipe.extensions.imageviewer;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.ImageWindow;
import ij.gui.Roi;
import ij.measure.Calibration;
import ij.util.Tools;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.imageviewer.plugins2d.*;
import org.hkijena.jipipe.extensions.imageviewer.plugins2d.maskdrawer.MeasurementDrawerPlugin2D;
import org.hkijena.jipipe.extensions.imageviewer.plugins2d.roimanager.ROIManagerPlugin2D;
import org.hkijena.jipipe.extensions.settings.ImageViewerUISettings;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.JIPipeWorkbenchAccess;
import org.hkijena.jipipe.ui.cache.JIPipeCacheDataViewerWindow;
import org.hkijena.jipipe.utils.ReflectionUtils;
import org.hkijena.jipipe.utils.StringUtils;
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

    private final ImageViewerPanel3D imageViewerPanel3D;

    private final List<ImageViewerPanelPlugin> plugins = new ArrayList<>();

    private final List<ImageViewerPanelPlugin2D> plugins2D = new ArrayList<>();

    private final List<ImageViewerPanelPlugin3D> plugins3D = new ArrayList<>();

    private final Map<Class<? extends ImageViewerPanelPlugin>, ImageViewerPanelPlugin> pluginMap = new HashMap<>();

    private ImagePlus image;

    private final JButton switchModeButton = new JButton();

    private final JLabel imageInfoLabel = new JLabel();

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
        imageViewerPanel3D = new ImageViewerPanel3D(this);
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
            else if(plugin instanceof ImageViewerPanelPlugin3D) {
                ImageViewerPanelPlugin3D plugin3D = (ImageViewerPanelPlugin3D) plugin;
                plugins.add(plugin3D);
                plugins3D.add(plugin3D);
                pluginMap.put(pluginType, plugin3D);
            }
        }
    }

    public ImageViewerUISettings getSettings() {
        return settings;
    }

    public static ImageViewerPanel createForCacheViewer(JIPipeCacheDataViewerWindow cacheDataViewerWindow) {
       return createForCacheViewer(cacheDataViewerWindow, Collections.emptyList());
    }

    public ImageViewerPanel2D getImageViewerPanel2D() {
        return imageViewerPanel2D;
    }

    public ImageViewerPanel3D getImageViewerPanel3D() {
        return imageViewerPanel3D;
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

        // Shared image controls
        JButton openInImageJButton = new JButton("Open in ImageJ", UIUtils.getIconFromResources("apps/imagej.png"));
        openInImageJButton.addActionListener(e -> openInImageJ());

        toolBar.add(openInImageJButton);
        toolBar.add(Box.createHorizontalStrut(8));
        toolBar.add(imageInfoLabel);

        // Dynamic content
        add(dynamicContent, BorderLayout.CENTER);

        // Dynamic toolbar
        toolBarDynamicContent.setLayout(new BoxLayout(toolBarDynamicContent, BoxLayout.X_AXIS));
        toolBar.add(toolBarDynamicContent);
    }

    private void openInImageJ() {
        if (image != null) {
            String title = image.getTitle();
            ImagePlus duplicate = ImageJUtils.duplicate(image);
            duplicate.setTitle(title);
            duplicate.show();
        }
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
//        if (rotation != 0) {
//            s += " (Rotated " + rotation + "°)";
//        }
        imageInfoLabel.setText(s);
    }

    private void switchTo3D() {
        switchModeButton.setText("3D Viewer");
        switchModeButton.setIcon(UIUtils.getIconFromResources("data-types/imgplus-3d.png"));

        toolBarDynamicContent.removeAll();
        dynamicContent.removeAll();

        toolBarDynamicContent.add(imageViewerPanel3D.getToolBar(), BorderLayout.CENTER);
        dynamicContent.add(imageViewerPanel3D, BorderLayout.CENTER);

        revalidate();
        repaint();

        imageViewerPanel3D.activate();
    }

    public void switchTo2D() {
        switchModeButton.setText("2D Viewer");
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

    public List<ImageViewerPanelPlugin3D> getPlugins3D() {
        return Collections.unmodifiableList(plugins3D);
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
        imageViewerPanel3D.setImage(image);

        refreshImageInfo();
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
            JLabel errorLabel3D = new JLabel(errorMessage, UIUtils.getIconFromResources("emblems/no-data.png"), JLabel.LEFT);
            imageViewerPanel2D.getCanvas().setError(errorLabel2D);
            imageViewerPanel3D.showDataError(errorLabel3D);
        }
        else {
            imageViewerPanel2D.getCanvas().setError(null);
            imageViewerPanel3D.showDataError(null);
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
