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

package org.hkijena.acaq5.extensions.filesystem.resultanalysis;

import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.api.data.ACAQExportedDataTable;
import org.hkijena.acaq5.extensions.filesystem.dataypes.PathData;
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
            try {
                PathData pathData = JsonUtils.getObjectMapper().readerFor(PathData.class).readValue(listFile.toFile());
                fileOrFolderPath = pathData.getPath();
            } catch (IOException ignored) {
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
