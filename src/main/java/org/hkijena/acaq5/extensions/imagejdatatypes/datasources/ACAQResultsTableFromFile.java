package org.hkijena.acaq5.extensions.imagejdatatypes.datasources;

import ij.measure.ResultsTable;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.*;
import org.hkijena.acaq5.extensions.filesystem.api.dataypes.ACAQFileData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.ACAQResultsTableData;

import java.io.IOException;

@ACAQDocumentation(name = "Results table from file")
@AlgorithmInputSlot(value = ACAQFileData.class, slotName = "Files", autoCreate = true)
@AlgorithmOutputSlot(value = ACAQResultsTableData.class, slotName = "Results table", autoCreate = true)
@AlgorithmMetadata(category = ACAQAlgorithmCategory.DataSource)
public class ACAQResultsTableFromFile extends ACAQIteratingAlgorithm {

    public ACAQResultsTableFromFile(ACAQAlgorithmDeclaration declaration) {
        super(declaration);
    }

    public ACAQResultsTableFromFile(ACAQResultsTableFromFile other) {
        super(other);
    }

    @Override
    protected void runIteration(ACAQDataInterface dataInterface) {
        ACAQFileData fileData = dataInterface.getInputData(getFirstInputSlot());
        try {
            ResultsTable resultsTable = ResultsTable.open(fileData.getFilePath().toString());
            dataInterface.addOutputData(getFirstOutputSlot(), new ACAQResultsTableData(resultsTable));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void reportValidity(ACAQValidityReport report) {
    }
}
