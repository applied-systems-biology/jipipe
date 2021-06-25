package org.hkijena.jipipe.extensions.annotation.algorithms;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeAnnotation;
import org.hkijena.jipipe.api.data.JIPipeAnnotationMergeStrategy;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.AnnotationsNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.parameters.primitives.StringParameterSettings;
import org.hkijena.jipipe.utils.ResourceUtils;

import java.util.ArrayList;
import java.util.List;

@JIPipeDocumentation(name = "Annotate with data", description = "Annotates the incoming data with the other data.")
@JIPipeOrganization(nodeTypeCategory = AnnotationsNodeTypeCategory.class, menuPath = "Generate")
@JIPipeInputSlot(value = JIPipeData.class, slotName = "Input", autoCreate = true)
@JIPipeInputSlot(value = JIPipeData.class, slotName = "Annotation", autoCreate = true)
@JIPipeOutputSlot(value = JIPipeData.class, slotName = "Output", autoCreate = true, inheritedSlot = "Input")
public class AnnotateWithData extends JIPipeIteratingAlgorithm {

    private String annotationName = "Label";
    private boolean mergeInputAnnotations = true;
    private boolean mergeLabelAnnotations = true;
    private JIPipeAnnotationMergeStrategy annotationMergeStrategy = JIPipeAnnotationMergeStrategy.Merge;

    public AnnotateWithData(JIPipeNodeInfo info) {
        super(info);
    }

    public AnnotateWithData(AnnotateWithData other) {
        super(other);
        this.annotationName = other.annotationName;
        this.mergeInputAnnotations = other.mergeInputAnnotations;
        this.mergeLabelAnnotations = other.mergeLabelAnnotations;
        this.annotationMergeStrategy = other.annotationMergeStrategy;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        JIPipeDataSlot inputDataSlot = getInputSlot("Input");
        JIPipeDataSlot inputAnnotationSlot = getInputSlot("Annotation");
        int dataRow = dataBatch.getInputSlotRows().get(inputDataSlot);
        int annotationRow = dataBatch.getInputSlotRows().get(inputAnnotationSlot);

        List<JIPipeAnnotation> annotationList = new ArrayList<>();
        if(mergeInputAnnotations)
            annotationList.addAll(inputDataSlot.getAnnotations(dataRow));
        if(mergeLabelAnnotations)
            annotationList.addAll(inputAnnotationSlot.getAnnotations(annotationRow));

        getFirstOutputSlot().addData(inputDataSlot.getVirtualData(dataRow),
                annotationList,
                annotationMergeStrategy);
        getFirstOutputSlot().setVirtualDataAnnotation(getFirstOutputSlot().getRowCount() - 1,
                annotationName,
                inputAnnotationSlot.getVirtualData(annotationRow));
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

    @JIPipeDocumentation(name = "Copy annotations from input", description = "If enabled, annotations from the 'Input' slot are copied into the output (Default).")
    @JIPipeParameter("merge-input-annotations")
    public boolean isMergeInputAnnotations() {
        return mergeInputAnnotations;
    }

    @JIPipeParameter("merge-input-annotations")
    public void setMergeInputAnnotations(boolean mergeInputAnnotations) {
        this.mergeInputAnnotations = mergeInputAnnotations;
    }

    @JIPipeDocumentation(name = "Copy annotations from data annotation", description = "If enabled, annotations from the 'Annotation' slot are copied into the output.")
    @JIPipeParameter("merge-label-annotations")
    public boolean isMergeLabelAnnotations() {
        return mergeLabelAnnotations;
    }

    @JIPipeParameter("merge-label-annotations")
    public void setMergeLabelAnnotations(boolean mergeLabelAnnotations) {
        this.mergeLabelAnnotations = mergeLabelAnnotations;
    }

    @JIPipeDocumentation(name = "Annotation merge strategy", description = "Determines how annotations from 'Input' and 'Annotation' are merged, " +
            "if both are enabled.")
    @JIPipeParameter("annotation-merge-strategy")
    public JIPipeAnnotationMergeStrategy getAnnotationMergeStrategy() {
        return annotationMergeStrategy;
    }

    @JIPipeParameter("annotation-merge-strategy")
    public void setAnnotationMergeStrategy(JIPipeAnnotationMergeStrategy annotationMergeStrategy) {
        this.annotationMergeStrategy = annotationMergeStrategy;
    }
}
