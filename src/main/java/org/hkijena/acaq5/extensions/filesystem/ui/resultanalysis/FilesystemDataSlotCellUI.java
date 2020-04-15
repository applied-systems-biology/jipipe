package org.hkijena.acaq5.extensions.filesystem.ui.resultanalysis;

import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.api.data.ACAQExportedDataTable;
import org.hkijena.acaq5.extensions.filesystem.api.dataypes.FileData;
import org.hkijena.acaq5.extensions.filesystem.api.dataypes.FolderData;
import org.hkijena.acaq5.ui.ACAQProjectWorkbench;
import org.hkijena.acaq5.ui.registries.ACAQUIDatatypeRegistry;
import org.hkijena.acaq5.ui.resultanalysis.ACAQResultDataSlotCellUI;
import org.hkijena.acaq5.utils.JsonUtils;
import org.hkijena.acaq5.utils.PathUtils;

import javax.swing.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Renders filesystem data as table cell
 */
public class FilesystemDataSlotCellUI extends ACAQResultDataSlotCellUI {

    /**
     * Creates a new renderer
     */
    public FilesystemDataSlotCellUI() {
        setOpaque(true);
        setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
    }

    private Path findListFile(ACAQDataSlot slot, ACAQExportedDataTable.Row row) {
        Path rowStorageFolder = getRowStorageFolder(slot, row);
        if (Files.isDirectory(rowStorageFolder)) {
            return PathUtils.findFileByExtensionIn(rowStorageFolder, ".json");
        }
        return null;
    }

    @Override
    public void render(ACAQProjectWorkbench workbenchUI, ACAQDataSlot slot, ACAQExportedDataTable.Row row) {
        setIcon(ACAQUIDatatypeRegistry.getInstance().getIconFor(slot.getAcceptedDataType()));
        Path listFile = findListFile(slot, row);
        if (listFile != null) {
            Path fileOrFolderPath = null;
            if (slot.getAcceptedDataType() == FileData.class) {
                try {
                    fileOrFolderPath = ((FileData) JsonUtils.getObjectMapper().readerFor(FileData.class).readValue(listFile.toFile())).getFilePath();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

            } else if (slot.getAcceptedDataType() == FolderData.class) {
                try {
                    fileOrFolderPath = ((FolderData) JsonUtils.getObjectMapper().readerFor(FolderData.class).readValue(listFile.toFile())).getFolderPath();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            if (fileOrFolderPath != null) {
                setText(fileOrFolderPath.toString());
            } else {
                setText("<Not found>");
            }
        } else {
            setText("<Not found>");
        }
    }
}
