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
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.AnnotationsNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.plugins.expressions.AnnotationQueryExpression;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameter;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.plugins.expressions.StringQueryExpression;
import org.hkijena.jipipe.plugins.parameters.library.primitives.StringParameterSettings;
import org.hkijena.jipipe.utils.StringUtils;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@SetJIPipeDocumentation(name = "Simplify annotations", description = "Merges multiple annotations that are used for iteration step generation into a single annotation. Deletes or downgrades annotations that were involved in the merge.")
@ConfigureJIPipeNode(nodeTypeCategory = AnnotationsNodeTypeCategory.class)
@AddJIPipeInputSlot(value = JIPipeData.class, slotName = "Input", create = true)
@AddJIPipeOutputSlot(value = JIPipeData.class, slotName = "Output", create = true)
public class SimplifyAnnotationsAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private AnnotationQueryExpression annotationFilter = new AnnotationQueryExpression("STRING_STARTS_WITH(key, \"#\")");
    private StringQueryExpression renameFunction = new StringQueryExpression("REPLACE_IN_STRING(value, \"#\", \"\")");
    private JIPipeExpressionParameter combinationFunction = new JIPipeExpressionParameter("SUMMARIZE_VARIABLES(\" \", \"=\")");
    private String generatedAnnotationName = "#Dataset";
    private AnnotationRemovalMode annotationRemovalMode = AnnotationRemovalMode.Rename;

    public SimplifyAnnotationsAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public SimplifyAnnotationsAlgorithm(SimplifyAnnotationsAlgorithm other) {
        super(other);
        this.annotationFilter = other.annotationFilter;
        this.renameFunction = other.renameFunction;
        this.generatedAnnotationName = other.generatedAnnotationName;
        this.annotationRemovalMode = other.annotationRemovalMode;
        this.combinationFunction = new JIPipeExpressionParameter(other.combinationFunction);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        List<JIPipeTextAnnotation> combinedAnnotations = annotationFilter.queryAll(iterationStep.getMergedTextAnnotations().values());

        String combinedValue;
        {
            JIPipeExpressionVariablesMap variables = new JIPipeExpressionVariablesMap();
            for (JIPipeTextAnnotation annotation : combinedAnnotations) {
                variables.set(annotation.getName(), annotation.getValue());
            }
            combinedValue = combinationFunction.evaluateToString(variables);
        }

        if (annotationRemovalMode != AnnotationRemovalMode.Keep) {
            for (JIPipeTextAnnotation annotation : combinedAnnotations) {
                iterationStep.getMergedTextAnnotations().remove(annotation.getName());
            }
        }
        if (annotationRemovalMode == AnnotationRemovalMode.Rename) {
            Set<String> existing = new HashSet<>();
            for (JIPipeTextAnnotation annotation : iterationStep.getMergedTextAnnotations().values()) {
                existing.add(annotation.getName());
            }
            JIPipeExpressionVariablesMap variables = new JIPipeExpressionVariablesMap();
            for (JIPipeTextAnnotation annotation : combinedAnnotations) {
                variables.set("value", annotation.getName());
                String newName = StringUtils.makeUniqueString(renameFunction.generate(variables), " ", existing);
                existing.add(newName);

                iterationStep.getMergedTextAnnotations().put(newName, new JIPipeTextAnnotation(newName, annotation.getValue()));
            }
        }

        iterationStep.addOutputData(getFirstOutputSlot(),
                iterationStep.getInputData(getFirstInputSlot(), JIPipeData.class, progressInfo),
                Collections.singletonList(new JIPipeTextAnnotation(generatedAnnotationName, combinedValue)),
                JIPipeTextAnnotationMergeMode.OverwriteExisting,
                progressInfo);
    }

    @SetJIPipeDocumentation(name = "Annotation filter", description = "Determines which annotations will be combined. ")
    @JIPipeParameter("annotation-filter")
    public AnnotationQueryExpression getAnnotationFilter() {
        return annotationFilter;
    }

    @JIPipeParameter("annotation-filter")
    public void setAnnotationFilter(AnnotationQueryExpression annotationFilter) {
        this.annotationFilter = annotationFilter;
    }

    @SetJIPipeDocumentation(name = "Rename function", description = "The function is applied to the annotation name to determine its new name. ")
    @JIPipeParameter(value = "rename-function", important = true)
    public StringQueryExpression getRenameFunction() {
        return renameFunction;
    }

    @JIPipeParameter("rename-function")
    public void setRenameFunction(StringQueryExpression renameFunction) {
        this.renameFunction = renameFunction;
    }

    @SetJIPipeDocumentation(name = "Generated annotation", description = "The name of the generated annotation.")
    @JIPipeParameter(value = "generated-annotation-name", important = true)
    @StringParameterSettings(monospace = true)
    public String getGeneratedAnnotationName() {
        return generatedAnnotationName;
    }

    @JIPipeParameter("generated-annotation-name")
    public void setGeneratedAnnotationName(String generatedAnnotationName) {
        this.generatedAnnotationName = generatedAnnotationName;
    }

    @SetJIPipeDocumentation(name = "Combined annotations", description = "The operation to apply with annotations that have been combined.")
    @JIPipeParameter(value = "annotation-removal-mode", important = true)
    public AnnotationRemovalMode getAnnotationRemovalMode() {
        return annotationRemovalMode;
    }

    @JIPipeParameter("annotation-removal-mode")
    public void setAnnotationRemovalMode(AnnotationRemovalMode annotationRemovalMode) {
        this.annotationRemovalMode = annotationRemovalMode;
        emitParameterUIChangedEvent();
    }

    @SetJIPipeDocumentation(name = "Combination function", description = "Function that determines how annotation names and values are combined.")
    @JIPipeParameter("combination-function")
    public JIPipeExpressionParameter getCombinationFunction() {
        return combinationFunction;
    }

    @JIPipeParameter("combination-function")
    public void setCombinationFunction(JIPipeExpressionParameter combinationFunction) {
        this.combinationFunction = combinationFunction;
    }

    @Override
    public boolean isParameterUIVisible(JIPipeParameterTree tree, JIPipeParameterAccess access) {
        if ("rename-function".equals(access.getKey()) && getAnnotationRemovalMode() != AnnotationRemovalMode.Rename) {
            return false;
        }
        return super.isParameterUIVisible(tree, access);
    }

    public enum AnnotationRemovalMode {
        Rename,
        Delete,
        Keep
    }
}
