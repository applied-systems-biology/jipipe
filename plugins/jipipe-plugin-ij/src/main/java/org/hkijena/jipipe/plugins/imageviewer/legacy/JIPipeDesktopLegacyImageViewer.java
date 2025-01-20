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
import ij.gui.ImageWindow;
import ij.measure.Calibration;
import ij.util.Tools;
import org.hkijena.jipipe.api.JIPipeWorkbench;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataSource;
import org.hkijena.jipipe.desktop.api.dataviewer.JIPipeDesktopDataViewer;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbenchAccess;
import org.hkijena.jipipe.desktop.app.JIPipeDummyWorkbench;
import org.hkijena.jipipe.desktop.commons.components.ribbon.JIPipeDesktopRibbon;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.plugins.imageviewer.legacy.api.JIPipeDesktopLegacyImageViewerOverlay;
import org.hkijena.jipipe.plugins.imageviewer.legacy.api.JIPipeDesktopLegacyImageViewerPlugin;
import org.hkijena.jipipe.plugins.imageviewer.legacy.api.JIPipeDesktopLegacyImageViewerPlugin2D;
import org.hkijena.jipipe.plugins.imageviewer.legacy.impl.JIPipeDesktopLegacyImageViewerPanel2D;
import org.hkijena.jipipe.plugins.imageviewer.legacy.plugins2d.*;
import org.hkijena.jipipe.plugins.imageviewer.legacy.plugins2d.maskdrawer.MeasurementDrawerPlugin2D;
import org.hkijena.jipipe.plugins.imageviewer.legacy.plugins2d.roimanager.ROIManagerPlugin2D;
import org.hkijena.jipipe.utils.ReflectionUtils;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.ui.JIPipeDesktopDockPanel;
import org.scijava.Disposable;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.*;

public class JIPipeDesktopLegacyImageViewer extends JPanel implements JIPipeDesktopWorkbenchAccess, Disposable {

    public static final List<Class<? extends JIPipeDesktopLegacyImageViewerPlugin>> DEFAULT_PLUGINS = new ArrayList<>(Arrays.asList(CalibrationPlugin2D.class,
            PixelInfoPlugin2D.class,
            LUTManagerPlugin2D.class,
            CompositeManagerPlugin2D.class,
            ROIManagerPlugin2D.class,
            MeasurementDrawerPlugin2D.class));
    private final JIPipeDesktopDataViewer dataViewer;
    private final JIPipeDesktopWorkbench workbench;
    private final Map<Class<?>, Object> contextObjects;
    private final JIPipeDesktopLegacyImageViewerPanel2D imageViewerPanel2D;
    private final List<JIPipeDesktopLegacyImageViewerPlugin> plugins = new ArrayList<>();
    private final List<JIPipeDesktopLegacyImageViewerPlugin2D> plugins2D = new ArrayList<>();

    private final Map<Class<? extends JIPipeDesktopLegacyImageViewerPlugin>, JIPipeDesktopLegacyImageViewerPlugin> pluginMap = new HashMap<>();
    private final JLabel imageInfoLabel = new JLabel(UIUtils.getIconFromResources("actions/viewimage.png"));
    private final List<Object> overlays = new ArrayList<>();
    private ImagePlusData image;
    private JIPipeDataSource dataSource;
    private boolean pluginsInitialized = false;

    /**
     * Initializes a new image viewer
     *
     * @param workbench the workbench. Use {@link JIPipeDummyWorkbench} if you do not have access to one.
     */
    public JIPipeDesktopLegacyImageViewer(JIPipeDesktopWorkbench workbench, List<Class<? extends JIPipeDesktopLegacyImageViewerPlugin>> pluginTypes, JIPipeDesktopDataViewer dataViewer, Map<Class<?>, Object> contextObjects) {
        this.workbench = workbench;
        this.dataViewer = dataViewer;
        this.contextObjects = contextObjects;
        imageViewerPanel2D = new JIPipeDesktopLegacyImageViewerPanel2D(this);
        registerPlugins(pluginTypes);
        initialize();
    }

    public JIPipeDesktopDataViewer getDataViewer() {
        return dataViewer;
    }

    public static void registerDefaultPlugin(Class<? extends JIPipeDesktopLegacyImageViewerPlugin> klass) {
        if (!DEFAULT_PLUGINS.contains(klass)) {
            if (Modifier.isAbstract(klass.getModifiers())) {
                throw new IllegalArgumentException("Plugin is abstract!");
            }
            DEFAULT_PLUGINS.add(klass);
        }
    }

    public void registerPlugins(Collection<Class<? extends JIPipeDesktopLegacyImageViewerPlugin>> pluginTypes) {
        boolean requireRebuild = false;
        try {
            for (Class<? extends JIPipeDesktopLegacyImageViewerPlugin> pluginType : pluginTypes) {
                if (!pluginMap.containsKey(pluginType)) {
                    Object plugin = ReflectionUtils.newInstance(pluginType, this);
                    if (plugin instanceof JIPipeDesktopLegacyImageViewerPlugin2D) {
                        JIPipeDesktopLegacyImageViewerPlugin2D plugin2D = (JIPipeDesktopLegacyImageViewerPlugin2D) plugin;
                        plugins.add(plugin2D);
                        plugins2D.add(plugin2D);
                        pluginMap.put(pluginType, plugin2D);
                        requireRebuild = pluginsInitialized;
                    }
                }
            }
        }
        finally {
            pluginsInitialized = true;
        }
        if(requireRebuild) {
            if(getDataViewer() != null) {
                SwingUtilities.invokeLater(() -> {
                    getDataViewer().getDataViewerWindow().rebuildUI();
                    imageViewerPanel2D.refreshFormPanel();
                });
            }
        }
    }

    public JIPipeDesktopLegacyImageViewerPanel2D getImageViewerPanel2D() {
        return imageViewerPanel2D;
    }

    public List<Object> getOverlays() {
        return Collections.unmodifiableList(overlays);
    }

    public void clearOverlays() {
        while (!overlays.isEmpty()) {
            Object o = overlays.get(0);
            overlays.remove(o);
            for (JIPipeDesktopLegacyImageViewerPlugin plugin : plugins) {
                plugin.onOverlayRemoved(o);
            }
        }
        for (JIPipeDesktopLegacyImageViewerPlugin plugin : plugins) {
            plugin.onOverlaysCleared();
        }
    }

    public void addOverlay(Object o) {
        if(o instanceof JIPipeDesktopLegacyImageViewerOverlay) {
            // Register missing plugins
            registerPlugins(((JIPipeDesktopLegacyImageViewerOverlay) o).getRequiredLegacyImageViewerPlugins());
        }
        overlays.add(o);
        for (JIPipeDesktopLegacyImageViewerPlugin plugin : plugins) {
            plugin.onOverlayAdded(o);
        }
    }

    private void initialize() {
        setLayout(new BorderLayout());
        add(imageViewerPanel2D, BorderLayout.CENTER);
    }

    public void buildRibbon(JIPipeDesktopRibbon ribbon) {

        imageViewerPanel2D.buildRibbon(ribbon);
    }

    public void buildDock(JIPipeDesktopDockPanel dockPanel) {
        imageViewerPanel2D.buildDock(dockPanel);
    }

    public void buildStatusBar(JToolBar statusBar) {
        statusBar.add(Box.createHorizontalStrut(8));
        statusBar.add(imageInfoLabel);
        imageViewerPanel2D.buildStatusBar(statusBar);
    }

    public void refreshImageInfo() {
        String s = "";
        if (image == null) {
            imageInfoLabel.setText("");
            return;
        }
        int type = image.getImage().getType();
        Calibration cal = image.getImage().getCalibration();
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
        if (image.getImage().isInvertedLut())
            s += " (inverting LUT)";
        s += "; " + ImageWindow.getImageSize(image.getImage());
//        if (rotation != 0) {
//            s += " (Rotated " + rotation + "°)";
//        }
        imageInfoLabel.setText(s);
    }

    public List<JIPipeDesktopLegacyImageViewerPlugin> getPlugins() {
        return Collections.unmodifiableList(plugins);
    }

    public List<JIPipeDesktopLegacyImageViewerPlugin2D> getPlugins2D() {
        return Collections.unmodifiableList(plugins2D);
    }

    public JIPipeDesktopWorkbench getDesktopWorkbench() {
        return workbench;
    }

    @Override
    public JIPipeWorkbench getWorkbench() {
        return workbench;
    }

    public JIPipeDataSource getDataSource() {
        return dataSource;
    }

    public void setDataSource(JIPipeDataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void fitImageToScreen() {
        imageViewerPanel2D.fitImageToScreen();
    }

    @Override
    public void dispose() {
        if (contextObjects != null) {
            contextObjects.clear();
        }
        clearOverlays();
        dataSource = null;
        image = null;
        for (JIPipeDesktopLegacyImageViewerPlugin plugin : plugins) {
            plugin.dispose();
        }
        imageViewerPanel2D.dispose();
    }

    public void setImageData(ImagePlusData image) {
        this.image = image;
        imageViewerPanel2D.setImage(image);

        refreshImageInfo();
    }

    public ImagePlusData getImage() {
        return image;
    }

    @Deprecated
    public void setImage(ImagePlus image) {
        setImagePlus(image);
    }

    public ImagePlus getImagePlus() {
        return ImageJUtils.unwrap(image);
    }

    public void setImagePlus(ImagePlus image) {
        setImageData(new ImagePlusData(image));
    }

    public JIPipeDesktopLegacyImageViewerPanel2D getViewerPanel2D() {
        return imageViewerPanel2D;
    }

    public <T> T getContextObject(Class<T> klass) {
        return (T) contextObjects.getOrDefault(klass, null);
    }

    public void setError(String errorMessage) {
        if (errorMessage != null) {
            JLabel errorLabel2D = new JLabel(errorMessage, UIUtils.getIconFromResources("emblems/no-data.png"), JLabel.LEFT);
            imageViewerPanel2D.getCanvas().setError(errorLabel2D);
        } else {
            imageViewerPanel2D.getCanvas().setError(null);
        }
    }

    public <T> T getPlugin(Class<T> klass) {
        return (T) pluginMap.getOrDefault(klass, null);
    }

    public void addOverlays(Collection<?> overlays) {
        for (Object overlay : overlays) {
            addOverlay(overlay);
        }
    }

    /**
     * Gets only the overlays that are of type {@link JIPipeData}
     * @return the overlays
     */
    public List<JIPipeData> getDataOverlays() {
        List<JIPipeData> result = new ArrayList<>();
        for (Object overlay : overlays) {
            if(overlay instanceof JIPipeData) {
                result.add((JIPipeData) overlay);
            }
        }
        return result;
    }
}
