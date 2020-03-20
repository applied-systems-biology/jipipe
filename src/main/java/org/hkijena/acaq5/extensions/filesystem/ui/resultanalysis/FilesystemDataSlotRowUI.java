package org.hkijena.acaq5.extensions.filesystem.ui.resultanalysis;

import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.api.data.ACAQExportedDataTable;
import org.hkijena.acaq5.extensions.filesystem.api.dataypes.ACAQFileData;
import org.hkijena.acaq5.extensions.filesystem.api.dataypes.ACAQFolderData;
import org.hkijena.acaq5.ui.ACAQWorkbenchUI;
import org.hkijena.acaq5.ui.resultanalysis.ACAQDefaultResultDataSlotRowUI;
import org.hkijena.acaq5.utils.JsonUtils;
import org.hkijena.acaq5.utils.PathUtils;
import org.hkijena.acaq5.utils.UIUtils;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FilesystemDataSlotRowUI extends ACAQDefaultResultDataSlotRowUI {

    public FilesystemDataSlotRowUI(ACAQWorkbenchUI workbenchUI, ACAQDataSlot slot, ACAQExportedDataTable.Row row) {
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
            Path fileOrFolderPath = null;
            if (getSlot().getAcceptedDataType() == ACAQFileData.class) {
                try {
                    fileOrFolderPath = ((ACAQFileData) JsonUtils.getObjectMapper().readerFor(ACAQFileData.class).readValue(listFile.toFile())).getFilePath();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

            } else if (getSlot().getAcceptedDataType() == ACAQFolderData.class) {
                try {
                    fileOrFolderPath = ((ACAQFolderData) JsonUtils.getObjectMapper().readerFor(ACAQFolderData.class).readValue(listFile.toFile())).getFolderPath();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            if (fileOrFolderPath != null) {
                Path finalFileOrFolderPath = fileOrFolderPath;
                registerAction("Copy to clipboard", "Copies the path '" + fileOrFolderPath + "' to the clipboard", UIUtils.getIconFromResources("copy.png"),
                        e -> copyPathToClipboard(finalFileOrFolderPath));
                registerAction("Open", "Opens '" + fileOrFolderPath + "' in the default application", UIUtils.getIconFromResources("open.png"),
                        e -> open(finalFileOrFolderPath));
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
