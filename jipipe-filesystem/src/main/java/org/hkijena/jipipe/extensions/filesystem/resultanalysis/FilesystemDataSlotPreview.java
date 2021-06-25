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

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeExportedDataTableRow;
import org.hkijena.jipipe.extensions.filesystem.dataypes.PathData;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbench;
import org.hkijena.jipipe.ui.resultanalysis.JIPipeResultDataSlotPreview;
import org.hkijena.jipipe.utils.PathUtils;

import javax.swing.*;
import java.awt.BorderLayout;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Renders filesystem data as table cell
 */
public class FilesystemDataSlotPreview extends JIPipeResultDataSlotPreview {

    private final JLabel label = new JLabel();

    /**
     * Creates a new renderer
     *
     * @param workbench the workbench
     * @param table     the table where the data is rendered in
     * @param slot      the data slot
     * @param row       the row
     */
    public FilesystemDataSlotPreview(JIPipeProjectWorkbench workbench, JTable table, JIPipeDataSlot slot, JIPipeExportedDataTableRow row) {
        super(workbench, table, slot, row);
        initialize();
    }

    private void initialize() {
        setOpaque(true);
        setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        add(label, BorderLayout.CENTER);
    }


    private Path findListFile(JIPipeDataSlot slot, JIPipeExportedDataTableRow row) {
        Path rowStorageFolder = getRowStorageFolder(slot, row);
        if (Files.isDirectory(rowStorageFolder)) {
            return PathUtils.findFileByExtensionIn(rowStorageFolder, ".json");
        }
        return null;
    }

    @Override
    public void renderPreview() {
        if (!label.getText().isEmpty())
            return;
        label.setIcon(JIPipe.getDataTypes().getIconFor(getSlot().getAcceptedDataType()));
        Path listFile = findListFile(getSlot(), getRow());
        if (listFile != null) {
            PathData pathData = PathData.importFrom(getRowStorageFolder(getSlot(), getRow()));
            label.setText(pathData.getPath() + "");
        } else {
            label.setText("<Not found>");
        }
        refreshTable();
    }
}
