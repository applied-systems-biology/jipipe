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

package org.hkijena.jipipe.plugins.tables.nodes.transform;

import com.google.common.primitives.Doubles;
import gnu.trove.list.TDoubleList;
import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import gnu.trove.set.TDoubleSet;
import gnu.trove.set.hash.TDoubleHashSet;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.TableNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.expressions.AddJIPipeExpressionParameterVariable;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameter;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.plugins.expressions.TableColumnSourceExpressionParameter;
import org.hkijena.jipipe.plugins.expressions.variables.JIPipeTextAnnotationsExpressionParameterVariablesInfo;
import org.hkijena.jipipe.plugins.parameters.library.collections.ParameterCollectionList;
import org.hkijena.jipipe.plugins.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.plugins.tables.datatypes.TableColumnData;
import org.hkijena.jipipe.utils.StringUtils;

import java.util.*;

@SetJIPipeDocumentation(name = "Count values in table column", description = "Counts the values in the provided column and outputs a table with the number of occurrences of each value. " +
        "This node supports string columns and converts numeric columns into string columns. Null values are replaced with empty strings.")
@ConfigureJIPipeNode(nodeTypeCategory = TableNodeTypeCategory.class, menuPath = "Transform")
@AddJIPipeInputSlot(value = ResultsTableData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = ResultsTableData.class, name = "Output", create = true)
public class TableColumnToCountAlgorithm extends JIPipeSimpleIteratingAlgorithm {
    private TableColumnSourceExpressionParameter inputColumn = new TableColumnSourceExpressionParameter(TableColumnSourceExpressionParameter.TableSourceType.ExistingColumn, "\"Column name\"");
    private JIPipeExpressionParameter valueFilter = new JIPipeExpressionParameter("value != \"\"");

    public TableColumnToCountAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public TableColumnToCountAlgorithm(TableColumnToCountAlgorithm other) {
        super(other);
        this.inputColumn = new TableColumnSourceExpressionParameter(other.inputColumn);
        this.valueFilter = new JIPipeExpressionParameter(other.valueFilter);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ResultsTableData inputTable = iterationStep.getInputData(getFirstInputSlot(), ResultsTableData.class, progressInfo);
        JIPipeExpressionVariablesMap variables = new JIPipeExpressionVariablesMap(iterationStep);

        TableColumnData inputColumn = this.inputColumn.pickOrGenerateColumn(inputTable, variables);

        String[] inputColumnValues = inputColumn.getDataAsString(inputColumn.getRows());

        // Filter values
        TObjectIntMap<String> counts = new TObjectIntHashMap<>();
        for (String value : inputColumnValues) {
            variables.set("value", StringUtils.nullToEmpty(value));
            if (valueFilter.evaluateToBoolean(variables)) {
                counts.adjustOrPutValue(StringUtils.nullToEmpty(value), 1, 1);
            }
        }

        // Create output table
        ResultsTableData outputTable = new ResultsTableData();
        for (String columnName : counts.keySet()) {
            outputTable.addStringColumn(columnName);
        }
        outputTable.addRow();
        for (String columnName : counts.keySet()) {
            outputTable.setValueAt(counts.get(columnName), 0, columnName);
        }

        iterationStep.addOutputData(getFirstOutputSlot(), outputTable, progressInfo);
    }

    @SetJIPipeDocumentation(name = "Input column", description = "The column to be used for creating a histogram")
    @JIPipeParameter(value = "input-column", uiOrder = -100, important = true)
    @AddJIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
    public TableColumnSourceExpressionParameter getInputColumn() {
        return inputColumn;
    }

    @JIPipeParameter("input-column")
    public void setInputColumn(TableColumnSourceExpressionParameter inputColumn) {
        this.inputColumn = inputColumn;
    }
}
