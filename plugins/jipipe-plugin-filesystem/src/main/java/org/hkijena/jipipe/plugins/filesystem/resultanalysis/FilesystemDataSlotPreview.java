/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.plugins.filesystem.resultanalysis;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeExportedDataAnnotation;
import org.hkijena.jipipe.api.data.serialization.JIPipeDataTableMetadataRow;
import org.hkijena.jipipe.api.data.storage.JIPipeFileSystemReadDataStorage;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWorkbench;
import org.hkijena.jipipe.desktop.app.resultanalysis.JIPipeDesktopResultDataSlotPreview;
import org.hkijena.jipipe.plugins.filesystem.dataypes.PathData;
import org.hkijena.jipipe.utils.PathUtils;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Renders filesystem data as table cell
 */
public class FilesystemDataSlotPreview extends JIPipeDesktopResultDataSlotPreview {

    private final JLabel label = new JLabel();

    /**
     * Creates a new renderer
     *
     * @param workbench      the workbench
     * @param table          the table where the data is rendered in
     * @param slot           the data slot
     * @param row            the row
     * @param dataAnnotation optional data annotation
     */
    public FilesystemDataSlotPreview(JIPipeDesktopProjectWorkbench workbench, JTable table, JIPipeDataSlot slot, JIPipeDataTableMetadataRow row, JIPipeExportedDataAnnotation dataAnnotation) {
        super(workbench, table, slot, row, dataAnnotation);
        initialize();
    }

    private void initialize() {
        setOpaque(true);
        setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        add(label, BorderLayout.CENTER);
    }


    private Path findListFile(JIPipeDataSlot slot, JIPipeDataTableMetadataRow row) {
        Path rowStorageFolder = getRowStorageFolder(slot, row, getDataAnnotation());
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
            JIPipeProgressInfo progressInfo = new JIPipeProgressInfo();
            PathData pathData = PathData.importData(new JIPipeFileSystemReadDataStorage(progressInfo, getRowStorageFolder(getSlot(), getRow(), getDataAnnotation())), progressInfo);
            label.setText(pathData.getPath() + "");
        } else {
            label.setText("<Not found>");
        }
        refreshTable();
    }
}
