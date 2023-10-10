package org.hkijena.jipipe.extensions.utils.algorithms.datatable;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeDataAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeDataAnnotationMergeMode;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.data.JIPipeDataTable;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.AnnotationsNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.databatch.JIPipeSingleDataBatch;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.expressions.AnnotationQueryExpression;
import org.hkijena.jipipe.extensions.expressions.DataAnnotationQueryExpression;

import java.util.ArrayList;
import java.util.List;

@JIPipeDocumentation(name = "Push annotations into data tables", description = "Copies the annotations of a data table into the table itself")
@JIPipeInputSlot(value = JIPipeDataTable.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = JIPipeDataTable.class, slotName = "Output", autoCreate = true)
@JIPipeNode(nodeTypeCategory = AnnotationsNodeTypeCategory.class, menuPath = "For data tables")
public class PushDataTableAnnotations extends JIPipeSimpleIteratingAlgorithm {

    private AnnotationQueryExpression textAnnotationFilter = new AnnotationQueryExpression();

    private DataAnnotationQueryExpression dataAnnotationFilter = new DataAnnotationQueryExpression();
    private JIPipeTextAnnotationMergeMode textAnnotationMergeMode = JIPipeTextAnnotationMergeMode.OverwriteExisting;

    private JIPipeDataAnnotationMergeMode dataAnnotationMergeMode = JIPipeDataAnnotationMergeMode.OverwriteExisting;

    public PushDataTableAnnotations(JIPipeNodeInfo info) {
        super(info);
    }

    public PushDataTableAnnotations(PushDataTableAnnotations other) {
        super(other);
        this.textAnnotationFilter = new AnnotationQueryExpression(other.textAnnotationFilter);
        this.dataAnnotationFilter = new DataAnnotationQueryExpression(other.dataAnnotationFilter);
        this.textAnnotationMergeMode = other.textAnnotationMergeMode;
        this.dataAnnotationMergeMode = other.dataAnnotationMergeMode;
    }

    @Override
    protected void runIteration(JIPipeSingleDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        JIPipeDataTable inputData = dataBatch.getInputData(getFirstInputSlot(), JIPipeDataTable.class, progressInfo);
        JIPipeDataTable outputData = new JIPipeDataTable(inputData, true, progressInfo);
        List<JIPipeTextAnnotation> textAnnotationList = new ArrayList<>(textAnnotationFilter.queryAll(dataBatch.getMergedTextAnnotations().values()));
        List<JIPipeDataAnnotation> dataAnnotationList = new ArrayList<>(dataAnnotationFilter.queryAll(dataBatch.getMergedDataAnnotations().values()));

        for (int row = 0; row < outputData.getRowCount(); row++) {

            // Text annotations
            List<JIPipeTextAnnotation> textAnnotationsForRow = new ArrayList<>(outputData.getTextAnnotations(row));
            textAnnotationsForRow.addAll(textAnnotationList);
            List<JIPipeTextAnnotation> mergedTextAnnotations = textAnnotationMergeMode.merge(textAnnotationsForRow);
            for (JIPipeTextAnnotation textAnnotation : mergedTextAnnotations) {
                outputData.setTextAnnotation(row, textAnnotation);
            }

            // Data annotations
            List<JIPipeDataAnnotation> dataAnnotationsForRow = new ArrayList<>(outputData.getDataAnnotations(row));
            dataAnnotationsForRow.addAll(dataAnnotationList);
            List<JIPipeDataAnnotation> mergedDataAnnotations = dataAnnotationMergeMode.merge(dataAnnotationsForRow);
            for (JIPipeDataAnnotation dataAnnotation : mergedDataAnnotations) {
                outputData.setDataAnnotation(row, dataAnnotation);
            }

        }

        dataBatch.addOutputData(getFirstOutputSlot(), outputData, progressInfo);
    }

    @JIPipeDocumentation(name = "Text annotation filter", description = "Allows to filter only specific text annotations. Set to false to completely ignore all text annotations.")
    @JIPipeParameter("text-annotation-filter")
    public AnnotationQueryExpression getTextAnnotationFilter() {
        return textAnnotationFilter;
    }

    @JIPipeParameter("text-annotation-filter")
    public void setTextAnnotationFilter(AnnotationQueryExpression textAnnotationFilter) {
        this.textAnnotationFilter = textAnnotationFilter;
    }

    @JIPipeDocumentation(name = "Data annotation filter", description = "Allows to filter only specific data annotations. Set to false to completely ignore all data annotations.")
    @JIPipeParameter("data-annotation-filter")
    public DataAnnotationQueryExpression getDataAnnotationFilter() {
        return dataAnnotationFilter;
    }

    @JIPipeParameter("data-annotation-filter")
    public void setDataAnnotationFilter(DataAnnotationQueryExpression dataAnnotationFilter) {
        this.dataAnnotationFilter = dataAnnotationFilter;
    }

    @JIPipeDocumentation(name = "Data annotation merge mode", description = "Determines what happens if there is already an existing data annotation with the same name")
    @JIPipeParameter("data-annotation-merge-mode")
    public JIPipeDataAnnotationMergeMode getDataAnnotationMergeMode() {
        return dataAnnotationMergeMode;
    }

    @JIPipeParameter("data-annotation-merge-mode")
    public void setDataAnnotationMergeMode(JIPipeDataAnnotationMergeMode dataAnnotationMergeMode) {
        this.dataAnnotationMergeMode = dataAnnotationMergeMode;
    }

    @JIPipeDocumentation(name = "Text annotation merge mode", description = "Determines what happens if there is already an existing text annotation with the same name")
    @JIPipeParameter("text-annotation-merge-mode")
    public JIPipeTextAnnotationMergeMode getTextAnnotationMergeMode() {
        return textAnnotationMergeMode;
    }

    @JIPipeParameter("text-annotation-merge-mode")
    public void setTextAnnotationMergeMode(JIPipeTextAnnotationMergeMode textAnnotationMergeMode) {
        this.textAnnotationMergeMode = textAnnotationMergeMode;
    }
}
