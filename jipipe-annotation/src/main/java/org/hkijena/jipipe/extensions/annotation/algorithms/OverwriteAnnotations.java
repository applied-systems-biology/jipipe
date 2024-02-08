package org.hkijena.jipipe.extensions.annotation.algorithms;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
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
import org.hkijena.jipipe.api.parameters.JIPipeParameterPersistence;
import org.hkijena.jipipe.extensions.expressions.*;
import org.hkijena.jipipe.extensions.expressions.custom.JIPipeCustomExpressionVariablesParameter;
import org.hkijena.jipipe.extensions.expressions.custom.JIPipeCustomExpressionVariablesParameterVariablesInfo;
import org.hkijena.jipipe.extensions.expressions.variables.JIPipeTextAnnotationsExpressionParameterVariablesInfo;
import org.hkijena.jipipe.utils.ResourceUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@JIPipeDocumentation(name = "Copy/overwrite annotations", description = "Copies annotations from the source data into the target data.")
@JIPipeInputSlot(value = JIPipeData.class, slotName = "Target", autoCreate = true, description = "The target data")
@JIPipeInputSlot(value = JIPipeData.class, slotName = "Source", autoCreate = true, description = "The data where the annotations are sourced from")
@JIPipeOutputSlot(value = JIPipeData.class, slotName = "Target", autoCreate = true, description = "Annotated target")
@JIPipeNode(nodeTypeCategory = AnnotationsNodeTypeCategory.class)
public class OverwriteAnnotations extends JIPipeIteratingAlgorithm {

    private final JIPipeCustomExpressionVariablesParameter customVariables;
    private JIPipeExpressionParameter removeExistingAnnotationsFilter = new AnnotationQueryExpression("false");
    private JIPipeExpressionParameter sourceAnnotationFilter = new AnnotationQueryExpression("true");
    private JIPipeTextAnnotationMergeMode mergeMode = JIPipeTextAnnotationMergeMode.OverwriteExisting;

    public OverwriteAnnotations(JIPipeNodeInfo info) {
        super(info);
        this.customVariables = new JIPipeCustomExpressionVariablesParameter(this);
    }

    public OverwriteAnnotations(OverwriteAnnotations other) {
        super(other);
        this.customVariables = new JIPipeCustomExpressionVariablesParameter(other.customVariables, this);
        this.removeExistingAnnotationsFilter = new AnnotationQueryExpression(other.removeExistingAnnotationsFilter);
        this.sourceAnnotationFilter = new AnnotationQueryExpression(other.sourceAnnotationFilter);
        this.mergeMode = other.mergeMode;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeProgressInfo progressInfo) {
        JIPipeData target = iterationStep.getInputData("Target", JIPipeData.class, progressInfo);

        JIPipeExpressionVariablesMap variables = new JIPipeExpressionVariablesMap();
        customVariables.writeToVariables(variables);

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

    @JIPipeDocumentation(name = "Remove existing annotations", description = "Expression that determines whether an existing annotation is removed. Set to <code>false</code> to not remove existing annotations.")
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

    @JIPipeDocumentation(name = "Selected source annotations", description = "Expression that determines whether a source annotation is copied into the target. Set to <code>true</code> to copy all annotations.")
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

    @JIPipeDocumentation(name = "Custom variables", description = "Here you can add parameters that will be included into the expressions as variables <code>custom.[key]</code>. Alternatively, you can access them via <code>GET_ITEM(custom, \"[key]\")</code>.")
    @JIPipeParameter(value = "custom-variables", iconURL = ResourceUtils.RESOURCE_BASE_PATH + "/icons/actions/insert-math-expression.png",
            iconDarkURL = ResourceUtils.RESOURCE_BASE_PATH + "/dark/icons/actions/insert-math-expression.png", persistence = JIPipeParameterPersistence.NestedCollection)
    public JIPipeCustomExpressionVariablesParameter getCustomVariables() {
        return customVariables;
    }

    @JIPipeDocumentation(name = "Merge mode", description = "Determines what to do if the target already has an annotation of the name")
    @JIPipeParameter("merge-mode")
    public JIPipeTextAnnotationMergeMode getMergeMode() {
        return mergeMode;
    }

    @JIPipeParameter("merge-mode")
    public void setMergeMode(JIPipeTextAnnotationMergeMode mergeMode) {
        this.mergeMode = mergeMode;
    }
}
