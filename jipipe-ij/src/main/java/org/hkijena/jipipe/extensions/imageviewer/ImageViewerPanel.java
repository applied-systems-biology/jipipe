package org.hkijena.jipipe.extensions.imageviewer;

import ij.ImagePlus;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.extensions.imageviewer.plugins2d.AnimationSpeedPlugin2D;
import org.hkijena.jipipe.extensions.imageviewer.plugins2d.CalibrationPlugin2D;
import org.hkijena.jipipe.extensions.imageviewer.plugins2d.LUTManagerPlugin2D;
import org.hkijena.jipipe.extensions.imageviewer.plugins2d.PixelInfoPlugin2D;
import org.hkijena.jipipe.extensions.imageviewer.plugins2d.maskdrawer.MeasurementDrawerPlugin2D;
import org.hkijena.jipipe.extensions.imageviewer.plugins2d.roimanager.ROIManagerPlugin2D;
import org.hkijena.jipipe.extensions.settings.ImageViewerUISettings;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.JIPipeWorkbenchAccess;
import org.scijava.Disposable;

import javax.swing.*;
import java.util.*;

public class ImageViewerPanel extends JPanel implements JIPipeWorkbenchAccess, Disposable {

    private static final Set<ImageViewerPanel> OPEN_PANELS = new HashSet<>();
    private static ImageViewerPanel ACTIVE_PANEL = null;
    private final JIPipeWorkbench workbench;
    private final ImageViewerUISettings settings;
    private final List<Class<? extends ImageViewerPanelPlugin>> pluginTypes;

    /**
     * Initializes a new image viewer
     *
     * @param workbench the workbench. Use {@link org.hkijena.jipipe.ui.JIPipeDummyWorkbench} if you do not have access to one.
     */
    public ImageViewerPanel(JIPipeWorkbench workbench, List<Class<? extends ImageViewerPanelPlugin>> pluginTypes) {
        this.workbench = workbench;
        this.pluginTypes = pluginTypes;
        if (JIPipe.getInstance() != null) {
            settings = ImageViewerUISettings.getInstance();
        } else {
            settings = null;
        }
        initialize();
    }

    @SafeVarargs
    public ImageViewerPanel(JIPipeWorkbench workbench, Class<? extends ImageViewerPanelPlugin>... pluginTypes) {
        this(workbench, Arrays.asList(pluginTypes));
    }

    private void initialize() {

    }

    public static ImageViewerPanel getActiveViewerPanel() {
        return ACTIVE_PANEL;
    }

    public static Set<ImageViewerPanel> getOpenViewerPanels() {
        return OPEN_PANELS;
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
                CalibrationPlugin2D.class,
                PixelInfoPlugin2D.class,
                LUTManagerPlugin2D.class,
                ROIManagerPlugin2D.class,
                AnimationSpeedPlugin2D.class,
                MeasurementDrawerPlugin2D.class);
        dataDisplay.setImage(image);
        ImageViewerWindow window = new ImageViewerWindow(dataDisplay);
        window.setTitle(title);
        window.setVisible(true);
        return dataDisplay;
    }

    public void fitImageToScreen() {

    }

    @Override
    public void dispose() {
        if (ACTIVE_PANEL == this) {
            ACTIVE_PANEL = null;
        }
        OPEN_PANELS.remove(this);

        // TODO DISPOSE VIEWER

    }

    public void setImage(ImagePlus image) {

    }

    public void addRoi2d(ROIListData data) {

    }
}
