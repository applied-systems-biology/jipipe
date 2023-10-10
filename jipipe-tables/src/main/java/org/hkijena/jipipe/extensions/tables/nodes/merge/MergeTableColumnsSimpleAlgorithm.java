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

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.TableNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.databatch.JIPipeMergingDataBatch;
import org.hkijena.jipipe.api.nodes.utils.JIPipeMergingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.expressions.StringQueryExpression;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.extensions.tables.datatypes.TableColumn;
import org.hkijena.jipipe.extensions.tables.datatypes.TableColumnNormalization;
import org.hkijena.jipipe.utils.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Algorithm that integrates columns
 */
@JIPipeDocumentation(name = "Merge table columns (simple)", description = "Merges multiple tables into one table by merging the list of columns. " +
        "The generated table is sized according to the table with the most rows. In missing columns, the values are filled in. " +
        "The algorithm behind this is not 'smart' and is not capable of supplementing a table with data from another table. ")
@JIPipeNode(nodeTypeCategory = TableNodeTypeCategory.class, menuPath = "Merge")
@JIPipeInputSlot(value = ResultsTableData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ResultsTableData.class, slotName = "Output", autoCreate = true)
public class MergeTableColumnsSimpleAlgorithm extends JIPipeMergingAlgorithm {

    private TableColumnNormalization rowNormalization = TableColumnNormalization.ZeroOrEmpty;
    private StringQueryExpression columnFilter = new StringQueryExpression();

    /**
     * Creates a new instance
     *
     * @param info algorithm info
     */
    public MergeTableColumnsSimpleAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public MergeTableColumnsSimpleAlgorithm(MergeTableColumnsSimpleAlgorithm other) {
        super(other);
        this.rowNormalization = other.rowNormalization;
        this.columnFilter = new StringQueryExpression(other.columnFilter);
    }

    @Override
    protected void runIteration(JIPipeMergingDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        List<TableColumn> columnList = new ArrayList<>();
        List<ResultsTableData> inputTables = dataBatch.getInputData(getFirstInputSlot(), ResultsTableData.class, progressInfo);
        int nRow = 0;
        for (ResultsTableData tableData : inputTables) {
            nRow = Math.max(nRow, tableData.getRowCount());
            for (int col = 0; col < tableData.getColumnCount(); col++) {
                if (columnFilter.test(tableData.getColumnName(col))) {
                    TableColumn column = tableData.getColumnReference(col);
                    columnList.add(column);
                }
            }
        }

        // No merging, we can use the faster and more simple algorithm
        // Normalize to the same number of rows
        columnList = rowNormalization.normalize(columnList);
        Set<String> existing = new HashSet<>();
        ResultsTableData outputData = new ResultsTableData();
        outputData.addRows(nRow);
        for (TableColumn column : columnList) {
            String name = StringUtils.makeUniqueString(column.getLabel(), ".", existing);
            existing.add(name);
            outputData.addColumn(name, column, true);
        }
        dataBatch.addOutputData(getFirstOutputSlot(), outputData, progressInfo);
    }

    @JIPipeDocumentation(name = "Row normalization", description = "Determines how missing column values are handled if the input tables have different numbers of rows. " +
            "You can set it to zero/empty (depending on numeric or string type), to the row number (starting with zero), copy the last value, or cycle.")
    @JIPipeParameter("row-normalization")
    public TableColumnNormalization getRowNormalization() {
        return rowNormalization;
    }

    @JIPipeParameter("row-normalization")
    public void setRowNormalization(TableColumnNormalization rowNormalization) {
        this.rowNormalization = rowNormalization;
    }

    @JIPipeDocumentation(name = "Column filter", description = "Allows to filter the columns for their name. ")
    @JIPipeParameter("column-filter")
    public StringQueryExpression getColumnFilter() {
        return columnFilter;
    }

    @JIPipeParameter("column-filter")
    public void setColumnFilter(StringQueryExpression columnFilter) {
        this.columnFilter = columnFilter;
    }
}
