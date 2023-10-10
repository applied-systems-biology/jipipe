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

package org.hkijena.jipipe.extensions.tables.nodes.columns;

import org.apache.commons.lang3.math.NumberUtils;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.TableNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.databatch.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.expressions.StringQueryExpression;
import org.hkijena.jipipe.extensions.tables.datatypes.DoubleArrayTableColumn;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.extensions.tables.datatypes.TableColumn;

/**
 * Algorithm that removes columns
 */
@JIPipeDocumentation(name = "To numeric column", description = "Converts one or multiple columns into a numeric column")
@JIPipeNode(nodeTypeCategory = TableNodeTypeCategory.class)
@JIPipeInputSlot(value = ResultsTableData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ResultsTableData.class, slotName = "Output", autoCreate = true)
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
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ResultsTableData table = (ResultsTableData) dataBatch.getInputData(getFirstInputSlot(), ResultsTableData.class, progressInfo).duplicate(progressInfo);
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
        dataBatch.addOutputData(getFirstOutputSlot(), table, progressInfo);
    }

    @JIPipeDocumentation(name = "Filters", description = "Filter expression that is used to find columns to be converted. ")
    @JIPipeParameter("filters")
    public StringQueryExpression getFilters() {
        return filters;
    }

    @JIPipeParameter("filters")
    public void setFilters(StringQueryExpression filters) {
        this.filters = filters;
    }

    @JIPipeDocumentation(name = "Only convert if possible", description = "If enabled, values are tested if they are numeric before a conversion is applied. If not, then " +
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
