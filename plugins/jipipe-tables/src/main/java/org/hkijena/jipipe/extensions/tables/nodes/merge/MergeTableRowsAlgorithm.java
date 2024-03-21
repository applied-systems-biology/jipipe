/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.extensions.tables.nodes.merge;

import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.TableNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeMultiIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeMergingAlgorithm;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;

/**
 * Algorithm that integrates columns
 */
@SetJIPipeDocumentation(name = "Merge table rows", description = "Merges multiple tables into one table by merging the list of rows. Columns are automatically created if they do not exist.")
@ConfigureJIPipeNode(nodeTypeCategory = TableNodeTypeCategory.class, menuPath = "Merge")
@AddJIPipeInputSlot(value = ResultsTableData.class, slotName = "Input", create = true)
@AddJIPipeOutputSlot(value = ResultsTableData.class, slotName = "Output", create = true)
public class MergeTableRowsAlgorithm extends JIPipeMergingAlgorithm {

    /**
     * Creates a new instance
     *
     * @param info algorithm info
     */
    public MergeTableRowsAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public MergeTableRowsAlgorithm(MergeTableRowsAlgorithm other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeMultiIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ResultsTableData resultsTableData = new ResultsTableData();
        for (ResultsTableData tableData : iterationStep.getInputData(getFirstInputSlot(), ResultsTableData.class, progressInfo)) {
            resultsTableData.addRows(tableData);
        }
        iterationStep.addOutputData(getFirstOutputSlot(), resultsTableData, progressInfo);
    }
}
