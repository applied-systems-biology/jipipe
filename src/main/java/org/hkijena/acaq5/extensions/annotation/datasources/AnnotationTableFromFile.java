package org.hkijena.acaq5.extensions.annotation.datasources;

import ij.measure.ResultsTable;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;
import org.hkijena.acaq5.api.ACAQRunnerSubStatus;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.*;
import org.hkijena.acaq5.extensions.annotation.datatypes.AnnotationTableData;
import org.hkijena.acaq5.extensions.filesystem.dataypes.FileData;

import java.io.IOException;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Imports {@link org.hkijena.acaq5.extensions.annotation.datatypes.AnnotationTableData} from a file
 */
@ACAQDocumentation(name = "Annotation table from file")
@AlgorithmInputSlot(value = FileData.class, slotName = "Files", autoCreate = true)
@AlgorithmOutputSlot(value = AnnotationTableData.class, slotName = "Annotation table", autoCreate = true)
@ACAQOrganization(algorithmCategory = ACAQAlgorithmCategory.DataSource)
public class AnnotationTableFromFile extends ACAQSimpleIteratingAlgorithm {

    /**
     * @param declaration algorithm declaration
     */
    public AnnotationTableFromFile(ACAQAlgorithmDeclaration declaration) {
        super(declaration);
    }

    /**
     * Copies the algorithm
     *
     * @param other the original
     */
    public AnnotationTableFromFile(AnnotationTableFromFile other) {
        super(other);
    }

    @Override
    protected void runIteration(ACAQDataInterface dataInterface, ACAQRunnerSubStatus subProgress, Consumer<ACAQRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        FileData fileData = dataInterface.getInputData(getFirstInputSlot(), FileData.class);
        try {
            ResultsTable resultsTable = ResultsTable.open(fileData.getPath().toString());
            dataInterface.addOutputData(getFirstOutputSlot(), new AnnotationTableData(resultsTable));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void reportValidity(ACAQValidityReport report) {
    }
}
