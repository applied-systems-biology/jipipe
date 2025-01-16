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

package org.hkijena.jipipe.plugins.utils.algorithms;

import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.JIPipeWorkbench;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataInfo;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.MiscellaneousNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.plugins.expressions.*;
import org.hkijena.jipipe.plugins.parameters.library.primitives.optional.OptionalTextAnnotationNameParameter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@SetJIPipeDocumentation(name = "Run expression", description = "Passes data through from the input to the output and runs the expression for each incoming data row. " +
        "The result of the expression may or may not be used as annotation.")
@ConfigureJIPipeNode(nodeTypeCategory = MiscellaneousNodeTypeCategory.class)
@AddJIPipeInputSlot(value = JIPipeData.class, name = "Data", create = true)
@AddJIPipeOutputSlot(value = JIPipeData.class, name = "Data", create = true)
public class RunExpressionAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private JIPipeExpressionParameter expression = new JIPipeExpressionParameter();
    private OptionalTextAnnotationNameParameter writeToAnnotation = new OptionalTextAnnotationNameParameter("", false);
    private JIPipeTextAnnotationMergeMode writeToAnnotationMergeStrategy = JIPipeTextAnnotationMergeMode.OverwriteExisting;

    public RunExpressionAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public RunExpressionAlgorithm(RunExpressionAlgorithm other) {
        super(other);
        this.expression = new JIPipeExpressionParameter(other.expression);
        this.writeToAnnotation = new OptionalTextAnnotationNameParameter(other.writeToAnnotation);
        this.writeToAnnotationMergeStrategy = other.writeToAnnotationMergeStrategy;
    }

    @SetJIPipeDocumentation(name = "Expression", description = "Expression that is executed per iteration step. " +
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

    @SetJIPipeDocumentation(name = "Write result to annotation", description = "If enabled, the expression result is written to an annotation.")
    @JIPipeParameter("write-to-annotation")
    public OptionalTextAnnotationNameParameter getWriteToAnnotation() {
        return writeToAnnotation;
    }

    @JIPipeParameter("write-to-annotation")
    public void setWriteToAnnotation(OptionalTextAnnotationNameParameter writeToAnnotation) {
        this.writeToAnnotation = writeToAnnotation;
    }

    @SetJIPipeDocumentation(name = "Annotation merge strategy", description = "If 'Write result to annotation' is enabled, apply following strategy if an annotation " +
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
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
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

    public static class VariablesInfo implements JIPipeExpressionVariablesInfo {
        @Override
        public Set<JIPipeExpressionParameterVariableInfo> getVariables(JIPipeWorkbench workbench, JIPipeParameterTree parameterTree, JIPipeParameterAccess parameterAccess) {
            Set<JIPipeExpressionParameterVariableInfo> result = new HashSet<>();
            result.add(new JIPipeExpressionParameterVariableInfo("data_string", "Data as string", "The data value, represented as string"));
            result.add(new JIPipeExpressionParameterVariableInfo("data_type", "Data type id", "The ID of the data type"));
            result.add(new JIPipeExpressionParameterVariableInfo("row", "Row", "The row inside the data type"));
            return result;
        }
    }
}
