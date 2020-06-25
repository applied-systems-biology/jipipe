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

package org.hkijena.acaq5.extensions.imagejdatatypes.resultanalysis;

import ij.measure.ResultsTable;
import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.api.data.ACAQExportedDataTable;
import org.hkijena.acaq5.ui.ACAQProjectWorkbench;
import org.hkijena.acaq5.ui.resultanalysis.ACAQDefaultResultDataSlotRowUI;
import org.hkijena.acaq5.ui.tableanalyzer.ACAQTableEditor;
import org.hkijena.acaq5.utils.PathUtils;
import org.hkijena.acaq5.utils.UIUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Results UI for {@link org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.ResultsTableData}
 */
public class ResultsTableDataSlotRowUI extends ACAQDefaultResultDataSlotRowUI {

    /**
     * @param workbenchUI the workbench
     * @param slot        the slot
     * @param row         the data row
     */
    public ResultsTableDataSlotRowUI(ACAQProjectWorkbench workbenchUI, ACAQDataSlot slot, ACAQExportedDataTable.Row row) {
        super(workbenchUI, slot, row);
    }

    private Path findResultsTableFile() {
        if (getRowStorageFolder() != null && Files.isDirectory(getRowStorageFolder())) {
            return PathUtils.findFileByExtensionIn(getRowStorageFolder(), ".csv");
        }
        return null;
    }

    @Override
    protected void registerActions() {
        super.registerActions();

        Path csvFile = findResultsTableFile();
        if (csvFile != null) {
            registerAction("Open in ACAQ5", "Opens the table in ACAQ5", UIUtils.getIconFromResources("acaq5.png"), e -> {
                ACAQTableEditor.importTableFromCSV(csvFile, getProjectWorkbench());
                getProjectWorkbench().getDocumentTabPane().switchToLastTab();
            });
            registerAction("Open in ImageJ", "Imports the table '" + csvFile + "' into ImageJ", UIUtils.getIconFromResources("imagej.png"), e -> {
                importCSV(csvFile);
            });
        }
    }

    private void importCSV(Path roiFile) {
        try {
            ResultsTable.open(roiFile.toString()).show(getDisplayName());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
