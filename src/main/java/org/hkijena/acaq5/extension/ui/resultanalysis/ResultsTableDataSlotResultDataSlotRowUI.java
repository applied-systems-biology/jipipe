package org.hkijena.acaq5.extension.ui.resultanalysis;

import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.api.data.ACAQExportedDataTable;
import org.hkijena.acaq5.ui.ACAQWorkbenchUI;
import org.hkijena.acaq5.ui.resultanalysis.ACAQDefaultDataSlotResultDataSlotRowUI;
import org.hkijena.acaq5.utils.PathUtils;
import org.hkijena.acaq5.utils.UIUtils;

import java.nio.file.Files;
import java.nio.file.Path;

public class ResultsTableDataSlotResultDataSlotRowUI extends ACAQDefaultDataSlotResultDataSlotRowUI {

    public ResultsTableDataSlotResultDataSlotRowUI(ACAQWorkbenchUI workbenchUI, ACAQDataSlot slot, ACAQExportedDataTable.Row row) {
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
            registerAction("Open", UIUtils.getIconFromResources("imagej.png"), e -> {
                importCSV(csvFile);
            });
        }
    }

    private void importCSV(Path roiFile) {
//        try {
//            ResultsTable.open(roiFile.toString()).show(getSample().getName() + "/" + getSlot().getAlgorithm().getName() + "/" + getSlot().getName());
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
    }
}
