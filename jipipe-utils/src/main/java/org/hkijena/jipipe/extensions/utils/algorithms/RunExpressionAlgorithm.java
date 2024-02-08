package org.hkijena.jipipe.extensions.utils.algorithms;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.MiscellaneousNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.extensions.expressions.*;
import org.hkijena.jipipe.extensions.parameters.library.primitives.optional.OptionalAnnotationNameParameter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@JIPipeDocumentation(name = "Run expression", description = "Passes data through from the input to the output and runs the expression for each incoming data row. " +
        "The result of the expression may or may not be used as annotation.")
@JIPipeNode(nodeTypeCategory = MiscellaneousNodeTypeCategory.class)
@JIPipeInputSlot(value = JIPipeData.class, slotName = "Data", autoCreate = true)
@JIPipeOutputSlot(value = JIPipeData.class, slotName = "Data", autoCreate = true)
public class RunExpressionAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private JIPipeExpressionParameter expression = new JIPipeExpressionParameter();
    private OptionalAnnotationNameParameter writeToAnnotation = new OptionalAnnotationNameParameter("", false);
    private JIPipeTextAnnotationMergeMode writeToAnnotationMergeStrategy = JIPipeTextAnnotationMergeMode.OverwriteExisting;

    public RunExpressionAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public RunExpressionAlgorithm(RunExpressionAlgorithm other) {
        super(other);
        this.expression = new JIPipeExpressionParameter(other.expression);
        this.writeToAnnotation = new OptionalAnnotationNameParameter(other.writeToAnnotation);
        this.writeToAnnotationMergeStrategy = other.writeToAnnotationMergeStrategy;
    }

    @JIPipeDocumentation(name = "Expression", description = "Expression that is executed per data batch. " +
            "All annotations are available as variables, including variables 'data_string' and 'data_type' that provide information about the current data.")
    @JIPipeParameter("expression")
    @JIPipeExpressionParameterSettings(variableSource = VariablesInfo.class)
    public JIPipeExpressionParameter getExpression() {
        return expression;
    }

    @JIPipeParameter("expression")
    public void setExpression(JIPipeExpressionParameter expression) {
        this.expression = expression;
    }

    @JIPipeDocumentation(name = "Write result to annotation", description = "If enabled, the expression result is written to an annotation.")
    @JIPipeParameter("write-to-annotation")
    public OptionalAnnotationNameParameter getWriteToAnnotation() {
        return writeToAnnotation;
    }

    @JIPipeParameter("write-to-annotation")
    public void setWriteToAnnotation(OptionalAnnotationNameParameter writeToAnnotation) {
        this.writeToAnnotation = writeToAnnotation;
    }

    @JIPipeDocumentation(name = "Annotation merge strategy", description = "If 'Write result to annotation' is enabled, apply following strategy if an annotation " +
            "already exists")
    @JIPipeParameter("write-to-annotation-merge-strategy")
    public JIPipeTextAnnotationMergeMode getWriteToAnnotationMergeStrategy() {
        return writeToAnnotationMergeStrategy;
    }

    @JIPipeParameter("write-to-annotation-merge-strategy")
    public void setWriteToAnnotationMergeStrategy(JIPipeTextAnnotationMergeMode writeToAnnotationMergeStrategy) {
        this.writeToAnnotationMergeStrategy = writeToAnnotationMergeStrategy;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeProgressInfo progressInfo) {
        JIPipeData data = iterationStep.getInputData(getFirstInputSlot(), JIPipeData.class, progressInfo);
        JIPipeExpressionVariablesMap variableSet = new JIPipeExpressionVariablesMap();
        for (JIPipeTextAnnotation annotation : iterationStep.getMergedTextAnnotations().values()) {
            variableSet.set(annotation.getName(), annotation.getValue());
        }
        variableSet.set("data_string", "" + data);
        variableSet.set("data_type", JIPipeDataInfo.getInstance(data.getClass()).getId());
        variableSet.set("row", iterationStep.getInputSlotRows().get(getFirstInputSlot()));
        Object result = expression.evaluate(variableSet);
        List<JIPipeTextAnnotation> annotationList = new ArrayList<>();
        if (result != null && writeToAnnotation.isEnabled()) {
            annotationList.add(writeToAnnotation.createAnnotation(result.toString()));
        }
        iterationStep.addOutputData(getFirstOutputSlot(), data, annotationList, writeToAnnotationMergeStrategy, progressInfo);
    }

    public static class VariablesInfo implements ExpressionParameterVariablesInfo {
        @Override
        public Set<JIPipeExpressionParameterVariableInfo> getVariables(JIPipeParameterTree parameterTree, JIPipeParameterAccess parameterAccess) {
            Set<JIPipeExpressionParameterVariableInfo> result = new HashSet<>();
            result.add(new JIPipeExpressionParameterVariableInfo("data_string", "Data as string", "The data value, represented as string"));
            result.add(new JIPipeExpressionParameterVariableInfo("data_type", "Data type id", "The ID of the data type"));
            result.add(new JIPipeExpressionParameterVariableInfo("row", "Row", "The row inside the data type"));
            return result;
        }
    }
}
