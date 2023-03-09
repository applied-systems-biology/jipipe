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

import com.google.common.eventbus.EventBus;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;

public class ImageViewerUIROI3DDisplaySettings implements JIPipeParameterCollection {
    public static String ID = "image-viewer-ui-roi-3d-display";
    private final EventBus eventBus = new EventBus();
    private boolean showROI = true;

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

    @Override
    public EventBus getEventBus() {
        return eventBus;
    }
}
