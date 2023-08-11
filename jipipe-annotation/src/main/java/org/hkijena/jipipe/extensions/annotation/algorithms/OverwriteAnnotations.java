package org.hkijena.jipipe.extensions.annotation.algorithms;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.AnnotationsNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterPersistence;
import org.hkijena.jipipe.extensions.expressions.*;
import org.hkijena.jipipe.extensions.expressions.variables.TextAnnotationsExpressionParameterVariableSource;
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

    private final CustomExpressionVariablesParameter customVariables;
    private DefaultExpressionParameter removeExistingAnnotationsFilter = new AnnotationQueryExpression("false");
    private DefaultExpressionParameter sourceAnnotationFilter = new AnnotationQueryExpression("true");
    private JIPipeTextAnnotationMergeMode mergeMode = JIPipeTextAnnotationMergeMode.OverwriteExisting;

    public OverwriteAnnotations(JIPipeNodeInfo info) {
        super(info);
        this.customVariables = new CustomExpressionVariablesParameter(this);
    }

    public OverwriteAnnotations(OverwriteAnnotations other) {
        super(other);
        this.customVariables = new CustomExpressionVariablesParameter(other.customVariables, this);
        this.removeExistingAnnotationsFilter = new AnnotationQueryExpression(other.removeExistingAnnotationsFilter);
        this.sourceAnnotationFilter = new AnnotationQueryExpression(other.sourceAnnotationFilter);
        this.mergeMode = other.mergeMode;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        JIPipeData target = dataBatch.getInputData("Target", JIPipeData.class, progressInfo);

        ExpressionVariables variables = new ExpressionVariables();
        customVariables.writeToVariables(variables, true, "custom.", true, "custom");

        // Remove annotations from target (via original annotations)
        Map<String, String> targetAnnotationMap = JIPipeTextAnnotation.annotationListToMap(dataBatch.getOriginalTextAnnotations("Target"), JIPipeTextAnnotationMergeMode.OverwriteExisting);
        variables.set("target.annotations", targetAnnotationMap);
        for (Map.Entry<String, String> entry : targetAnnotationMap.entrySet()) {
            variables.set("name", entry.getValue());
            variables.set("value", entry.getValue());
            if (removeExistingAnnotationsFilter.test(variables)) {
                dataBatch.removeMergedTextAnnotation(entry.getKey());
            }
        }

        // Overwrite
        List<JIPipeTextAnnotation> annotations = new ArrayList<>();
        variables.set("target.annotations", JIPipeTextAnnotation.annotationListToMap(dataBatch.getOriginalTextAnnotations("Source"), JIPipeTextAnnotationMergeMode.OverwriteExisting));
        for (JIPipeTextAnnotation originalAnnotation : dataBatch.getOriginalTextAnnotations("Source")) {
            variables.set("name", originalAnnotation.getName());
            variables.set("source.value", originalAnnotation.getValue());
            variables.set("target.value", targetAnnotationMap.getOrDefault(originalAnnotation.getName(), null));
            if (sourceAnnotationFilter.test(variables)) {
                annotations.add(originalAnnotation);
            }
        }

        dataBatch.addOutputData(getFirstOutputSlot(), target, annotations, mergeMode, progressInfo);
    }

    @JIPipeDocumentation(name = "Remove existing annotations", description = "Expression that determines whether an existing annotation is removed. Set to <code>false</code> to not remove existing annotations.")
    @JIPipeParameter("remove-existing-annotations-filter")
    @ExpressionParameterSettingsVariable(key = "custom", name = "Custom variables", description = "A map containing custom expression variables (keys are the parameter keys)")
    @ExpressionParameterSettingsVariable(name = "custom.<Custom variable key>", description = "Custom variable parameters are added with a prefix 'custom.'")
    @ExpressionParameterSettingsVariable(name = "Source annotations map", description = "Map of all source annotations (key to value)", key = "source.annotations")
    @ExpressionParameterSettingsVariable(name = "Target annotations map", description = "Map of all target annotations (key to value)", key = "target.annotations")
    @ExpressionParameterSettingsVariable(name = "Annotation name", key = "name", description = "The name of the currently processed annotation")
    @ExpressionParameterSettingsVariable(name = "Annotation value", key = "value", description = "The value of the currently processed annotation")
    @ExpressionParameterSettingsVariable(fromClass = TextAnnotationsExpressionParameterVariableSource.class)
    public DefaultExpressionParameter getRemoveExistingAnnotationsFilter() {
        return removeExistingAnnotationsFilter;
    }

    @JIPipeParameter("remove-existing-annotations-filter")
    public void setRemoveExistingAnnotationsFilter(DefaultExpressionParameter removeExistingAnnotationsFilter) {
        this.removeExistingAnnotationsFilter = removeExistingAnnotationsFilter;
    }

    @JIPipeDocumentation(name = "Selected source annotations", description = "Expression that determines whether a source annotation is copied into the target. Set to <code>true</code> to copy all annotations.")
    @ExpressionParameterSettingsVariable(key = "custom", name = "Custom variables", description = "A map containing custom expression variables (keys are the parameter keys)")
    @ExpressionParameterSettingsVariable(name = "custom.<Custom variable key>", description = "Custom variable parameters are added with a prefix 'custom.'")
    @ExpressionParameterSettingsVariable(name = "Source annotations map", description = "Map of all source annotations (key to value)", key = "source.annotations")
    @ExpressionParameterSettingsVariable(name = "Target annotations map", description = "Map of all target annotations (key to value)", key = "target.annotations")
    @ExpressionParameterSettingsVariable(name = "Annotation name", key = "name", description = "The name of the currently processed annotation")
    @ExpressionParameterSettingsVariable(name = "Source annotation value", key = "source.value", description = "The value of the currently processed source annotation")
    @ExpressionParameterSettingsVariable(name = "Target annotation value", key = "target.value", description = "The value of the target annotation. NULL if it is not set.")
    @ExpressionParameterSettingsVariable(fromClass = TextAnnotationsExpressionParameterVariableSource.class)
    @JIPipeParameter("source-annotation-filter")
    public DefaultExpressionParameter getSourceAnnotationFilter() {
        return sourceAnnotationFilter;
    }

    @JIPipeParameter("source-annotation-filter")
    public void setSourceAnnotationFilter(DefaultExpressionParameter sourceAnnotationFilter) {
        this.sourceAnnotationFilter = sourceAnnotationFilter;
    }

    @JIPipeDocumentation(name = "Custom variables", description = "Here you can add parameters that will be included into the expressions as variables <code>custom.[key]</code>. Alternatively, you can access them via <code>GET_ITEM(\"custom\", \"[key]\")</code>.")
    @JIPipeParameter(value = "custom-variables", iconURL = ResourceUtils.RESOURCE_BASE_PATH + "/icons/actions/insert-math-expression.png",
            iconDarkURL = ResourceUtils.RESOURCE_BASE_PATH + "/dark/icons/actions/insert-math-expression.png", persistence = JIPipeParameterPersistence.NestedCollection)
    public CustomExpressionVariablesParameter getCustomVariables() {
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
