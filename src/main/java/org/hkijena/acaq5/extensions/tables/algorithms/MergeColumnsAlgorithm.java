package org.hkijena.acaq5.extensions.tables.algorithms;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;
import org.hkijena.acaq5.api.ACAQRunnerSubStatus;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.*;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.ResultsTableData;
import org.hkijena.acaq5.extensions.tables.datatypes.TableColumn;
import org.hkijena.acaq5.utils.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.hkijena.acaq5.api.algorithm.ACAQMergingAlgorithm.MERGING_ALGORITHM_DESCRIPTION;

/**
 * Algorithm that integrates columns
 */
@ACAQDocumentation(name = "Merge columns", description = "Merges multiple table columns into a table." + "\n\n" + MERGING_ALGORITHM_DESCRIPTION)
@ACAQOrganization(algorithmCategory = ACAQAlgorithmCategory.Processor, menuPath = "Tables")
@AlgorithmInputSlot(value = TableColumn.class, slotName = "Input", autoCreate = true)
@AlgorithmOutputSlot(value = ResultsTableData.class, slotName = "Output", autoCreate = true)
public class MergeColumnsAlgorithm extends ACAQMergingAlgorithm {

    /**
     * Creates a new instance
     *
     * @param declaration algorithm declaration
     */
    public MergeColumnsAlgorithm(ACAQAlgorithmDeclaration declaration) {
        super(declaration);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public MergeColumnsAlgorithm(MergeColumnsAlgorithm other) {
        super(other);
    }

    @Override
    protected void runIteration(ACAQMultiDataInterface dataInterface, ACAQRunnerSubStatus subProgress, Consumer<ACAQRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        Map<String, TableColumn> columnMap = new HashMap<>();
        for (TableColumn tableColumn : dataInterface.getInputData(getFirstInputSlot(), TableColumn.class)) {
            String name = StringUtils.isNullOrEmpty(tableColumn.getLabel()) ? "Column" : tableColumn.getLabel();
            name = StringUtils.makeUniqueString(name, " ", columnMap.keySet());
            columnMap.put(name, tableColumn);
        }
        ResultsTableData resultsTableData = new ResultsTableData(columnMap);
        dataInterface.addOutputData(getFirstOutputSlot(), resultsTableData);
    }

    @Override
    public void reportValidity(ACAQValidityReport report) {
    }
}
