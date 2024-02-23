package org.hkijena.jipipe.extensions.annotation.algorithms;

import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.AnnotationsNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.expressions.*;
import org.hkijena.jipipe.extensions.expressions.custom.JIPipeCustomExpressionVariablesParameterVariablesInfo;
import org.hkijena.jipipe.extensions.expressions.variables.JIPipeTextAnnotationsExpressionParameterVariablesInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@SetJIPipeDocumentation(name = "Copy/overwrite annotations", description = "Copies annotations from the source data into the target data.")
@AddJIPipeInputSlot(value = JIPipeData.class, slotName = "Target", create = true, description = "The target data")
@AddJIPipeInputSlot(value = JIPipeData.class, slotName = "Source", create = true, description = "The data where the annotations are sourced from")
@AddJIPipeOutputSlot(value = JIPipeData.class, slotName = "Target", create = true, description = "Annotated target")
@ConfigureJIPipeNode(nodeTypeCategory = AnnotationsNodeTypeCategory.class)
public class OverwriteAnnotations extends JIPipeIteratingAlgorithm {
    private JIPipeExpressionParameter removeExistingAnnotationsFilter = new AnnotationQueryExpression("false");
    private JIPipeExpressionParameter sourceAnnotationFilter = new AnnotationQueryExpression("true");
    private JIPipeTextAnnotationMergeMode mergeMode = JIPipeTextAnnotationMergeMode.OverwriteExisting;

    public OverwriteAnnotations(JIPipeNodeInfo info) {
        super(info);
    }

    public OverwriteAnnotations(OverwriteAnnotations other) {
        super(other);
        this.removeExistingAnnotationsFilter = new AnnotationQueryExpression(other.removeExistingAnnotationsFilter);
        this.sourceAnnotationFilter = new AnnotationQueryExpression(other.sourceAnnotationFilter);
        this.mergeMode = other.mergeMode;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        JIPipeData target = iterationStep.getInputData("Target", JIPipeData.class, progressInfo);

        JIPipeExpressionVariablesMap variables = new JIPipeExpressionVariablesMap();
        getDefaultCustomExpressionVariables().writeToVariables(variables);

        // Remove annotations from target (via original annotations)
        Map<String, String> targetAnnotationMap = JIPipeTextAnnotation.annotationListToMap(iterationStep.getOriginalTextAnnotations("Target"), JIPipeTextAnnotationMergeMode.OverwriteExisting);
        variables.set("target.annotations", targetAnnotationMap);
        for (Map.Entry<String, String> entry : targetAnnotationMap.entrySet()) {
            variables.set("name", entry.getValue());
            variables.set("value", entry.getValue());
            if (removeExistingAnnotationsFilter.test(variables)) {
                iterationStep.removeMergedTextAnnotation(entry.getKey());
            }
        }

        // Overwrite
        List<JIPipeTextAnnotation> annotations = new ArrayList<>();
        variables.set("target.annotations", JIPipeTextAnnotation.annotationListToMap(iterationStep.getOriginalTextAnnotations("Source"), JIPipeTextAnnotationMergeMode.OverwriteExisting));
        for (JIPipeTextAnnotation originalAnnotation : iterationStep.getOriginalTextAnnotations("Source")) {
            variables.set("name", originalAnnotation.getName());
            variables.set("source.value", originalAnnotation.getValue());
            variables.set("target.value", targetAnnotationMap.getOrDefault(originalAnnotation.getName(), null));
            if (sourceAnnotationFilter.test(variables)) {
                annotations.add(originalAnnotation);
            }
        }

        iterationStep.addOutputData(getFirstOutputSlot(), target, annotations, mergeMode, progressInfo);
    }

    @SetJIPipeDocumentation(name = "Remove existing annotations", description = "Expression that determines whether an existing annotation is removed. Set to <code>false</code> to not remove existing annotations.")
    @JIPipeParameter("remove-existing-annotations-filter")
    @JIPipeExpressionParameterVariable(fromClass = JIPipeCustomExpressionVariablesParameterVariablesInfo.class)
    @JIPipeExpressionParameterVariable(name = "Source annotations map", description = "Map of all source annotations (key to value)", key = "source.annotations")
    @JIPipeExpressionParameterVariable(name = "Target annotations map", description = "Map of all target annotations (key to value)", key = "target.annotations")
    @JIPipeExpressionParameterVariable(name = "Annotation name", key = "name", description = "The name of the currently processed annotation")
    @JIPipeExpressionParameterVariable(name = "Annotation value", key = "value", description = "The value of the currently processed annotation")
    @JIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
    public JIPipeExpressionParameter getRemoveExistingAnnotationsFilter() {
        return removeExistingAnnotationsFilter;
    }

    @JIPipeParameter("remove-existing-annotations-filter")
    public void setRemoveExistingAnnotationsFilter(JIPipeExpressionParameter removeExistingAnnotationsFilter) {
        this.removeExistingAnnotationsFilter = removeExistingAnnotationsFilter;
    }

    @SetJIPipeDocumentation(name = "Selected source annotations", description = "Expression that determines whether a source annotation is copied into the target. Set to <code>true</code> to copy all annotations.")
    @JIPipeExpressionParameterVariable(fromClass = JIPipeCustomExpressionVariablesParameterVariablesInfo.class)
    @JIPipeExpressionParameterVariable(name = "Source annotations map", description = "Map of all source annotations (key to value)", key = "source.annotations")
    @JIPipeExpressionParameterVariable(name = "Target annotations map", description = "Map of all target annotations (key to value)", key = "target.annotations")
    @JIPipeExpressionParameterVariable(name = "Annotation name", key = "name", description = "The name of the currently processed annotation")
    @JIPipeExpressionParameterVariable(name = "Source annotation value", key = "source.value", description = "The value of the currently processed source annotation")
    @JIPipeExpressionParameterVariable(name = "Target annotation value", key = "target.value", description = "The value of the target annotation. NULL if it is not set.")
    @JIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
    @JIPipeParameter("source-annotation-filter")
    public JIPipeExpressionParameter getSourceAnnotationFilter() {
        return sourceAnnotationFilter;
    }

    @JIPipeParameter("source-annotation-filter")
    public void setSourceAnnotationFilter(JIPipeExpressionParameter sourceAnnotationFilter) {
        this.sourceAnnotationFilter = sourceAnnotationFilter;
    }

    @Override
    public boolean isEnableDefaultCustomExpressionVariables() {
        return true;
    }

    @SetJIPipeDocumentation(name = "Merge mode", description = "Determines what to do if the target already has an annotation of the name")
    @JIPipeParameter("merge-mode")
    public JIPipeTextAnnotationMergeMode getMergeMode() {
        return mergeMode;
    }

    @JIPipeParameter("merge-mode")
    public void setMergeMode(JIPipeTextAnnotationMergeMode mergeMode) {
        this.mergeMode = mergeMode;
    }
}
