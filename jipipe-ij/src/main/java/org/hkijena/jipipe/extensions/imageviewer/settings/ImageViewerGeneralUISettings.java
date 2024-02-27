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

package org.hkijena.jipipe.extensions.imageviewer.settings;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.ui.grapheditor.general.JIPipeGraphEditorUI;

/**
 * All settings for {@link JIPipeGraphEditorUI}
 */
public class ImageViewerGeneralUISettings extends AbstractJIPipeParameterCollection {

    public static String ID = "image-viewer-ui-general";
    private boolean autoSwitch2D3DViewer = true;

    public static ImageViewerGeneralUISettings getInstance() {
        return JIPipe.getSettings().getSettings(ID, ImageViewerGeneralUISettings.class);
    }

    @SetJIPipeDocumentation(name = "Automatically select 2D/3D viewer", description = "If enabled, the viewer automatically switches to the 3D mode if 3D data is opened")
    @JIPipeParameter("auto-switch-2d-3d")
    public boolean isAutoSwitch2D3DViewer() {
        return autoSwitch2D3DViewer;
    }

    @JIPipeParameter("auto-switch-2d-3d")
    public void setAutoSwitch2D3DViewer(boolean autoSwitch2D3DViewer) {
        this.autoSwitch2D3DViewer = autoSwitch2D3DViewer;
    }
}
