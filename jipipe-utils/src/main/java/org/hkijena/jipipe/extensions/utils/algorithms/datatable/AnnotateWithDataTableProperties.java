package org.hkijena.jipipe.extensions.utils.algorithms.datatable;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.data.JIPipeDataTable;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.AnnotationsNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.databatch.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.utils.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.parameters.library.primitives.optional.OptionalAnnotationNameParameter;

import java.util.ArrayList;
import java.util.List;

@JIPipeDocumentation(name = "Annotate with data table properties", description = "Annotates data table data with properties (e.g., the size)")
@JIPipeInputSlot(value = JIPipeDataTable.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = JIPipeDataTable.class, slotName = "Output", autoCreate = true)
@JIPipeNode(nodeTypeCategory = AnnotationsNodeTypeCategory.class, menuPath = "For data tables")
public class AnnotateWithDataTableProperties extends JIPipeSimpleIteratingAlgorithm {

    private OptionalAnnotationNameParameter numRowsAnnotation = new OptionalAnnotationNameParameter("Num Rows", true);
    private OptionalAnnotationNameParameter numTextAnnotationColumns = new OptionalAnnotationNameParameter("Num Text Annotation Columns", false);
    private OptionalAnnotationNameParameter numDataAnnotationColumns = new OptionalAnnotationNameParameter("Num Text Annotation Columns", false);

    private JIPipeTextAnnotationMergeMode mergeMode = JIPipeTextAnnotationMergeMode.OverwriteExisting;

    public AnnotateWithDataTableProperties(JIPipeNodeInfo info) {
        super(info);
    }

    public AnnotateWithDataTableProperties(AnnotateWithDataTableProperties other) {
        super(other);
        this.numRowsAnnotation = new OptionalAnnotationNameParameter(other.numRowsAnnotation);
        this.numTextAnnotationColumns = new OptionalAnnotationNameParameter(other.numTextAnnotationColumns);
        this.numDataAnnotationColumns = new OptionalAnnotationNameParameter(other.numDataAnnotationColumns);
        this.mergeMode = other.mergeMode;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        JIPipeDataTable inputData = dataBatch.getInputData(getFirstInputSlot(), JIPipeDataTable.class, progressInfo);
        List<JIPipeTextAnnotation> annotationList = new ArrayList<>();
        numRowsAnnotation.addAnnotationIfEnabled(annotationList, inputData.getRowCount() + "");
        numTextAnnotationColumns.addAnnotationIfEnabled(annotationList, inputData.getTextAnnotationColumns().size() + "");
        numDataAnnotationColumns.addAnnotationIfEnabled(annotationList, inputData.getDataAnnotationColumns().size() + "");
        dataBatch.addOutputData(getFirstOutputSlot(), inputData, annotationList, mergeMode, progressInfo);
    }

    @JIPipeDocumentation(name = "Annotate with number of rows", description = "If enabled, annotate the data table with its number of rows")
    @JIPipeParameter("num-rows-annotation")
    public OptionalAnnotationNameParameter getNumRowsAnnotation() {
        return numRowsAnnotation;
    }

    @JIPipeParameter("num-rows-annotation")
    public void setNumRowsAnnotation(OptionalAnnotationNameParameter numRowsAnnotation) {
        this.numRowsAnnotation = numRowsAnnotation;
    }

    @JIPipeDocumentation(name = "Annotate with number of text annotation columns", description = "If enabled, annotate the data table with the number of text annotation columns")
    @JIPipeParameter("num-text-annotation-columns-annotation")
    public OptionalAnnotationNameParameter getNumTextAnnotationColumns() {
        return numTextAnnotationColumns;
    }

    @JIPipeParameter("num-text-annotation-columns-annotation")
    public void setNumTextAnnotationColumns(OptionalAnnotationNameParameter numTextAnnotationColumns) {
        this.numTextAnnotationColumns = numTextAnnotationColumns;
    }

    @JIPipeDocumentation(name = "Annotate with number of data annotation columns", description = "If enabled, annotate the data table with the number of data annotation columns")
    @JIPipeParameter("num-data-annotation-columns-annotation")
    public OptionalAnnotationNameParameter getNumDataAnnotationColumns() {
        return numDataAnnotationColumns;
    }

    @JIPipeParameter("num-data-annotation-columns-annotation")
    public void setNumDataAnnotationColumns(OptionalAnnotationNameParameter numDataAnnotationColumns) {
        this.numDataAnnotationColumns = numDataAnnotationColumns;
    }

    @JIPipeDocumentation(name = "Annotation merge mode", description = "Determines what happens if there is already an existing annotation with the same name")
    @JIPipeParameter("merge-mode")
    public JIPipeTextAnnotationMergeMode getMergeMode() {
        return mergeMode;
    }

    @JIPipeParameter("merge-mode")
    public void setMergeMode(JIPipeTextAnnotationMergeMode mergeMode) {
        this.mergeMode = mergeMode;
    }
}
