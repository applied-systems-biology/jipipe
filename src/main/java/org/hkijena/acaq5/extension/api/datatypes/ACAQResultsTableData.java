package org.hkijena.acaq5.extension.api.datatypes;

import ij.measure.ResultsTable;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.data.ACAQData;
import org.hkijena.acaq5.utils.PathUtils;

import java.io.IOException;
import java.nio.file.Path;

@ACAQDocumentation(name = "Results table")
public class ACAQResultsTableData implements ACAQData {

    private ResultsTable table;

    public ACAQResultsTableData(Path storageFilePath) throws IOException {
        table = ResultsTable.open(PathUtils.findFileByExtensionIn(storageFilePath, ".csv").toString());
    }

    public ACAQResultsTableData(ResultsTable table) {
        this.table = table;
    }

    @Override
    public void saveTo(Path storageFilePath, String name) {
        try {
            table.saveAs(storageFilePath.resolve(name + ".csv").toString());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public ResultsTable getTable() {
        return table;
    }
}
