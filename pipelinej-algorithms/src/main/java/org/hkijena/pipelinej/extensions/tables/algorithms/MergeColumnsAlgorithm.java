/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.pipelinej.extensions.tables.algorithms;

import org.hkijena.pipelinej.api.ACAQDocumentation;
import org.hkijena.pipelinej.api.ACAQOrganization;
import org.hkijena.pipelinej.api.ACAQRunnerSubStatus;
import org.hkijena.pipelinej.api.ACAQValidityReport;
import org.hkijena.pipelinej.api.algorithm.*;
import org.hkijena.pipelinej.extensions.tables.ResultsTableData;
import org.hkijena.pipelinej.extensions.tables.TableColumn;
import org.hkijena.pipelinej.utils.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.hkijena.pipelinej.api.algorithm.ACAQMergingAlgorithm.MERGING_ALGORITHM_DESCRIPTION;

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
    protected void runIteration(ACAQMergingDataBatch dataInterface, ACAQRunnerSubStatus subProgress, Consumer<ACAQRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
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
