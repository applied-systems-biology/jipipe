package org.hkijena.jipipe.extensions.utils.algorithms;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeAnnotation;
import org.hkijena.jipipe.api.data.JIPipeAnnotationMergeStrategy;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataInfo;
import org.hkijena.jipipe.api.nodes.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.MiscellaneousNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.extensions.expressions.DefaultExpressionParameter;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameterSettings;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameterVariable;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameterVariableSource;
import org.hkijena.jipipe.extensions.expressions.ExpressionVariables;
import org.hkijena.jipipe.extensions.parameters.primitives.OptionalAnnotationNameParameter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@JIPipeDocumentation(name = "Run expression", description = "Passes data through from the input to the output and runs the expression for each incoming data row. " +
        "The result of the expression may or may not be used as annotation.")
@JIPipeNode(nodeTypeCategory = MiscellaneousNodeTypeCategory.class)
@JIPipeInputSlot(value = JIPipeData.class, slotName = "Data", autoCreate = true)
@JIPipeOutputSlot(value = JIPipeData.class, slotName = "Data", autoCreate = true, inheritedSlot = "Data")
public class RunExpressionAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private DefaultExpressionParameter expression = new DefaultExpressionParameter();
    private OptionalAnnotationNameParameter writeToAnnotation = new OptionalAnnotationNameParameter("", false);
    private JIPipeAnnotationMergeStrategy writeToAnnotationMergeStrategy = JIPipeAnnotationMergeStrategy.OverwriteExisting;

    public RunExpressionAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public RunExpressionAlgorithm(RunExpressionAlgorithm other) {
        super(other);
        this.expression = new DefaultExpressionParameter(other.expression);
        this.writeToAnnotation = new OptionalAnnotationNameParameter(other.writeToAnnotation);
        this.writeToAnnotationMergeStrategy = other.writeToAnnotationMergeStrategy;
    }

    @JIPipeDocumentation(name = "Expression", description = "Expression that is executed per data batch. " +
            "All annotations are available as variables, including variables 'data_string' and 'data_type' that provide information about the current data.")
    @JIPipeParameter("expression")
    @ExpressionParameterSettings(variableSource = VariableSource.class)
    public DefaultExpressionParameter getExpression() {
        return expression;
    }

    @JIPipeParameter("expression")
    public void setExpression(DefaultExpressionParameter expression) {
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
    public JIPipeAnnotationMergeStrategy getWriteToAnnotationMergeStrategy() {
        return writeToAnnotationMergeStrategy;
    }

    @JIPipeParameter("write-to-annotation-merge-strategy")
    public void setWriteToAnnotationMergeStrategy(JIPipeAnnotationMergeStrategy writeToAnnotationMergeStrategy) {
        this.writeToAnnotationMergeStrategy = writeToAnnotationMergeStrategy;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        JIPipeData data = dataBatch.getInputData(getFirstInputSlot(), JIPipeData.class, progressInfo);
        ExpressionVariables variableSet = new ExpressionVariables();
        for (JIPipeAnnotation annotation : dataBatch.getMergedAnnotations().values()) {
            variableSet.set(annotation.getName(), annotation.getValue());
        }
        variableSet.set("data_string", "" + data);
        variableSet.set("data_type", JIPipeDataInfo.getInstance(data.getClass()).getId());
        variableSet.set("row", dataBatch.getInputSlotRows().get(getFirstInputSlot()));
        Object result = expression.evaluate(variableSet);
        List<JIPipeAnnotation> annotationList = new ArrayList<>();
        if (result != null && writeToAnnotation.isEnabled()) {
            annotationList.add(writeToAnnotation.createAnnotation(result.toString()));
        }
        dataBatch.addOutputData(getFirstOutputSlot(), data, annotationList, writeToAnnotationMergeStrategy, progressInfo);
    }

    public static class VariableSource implements ExpressionParameterVariableSource {
        @Override
        public Set<ExpressionParameterVariable> getVariables(JIPipeParameterAccess parameterAccess) {
            Set<ExpressionParameterVariable> result = new HashSet<>();
            result.add(new ExpressionParameterVariable("Data as string", "The data value, represented as string", "data_string"));
            result.add(new ExpressionParameterVariable("Data type id", "The ID of the data type", "data_type"));
            result.add(new ExpressionParameterVariable("Row", "The row inside the data type", "row"));
            return result;
        }
    }
}
