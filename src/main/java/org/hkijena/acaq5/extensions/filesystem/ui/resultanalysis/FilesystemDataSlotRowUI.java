package org.hkijena.acaq5.extensions.filesystem.ui.resultanalysis;

import ij.IJ;
import ij.io.Opener;
import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.api.data.ACAQExportedDataTable;
import org.hkijena.acaq5.extensions.filesystem.api.dataypes.PathData;
import org.hkijena.acaq5.ui.ACAQProjectWorkbench;
import org.hkijena.acaq5.ui.resultanalysis.ACAQDefaultResultDataSlotRowUI;
import org.hkijena.acaq5.utils.JsonUtils;
import org.hkijena.acaq5.utils.PathUtils;
import org.hkijena.acaq5.utils.UIUtils;

import java.awt.Desktop;
import java.awt.Toolkit;
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
