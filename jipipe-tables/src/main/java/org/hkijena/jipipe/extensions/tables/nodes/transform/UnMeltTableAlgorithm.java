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

package org.hkijena.jipipe.extensions.tables.nodes.transform;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeIssueReport;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.TableNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.expressions.StringQueryExpression;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.utils.StringUtils;

import java.util.HashSet;
import java.util.Set;


@JIPipeDocumentation(name = "Un-Melt table", description = "Moves values located in a value column into separate columns according to a set of categorization columns.")
@JIPipeNode(nodeTypeCategory = TableNodeTypeCategory.class)
@JIPipeInputSlot(value = ResultsTableData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ResultsTableData.class, slotName = "Output", autoCreate = true)
public class UnMeltTableAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private StringQueryExpression valueColumns = new StringQueryExpression();
    private String outputValueColumnName = "Value";

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
        this.valueColumns = new StringQueryExpression(other.valueColumns);
        this.outputValueColumnName = other.outputValueColumnName;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ResultsTableData input = dataBatch.getInputData(getFirstInputSlot(), ResultsTableData.class, progressInfo);
        ResultsTableData output = new ResultsTableData();

        Set<String> valueColumnNames = new HashSet<>();
        for (String columnName : input.getColumnNames()) {
            if (valueColumns.test(columnName))
                valueColumnNames.add(columnName);
            else
                output.addColumn(columnName, input.isStringColumn(columnName));
        }

        // Create the output value column
        boolean valueColumnIsStringColumn = valueColumnNames.stream().anyMatch(input::isStringColumn);
        String uniqueOutputValueColumn = StringUtils.makeUniqueString(outputValueColumnName, ".", output.getColumnNames());
        output.addColumn(uniqueOutputValueColumn, valueColumnIsStringColumn);

        for (int row = 0; row < input.getRowCount(); row++) {
            for (String columnName : valueColumnNames) {
                output.addRow();
                int targetRow = output.getRowCount() - 1;

                // Write value
                output.setValueAt(input.getValueAt(row, input.getColumnIndex(columnName)),
                        targetRow,
                        uniqueOutputValueColumn);

                // Copy category data
                for (String categoryColumnName : input.getColumnNames()) {
                    if (!valueColumnNames.contains(categoryColumnName)) {
                        output.setValueAt(input.getValueAt(row, input.getColumnIndex(categoryColumnName)),
                                targetRow, categoryColumnName);
                    }
                }
            }
        }

        dataBatch.addOutputData(getFirstOutputSlot(), output, progressInfo);
    }

    @Override
    public void reportValidity(JIPipeIssueReport report) {
        super.reportValidity(report);
        report.resolve("Output value column name").checkNonEmpty(outputValueColumnName, this);
    }

    @JIPipeDocumentation(name = "Value columns", description = "Allows to select the value columns by their name via a filter expression. ")
    @JIPipeParameter("value-columns")
    public StringQueryExpression getValueColumns() {
        return valueColumns;
    }

    @JIPipeParameter("value-columns")
    public void setValueColumns(StringQueryExpression valueColumns) {
        this.valueColumns = valueColumns;
    }

    @JIPipeDocumentation(name = "Output value column name", description = "Name of the output value column. If the column already exists as category column," +
            " a unique name is generated based on this one.")
    @JIPipeParameter("output-value-column-name")
    public String getOutputValueColumnName() {
        return outputValueColumnName;
    }

    @JIPipeParameter("output-value-column-name")
    public void setOutputValueColumnName(String outputValueColumnName) {
        this.outputValueColumnName = outputValueColumnName;
    }
}
