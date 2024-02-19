package org.hkijena.jipipe.extensions.filesystem;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;

/**
 * Settings for Filesystem.
 * Stored in the core library for drag and drop reasons
 */
public class FilesystemExtensionSettings extends AbstractJIPipeParameterCollection {

    public static String ID = "org.hkijena.jipipe:filesystem";
    private boolean relativizePaths = true;
    private boolean autoLabelOutputWithFileName = true;

    public FilesystemExtensionSettings() {
    }

    public static FilesystemExtensionSettings getInstance() {
        if (JIPipe.getInstance() != null)
            return JIPipe.getSettings().getSettings(ID, FilesystemExtensionSettings.class);
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
}
