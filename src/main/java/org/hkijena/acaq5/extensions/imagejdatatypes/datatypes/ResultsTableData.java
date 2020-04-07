package org.hkijena.acaq5.extensions.imagejdatatypes.datatypes;

import ij.measure.ResultsTable;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.data.ACAQData;
import org.hkijena.acaq5.utils.PathUtils;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Data containing a {@link ResultsTable}
 */
@ACAQDocumentation(name = "Results table")
public class ResultsTableData implements ACAQData {

    private ResultsTable table;

    /**
     * Loads a results table from a folder containing CSV file
     *
     * @param storageFilePath storage folder
     * @throws IOException triggered by {@link ResultsTable}
     */
    public ResultsTableData(Path storageFilePath) throws IOException {
        table = ResultsTable.open(PathUtils.findFileByExtensionIn(storageFilePath, ".csv").toString());
    }

    /**
     * Wraps a results table
     *
     * @param table wrapped table
     */
    public ResultsTableData(ResultsTable table) {
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

    @Override
    public ACAQData duplicate() {
        return new ResultsTableData((ResultsTable) table.clone());
    }

    public ResultsTable getTable() {
        return table;
    }

    /**
     * Adds the table to an existing table
     *
     * @param destination Target table
     */
    public void addToTable(ResultsTable destination) {
        for (int row = 0; row < table.size(); ++row) {
            destination.incrementCounter();
            for (int columnIndex = 0; columnIndex < table.getLastColumn(); ++columnIndex) {
                destination.addValue(table.getColumnHeading(columnIndex), table.getValue(table.getColumnHeading(columnIndex), row));
            }
        }
    }
}
