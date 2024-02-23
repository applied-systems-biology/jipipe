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
 *
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
import org.hkijena.jipipe.extensions.tables.datatypes.TableColumn;
import org.hkijena.jipipe.utils.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Algorithm that integrates columns
 */
@SetJIPipeDocumentation(name = "Merge columns", description = "Merges multiple table columns into a table.")
@ConfigureJIPipeNode(nodeTypeCategory = TableNodeTypeCategory.class, menuPath = "Merge")
@AddJIPipeInputSlot(value = TableColumn.class, slotName = "Input", create = true)
@AddJIPipeOutputSlot(value = ResultsTableData.class, slotName = "Output", create = true)
public class MergeColumnsAlgorithm extends JIPipeMergingAlgorithm {

    /**
     * Creates a new instance
     *
     * @param info algorithm info
     */
    public MergeColumnsAlgorithm(JIPipeNodeInfo info) {
        super(info);
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
    protected void runIteration(JIPipeMultiIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        Map<String, TableColumn> columnMap = new HashMap<>();
        for (TableColumn tableColumn : iterationStep.getInputData(getFirstInputSlot(), TableColumn.class, progressInfo)) {
            String name = StringUtils.isNullOrEmpty(tableColumn.getLabel()) ? "Column" : tableColumn.getLabel();
            name = StringUtils.makeUniqueString(name, " ", columnMap.keySet());
            columnMap.put(name, tableColumn);
        }
        ResultsTableData resultsTableData = new ResultsTableData(columnMap);
        iterationStep.addOutputData(getFirstOutputSlot(), resultsTableData, progressInfo);
    }
}
