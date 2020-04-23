package org.hkijena.acaq5.extensions.imagejdatatypes.datasources;

import ij.measure.ResultsTable;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQRunnerSubStatus;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.*;
import org.hkijena.acaq5.extensions.filesystem.api.dataypes.FileData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.ResultsTableData;

import java.io.IOException;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Imports {@link ResultsTableData} from a file
 */
@ACAQDocumentation(name = "Results table from file")
@AlgorithmInputSlot(value = FileData.class, slotName = "Files", autoCreate = true)
@AlgorithmOutputSlot(value = ResultsTableData.class, slotName = "Results table", autoCreate = true)
@AlgorithmMetadata(category = ACAQAlgorithmCategory.DataSource)
public class ResultsTableFromFile extends ACAQIteratingAlgorithm {

    /**
     * @param declaration algorithm declaration
     */
    public ResultsTableFromFile(ACAQAlgorithmDeclaration declaration) {
        super(declaration);
    }

    /**
     * Copies the algorithm
     *
     * @param other the original
     */
    public ResultsTableFromFile(ResultsTableFromFile other) {
        super(other);
    }

    @Override
    protected void runIteration(ACAQDataInterface dataInterface, ACAQRunnerSubStatus subProgress, Consumer<ACAQRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        FileData fileData = dataInterface.getInputData(getFirstInputSlot(), FileData.class);
        try {
            ResultsTable resultsTable = ResultsTable.open(fileData.getFilePath().toString());
            dataInterface.addOutputData(getFirstOutputSlot(), new ResultsTableData(resultsTable));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void reportValidity(ACAQValidityReport report) {
    }
}
