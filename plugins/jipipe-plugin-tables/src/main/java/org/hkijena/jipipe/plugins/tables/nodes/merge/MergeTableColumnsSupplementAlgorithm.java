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

package org.hkijena.jipipe.plugins.tables.nodes.merge;

import com.google.common.collect.Sets;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.TableNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.expressions.AddJIPipeExpressionParameterVariable;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.plugins.expressions.StringQueryExpression;
import org.hkijena.jipipe.plugins.expressions.variables.JIPipeTextAnnotationsExpressionParameterVariablesInfo;
import org.hkijena.jipipe.plugins.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.plugins.tables.datatypes.TableColumnNormalization;
import org.hkijena.jipipe.utils.StringUtils;

import java.util.*;

/**
 * Algorithm that integrates columns
 */
@SetJIPipeDocumentation(name = "Merge table columns (supplement, iterative)", description = "Merges columns from the source table into the target table. You can choose one or multiple reference columns that determine which rows from each table should be merged together. " +
        "Please note that this node can only merge two tables per iteration step. Use the 'Merge table columns (supplement)' node if you for example want to merge multiple sources into one/multiple target(s).")
@ConfigureJIPipeNode(nodeTypeCategory = TableNodeTypeCategory.class, menuPath = "Merge")
@AddJIPipeInputSlot(value = ResultsTableData.class, name = "Target", create = true)
@AddJIPipeInputSlot(value = ResultsTableData.class, name = "Source", create = true)
@AddJIPipeOutputSlot(value = ResultsTableData.class, name = "Output", create = true)
public class MergeTableColumnsSupplementAlgorithm extends JIPipeIteratingAlgorithm {

    private TableColumnNormalization rowNormalization = TableColumnNormalization.ZeroOrEmpty;
    private StringQueryExpression columnFilter = new StringQueryExpression();
    private StringQueryExpression referenceColumns = new StringQueryExpression("false");

    private boolean extendTarget = true;

    private boolean skipEmptyTarget = true;

    /**
     * Creates a new instance
     *
     * @param info algorithm info
     */
    public MergeTableColumnsSupplementAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public MergeTableColumnsSupplementAlgorithm(MergeTableColumnsSupplementAlgorithm other) {
        super(other);
        this.rowNormalization = other.rowNormalization;
        this.columnFilter = new StringQueryExpression(other.columnFilter);
        this.referenceColumns = new StringQueryExpression(other.referenceColumns);
        this.extendTarget = other.extendTarget;
        this.skipEmptyTarget = other.skipEmptyTarget;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ResultsTableData inputTarget = new ResultsTableData(iterationStep.getInputData("Target", ResultsTableData.class, progressInfo));
        ResultsTableData inputSource = new ResultsTableData(iterationStep.getInputData("Source", ResultsTableData.class, progressInfo));

        JIPipeExpressionVariablesMap variables = new JIPipeExpressionVariablesMap(iterationStep);

        // Choose the reference columns
        List<String> selectedReferenceColumns = referenceColumns.queryAll(new ArrayList<>(Sets.union(new HashSet<>(inputTarget.getColumnNames()), new HashSet<>(inputSource.getColumnNames()))), variables);
        Map<String, ResultsTableData> splitInputTarget = splitTableByCondition(inputTarget, selectedReferenceColumns, "target");
        Map<String, ResultsTableData> splitInputSource = splitTableByCondition(inputSource, selectedReferenceColumns, "source");

        // For non-unique column names from source -> target generate new values
        Map<String, String> sourceToTargetColumnNameMap = new HashMap<>();
        {
            Set<String> existing = new HashSet<>(inputTarget.getColumnNames());
            for (String columnName : inputSource.getColumnNames()) {
                String newColumnName;
                if (selectedReferenceColumns.contains(columnName))
                    newColumnName = columnName;
                else {
                    newColumnName = StringUtils.makeUniqueString(columnName, ".", existing);
                }
                existing.add(newColumnName);
                sourceToTargetColumnNameMap.put(columnName, newColumnName);
            }
        }

        ResultsTableData outputTable = new ResultsTableData();
        outputTable.copyColumnSchemaFrom(inputTarget);

        for (String condition : Sets.union(splitInputTarget.keySet(), splitInputSource.keySet())) {
            ResultsTableData target = splitInputTarget.getOrDefault(condition, new ResultsTableData());
            ResultsTableData source = splitInputSource.getOrDefault(condition, new ResultsTableData());
            boolean targetWasEmpty = target.getRowCount() == 0;
            if (target.getRowCount() <= 0 && skipEmptyTarget)
                continue;
            if (!extendTarget && source.getRowCount() > target.getRowCount()) {
                // Downsize the source table
                source = source.getRows(0, target.getRowCount());
            }
            // Apply normalization
            int nRow = Math.max(target.getRowCount(), source.getRowCount());
            target = rowNormalization.normalize(target, nRow);
            source = rowNormalization.normalize(source, nRow);

            // Apply merging
            ResultsTableData merged = target;
            for (String columnName : source.getColumnNames()) {
                if ((targetWasEmpty || !selectedReferenceColumns.contains(columnName)) && columnFilter.test(columnName, variables)) {
                    String newColumnName = sourceToTargetColumnNameMap.get(columnName);
                    merged.addColumn(newColumnName, source.getColumnReference(columnName), true);
                }
            }

            // Write to output
            outputTable.addRows(merged);
        }

        iterationStep.addOutputData(getFirstOutputSlot(), outputTable, progressInfo);
    }

    private Map<String, ResultsTableData> splitTableByCondition(ResultsTableData data, List<String> selectedReferenceColumns, String dummyConditionLabel) {
        Map<String, ResultsTableData> result = new HashMap<>();
        Set<String> existingConditions = new HashSet<>();
        StringBuilder conditionBuilder = new StringBuilder();
        for (int row = 0; row < data.getRowCount(); row++) {

            // Generate the condition
            conditionBuilder.setLength(0);
            for (String column : selectedReferenceColumns) {
                int columnIndex = data.getColumnIndex(column);
                if (columnIndex >= 0)
                    conditionBuilder.append(StringUtils.nullToEmpty(data.getValueAt(row, columnIndex))).append("\n");
                else
                    conditionBuilder.append("\n");
            }

            // Fix case where there is no reference column
            String condition = conditionBuilder.toString();
            if (conditionBuilder.length() == 0) {
                // Generate unique condition
                condition = StringUtils.makeUniqueString(dummyConditionLabel, " ", existingConditions);
            }
            existingConditions.add(condition);

            // Merge into existing table for this condition
            ResultsTableData conditionTargetTable = result.getOrDefault(condition, null);
            if (conditionTargetTable == null) {
                conditionTargetTable = data.getRow(row);
                result.put(condition, conditionTargetTable);
            } else {
                conditionTargetTable.addRows(data.getRow(row));
            }
        }

        return result;
    }

    @SetJIPipeDocumentation(name = "Row normalization", description = "Determines how missing column values are handled if the input tables have different numbers of rows. " +
            "You can set it to zero/empty (depending on numeric or string type), to the row number (starting with zero), copy the last value, or cycle.")
    @JIPipeParameter("row-normalization")
    public TableColumnNormalization getRowNormalization() {
        return rowNormalization;
    }

    @JIPipeParameter("row-normalization")
    public void setRowNormalization(TableColumnNormalization rowNormalization) {
        this.rowNormalization = rowNormalization;
    }

    @SetJIPipeDocumentation(name = "Column filter", description = "Determines which columns are copied from source to target.")
    @JIPipeParameter("column-filter")
    @AddJIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
    public StringQueryExpression getColumnFilter() {
        return columnFilter;
    }

    @JIPipeParameter("column-filter")
    public void setColumnFilter(StringQueryExpression columnFilter) {
        this.columnFilter = columnFilter;
    }

    @SetJIPipeDocumentation(name = "Reference columns", description = "Columns that should act as reference points for the merging of the rows from different tables.")
    @JIPipeParameter(value = "reference-columns", important = true)
    @AddJIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
    public StringQueryExpression getReferenceColumns() {
        return referenceColumns;
    }

    @JIPipeParameter("reference-columns")
    public void setReferenceColumns(StringQueryExpression referenceColumns) {
        this.referenceColumns = referenceColumns;
    }

    @SetJIPipeDocumentation(name = "Extend target to fit source", description = "If enabled, values of the target table are added if there are more rows in the source than in the target.")
    @JIPipeParameter("extend-target")
    public boolean isExtendTarget() {
        return extendTarget;
    }

    @JIPipeParameter("extend-target")
    public void setExtendTarget(boolean extendTarget) {
        this.extendTarget = extendTarget;
    }

    @SetJIPipeDocumentation(name = "Skip empty target conditions", description = "If enabled, skips the addition of values from the source if the target does not have an equivalent condition.")
    @JIPipeParameter("skip-empty-target")
    public boolean isSkipEmptyTarget() {
        return skipEmptyTarget;
    }

    @JIPipeParameter("skip-empty-target")
    public void setSkipEmptyTarget(boolean skipEmptyTarget) {
        this.skipEmptyTarget = skipEmptyTarget;
    }
}
