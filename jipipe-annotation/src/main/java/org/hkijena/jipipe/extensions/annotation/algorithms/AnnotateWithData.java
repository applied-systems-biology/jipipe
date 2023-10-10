package org.hkijena.jipipe.extensions.annotation.algorithms;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeDataAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeDataAnnotationMergeMode;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.AnnotationsNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.databatch.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.utils.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.parameters.library.primitives.StringParameterSettings;
import org.hkijena.jipipe.utils.ResourceUtils;

import java.util.ArrayList;
import java.util.List;

@JIPipeDocumentation(name = "Annotate with data", description = "Annotates the incoming data with the other data.")
@JIPipeNode(nodeTypeCategory = AnnotationsNodeTypeCategory.class, menuPath = "For all data")
@JIPipeInputSlot(value = JIPipeData.class, slotName = "Input", autoCreate = true)
@JIPipeInputSlot(value = JIPipeData.class, slotName = "Annotation", autoCreate = true)
@JIPipeOutputSlot(value = JIPipeData.class, slotName = "Output", autoCreate = true)
public class AnnotateWithData extends JIPipeIteratingAlgorithm {

    private String annotationName = "Label";
    private boolean mergeInputAnnotations = true;
    private boolean mergeLabelAnnotations = true;
    private JIPipeTextAnnotationMergeMode annotationMergeStrategy = JIPipeTextAnnotationMergeMode.Merge;
    private JIPipeDataAnnotationMergeMode dataAnnotationMergeMode = JIPipeDataAnnotationMergeMode.OverwriteExisting;

    public AnnotateWithData(JIPipeNodeInfo info) {
        super(info);
    }

    public AnnotateWithData(AnnotateWithData other) {
        super(other);
        this.annotationName = other.annotationName;
        this.mergeInputAnnotations = other.mergeInputAnnotations;
        this.mergeLabelAnnotations = other.mergeLabelAnnotations;
        this.annotationMergeStrategy = other.annotationMergeStrategy;
        this.dataAnnotationMergeMode = other.dataAnnotationMergeMode;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        JIPipeDataSlot inputDataSlot = getInputSlot("Input");
        JIPipeDataSlot inputAnnotationSlot = getInputSlot("Annotation");
        int dataRow = dataBatch.getInputSlotRows().get(inputDataSlot);
        int annotationRow = dataBatch.getInputSlotRows().get(inputAnnotationSlot);

        List<JIPipeTextAnnotation> annotationList = new ArrayList<>();
        if (mergeInputAnnotations)
            annotationList.addAll(inputDataSlot.getTextAnnotations(dataRow));
        if (mergeLabelAnnotations)
            annotationList.addAll(inputAnnotationSlot.getTextAnnotations(annotationRow));
        List<JIPipeDataAnnotation> dataAnnotationList = new ArrayList<>();
        dataAnnotationList.add(new JIPipeDataAnnotation(annotationName, inputAnnotationSlot.getDataItemStore(annotationRow)));

        getFirstOutputSlot().addData(inputDataSlot.getDataItemStore(dataRow),
                annotationList,
                annotationMergeStrategy,
                dataAnnotationList,
                dataAnnotationMergeMode,
                progressInfo);
    }

    @JIPipeDocumentation(name = "Generated annotation", description = "The name of the generated data annotation")
    @StringParameterSettings(monospace = true, icon = ResourceUtils.RESOURCE_BASE_PATH + "/icons/data-types/data-annotation.png")
    @JIPipeParameter("annotation-name")
    public String getAnnotationName() {
        return annotationName;
    }

    @JIPipeParameter("annotation-name")
    public void setAnnotationName(String annotationName) {
        this.annotationName = annotationName;
    }

    @JIPipeDocumentation(name = "Data annotation merge strategy", description = "Determines how the created data annotation is merged with existing ones")
    @JIPipeParameter("data-annotation-merge-strategy")
    public JIPipeDataAnnotationMergeMode getDataAnnotationMergeMode() {
        return dataAnnotationMergeMode;
    }

    @JIPipeParameter("data-annotation-merge-strategy")
    public void setDataAnnotationMergeMode(JIPipeDataAnnotationMergeMode dataAnnotationMergeMode) {
        this.dataAnnotationMergeMode = dataAnnotationMergeMode;
    }

    @JIPipeDocumentation(name = "Copy text annotations from input", description = "If enabled, annotations from the 'Input' slot are copied into the output (Default).")
    @JIPipeParameter("merge-input-annotations")
    public boolean isMergeInputAnnotations() {
        return mergeInputAnnotations;
    }

    @JIPipeParameter("merge-input-annotations")
    public void setMergeInputAnnotations(boolean mergeInputAnnotations) {
        this.mergeInputAnnotations = mergeInputAnnotations;
    }

    @JIPipeDocumentation(name = "Copy text annotations from data annotation", description = "If enabled, annotations from the 'Annotation' slot are copied into the output.")
    @JIPipeParameter("merge-label-annotations")
    public boolean isMergeLabelAnnotations() {
        return mergeLabelAnnotations;
    }

    @JIPipeParameter("merge-label-annotations")
    public void setMergeLabelAnnotations(boolean mergeLabelAnnotations) {
        this.mergeLabelAnnotations = mergeLabelAnnotations;
    }

    @JIPipeDocumentation(name = "Text annotation merge strategy", description = "Determines how text annotations from 'Input' and 'Annotation' are merged, " +
            "if both are enabled.")
    @JIPipeParameter("annotation-merge-strategy")
    public JIPipeTextAnnotationMergeMode getAnnotationMergeStrategy() {
        return annotationMergeStrategy;
    }

    @JIPipeParameter("annotation-merge-strategy")
    public void setAnnotationMergeStrategy(JIPipeTextAnnotationMergeMode annotationMergeStrategy) {
        this.annotationMergeStrategy = annotationMergeStrategy;
    }
}
