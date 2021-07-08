package org.hkijena.jipipe.extensions.tables.algorithms;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeAnnotation;
import org.hkijena.jipipe.api.data.JIPipeAnnotationMergeStrategy;
import org.hkijena.jipipe.api.nodes.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.AnnotationsNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.parameters.primitives.OptionalAnnotationNameParameter;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.utils.JsonUtils;

import java.util.ArrayList;
import java.util.List;

@JIPipeDocumentation(name = "Annotate with table properties", description = "Annotates tables with information about the table")
@JIPipeOrganization(nodeTypeCategory = AnnotationsNodeTypeCategory.class, menuPath = "Generate")
@JIPipeInputSlot(value = ResultsTableData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ResultsTableData.class, slotName = "Output", autoCreate = true)
public class AnnotateByTablePropertiesAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private JIPipeAnnotationMergeStrategy annotationMergeStrategy = JIPipeAnnotationMergeStrategy.OverwriteExisting;
    private OptionalAnnotationNameParameter rowCountAnnotation = new OptionalAnnotationNameParameter("Num Rows", true);
    private OptionalAnnotationNameParameter columnCountAnnotation = new OptionalAnnotationNameParameter("Num Columns", true);
    private OptionalAnnotationNameParameter columnNamesAnnotation = new OptionalAnnotationNameParameter("Column names", false);

    public AnnotateByTablePropertiesAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public AnnotateByTablePropertiesAlgorithm(AnnotateByTablePropertiesAlgorithm other) {
        super(other);
        this.annotationMergeStrategy = other.annotationMergeStrategy;
        this.rowCountAnnotation = new OptionalAnnotationNameParameter(other.rowCountAnnotation);
        this.columnCountAnnotation = new OptionalAnnotationNameParameter(other.columnCountAnnotation);
        this.columnNamesAnnotation = new OptionalAnnotationNameParameter(other.columnNamesAnnotation);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ResultsTableData data = dataBatch.getInputData(getFirstInputSlot(), ResultsTableData.class, progressInfo);

        List<JIPipeAnnotation> annotationList = new ArrayList<>();

        rowCountAnnotation.addAnnotationIfEnabled(annotationList, "" + data.getRowCount());
        columnCountAnnotation.addAnnotationIfEnabled(annotationList, "" + data.getColumnCount());
        if (columnNamesAnnotation.isEnabled())
            columnNamesAnnotation.addAnnotationIfEnabled(annotationList, JsonUtils.toJsonString(data.getColumnNames()));

        dataBatch.addOutputData(getFirstOutputSlot(), data, annotationList, annotationMergeStrategy, progressInfo);
    }

    @JIPipeDocumentation(name = "Annotation merge strategy", description = "Determines how the newly generated annotations are merged with existing annotations.")
    @JIPipeParameter("annotation-merge-strategy")
    public JIPipeAnnotationMergeStrategy getAnnotationMergeStrategy() {
        return annotationMergeStrategy;
    }

    @JIPipeParameter("annotation-merge-strategy")
    public void setAnnotationMergeStrategy(JIPipeAnnotationMergeStrategy annotationMergeStrategy) {
        this.annotationMergeStrategy = annotationMergeStrategy;
    }

    @JIPipeDocumentation(name = "Annotate with row count", description = "If enabled, annotate with the row count")
    @JIPipeParameter("row-count-annotation")
    public OptionalAnnotationNameParameter getRowCountAnnotation() {
        return rowCountAnnotation;
    }

    @JIPipeParameter("row-count-annotation")
    public void setRowCountAnnotation(OptionalAnnotationNameParameter rowCountAnnotation) {
        this.rowCountAnnotation = rowCountAnnotation;
    }

    @JIPipeDocumentation(name = "Annotate with column count", description = "If enabled, annotate with the column count")
    @JIPipeParameter("column-count-annotation")
    public OptionalAnnotationNameParameter getColumnCountAnnotation() {
        return columnCountAnnotation;
    }

    @JIPipeParameter("column-count-annotation")
    public void setColumnCountAnnotation(OptionalAnnotationNameParameter columnCountAnnotation) {
        this.columnCountAnnotation = columnCountAnnotation;
    }

    @JIPipeDocumentation(name = "Annotate with column names", description = "If enabled, annotate with the column names as JSON string")
    @JIPipeParameter("column-names-annotation")
    public OptionalAnnotationNameParameter getColumnNamesAnnotation() {
        return columnNamesAnnotation;
    }

    @JIPipeParameter("column-names-annotation")
    public void setColumnNamesAnnotation(OptionalAnnotationNameParameter columnNamesAnnotation) {
        this.columnNamesAnnotation = columnNamesAnnotation;
    }
}
