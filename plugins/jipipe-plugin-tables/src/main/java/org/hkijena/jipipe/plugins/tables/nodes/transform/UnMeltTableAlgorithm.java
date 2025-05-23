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
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.expressions.AddJIPipeExpressionParameterVariable;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameter;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.plugins.expressions.StringQueryExpression;
import org.hkijena.jipipe.plugins.expressions.variables.JIPipeTextAnnotationsExpressionParameterVariablesInfo;
import org.hkijena.jipipe.plugins.tables.datatypes.*;
import org.hkijena.jipipe.utils.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@SetJIPipeDocumentation(name = "Pivot table", description = "Moves values located in a value column into separate columns according to a set of categorization columns. Also known as dcast in R.")
@ConfigureJIPipeNode(nodeTypeCategory = TableNodeTypeCategory.class, menuPath = "Transform")
@AddJIPipeInputSlot(value = ResultsTableData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = ResultsTableData.class, name = "Output", create = true)
public class UnMeltTableAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private StringQueryExpression valueColumn = new StringQueryExpression();
    private StringQueryExpression categoryColumns = new StringQueryExpression();
    private JIPipeExpressionParameter newColumnName = new JIPipeExpressionParameter("JOIN_STRING(category_values, \"_\")");
    private TableColumnNormalization columnNormalization = TableColumnNormalization.ZeroOrEmpty;

    /**
     * Creates a new instance
     *
     * @param info algorithm info
     */
    public UnMeltTableAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public UnMeltTableAlgorithm(UnMeltTableAlgorithm other) {
        super(other);
        this.valueColumn = new StringQueryExpression(other.valueColumn);
        this.categoryColumns = new StringQueryExpression(other.categoryColumns);
        this.newColumnName = new JIPipeExpressionParameter(other.newColumnName);
        this.columnNormalization = other.columnNormalization;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ResultsTableData input = iterationStep.getInputData(getFirstInputSlot(), ResultsTableData.class, progressInfo);

        JIPipeExpressionVariablesMap variables = new JIPipeExpressionVariablesMap(iterationStep);

        List<String> categoryColumnNames = new ArrayList<>();
        for (String columnName : input.getColumnNames()) {
            if (categoryColumns.test(columnName, variables)) {
                categoryColumnNames.add(columnName);
            }
        }
        String valueColumnName = valueColumn.queryFirst(input.getColumnNames(), variables);
        int valueColumnIndex = input.getColumnIndex(valueColumnName);
        Map<String, List<Object>> categorizedValues = new HashMap<>();

        List<String> categoryColumnValues = new ArrayList<>();
        for (int row = 0; row < input.getRowCount(); row++) {
            Object value = input.getValueAt(row, valueColumnIndex);
            categoryColumnValues.clear();
            for (String columnName : categoryColumnNames) {
                categoryColumnValues.add(input.getValueAsString(row, columnName));
            }

            variables.set("value", value);
            variables.set("category_values", categoryColumnValues);
            variables.set("category_columns", categoryColumnNames);

            String category = newColumnName.evaluateToString(variables);
            if (StringUtils.isNullOrEmpty(category))
                continue;
            List<Object> values = categorizedValues.getOrDefault(category, null);
            if (values == null) {
                values = new ArrayList<>();
                categorizedValues.put(category, values);
            }
            values.add(value);
        }

        List<TableColumnData> unNormalizedColumns = new ArrayList<>();
        for (Map.Entry<String, List<Object>> entry : categorizedValues.entrySet()) {
            List<Object> values = entry.getValue();
            if (values.stream().anyMatch(o -> !(o instanceof Number))) {
                String[] array = new String[values.size()];
                for (int i = 0; i < values.size(); i++) {
                    array[i] = StringUtils.nullToEmpty(values.get(i));
                }
                unNormalizedColumns.add(new StringArrayTableColumnData(array, entry.getKey()));
            } else {
                double[] array = new double[values.size()];
                for (int i = 0; i < values.size(); i++) {
                    array[i] = ((Number) values.get(i)).doubleValue();
                }
                unNormalizedColumns.add(new DoubleArrayTableColumnData(array, entry.getKey()));
            }
        }

        List<TableColumnData> normalizedColumns = columnNormalization.normalize(unNormalizedColumns);
        ResultsTableData output = new ResultsTableData(normalizedColumns);


        iterationStep.addOutputData(getFirstOutputSlot(), output, progressInfo);
    }

    @SetJIPipeDocumentation(name = "New column name", description = "The function that creates the new column name. If the returned string is empty or null, then the value will be skipped.")
    @JIPipeParameter(value = "new-column-name")
    @AddJIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
    @AddJIPipeExpressionParameterVariable(name = "Category values", key = "category_values", description = "The values of the selected categories")
    @AddJIPipeExpressionParameterVariable(name = "Category columns", key = "category_columns", description = "The column names of the selected categories")
    @AddJIPipeExpressionParameterVariable(name = "Value", key = "value", description = "The current value")
    public JIPipeExpressionParameter getNewColumnName() {
        return newColumnName;
    }

    @JIPipeParameter("new-column-name")
    public void setNewColumnName(JIPipeExpressionParameter newColumnName) {
        this.newColumnName = newColumnName;
    }

    @SetJIPipeDocumentation(name = "Value column", description = "Determines the column that contains the value")
    @JIPipeParameter(value = "value-column", important = true, uiOrder = -100)
    public StringQueryExpression getValueColumn() {
        return valueColumn;
    }

    @JIPipeParameter("value-column")
    public void setValueColumn(StringQueryExpression valueColumn) {
        this.valueColumn = valueColumn;
    }

    @SetJIPipeDocumentation(name = "Category columns")
    @JIPipeParameter(value = "category-columns", important = true, uiOrder = -90)
    public StringQueryExpression getCategoryColumns() {
        return categoryColumns;
    }

    @JIPipeParameter("category-columns")
    public void setCategoryColumns(StringQueryExpression categoryColumns) {
        this.categoryColumns = categoryColumns;
    }

    @SetJIPipeDocumentation(name = "Column length normalization", description = "Determines what happens with columns that have fewer values than the number of output table rows")
    @JIPipeParameter("column-normalization")
    public TableColumnNormalization getColumnNormalization() {
        return columnNormalization;
    }

    @JIPipeParameter("column-normalization")
    public void setColumnNormalization(TableColumnNormalization columnNormalization) {
        this.columnNormalization = columnNormalization;
    }
}
