package org.hkijena.acaq5.extension.api.datatypes;

import ij.measure.ResultsTable;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.data.ACAQData;

import java.io.IOException;
import java.nio.file.Path;

@ACAQDocumentation(name = "Results table")
public class ACAQResultsTableData implements ACAQData {

    private ResultsTable table;

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
