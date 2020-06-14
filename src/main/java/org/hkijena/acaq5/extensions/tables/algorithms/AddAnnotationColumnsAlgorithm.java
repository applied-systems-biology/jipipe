package org.hkijena.acaq5.extensions.tables.algorithms;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;
import org.hkijena.acaq5.api.ACAQRunnerSubStatus;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.*;
import org.hkijena.acaq5.api.data.ACAQAnnotation;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.ResultsTableData;

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.hkijena.acaq5.api.algorithm.ACAQMergingAlgorithm.MERGING_ALGORITHM_DESCRIPTION;

/**
 * Algorithm that integrates columns
 */
@ACAQDocumentation(name = "Add annotations as columns", description = "Adds column annotations to the table as new columns.")
@ACAQOrganization(algorithmCategory = ACAQAlgorithmCategory.Processor, menuPath = "Tables")
@AlgorithmInputSlot(value = ResultsTableData.class, slotName = "Input", autoCreate = true)
@AlgorithmOutputSlot(value = ResultsTableData.class, slotName = "Output", autoCreate = true)
public class AddAnnotationColumnsAlgorithm extends ACAQSimpleIteratingAlgorithm {

    private String annotationPrefix = "annotation:";

    /**
     * Creates a new instance
     *
     * @param declaration algorithm declaration
     */
    public AddAnnotationColumnsAlgorithm(ACAQAlgorithmDeclaration declaration) {
        super(declaration);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public AddAnnotationColumnsAlgorithm(AddAnnotationColumnsAlgorithm other) {
        super(other);
        this.annotationPrefix = other.annotationPrefix;
    }

    @Override
    protected void runIteration(ACAQDataInterface dataInterface, ACAQRunnerSubStatus subProgress, Consumer<ACAQRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        ResultsTableData resultsTableData = new ResultsTableData(dataInterface.getInputData(getFirstInputSlot(), ResultsTableData.class));
        for (Map.Entry<String, ACAQAnnotation> entry : dataInterface.getAnnotations().entrySet()) {
            int col = resultsTableData.addColumn(annotationPrefix + entry.getKey(), true);
            for (int row = 0; row < resultsTableData.getRowCount(); row++) {
                resultsTableData.setValueAt(entry.getValue() != null ? "" + entry.getValue().getValue() : "", row, col);
            }
        }
        dataInterface.addOutputData(getFirstOutputSlot(), resultsTableData);
    }

    @ACAQDocumentation(name = "Annotation prefix", description = "Prefix added to columns generated from data annotations.")
    @ACAQParameter("annotation-prefix")
    public String getAnnotationPrefix() {
        return annotationPrefix;
    }

    @ACAQParameter("annotation-prefix")
    public void setAnnotationPrefix(String annotationPrefix) {
        this.annotationPrefix = annotationPrefix;
    }

    @Override
    public void reportValidity(ACAQValidityReport report) {
    }
}
