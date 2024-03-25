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

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
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
import org.hkijena.jipipe.api.nodes.categories.AnnotationsNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.expressions.TableCellValueQueryExpression;
import org.hkijena.jipipe.plugins.tables.datatypes.ResultsTableData;

import java.util.*;

@SetJIPipeDocumentation(name = "Annotate table by merged columns", description = "Copies column values into an annotation. If the column has different values, the annotations are merged according to the selected merging strategy.")
@ConfigureJIPipeNode(nodeTypeCategory = AnnotationsNodeTypeCategory.class, menuPath = "For tables")
@AddJIPipeInputSlot(value = ResultsTableData.class, slotName = "Input", create = true)
@AddJIPipeOutputSlot(value = ResultsTableData.class, slotName = "Output", create = true)
public class ColumnsToAnnotationsAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private JIPipeTextAnnotationMergeMode rowMergingStrategy = JIPipeTextAnnotationMergeMode.Merge;
    private JIPipeTextAnnotationMergeMode annotationMergeStrategy = JIPipeTextAnnotationMergeMode.Merge;
    private TableCellValueQueryExpression filter = new TableCellValueQueryExpression();

    public ColumnsToAnnotationsAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public ColumnsToAnnotationsAlgorithm(ColumnsToAnnotationsAlgorithm other) {
        super(other);
        this.rowMergingStrategy = other.rowMergingStrategy;
        this.annotationMergeStrategy = other.annotationMergeStrategy;
        this.filter = new TableCellValueQueryExpression(other.filter);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ResultsTableData tableData = iterationStep.getInputData(getFirstInputSlot(), ResultsTableData.class, progressInfo);
        Multimap<String, String> annotationsToGenerate = HashMultimap.create();
        for (int row = 0; row < tableData.getRowCount(); row++) {
            for (int col = 0; col < tableData.getColumnCount(); col++) {
                if (filter.test(tableData, row, col)) {
                    annotationsToGenerate.put(tableData.getColumnName(col), "" + tableData.getValueAt(row, col));
                }
            }
        }
        List<JIPipeTextAnnotation> annotationList = new ArrayList<>();
        for (Map.Entry<String, Collection<String>> entry : annotationsToGenerate.asMap().entrySet()) {
            Set<String> values = new LinkedHashSet<>(entry.getValue());
            List<JIPipeTextAnnotation> forName = new ArrayList<>();
            for (String value : values) {
                forName.add(new JIPipeTextAnnotation(entry.getKey(), value));
            }
            annotationList.addAll(rowMergingStrategy.merge(forName));
        }
        iterationStep.addOutputData(getFirstOutputSlot(), tableData, annotationList, annotationMergeStrategy, progressInfo);
    }

    @SetJIPipeDocumentation(name = "Row merging strategy", description = "Important if the rows of the selected columns have different values. Determines how they are merged. " +
            "Ordered by row index.")
    @JIPipeParameter("row-merging-strategy")
    public JIPipeTextAnnotationMergeMode getRowMergingStrategy() {
        return rowMergingStrategy;
    }

    @JIPipeParameter("row-merging-strategy")
    public void setRowMergingStrategy(JIPipeTextAnnotationMergeMode rowMergingStrategy) {
        this.rowMergingStrategy = rowMergingStrategy;
    }

    @SetJIPipeDocumentation(name = "Annotation merge strategy", description = "Determines what happens if there is already an annotation with the same column name.")
    @JIPipeParameter("annotation-merge-strategy")
    public JIPipeTextAnnotationMergeMode getAnnotationMergeStrategy() {
        return annotationMergeStrategy;
    }

    @JIPipeParameter("annotation-merge-strategy")
    public void setAnnotationMergeStrategy(JIPipeTextAnnotationMergeMode annotationMergeStrategy) {
        this.annotationMergeStrategy = annotationMergeStrategy;
    }

    @SetJIPipeDocumentation(name = "Filter", description = "Filters the columns and/or table cell values. ")
    @JIPipeParameter("filter")
    public TableCellValueQueryExpression getFilter() {
        return filter;
    }

    @JIPipeParameter("filter")
    public void setFilter(TableCellValueQueryExpression filter) {
        this.filter = filter;
    }
}
