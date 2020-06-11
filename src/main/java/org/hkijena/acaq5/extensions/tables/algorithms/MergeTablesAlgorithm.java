package org.hkijena.acaq5.extensions.tables.algorithms;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;
import org.hkijena.acaq5.api.ACAQRunnerSubStatus;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.*;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.ResultsTableData;

import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.hkijena.acaq5.api.algorithm.ACAQMergingAlgorithm.MERGING_ALGORITHM_DESCRIPTION;

/**
 * Algorithm that integrates columns
 */
@ACAQDocumentation(name = "Merge tables", description = "Merges multiple tables into one table. Columns are automatically created if they do not exist."
        + "\n\n" + MERGING_ALGORITHM_DESCRIPTION)
@ACAQOrganization(algorithmCategory = ACAQAlgorithmCategory.Processor, menuPath = "Tables")
@AlgorithmInputSlot(value = ResultsTableData.class, slotName = "Input", autoCreate = true)
@AlgorithmOutputSlot(value = ResultsTableData.class, slotName = "Output", autoCreate = true)
public class MergeTablesAlgorithm extends ACAQMergingAlgorithm {

    /**
     * Creates a new instance
     *
     * @param declaration algorithm declaration
     */
    public MergeTablesAlgorithm(ACAQAlgorithmDeclaration declaration) {
        super(declaration);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public MergeTablesAlgorithm(MergeTablesAlgorithm other) {
        super(other);
    }

    @Override
    protected void runIteration(ACAQMultiDataInterface dataInterface, ACAQRunnerSubStatus subProgress, Consumer<ACAQRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        ResultsTableData resultsTableData = new ResultsTableData();
        for (ResultsTableData tableData : dataInterface.getInputData(getFirstInputSlot(), ResultsTableData.class)) {
            resultsTableData.mergeWith(tableData);
        }
        dataInterface.addOutputData(getFirstOutputSlot(), resultsTableData);
    }

    @Override
    public void reportValidity(ACAQValidityReport report) {
    }
}
