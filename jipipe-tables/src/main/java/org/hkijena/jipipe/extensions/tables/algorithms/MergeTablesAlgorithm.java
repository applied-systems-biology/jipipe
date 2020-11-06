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

package org.hkijena.jipipe.extensions.tables.algorithms;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeRunnerSubStatus;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeMergingAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeMergingDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.categories.TableNodeTypeCategory;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;

import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.hkijena.jipipe.api.nodes.JIPipeMergingAlgorithm.MERGING_ALGORITHM_DESCRIPTION;

/**
 * Algorithm that integrates columns
 */
@JIPipeDocumentation(name = "Merge tables", description = "Merges multiple tables into one table. Columns are automatically created if they do not exist."
        + "\n\n" + MERGING_ALGORITHM_DESCRIPTION)
@JIPipeOrganization(nodeTypeCategory = TableNodeTypeCategory.class, menuPath = "Merge")
@JIPipeInputSlot(value = ResultsTableData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ResultsTableData.class, slotName = "Output", autoCreate = true)
public class MergeTablesAlgorithm extends JIPipeMergingAlgorithm {

    /**
     * Creates a new instance
     *
     * @param info algorithm info
     */
    public MergeTablesAlgorithm(JIPipeNodeInfo info) {
        super(info);
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
    protected void runIteration(JIPipeMergingDataBatch dataBatch, JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        ResultsTableData resultsTableData = new ResultsTableData();
        for (ResultsTableData tableData : dataBatch.getInputData(getFirstInputSlot(), ResultsTableData.class)) {
            resultsTableData.mergeWith(tableData);
        }
        dataBatch.addOutputData(getFirstOutputSlot(), resultsTableData);
    }
}
