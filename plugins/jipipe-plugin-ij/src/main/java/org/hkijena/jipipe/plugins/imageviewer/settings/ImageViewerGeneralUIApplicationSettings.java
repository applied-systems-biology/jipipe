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

package org.hkijena.jipipe.plugins.imageviewer.settings;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.settings.JIPipeDefaultApplicationSettingsSheetCategory;
import org.hkijena.jipipe.api.settings.JIPipeDefaultApplicationsSettingsSheet;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.AbstractJIPipeDesktopGraphEditorUI;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;

/**
 * All settings for {@link AbstractJIPipeDesktopGraphEditorUI}
 */
public class ImageViewerGeneralUIApplicationSettings extends JIPipeDefaultApplicationsSettingsSheet {

    public static String ID = "image-viewer-ui-general";
    private boolean autoSwitch2D3DViewer = false;
    private boolean forceLegacyImageViewer = false;

    public static ImageViewerGeneralUIApplicationSettings getInstance() {
        return JIPipe.getSettings().getById(ID, ImageViewerGeneralUIApplicationSettings.class);
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

    @SetJIPipeDocumentation(name = "Force legacy image viewer", description = "If enabled, always use the legacy image viewer instead of the VTK-based one")
    @JIPipeParameter("force-legacy-image-viewer")
    public boolean isForceLegacyImageViewer() {
        return forceLegacyImageViewer;
    }

    @JIPipeParameter("force-legacy-image-viewer")
    public void setForceLegacyImageViewer(boolean forceLegacyImageViewer) {
        this.forceLegacyImageViewer = forceLegacyImageViewer;
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
        return UIUtils.getIconFromResources("actions/configure.png");
    }

    @Override
    public String getName() {
        return "General";
    }

    @Override
    public String getDescription() {
        return "General settings for the image viewer";
    }
}
