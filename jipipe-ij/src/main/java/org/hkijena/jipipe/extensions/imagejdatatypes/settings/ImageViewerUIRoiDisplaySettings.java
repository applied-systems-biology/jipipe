/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 *
 */

package org.hkijena.jipipe.extensions.imagejdatatypes.settings;

import com.google.common.eventbus.EventBus;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.RoiDrawer;

public class ImageViewerUIRoiDisplaySettings implements JIPipeParameterCollection {
    public static String ID = "image-viewer-ui-roi-display";
    private final EventBus eventBus = new EventBus();

    private RoiDrawer roiDrawer = new RoiDrawer();
    private boolean showROI = true;
    private boolean renderROIAsOverlay = true;

    public ImageViewerUIRoiDisplaySettings() {
    }

    public static ImageViewerUIRoiDisplaySettings getInstance() {
        return JIPipe.getSettings().getSettings(ID, ImageViewerUIRoiDisplaySettings.class);
    }

    @JIPipeDocumentation(name = "ROI visualization")
    @JIPipeParameter("roi-drawer")
    public RoiDrawer getRoiDrawer() {
        return roiDrawer;
    }

    public void setRoiDrawer(RoiDrawer roiDrawer) {
        this.roiDrawer = roiDrawer;
    }

    @JIPipeDocumentation(name = "Show ROI", description = "If enabled, ROI are visible")
    @JIPipeParameter("show-roi")
    public boolean isShowROI() {
        return showROI;
    }

    @JIPipeParameter("show-roi")
    public void setShowROI(boolean showROI) {
        this.showROI = showROI;
    }

    @JIPipeDocumentation(name = "Render ROI as overlay", description = "If enabled, ROI are rendered as overlay.")
    @JIPipeParameter("render-roi-as-overlay")
    public boolean isRenderROIAsOverlay() {
        return renderROIAsOverlay;
    }

    @JIPipeParameter("render-roi-as-overlay")
    public void setRenderROIAsOverlay(boolean renderROIAsOverlay) {
        this.renderROIAsOverlay = renderROIAsOverlay;
    }

    @Override
    public EventBus getEventBus() {
        return eventBus;
    }
}
