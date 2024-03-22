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

package org.hkijena.jipipe.extensions.tables.nodes.split;

import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.TableNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.extensions.expressions.StringQueryExpression;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Algorithm that integrates columns
 */
@SetJIPipeDocumentation(name = "Split table by columns", description = "Splits a table into multiple tables according to list of selected columns. " +
        "Sub-tables that have the same values in the selected columns are put into the same output table.")
@ConfigureJIPipeNode(nodeTypeCategory = TableNodeTypeCategory.class, menuPath = "Split")
@AddJIPipeInputSlot(value = ResultsTableData.class, slotName = "Input", create = true)
@AddJIPipeOutputSlot(value = ResultsTableData.class, slotName = "Output", create = true)
public class SplitTableByColumnsAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private StringQueryExpression columns = new StringQueryExpression();
    private boolean addAsAnnotations = false;

    /**
     * Creates a new instance
     *
     * @param info algorithm info
     */
    public SplitTableByColumnsAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public SplitTableByColumnsAlgorithm(SplitTableByColumnsAlgorithm other) {
        super(other);
        this.columns = new StringQueryExpression(other.columns);
        this.addAsAnnotations = other.addAsAnnotations;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ResultsTableData input = iterationStep.getInputData(getFirstInputSlot(), ResultsTableData.class, progressInfo);
        List<String> interestingColumns = columns.queryAll(input.getColumnNames(), new JIPipeExpressionVariablesMap());
        if (interestingColumns.isEmpty()) {
            iterationStep.addOutputData(getFirstOutputSlot(), input.duplicate(progressInfo), progressInfo);
        } else {
            List<String> rowConditions = new ArrayList<>();
            for (int row = 0; row < input.getRowCount(); row++) {
                int finalRow = row;
                rowConditions.add(interestingColumns.stream().map(col -> col + "=" + input.getValueAsString(finalRow, col)).collect(Collectors.joining(";")));
            }
            List<Integer> rows = new ArrayList<>(input.getRowCount());
            for (int i = 0; i < input.getRowCount(); i++) {
                rows.add(i);
            }
            Map<String, List<Integer>> groupedByCondition = rows.stream().collect(Collectors.groupingBy(rowConditions::get));
            for (Map.Entry<String, List<Integer>> entry : groupedByCondition.entrySet()) {
                ResultsTableData output = input.getRows(entry.getValue());
                List<JIPipeTextAnnotation> annotations = new ArrayList<>();
                if (addAsAnnotations && output.getRowCount() > 0) {
                    for (String column : interestingColumns) {
                        annotations.add(new JIPipeTextAnnotation(column, output.getValueAsString(0, column)));
                    }
                }
                iterationStep.addOutputData(getFirstOutputSlot(), output, annotations, JIPipeTextAnnotationMergeMode.Merge, progressInfo);
            }
        }
    }

    @SetJIPipeDocumentation(name = "Selected columns", description = "Expression that selects the columns. ")
    @JIPipeParameter("columns")
    public StringQueryExpression getColumns() {
        return columns;
    }

    @JIPipeParameter("columns")
    public void setColumns(StringQueryExpression columns) {
        this.columns = columns;
    }

    @SetJIPipeDocumentation(name = "Add as annotations", description = "If enabled, columns are added as annotation columns")
    @JIPipeParameter("add-as-annotations")
    public boolean isAddAsAnnotations() {
        return addAsAnnotations;
    }

    @JIPipeParameter("add-as-annotations")
    public void setAddAsAnnotations(boolean addAsAnnotations) {
        this.addAsAnnotations = addAsAnnotations;
    }
}
