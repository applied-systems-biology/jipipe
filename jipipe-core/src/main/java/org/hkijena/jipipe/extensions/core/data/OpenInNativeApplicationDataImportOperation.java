package org.hkijena.jipipe.extensions.core.data;

import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataImportOperation;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeExportedDataTable;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.utils.PathUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.nio.file.Files;
import java.nio.file.Path;

public class OpenInNativeApplicationDataImportOperation implements JIPipeDataImportOperation {

    private final String[] extensions;
    private String name = "Open in native application";
    private String description = "Opens the file as you would open it from your file browser.";

    public OpenInNativeApplicationDataImportOperation(String name, String description, String[] extensions) {
        this.name = name;
        this.description = description;
        this.extensions = extensions;
    }

    public OpenInNativeApplicationDataImportOperation(String... extensions) {
        this.extensions = extensions;
    }

    @Override
    public String getId() {
        return "jipipe:open-in-native-application";
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public int getOrder() {
        return 9800;
    }

    @Override
    public Icon getIcon() {
        return UIUtils.getIconFromResources("actions/quickopen.png");
    }

    @Override
    public JIPipeData show(JIPipeDataSlot slot, JIPipeExportedDataTable.Row row, Path rowStorageFolder, String compartmentName, String algorithmName, String displayName, JIPipeWorkbench workbench) {
        if (rowStorageFolder == null || !Files.isDirectory(rowStorageFolder))
            return null;
        Path targetFile = PathUtils.findFileByExtensionIn(rowStorageFolder, extensions);
        if (targetFile != null) {
            UIUtils.openFileInNative(targetFile);
        }
        return null;
    }

}
