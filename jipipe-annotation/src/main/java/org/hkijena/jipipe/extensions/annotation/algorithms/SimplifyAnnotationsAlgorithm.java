package org.hkijena.jipipe.extensions.annotation.algorithms;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.nodes.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.AnnotationsNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.extensions.expressions.AnnotationQueryExpression;
import org.hkijena.jipipe.extensions.expressions.DefaultExpressionParameter;
import org.hkijena.jipipe.extensions.expressions.ExpressionVariables;
import org.hkijena.jipipe.extensions.expressions.StringQueryExpression;
import org.hkijena.jipipe.extensions.parameters.primitives.StringParameterSettings;
import org.hkijena.jipipe.utils.StringUtils;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@JIPipeDocumentation(name = "Simplify annotations", description = "Merges multiple annotations that are used for data batch generation into a single annotation. Deletes or downgrades annotations that were involved in the merge.")
@JIPipeNode(nodeTypeCategory = AnnotationsNodeTypeCategory.class)
@JIPipeInputSlot(value = JIPipeData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = JIPipeData.class, slotName = "Output", autoCreate = true, inheritedSlot = "Input")
public class SimplifyAnnotationsAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private AnnotationQueryExpression annotationFilter = new AnnotationQueryExpression("STRING_STARTS_WITH(key, \"#\")");
    private StringQueryExpression renameFunction = new StringQueryExpression("REPLACE_IN_STRING(value, \"#\", \"\")");
    private DefaultExpressionParameter combinationFunction = new DefaultExpressionParameter("SUMMARIZE_VARIABLES(\" \", \"=\")");
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
        this.combinationFunction = new DefaultExpressionParameter(other.combinationFunction);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        List<JIPipeTextAnnotation> combinedAnnotations = annotationFilter.queryAll(dataBatch.getMergedAnnotations().values());

        String combinedValue;
        {
            ExpressionVariables variables = new ExpressionVariables();
            for (JIPipeTextAnnotation annotation : combinedAnnotations) {
                variables.set(annotation.getName(), annotation.getValue());
            }
            combinedValue = combinationFunction.evaluateToString(variables);
        }

        if (annotationRemovalMode != AnnotationRemovalMode.Keep) {
            for (JIPipeTextAnnotation annotation : combinedAnnotations) {
                dataBatch.getMergedAnnotations().remove(annotation.getName());
            }
        }
        if (annotationRemovalMode == AnnotationRemovalMode.Rename) {
            Set<String> existing = new HashSet<>();
            for (JIPipeTextAnnotation annotation : dataBatch.getMergedAnnotations().values()) {
                existing.add(annotation.getName());
            }
            ExpressionVariables variables = new ExpressionVariables();
            for (JIPipeTextAnnotation annotation : combinedAnnotations) {
                variables.set("value", annotation.getName());
                String newName = StringUtils.makeUniqueString(renameFunction.generate(variables), " ", existing);
                existing.add(newName);

                dataBatch.getMergedAnnotations().put(newName, new JIPipeTextAnnotation(newName, annotation.getValue()));
            }
        }

        dataBatch.addOutputData(getFirstOutputSlot(),
                dataBatch.getInputData(getFirstInputSlot(), JIPipeData.class, progressInfo),
                Collections.singletonList(new JIPipeTextAnnotation(generatedAnnotationName, combinedValue)),
                JIPipeTextAnnotationMergeMode.OverwriteExisting,
                progressInfo);
    }

    @JIPipeDocumentation(name = "Annotation filter", description = "Determines which annotations will be combined. " + AnnotationQueryExpression.DOCUMENTATION_DESCRIPTION)
    @JIPipeParameter("annotation-filter")
    public AnnotationQueryExpression getAnnotationFilter() {
        return annotationFilter;
    }

    @JIPipeParameter("annotation-filter")
    public void setAnnotationFilter(AnnotationQueryExpression annotationFilter) {
        this.annotationFilter = annotationFilter;
    }

    @JIPipeDocumentation(name = "Rename function", description = "The function is applied to the annotation name to determine its new name. " + StringQueryExpression.DOCUMENTATION_DESCRIPTION)
    @JIPipeParameter(value = "rename-function", important = true)
    public StringQueryExpression getRenameFunction() {
        return renameFunction;
    }

    @JIPipeParameter("rename-function")
    public void setRenameFunction(StringQueryExpression renameFunction) {
        this.renameFunction = renameFunction;
    }

    @JIPipeDocumentation(name = "Generated annotation", description = "The name of the generated annotation.")
    @JIPipeParameter(value = "generated-annotation-name", important = true)
    @StringParameterSettings(monospace = true)
    public String getGeneratedAnnotationName() {
        return generatedAnnotationName;
    }

    @JIPipeParameter("generated-annotation-name")
    public void setGeneratedAnnotationName(String generatedAnnotationName) {
        this.generatedAnnotationName = generatedAnnotationName;
    }

    @JIPipeDocumentation(name = "Combined annotations", description = "The operation to apply with annotations that have been combined.")
    @JIPipeParameter(value = "annotation-removal-mode", important = true)
    public AnnotationRemovalMode getAnnotationRemovalMode() {
        return annotationRemovalMode;
    }

    @JIPipeParameter("annotation-removal-mode")
    public void setAnnotationRemovalMode(AnnotationRemovalMode annotationRemovalMode) {
        this.annotationRemovalMode = annotationRemovalMode;
        triggerParameterUIChange();
    }

    @JIPipeDocumentation(name = "Combination function", description = "Function that determines how annotation names and values are combined.")
    @JIPipeParameter("combination-function")
    public DefaultExpressionParameter getCombinationFunction() {
        return combinationFunction;
    }

    @JIPipeParameter("combination-function")
    public void setCombinationFunction(DefaultExpressionParameter combinationFunction) {
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
