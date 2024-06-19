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

package org.hkijena.jipipe.plugins.tables.nodes.columns;

import org.apache.commons.lang3.math.NumberUtils;
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
import org.hkijena.jipipe.plugins.expressions.StringQueryExpression;
import org.hkijena.jipipe.plugins.tables.datatypes.DoubleArrayTableColumn;
import org.hkijena.jipipe.plugins.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.plugins.tables.datatypes.TableColumn;

/**
 * Algorithm that removes columns
 */
@SetJIPipeDocumentation(name = "To numeric column", description = "Converts one or multiple columns into a numeric column")
@ConfigureJIPipeNode(nodeTypeCategory = TableNodeTypeCategory.class)
@AddJIPipeInputSlot(value = ResultsTableData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = ResultsTableData.class, slotName = "Output", create = true)
public class ColumnToNumericAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private StringQueryExpression filters = new StringQueryExpression();
    private boolean onlyIfPossible = false;

    /**
     * Creates a new instance
     *
     * @param info algorithm info
     */
    public ColumnToNumericAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public ColumnToNumericAlgorithm(ColumnToNumericAlgorithm other) {
        super(other);
        this.filters = new StringQueryExpression(other.filters);
        this.onlyIfPossible = other.onlyIfPossible;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ResultsTableData table = (ResultsTableData) iterationStep.getInputData(getFirstInputSlot(), ResultsTableData.class, progressInfo).duplicate(progressInfo);
        int columnCount = table.getColumnCount();
        for (int col = 0; col < columnCount; col++) {
            String columnName = table.getColumnName(col);
            TableColumn columnReference = table.getColumnReference(col);
            if (filters.test(columnName) && !columnReference.isNumeric()) {
                if (onlyIfPossible) {
                    boolean success = true;
                    for (int i = 0; i < columnReference.getRows(); i++) {
                        if (!NumberUtils.isCreatable(columnReference.getRowAsString(i))) {
                            success = false;
                            break;
                        }
                    }
                    if (!success) {
                        continue;
                    }
                }
                double[] data = columnReference.getDataAsDouble(table.getRowCount());
                table.removeColumnAt(col);
                --col;
                --columnCount;
                table.addColumn(columnName, new DoubleArrayTableColumn(data, columnName), true);
            }
        }
        iterationStep.addOutputData(getFirstOutputSlot(), table, progressInfo);
    }

    @SetJIPipeDocumentation(name = "Filters", description = "Filter expression that is used to find columns to be converted. ")
    @JIPipeParameter("filters")
    public StringQueryExpression getFilters() {
        return filters;
    }

    @JIPipeParameter("filters")
    public void setFilters(StringQueryExpression filters) {
        this.filters = filters;
    }

    @SetJIPipeDocumentation(name = "Only convert if possible", description = "If enabled, values are tested if they are numeric before a conversion is applied. If not, then " +
            "the column is left alone.")
    @JIPipeParameter("only-if-possible")
    public boolean isOnlyIfPossible() {
        return onlyIfPossible;
    }

    @JIPipeParameter("only-if-possible")
    public void setOnlyIfPossible(boolean onlyIfPossible) {
        this.onlyIfPossible = onlyIfPossible;
    }
}
