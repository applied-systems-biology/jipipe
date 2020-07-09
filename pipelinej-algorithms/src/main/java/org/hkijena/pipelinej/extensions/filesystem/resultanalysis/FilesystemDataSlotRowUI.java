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

package org.hkijena.pipelinej.extensions.filesystem.resultanalysis;

import ij.IJ;
import ij.io.Opener;
import org.hkijena.pipelinej.api.data.ACAQDataSlot;
import org.hkijena.pipelinej.api.data.ACAQExportedDataTable;
import org.hkijena.pipelinej.extensions.filesystem.dataypes.PathData;
import org.hkijena.pipelinej.ui.ACAQProjectWorkbench;
import org.hkijena.pipelinej.ui.resultanalysis.ACAQDefaultResultDataSlotRowUI;
import org.hkijena.pipelinej.utils.JsonUtils;
import org.hkijena.pipelinej.utils.PathUtils;
import org.hkijena.pipelinej.utils.UIUtils;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Shows filesystem data results
 */
public class FilesystemDataSlotRowUI extends ACAQDefaultResultDataSlotRowUI {

    /**
     * Creates a new UI
     *
     * @param workbenchUI the workbench
     * @param slot        the data slot
     * @param row         the slot row
     */
    public FilesystemDataSlotRowUI(ACAQProjectWorkbench workbenchUI, ACAQDataSlot slot, ACAQExportedDataTable.Row row) {
        super(workbenchUI, slot, row);
    }

    private Path findListFile() {
        if (getRowStorageFolder() != null && Files.isDirectory(getRowStorageFolder())) {
            return PathUtils.findFileByExtensionIn(getRowStorageFolder(), ".json");
        }
        return null;
    }

    @Override
    protected void registerActions() {
        super.registerActions();

        Path listFile = findListFile();
        if (listFile != null) {
            Path fileOrFolderPath;
            try {
                PathData pathData = JsonUtils.getObjectMapper().readerFor(PathData.class).readValue(listFile.toFile());
                fileOrFolderPath = pathData.getPath();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            if (fileOrFolderPath != null) {
                registerAction("Copy to clipboard", "Copies the path '" + fileOrFolderPath + "' to the clipboard", UIUtils.getIconFromResources("copy.png"),
                        e -> copyPathToClipboard(fileOrFolderPath));
                registerAction("Open", "Opens '" + fileOrFolderPath + "' in the default application", UIUtils.getIconFromResources("open.png"),
                        e -> open(fileOrFolderPath));
                if (Files.isRegularFile(fileOrFolderPath)) {
                    String fileType = Opener.getFileFormat(fileOrFolderPath.toString());
                    switch (fileType) {
                        case "tif":
                        case "dcm":
                        case "fits":
                        case "pgm":
                        case "jpg":
                        case "gif":
                        case "lut":
                        case "bmp":
                        case "roi": {
                            registerAction("Import", "Imports the data into ImageJ",
                                    UIUtils.getIconFromResources("imagej.png"), e -> IJ.open(fileOrFolderPath.toString()));
                        }
                    }
                }
            }
        }
    }

    private void open(Path fileOrFolderPath) {
        try {
            Desktop.getDesktop().open(fileOrFolderPath.toFile());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void copyPathToClipboard(Path fileOrFolderPath) {
        StringSelection selection = new StringSelection(fileOrFolderPath.toString());
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(selection, selection);
    }
}
