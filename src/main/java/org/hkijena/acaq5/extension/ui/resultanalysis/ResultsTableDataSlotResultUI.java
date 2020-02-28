package org.hkijena.acaq5.extension.ui.resultanalysis;

import ij.measure.ResultsTable;
import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.ui.ACAQWorkbenchUI;
import org.hkijena.acaq5.ui.resultanalysis.ACAQDefaultDataSlotResultUI;
import org.hkijena.acaq5.utils.PathUtils;
import org.hkijena.acaq5.utils.UIUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ResultsTableDataSlotResultUI extends ACAQDefaultDataSlotResultUI {

    public ResultsTableDataSlotResultUI(ACAQWorkbenchUI workbenchUI, ACAQDataSlot slot) {
        super(workbenchUI, slot);
    }

    private Path findResultsTableFile() {
        if (getSlot().getStoragePath() != null && Files.isDirectory(getSlot().getStoragePath())) {
            return PathUtils.findFileByExtensionIn(getSlot().getStoragePath(), ".csv");
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
