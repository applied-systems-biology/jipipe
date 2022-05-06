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

import com.google.common.collect.ImmutableList;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.TableNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.expressions.StringQueryExpression;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.extensions.tables.datatypes.TableColumn;
import org.hkijena.jipipe.extensions.tables.datatypes.TableColumnNormalization;
import org.hkijena.jipipe.utils.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Algorithm that integrates columns
 */
@JIPipeDocumentation(name = "Merge table columns", description = "Merges multiple tables into one table by merging the list of columns. " +
        "The generated table is sized according to the table with the most rows. In missing columns, the values are filled in. " +
        "This node allows the merging of columns (e.g. merging multiple time series tables by their time). To do this, determine such " +
        "merged columns via a parameter.")
@JIPipeNode(nodeTypeCategory = TableNodeTypeCategory.class, menuPath = "Merge")
@JIPipeInputSlot(value = ResultsTableData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ResultsTableData.class, slotName = "Output", autoCreate = true)
public class MergeTableColumnsAlgorithm extends JIPipeMergingAlgorithm {

    private TableColumnNormalization rowNormalization = TableColumnNormalization.ZeroOrEmpty;
    private StringQueryExpression columnFilter = new StringQueryExpression();
    private StringQueryExpression mergedColumns = new StringQueryExpression("false");

    /**
     * Creates a new instance
     *
     * @param info algorithm info
     */
    public MergeTableColumnsAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public MergeTableColumnsAlgorithm(MergeTableColumnsAlgorithm other) {
        super(other);
        this.rowNormalization = other.rowNormalization;
        this.columnFilter = new StringQueryExpression(other.columnFilter);
        this.mergedColumns = new StringQueryExpression(other.mergedColumns);
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

        // Find which columns should be merged
        Set<String> mergedColumns = new HashSet<>();
        Map<String, List<TableColumn>> groups = columnList.stream().collect(Collectors.groupingBy(TableColumn::getLabel));
        for (Map.Entry<String, List<TableColumn>> entry : groups.entrySet()) {
            if (entry.getValue().size() > 1 && this.mergedColumns.test(entry.getKey())) {
                mergedColumns.add(entry.getKey());
            }
        }

        if (mergedColumns.isEmpty()) {
            // No merging, we can use the faster and more simple algorithm
            // Normalize to the same number of rows
            columnList = rowNormalization.normalize(columnList);
            Set<String> existing = new HashSet<>();
            ResultsTableData outputData = new ResultsTableData();
            outputData.addRows(nRow);
            for (TableColumn column : columnList) {
                String name = StringUtils.makeUniqueString(column.getLabel(), ".", existing);
                existing.add(name);
                outputData.addColumn(name, column);
            }
            dataBatch.addOutputData(getFirstOutputSlot(), outputData, progressInfo);
        } else {
            ResultsTableData outputData = new ResultsTableData();
            Map<String, ResultsTableData> conditionTables = new HashMap<>();
            StringBuilder stringBuilder = new StringBuilder();
            Set<String> existing = new HashSet<>();

            // Collect all per-condition tables
            for (ResultsTableData inputTable : inputTables) {
                ResultsTableData uniqueColumnInputTable = (ResultsTableData) inputTable.duplicate(progressInfo);

                // Rename to make columns unique (except merged ones)
                for (String columnName : ImmutableList.copyOf(uniqueColumnInputTable.getColumnNames())) {
                    if (mergedColumns.contains(columnName))
                        continue;
                    String newName = StringUtils.makeUniqueString(columnName, ".", existing);
                    existing.add(newName);
                    uniqueColumnInputTable.renameColumn(columnName, newName);
                }

                for (int row = 0; row < uniqueColumnInputTable.getRowCount(); row++) {
                    stringBuilder.setLength(0);
                    for (String mergedColumn : mergedColumns) {
                        stringBuilder.append("\n");
                        int col = uniqueColumnInputTable.getColumnIndex(mergedColumn);
                        if (col >= 0)
                            stringBuilder.append(uniqueColumnInputTable.getValueAt(row, col));
                    }
                    String condition = stringBuilder.toString();

                    ResultsTableData rowTable = uniqueColumnInputTable.getRow(row);
                    ResultsTableData mergedRowTable = conditionTables.getOrDefault(condition, null);
                    if (mergedRowTable == null) {
                        conditionTables.put(condition, rowTable);
                    } else {
                        rowTable.removeColumns(mergedColumns);
                        mergedRowTable.addColumns(Collections.singleton(rowTable), true, rowNormalization);
                    }
                }
            }

            // Merge per-condition tables into the output
            for (ResultsTableData value : conditionTables.values()) {
                outputData.addRows(value);
            }

            dataBatch.addOutputData(getFirstOutputSlot(), outputData, progressInfo);
        }
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

    @JIPipeDocumentation(name = "Merged columns", description = "Expression to filter all columns that should be merged. " +
            "By default columns will be assigned unique names if there are duplicates. This filter determines which input columns should be " +
            "merged by their value. All merged columns are handled together.")
    @JIPipeParameter("merged-columns")
    public StringQueryExpression getMergedColumns() {
        return mergedColumns;
    }

    @JIPipeParameter("merged-columns")
    public void setMergedColumns(StringQueryExpression mergedColumns) {
        this.mergedColumns = mergedColumns;
    }
}
