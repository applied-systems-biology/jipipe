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
import org.hkijena.jipipe.plugins.imageviewer.utils.viewer3d.Image3DRendererSettings;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;

/**
 * All settings for {@link AbstractJIPipeDesktopGraphEditorUI}
 */
public class ImageViewer3DUIApplicationSettings extends JIPipeDefaultApplicationsSettingsSheet {

    public static String ID = "org.hkijena.jipipe:image-viewer-ui-3d";
    private final Image3DRendererSettings rendererSettings = new Image3DRendererSettings();
    private boolean showSideBar = true;
    private double defaultAnimationFPS = 24;

    public static ImageViewer3DUIApplicationSettings getInstance() {
        return JIPipe.getSettings().getById(ID, ImageViewer3DUIApplicationSettings.class);
    }

    @SetJIPipeDocumentation(name = "Show side bar", description = "If enabled, show a side bar with additional settings and tools")
    @JIPipeParameter("show-side-bar")
    public boolean isShowSideBar() {
        return showSideBar;
    }

    @JIPipeParameter("show-side-bar")
    public void setShowSideBar(boolean showSideBar) {
        this.showSideBar = showSideBar;
    }

    @SetJIPipeDocumentation(name = "Default image rendering settings", description = "Default settings for the image rendering")
    @JIPipeParameter("renderer-settings")
    public Image3DRendererSettings getRendererSettings() {
        return rendererSettings;
    }

    @SetJIPipeDocumentation(name = "Animation FPS", description = "The default animation FPS")
    @JIPipeParameter("default-animation-fps")
    public double getDefaultAnimationFPS() {
        return defaultAnimationFPS;
    }

    @JIPipeParameter("default-animation-fps")
    public void setDefaultAnimationFPS(double defaultAnimationFPS) {
        this.defaultAnimationFPS = defaultAnimationFPS;
    }

    @Override
    public JIPipeDefaultApplicationSettingsSheetCategory getDefaultCategory() {
        return JIPipeDefaultApplicationSettingsSheetCategory.Plugins;
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public Icon getIcon() {
        return UIUtils.getIconFromResources("data-types/imgplus-3d.png");
    }

    @Override
    public String getName() {
        return "3D image viewer";
    }

    @Override
    public String getDescription() {
        return "Settings for the JIPipe 2D image viewer";
    }
}
