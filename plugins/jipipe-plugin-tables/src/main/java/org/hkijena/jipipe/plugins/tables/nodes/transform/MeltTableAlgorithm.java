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
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.TableNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.expressions.StringQueryExpression;
import org.hkijena.jipipe.plugins.parameters.library.primitives.optional.OptionalStringParameter;
import org.hkijena.jipipe.plugins.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.utils.StringUtils;

import java.util.HashSet;
import java.util.Set;

/**
 * Algorithm that integrates columns
 */
@SetJIPipeDocumentation(name = "Melt table", description = "Moves values from different columns (value columns) into their own rows, " +
        "while the other columns are copied (category columns). Also known as unpivot operation.")
@AddJIPipeNodeAlias(nodeTypeCategory = TableNodeTypeCategory.class, menuPath = "Transform", aliasName = "Unpivot table")
@ConfigureJIPipeNode(nodeTypeCategory = TableNodeTypeCategory.class, menuPath = "Transform")
@AddJIPipeInputSlot(value = ResultsTableData.class, slotName = "Input", create = true)
@AddJIPipeOutputSlot(value = ResultsTableData.class, slotName = "Output", create = true)
public class MeltTableAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private StringQueryExpression valueColumns = new StringQueryExpression();
    private String outputValueColumnName = "Value";
    private OptionalStringParameter outputCategoryColumnName = new OptionalStringParameter("Category", true);

    /**
     * Creates a new instance
     *
     * @param info algorithm info
     */
    public MeltTableAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public MeltTableAlgorithm(MeltTableAlgorithm other) {
        super(other);
        this.valueColumns = new StringQueryExpression(other.valueColumns);
        this.outputCategoryColumnName = new OptionalStringParameter(other.outputCategoryColumnName);
        this.outputValueColumnName = other.outputValueColumnName;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ResultsTableData input = iterationStep.getInputData(getFirstInputSlot(), ResultsTableData.class, progressInfo);
        ResultsTableData output = new ResultsTableData();

        Set<String> valueColumnNames = new HashSet<>();
        for (String columnName : input.getColumnNames()) {
            if (valueColumns.test(columnName))
                valueColumnNames.add(columnName);
            else
                output.addColumn(columnName, input.isStringColumn(columnName));
        }

        // Create output category value column
        String uniqueOutputCategoryColumn;
        if (outputCategoryColumnName.isEnabled()) {
            uniqueOutputCategoryColumn = StringUtils.makeUniqueString(outputCategoryColumnName.getContent(), ".", output.getColumnNames());
            output.addColumn(uniqueOutputCategoryColumn, true);
        } else {
            uniqueOutputCategoryColumn = null;
        }

        // Create the output value column
        String uniqueOutputValueColumn;
        {
            boolean valueColumnIsStringColumn = valueColumnNames.stream().anyMatch(input::isStringColumn);
            uniqueOutputValueColumn = StringUtils.makeUniqueString(outputValueColumnName, ".", output.getColumnNames());
            output.addColumn(uniqueOutputValueColumn, valueColumnIsStringColumn);
        }

        for (int row = 0; row < input.getRowCount(); row++) {
            for (String columnName : valueColumnNames) {
                output.addRow();
                int targetRow = output.getRowCount() - 1;

                // Write value
                output.setValueAt(input.getValueAt(row, input.getColumnIndex(columnName)),
                        targetRow,
                        uniqueOutputValueColumn);

                // Write column
                if (uniqueOutputCategoryColumn != null) {
                    output.setValueAt(columnName,
                            targetRow,
                            uniqueOutputCategoryColumn);
                }

                // Copy category data
                for (String categoryColumnName : input.getColumnNames()) {
                    if (!valueColumnNames.contains(categoryColumnName)) {
                        output.setValueAt(input.getValueAt(row, input.getColumnIndex(categoryColumnName)),
                                targetRow, categoryColumnName);
                    }
                }
            }
        }

        iterationStep.addOutputData(getFirstOutputSlot(), output, progressInfo);
    }

    @SetJIPipeDocumentation(name = "Value columns", description = "Allows to select the value columns by their name via a filter expression. ")
    @JIPipeParameter("value-columns")
    public StringQueryExpression getValueColumns() {
        return valueColumns;
    }

    @JIPipeParameter("value-columns")
    public void setValueColumns(StringQueryExpression valueColumns) {
        this.valueColumns = valueColumns;
    }

    @SetJIPipeDocumentation(name = "Output value column name", description = "Name of the output value column. If the column already exists as category column," +
            " a unique name is generated based on this one.")
    @JIPipeParameter("output-value-column-name")
    public String getOutputValueColumnName() {
        return outputValueColumnName;
    }

    @JIPipeParameter("output-value-column-name")
    public void setOutputValueColumnName(String outputValueColumnName) {
        this.outputValueColumnName = outputValueColumnName;
    }

    @SetJIPipeDocumentation(name = "Output category column name", description = "If enabled, the source column name is added into another column. If the column already exists," +
            " a unique name is generated.")
    @JIPipeParameter("output-category-column-name")
    public OptionalStringParameter getOutputCategoryColumnName() {
        return outputCategoryColumnName;
    }

    @JIPipeParameter("output-category-column-name")
    public void setOutputCategoryColumnName(OptionalStringParameter outputCategoryColumnName) {
        this.outputCategoryColumnName = outputCategoryColumnName;
    }
}
