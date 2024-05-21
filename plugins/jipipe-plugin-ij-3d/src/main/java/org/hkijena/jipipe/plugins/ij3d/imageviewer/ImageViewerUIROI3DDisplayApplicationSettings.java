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

package org.hkijena.jipipe.plugins.ij3d.imageviewer;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.settings.JIPipeDefaultApplicationSettingsSheetCategory;
import org.hkijena.jipipe.api.settings.JIPipeDefaultApplicationsSettingsSheet;
import org.hkijena.jipipe.plugins.ij3d.IJ3DPlugin;

import javax.swing.*;

public class ImageViewerUIROI3DDisplayApplicationSettings extends JIPipeDefaultApplicationsSettingsSheet {
    public static String ID = "org.hkijena.jipipe:image-viewer-ui-roi-3d-display";
    private boolean showROI = true;

    private boolean renderROIAsVolume = false;

    public ImageViewerUIROI3DDisplayApplicationSettings() {
    }

    public static ImageViewerUIROI3DDisplayApplicationSettings getInstance() {
        return JIPipe.getSettings().getById(ID, ImageViewerUIROI3DDisplayApplicationSettings.class);
    }

    @SetJIPipeDocumentation(name = "Show ROI", description = "If enabled, ROI are visible")
    @JIPipeParameter("show-roi")
    public boolean isShowROI() {
        return showROI;
    }

    @JIPipeParameter("show-roi")
    public void setShowROI(boolean showROI) {
        this.showROI = showROI;
    }

    @SetJIPipeDocumentation(name = "Render ROI as volume", description = "If enabled, render ROI as volume")
    @JIPipeParameter("render-roi-as-volume")
    public boolean isRenderROIAsVolume() {
        return renderROIAsVolume;
    }

    @JIPipeParameter("render-roi-as-volume")
    public void setRenderROIAsVolume(boolean renderROIAsVolume) {
        this.renderROIAsVolume = renderROIAsVolume;
    }

    @Override
    public JIPipeDefaultApplicationSettingsSheetCategory getDefaultCategory() {
        return JIPipeDefaultApplicationSettingsSheetCategory.ImageViewer;
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public Icon getIcon() {
        return IJ3DPlugin.RESOURCES.getIconFromResources("data-type-roi3d.png");
    }

    @Override
    public String getName() {
        return "3D ROI display";
    }

    @Override
    public String getDescription() {
        return "Settings for the ROI manager component of the JIPipe image viewer";
    }
}
