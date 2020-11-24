package org.hkijena.jipipe.extensions.tables.algorithms;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeAnnotation;
import org.hkijena.jipipe.api.data.JIPipeAnnotationMergeStrategy;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.AnnotationsNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.parameters.expressions.TableCellValueQueryExpression;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;

import java.util.*;

@JIPipeDocumentation(name = "Annotate table by merged columns", description = "Copies column values into an annotation. If the column has different values, the annotations are merged according to the selected merging strategy.")
@JIPipeOrganization(nodeTypeCategory = AnnotationsNodeTypeCategory.class, menuPath = "Generate")
@JIPipeInputSlot(value = ResultsTableData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ResultsTableData.class, slotName = "Output", autoCreate = true)
public class ColumnsToAnnotationsAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private JIPipeAnnotationMergeStrategy rowMergingStrategy = JIPipeAnnotationMergeStrategy.Merge;
    private JIPipeAnnotationMergeStrategy annotationMergeStrategy = JIPipeAnnotationMergeStrategy.Merge;
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
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ResultsTableData tableData = dataBatch.getInputData(getFirstInputSlot(), ResultsTableData.class, progressInfo);
        Multimap<String, String> annotationsToGenerate = HashMultimap.create();
        for (int row = 0; row < tableData.getRowCount(); row++) {
            for (int col = 0; col < tableData.getColumnCount(); col++) {
                if(filter.test(tableData, row, col)) {
                    annotationsToGenerate.put(tableData.getColumnName(col), "" + tableData.getValueAt(row, col));
                }
            }
        }
        List<JIPipeAnnotation> annotationList = new ArrayList<>();
        for (Map.Entry<String, Collection<String>> entry : annotationsToGenerate.asMap().entrySet()) {
            Set<String> values = new LinkedHashSet<>(entry.getValue());
            List<JIPipeAnnotation> forName = new ArrayList<>();
            for (String value : values) {
                forName.add(new JIPipeAnnotation(entry.getKey(), value));
            }
            annotationList.addAll(rowMergingStrategy.merge(forName));
        }
        dataBatch.addOutputData(getFirstOutputSlot(), tableData, annotationList, annotationMergeStrategy, progressInfo);
    }

    @JIPipeDocumentation(name = "Row merging strategy", description = "Important if the rows of the selected columns have different values. Determines how they are merged. " +
            "Ordered by row index.")
    @JIPipeParameter("row-merging-strategy")
    public JIPipeAnnotationMergeStrategy getRowMergingStrategy() {
        return rowMergingStrategy;
    }

    @JIPipeParameter("row-merging-strategy")
    public void setRowMergingStrategy(JIPipeAnnotationMergeStrategy rowMergingStrategy) {
        this.rowMergingStrategy = rowMergingStrategy;
    }

    @JIPipeDocumentation(name = "Annotation merge strategy", description = "Determines what happens if there is already an annotation with the same column name.")
    @JIPipeParameter("annotation-merge-strategy")
    public JIPipeAnnotationMergeStrategy getAnnotationMergeStrategy() {
        return annotationMergeStrategy;
    }

    @JIPipeParameter("annotation-merge-strategy")
    public void setAnnotationMergeStrategy(JIPipeAnnotationMergeStrategy annotationMergeStrategy) {
        this.annotationMergeStrategy = annotationMergeStrategy;
    }

    @JIPipeDocumentation(name = "Filter", description = "Filters the columns and/or table cell values. " + TableCellValueQueryExpression.DOCUMENTATION_DESCRIPTION)
    @JIPipeParameter("filter")
    public TableCellValueQueryExpression getFilter() {
        return filter;
    }

    @JIPipeParameter("filter")
    public void setFilter(TableCellValueQueryExpression filter) {
        this.filter = filter;
    }
}
