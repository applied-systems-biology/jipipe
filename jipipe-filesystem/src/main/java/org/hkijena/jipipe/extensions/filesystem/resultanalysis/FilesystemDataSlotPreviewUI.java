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

package org.hkijena.jipipe.extensions.filesystem.resultanalysis;

import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeExportedDataTable;
import org.hkijena.jipipe.extensions.filesystem.dataypes.PathData;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbench;
import org.hkijena.jipipe.ui.registries.JIPipeUIDatatypeRegistry;
import org.hkijena.jipipe.ui.resultanalysis.JIPipeResultDataSlotPreviewUI;
import org.hkijena.jipipe.utils.JsonUtils;
import org.hkijena.jipipe.utils.PathUtils;

import javax.swing.*;
import java.awt.BorderLayout;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Renders filesystem data as table cell
 */
public class FilesystemDataSlotPreviewUI extends JIPipeResultDataSlotPreviewUI {

    private final JLabel label = new JLabel();

    /**
     * Creates a new renderer
     * @param table the table that renders the preview
     */
    public FilesystemDataSlotPreviewUI(JTable table) {
        super(table);
        setOpaque(true);
        setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        add(label, BorderLayout.CENTER);
    }

    private Path findListFile(JIPipeDataSlot slot, JIPipeExportedDataTable.Row row) {
        Path rowStorageFolder = getRowStorageFolder(slot, row);
        if (Files.isDirectory(rowStorageFolder)) {
            return PathUtils.findFileByExtensionIn(rowStorageFolder, ".json");
        }
        return null;
    }

    @Override
    public void render(JIPipeProjectWorkbench workbenchUI, JIPipeDataSlot slot, JIPipeExportedDataTable.Row row) {

        label.setIcon(JIPipeUIDatatypeRegistry.getInstance().getIconFor(slot.getAcceptedDataType()));
        Path listFile = findListFile(slot, row);
        if (listFile != null) {
            Path fileOrFolderPath = null;
            try {
                PathData pathData = JsonUtils.getObjectMapper().readerFor(PathData.class).readValue(listFile.toFile());
                fileOrFolderPath = pathData.getPath();
            } catch (IOException ignored) {
            }
            if (fileOrFolderPath != null) {
                label.setText(fileOrFolderPath.toString());
            } else {
                label.setText("<Not found>");
            }
        } else {
            label.setText("<Not found>");
        }
    }
}
