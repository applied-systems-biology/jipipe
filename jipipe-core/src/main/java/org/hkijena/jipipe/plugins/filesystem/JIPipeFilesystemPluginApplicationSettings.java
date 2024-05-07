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

package org.hkijena.jipipe.plugins.filesystem;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.settings.JIPipeDefaultApplicationSettingsSheetCategory;
import org.hkijena.jipipe.api.settings.JIPipeDefaultApplicationsSettingsSheet;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;

/**
 * Settings for Filesystem.
 * Stored in the core library for drag and drop reasons
 */
public class JIPipeFilesystemPluginApplicationSettings extends JIPipeDefaultApplicationsSettingsSheet {

    public static String ID = "org.hkijena.jipipe:filesystem";
    private boolean relativizePaths = true;
    private boolean autoLabelOutputWithFileName = true;

    public JIPipeFilesystemPluginApplicationSettings() {
    }

    public static JIPipeFilesystemPluginApplicationSettings getInstance() {
        if (JIPipe.getInstance() != null)
            return JIPipe.getSettings().getById(ID, JIPipeFilesystemPluginApplicationSettings.class);
        else
            return null;
    }

    @JIPipeParameter("relativize-paths")
    @SetJIPipeDocumentation(name = "Relativize paths", description = "If enabled, file/folder/path data sources will relativize " +
            "their stored paths to the project directory, if paths are pointing to an element next or in a sub-folder of the project.")
    public boolean isRelativizePaths() {
        return relativizePaths;
    }

    @JIPipeParameter("relativize-paths")
    public void setRelativizePaths(boolean relativizePaths) {
        this.relativizePaths = relativizePaths;
    }

    @SetJIPipeDocumentation(name = "Auto-label output with file name", description = "If enabled, single file/folder/path data sources will automatically " +
            "label their output slot with the last path component (file/folder) name.")
    @JIPipeParameter("auto-label-output-with-filename")
    public boolean isAutoLabelOutputWithFileName() {
        return autoLabelOutputWithFileName;
    }

    @JIPipeParameter("auto-label-output-with-filename")
    public void setAutoLabelOutputWithFileName(boolean autoLabelOutputWithFileName) {
        this.autoLabelOutputWithFileName = autoLabelOutputWithFileName;
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
        return UIUtils.getIconFromResources("actions/document-open-folder.png");
    }

    @Override
    public String getName() {
        return "File system";
    }

    @Override
    public String getDescription() {
        return "Settings for the filesystem nodes";
    }
}
