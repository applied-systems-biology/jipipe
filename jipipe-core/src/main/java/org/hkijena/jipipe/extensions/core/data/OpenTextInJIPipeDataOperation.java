package org.hkijena.jipipe.extensions.core.data;

import com.google.common.base.Charsets;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataDisplayOperation;
import org.hkijena.jipipe.api.data.JIPipeDataImportOperation;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeDataSource;
import org.hkijena.jipipe.api.data.JIPipeExportedDataTable;
import org.hkijena.jipipe.extensions.strings.StringData;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.texteditor.JIPipeTextEditor;
import org.hkijena.jipipe.utils.PathUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class OpenTextInJIPipeDataOperation implements JIPipeDataImportOperation, JIPipeDataDisplayOperation {

    private final String[] extensions;

    public OpenTextInJIPipeDataOperation(String... extensions) {
        this.extensions = extensions;
    }

    @Override
    public void display(JIPipeData data, String displayName, JIPipeWorkbench workbench, JIPipeDataSource source) {

    }

    @Override
    public String getName() {
        return "Open in JIPipe";
    }

    @Override
    public String getDescription() {
        return "Opens the data in JIPipe";
    }

    @Override
    public int getOrder() {
        return 0;
    }

    @Override
    public Icon getIcon() {
        return UIUtils.getIconFromResources("apps/jipipe.png");
    }

    @Override
    public JIPipeData show(JIPipeDataSlot slot, JIPipeExportedDataTable.Row row, Path rowStorageFolder, String compartmentName, String algorithmName, String displayName, JIPipeWorkbench workbench) {
        if (rowStorageFolder == null || !Files.isDirectory(rowStorageFolder))
            return null;
        Path textFile = PathUtils.findFileByExtensionIn(rowStorageFolder, extensions);
        try {
            String data = new String(Files.readAllBytes(textFile), Charsets.UTF_8);
            JIPipeTextEditor editor = JIPipeTextEditor.openInNewTab(workbench, displayName);
            editor.setText(data);
            return new StringData(data);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
