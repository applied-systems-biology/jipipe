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
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.TableNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.expressions.StringQueryExpression;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.extensions.tables.datatypes.StringArrayTableColumn;

/**
 * Algorithm that removes columns
 */
@JIPipeDocumentation(name = "To string column", description = "Converts one or multiple columns into a string column")
@JIPipeOrganization(nodeTypeCategory = TableNodeTypeCategory.class)
@JIPipeInputSlot(value = ResultsTableData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ResultsTableData.class, slotName = "Output", autoCreate = true)
public class ColumnToStringAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private StringQueryExpression filters = new StringQueryExpression();

    /**
     * Creates a new instance
     *
     * @param info algorithm info
     */
    public ColumnToStringAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public ColumnToStringAlgorithm(ColumnToStringAlgorithm other) {
        super(other);
        this.filters = new StringQueryExpression(other.filters);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ResultsTableData table = (ResultsTableData) dataBatch.getInputData(getFirstInputSlot(), ResultsTableData.class, progressInfo).duplicate();
        int columnCount = table.getColumnCount();
        for (int col = 0; col < columnCount; col++) {
            String columnName = table.getColumnName(col);
            if (filters.test(columnName)) {
                String[] data = table.getColumnReference(col).getDataAsString(table.getRowCount());
                table.removeColumnAt(col);
                --col;
                --columnCount;
                table.addColumn(columnName, new StringArrayTableColumn(data, columnName));
            }
        }
        dataBatch.addOutputData(getFirstOutputSlot(), table, progressInfo);
    }

    @JIPipeDocumentation(name = "Filters", description = "Filter expression that is used to find columns to be converted. " + StringQueryExpression.DOCUMENTATION_DESCRIPTION)
    @JIPipeParameter("filters")
    public StringQueryExpression getFilters() {
        return filters;
    }

    @JIPipeParameter("filters")
    public void setFilters(StringQueryExpression filters) {
        this.filters = filters;
    }
}
