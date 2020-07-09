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

package org.hkijena.pipelinej.extensions.tables;

import ij.measure.ResultsTable;
import org.hkijena.pipelinej.api.data.ACAQDataSlot;
import org.hkijena.pipelinej.api.data.ACAQExportedDataTable;
import org.hkijena.pipelinej.ui.ACAQProjectWorkbench;
import org.hkijena.pipelinej.ui.resultanalysis.ACAQDefaultResultDataSlotRowUI;
import org.hkijena.pipelinej.ui.tableanalyzer.ACAQTableEditor;
import org.hkijena.pipelinej.utils.PathUtils;
import org.hkijena.pipelinej.utils.UIUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Results UI for {@link ResultsTableData}
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
