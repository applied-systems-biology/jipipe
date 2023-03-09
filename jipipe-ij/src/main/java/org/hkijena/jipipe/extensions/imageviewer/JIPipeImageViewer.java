package org.hkijena.jipipe.extensions.imageviewer;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.ImageWindow;
import ij.measure.Calibration;
import ij.util.Tools;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.data.JIPipeDataSource;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.imageviewer.plugins2d.*;
import org.hkijena.jipipe.extensions.imageviewer.plugins2d.maskdrawer.MeasurementDrawerPlugin2D;
import org.hkijena.jipipe.extensions.imageviewer.plugins2d.roimanager.ROIManagerPlugin2D;
import org.hkijena.jipipe.extensions.imageviewer.plugins3d.CalibrationPlugin3D;
import org.hkijena.jipipe.extensions.imageviewer.plugins3d.LUTManagerPlugin3D;
import org.hkijena.jipipe.extensions.imageviewer.plugins3d.RenderSettingsPlugin3D;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.JIPipeWorkbenchAccess;
import org.hkijena.jipipe.utils.ReflectionUtils;
import org.hkijena.jipipe.utils.UIUtils;
import org.scijava.Disposable;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

public class JIPipeImageViewer extends JPanel implements JIPipeWorkbenchAccess, Disposable {

    private static final Set<JIPipeImageViewer> OPEN_PANELS = new HashSet<>();
    public static final List<Class<? extends JIPipeImageViewerPlugin>> DEFAULT_PLUGINS = Arrays.asList(CalibrationPlugin2D.class,
            PixelInfoPlugin2D.class,
            LUTManagerPlugin2D.class,
            ROIManagerPlugin2D.class,
            AnimationSpeedPlugin2D.class,
            MeasurementDrawerPlugin2D.class,
            AnnotationInfoPlugin2D.class,
            CalibrationPlugin3D.class,
            LUTManagerPlugin3D.class,
            RenderSettingsPlugin3D.class);
    private static JIPipeImageViewer ACTIVE_PANEL = null;
    private final JIPipeWorkbench workbench;
    private final Map<Class<?>, Object> contextObjects;
    private final JToolBar toolBar = new JToolBar();

    private final JPanel toolBarDynamicContent = new JPanel(new BorderLayout());

    private final JPanel dynamicContent = new JPanel(new BorderLayout());
    private final ImageViewerPanel2D imageViewerPanel2D;

    private final ImageViewerPanel3D imageViewerPanel3D;

    private final List<JIPipeImageViewerPlugin> plugins = new ArrayList<>();

    private final List<JIPipeImageViewerPlugin2D> plugins2D = new ArrayList<>();

    private final List<JIPipeImageViewerPlugin3D> plugins3D = new ArrayList<>();

    private final Map<Class<? extends JIPipeImageViewerPlugin>, JIPipeImageViewerPlugin> pluginMap = new HashMap<>();

    private ImagePlus image;

    private final JButton switchModeButton = new JButton();

    private final JLabel imageInfoLabel = new JLabel();

    private final List<Object> overlays = new ArrayList<>();

    private JIPipeDataSource dataSource;

    /**
     * Initializes a new image viewer
     *
     * @param workbench the workbench. Use {@link org.hkijena.jipipe.ui.JIPipeDummyWorkbench} if you do not have access to one.
     */
    public JIPipeImageViewer(JIPipeWorkbench workbench, List<Class<? extends JIPipeImageViewerPlugin>> pluginTypes, Map<Class<?>, Object> contextObjects) {
        this.workbench = workbench;
        this.contextObjects = contextObjects;
        imageViewerPanel2D = new ImageViewerPanel2D(this);
        imageViewerPanel3D = new ImageViewerPanel3D(this);
        initializePlugins(pluginTypes);
        initialize();
        switchTo2D();
    }

    private void initializePlugins(List<Class<? extends JIPipeImageViewerPlugin>> pluginTypes) {
        for (Class<? extends JIPipeImageViewerPlugin> pluginType : pluginTypes) {
            Object plugin = ReflectionUtils.newInstance(pluginType, this);
            if(plugin instanceof JIPipeImageViewerPlugin2D) {
                JIPipeImageViewerPlugin2D plugin2D = (JIPipeImageViewerPlugin2D) plugin;
                plugins.add(plugin2D);
                plugins2D.add(plugin2D);
                pluginMap.put(pluginType, plugin2D);
            }
            else if(plugin instanceof JIPipeImageViewerPlugin3D) {
                JIPipeImageViewerPlugin3D plugin3D = (JIPipeImageViewerPlugin3D) plugin;
                plugins.add(plugin3D);
                plugins3D.add(plugin3D);
                pluginMap.put(pluginType, plugin3D);
            }
        }
    }

    public ImageViewerPanel2D getImageViewerPanel2D() {
        return imageViewerPanel2D;
    }

    public ImageViewerPanel3D getImageViewerPanel3D() {
        return imageViewerPanel3D;
    }

    public List<Object> getOverlays() {
        return Collections.unmodifiableList(overlays);
    }

    public void clearOverlays() {
        while(!overlays.isEmpty()) {
            Object o = overlays.get(0);
            overlays.remove(o);
            for (JIPipeImageViewerPlugin plugin : plugins) {
                plugin.onOverlayRemoved(o);
            }
        }
        for (JIPipeImageViewerPlugin plugin : plugins) {
            plugin.onOverlaysCleared();
        }
    }

    public void addOverlay(Object o) {
        overlays.add(o);
        for (JIPipeImageViewerPlugin plugin : plugins) {
            plugin.onOverlayAdded(o);
        }
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

    public static JIPipeImageViewer getActiveViewerPanel() {
        return ACTIVE_PANEL;
    }

    public static Set<JIPipeImageViewer> getOpenViewerPanels() {
        return OPEN_PANELS;
    }

    public List<JIPipeImageViewerPlugin> getPlugins() {
        return Collections.unmodifiableList(plugins);
    }

    public List<JIPipeImageViewerPlugin2D> getPlugins2D() {
        return Collections.unmodifiableList(plugins2D);
    }

    public List<JIPipeImageViewerPlugin3D> getPlugins3D() {
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

    public JIPipeDataSource getDataSource() {
        return dataSource;
    }

    public void setDataSource(JIPipeDataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Opens the image in a new frame
     *
     * @param workbench the workbench
     * @param image     the image
     * @param title     the title
     * @return the panel
     */
    public static JIPipeImageViewer showImage(JIPipeWorkbench workbench, ImagePlus image, String title) {
        JIPipeImageViewer dataDisplay = new JIPipeImageViewer(workbench,
                Arrays.asList(CalibrationPlugin2D.class,
                        PixelInfoPlugin2D.class,
                        LUTManagerPlugin2D.class,
                        ROIManagerPlugin2D.class,
                        AnimationSpeedPlugin2D.class,
                        MeasurementDrawerPlugin2D.class,
                        LUTManagerPlugin3D.class,
                        CalibrationPlugin3D.class),
                Collections.emptyMap());
        dataDisplay.setImage(image);
        JIPipeImageViewerWindow window = new JIPipeImageViewerWindow(dataDisplay);
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
        clearOverlays();
        dataSource = null;
        image = null;
        imageViewerPanel2D.dispose();
        imageViewerPanel3D.dispose();
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

    public JToolBar getToolBar() {
        return toolBar;
    }

    public <T> T getPlugin(Class<T> klass) {
        return (T) pluginMap.getOrDefault(klass, null);
    }

}
