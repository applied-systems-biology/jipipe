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
 */

package org.hkijena.jipipe.extensions.strings;

import com.google.common.base.Charsets;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeExportedDataTable;
import org.hkijena.jipipe.extensions.tables.ResultsTableDataSlotRowUI;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbench;
import org.hkijena.jipipe.ui.resultanalysis.JIPipeDefaultResultDataSlotRowUI;
import org.hkijena.jipipe.ui.texteditor.JIPipeTextEditor;
import org.hkijena.jipipe.utils.PathUtils;
import org.hkijena.jipipe.utils.UIUtils;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class StringResultDataSlotRowUI extends JIPipeDefaultResultDataSlotRowUI {
    /**
     * Creates a new UI
     *
     * @param workbenchUI The workbench UI
     * @param slot        The data slot
     * @param row         The data slow row
     */
    public StringResultDataSlotRowUI(JIPipeProjectWorkbench workbenchUI, JIPipeDataSlot slot, JIPipeExportedDataTable.Row row) {
        super(workbenchUI, slot, row);
    }

    @Override
    protected void registerActions() {
        super.registerActions();
        if(findTextFile() != null) {
            registerAction("Open", "Opens the file in the native application", UIUtils.getIconFromResources("actions/folder-open.png"), this::openInNativeApplication);
            registerAction("Open in JIPipe", "Opens the file in JIPipe", UIUtils.getIconFromResources("apps/jipipe.png"), this::openInJIPipe);
        }
    }

    private Path findTextFile() {
        if (getRowStorageFolder() != null && Files.isDirectory(getRowStorageFolder())) {
            return PathUtils.findFileByExtensionIn(getRowStorageFolder(), getFileExtension());
        }
        return null;
    }

    public String getFileExtension() {
        return ".txt";
    }

    public String getFileMimeType() {
        return "text-plain";
    }

    private void openInNativeApplication(JIPipeDataSlot dataSlot) {
        Path textFile = findTextFile();
        if(textFile != null) {
            UIUtils.openFileInNative(textFile);
        }
    }

    private void openInJIPipe(JIPipeDataSlot dataSlot) {
        Path textFile = findTextFile();
        try {
            String data = new String(Files.readAllBytes(textFile), Charsets.UTF_8);
            JIPipeTextEditor editor = JIPipeTextEditor.openInNewTab(getWorkbench(), getDisplayName());
            editor.setMimeType(getFileMimeType());
            editor.setText(data);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
