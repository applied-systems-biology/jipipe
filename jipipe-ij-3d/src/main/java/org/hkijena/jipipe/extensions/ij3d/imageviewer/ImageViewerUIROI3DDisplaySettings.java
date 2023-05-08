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

package org.hkijena.jipipe.extensions.ij3d.imageviewer;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;

public class ImageViewerUIROI3DDisplaySettings extends AbstractJIPipeParameterCollection {
    public static String ID = "image-viewer-ui-roi-3d-display";
    private boolean showROI = true;

    private boolean renderROIAsVolume = false;

    public ImageViewerUIROI3DDisplaySettings() {
    }

    public static ImageViewerUIROI3DDisplaySettings getInstance() {
        return JIPipe.getSettings().getSettings(ID, ImageViewerUIROI3DDisplaySettings.class);
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

    @JIPipeDocumentation(name = "Render ROI as volume", description = "If enabled, render ROI as volume")
    @JIPipeParameter("render-roi-as-volume")
    public boolean isRenderROIAsVolume() {
        return renderROIAsVolume;
    }

    @JIPipeParameter("render-roi-as-volume")
    public void setRenderROIAsVolume(boolean renderROIAsVolume) {
        this.renderROIAsVolume = renderROIAsVolume;
    }
}
