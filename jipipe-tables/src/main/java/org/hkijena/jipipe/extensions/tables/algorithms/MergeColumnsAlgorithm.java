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
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeMergingAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeMergingDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.categories.TableNodeTypeCategory;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.extensions.tables.datatypes.TableColumn;
import org.hkijena.jipipe.utils.StringUtils;

import java.util.HashMap;
import java.util.Map;

import static org.hkijena.jipipe.api.nodes.JIPipeMergingAlgorithm.MERGING_ALGORITHM_DESCRIPTION;

/**
 * Algorithm that integrates columns
 */
@JIPipeDocumentation(name = "Merge columns", description = "Merges multiple table columns into a table." + "\n\n" + MERGING_ALGORITHM_DESCRIPTION)
@JIPipeNode(nodeTypeCategory = TableNodeTypeCategory.class, menuPath = "Merge")
@JIPipeInputSlot(value = TableColumn.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ResultsTableData.class, slotName = "Output", autoCreate = true)
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
    protected void runIteration(JIPipeMergingDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        Map<String, TableColumn> columnMap = new HashMap<>();
        for (TableColumn tableColumn : dataBatch.getInputData(getFirstInputSlot(), TableColumn.class, progressInfo)) {
            String name = StringUtils.isNullOrEmpty(tableColumn.getLabel()) ? "Column" : tableColumn.getLabel();
            name = StringUtils.makeUniqueString(name, " ", columnMap.keySet());
            columnMap.put(name, tableColumn);
        }
        ResultsTableData resultsTableData = new ResultsTableData(columnMap);
        dataBatch.addOutputData(getFirstOutputSlot(), resultsTableData, progressInfo);
    }
}
