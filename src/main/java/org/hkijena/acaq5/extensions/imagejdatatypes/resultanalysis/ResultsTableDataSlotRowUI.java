package org.hkijena.acaq5.extensions.imagejdatatypes.resultanalysis;

import ij.measure.ResultsTable;
import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.api.data.ACAQExportedDataTable;
import org.hkijena.acaq5.ui.ACAQProjectUI;
import org.hkijena.acaq5.ui.resultanalysis.ACAQDefaultResultDataSlotRowUI;
import org.hkijena.acaq5.ui.tableanalyzer.ACAQTableAnalyzerUI;
import org.hkijena.acaq5.utils.PathUtils;
import org.hkijena.acaq5.utils.UIUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ResultsTableDataSlotRowUI extends ACAQDefaultResultDataSlotRowUI {

    public ResultsTableDataSlotRowUI(ACAQProjectUI workbenchUI, ACAQDataSlot slot, ACAQExportedDataTable.Row row) {
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
                ACAQTableAnalyzerUI.importTableFromCSV(csvFile, getWorkbenchUI());
                getWorkbenchUI().getDocumentTabPane().switchToLastTab();
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
