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

package org.hkijena.jipipe.plugins.annotation.algorithms;

import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.AnnotationsNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.expressions.AddJIPipeExpressionParameterVariable;
import org.hkijena.jipipe.plugins.expressions.AnnotationQueryExpression;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameter;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.plugins.expressions.custom.JIPipeCustomExpressionVariablesParameterVariablesInfo;
import org.hkijena.jipipe.plugins.expressions.variables.JIPipeTextAnnotationsExpressionParameterVariablesInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@SetJIPipeDocumentation(name = "Copy/overwrite annotations", description = "Copies annotations from the source data into the target data.")
@AddJIPipeInputSlot(value = JIPipeData.class, name = "Target", create = true, description = "The target data")
@AddJIPipeInputSlot(value = JIPipeData.class, name = "Source", create = true, description = "The data where the annotations are sourced from")
@AddJIPipeOutputSlot(value = JIPipeData.class, name = "Target", create = true, description = "Annotated target")
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

        JIPipeExpressionVariablesMap variables = new JIPipeExpressionVariablesMap(iterationStep);

        // Remove annotations from target (via original annotations)
        Map<String, String> targetAnnotationMap = JIPipeTextAnnotation.annotationListToMap(iterationStep.getInputTextAnnotations("Target"), JIPipeTextAnnotationMergeMode.OverwriteExisting);
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
        variables.set("target.annotations", JIPipeTextAnnotation.annotationListToMap(iterationStep.getInputTextAnnotations("Source"), JIPipeTextAnnotationMergeMode.OverwriteExisting));
        for (JIPipeTextAnnotation originalAnnotation : iterationStep.getInputTextAnnotations("Source")) {
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
    @AddJIPipeExpressionParameterVariable(fromClass = JIPipeCustomExpressionVariablesParameterVariablesInfo.class)
    @AddJIPipeExpressionParameterVariable(name = "Source annotations map", description = "Map of all source annotations (key to value)", key = "source.annotations")
    @AddJIPipeExpressionParameterVariable(name = "Target annotations map", description = "Map of all target annotations (key to value)", key = "target.annotations")
    @AddJIPipeExpressionParameterVariable(name = "Annotation name", key = "name", description = "The name of the currently processed annotation")
    @AddJIPipeExpressionParameterVariable(name = "Annotation value", key = "value", description = "The value of the currently processed annotation")
    @AddJIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
    public JIPipeExpressionParameter getRemoveExistingAnnotationsFilter() {
        return removeExistingAnnotationsFilter;
    }

    @JIPipeParameter("remove-existing-annotations-filter")
    public void setRemoveExistingAnnotationsFilter(JIPipeExpressionParameter removeExistingAnnotationsFilter) {
        this.removeExistingAnnotationsFilter = removeExistingAnnotationsFilter;
    }

    @SetJIPipeDocumentation(name = "Selected source annotations", description = "Expression that determines whether a source annotation is copied into the target. Set to <code>true</code> to copy all annotations.")
    @AddJIPipeExpressionParameterVariable(fromClass = JIPipeCustomExpressionVariablesParameterVariablesInfo.class)
    @AddJIPipeExpressionParameterVariable(name = "Source annotations map", description = "Map of all source annotations (key to value)", key = "source.annotations")
    @AddJIPipeExpressionParameterVariable(name = "Target annotations map", description = "Map of all target annotations (key to value)", key = "target.annotations")
    @AddJIPipeExpressionParameterVariable(name = "Annotation name", key = "name", description = "The name of the currently processed annotation")
    @AddJIPipeExpressionParameterVariable(name = "Source annotation value", key = "source.value", description = "The value of the currently processed source annotation")
    @AddJIPipeExpressionParameterVariable(name = "Target annotation value", key = "target.value", description = "The value of the target annotation. NULL if it is not set.")
    @AddJIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
    @JIPipeParameter("source-annotation-filter")
    public JIPipeExpressionParameter getSourceAnnotationFilter() {
        return sourceAnnotationFilter;
    }

    @JIPipeParameter("source-annotation-filter")
    public void setSourceAnnotationFilter(JIPipeExpressionParameter sourceAnnotationFilter) {
        this.sourceAnnotationFilter = sourceAnnotationFilter;
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
