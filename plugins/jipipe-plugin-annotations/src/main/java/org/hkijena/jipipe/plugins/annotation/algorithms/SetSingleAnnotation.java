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

import org.hkijena.jipipe.JIPipe;
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
import org.hkijena.jipipe.plugins.expressions.*;
import org.hkijena.jipipe.utils.StringUtils;

import java.util.HashSet;
import java.util.Set;

/**
 * Algorithm that annotates all data with the same annotation
 */
@SetJIPipeDocumentation(name = "Set annotation (expression)", description = "Sets a single annotation. The name and the value are determined by expressions")
@ConfigureJIPipeNode(nodeTypeCategory = AnnotationsNodeTypeCategory.class, menuPath = "Modify")
@AddJIPipeInputSlot(value = JIPipeData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = JIPipeData.class, name = "Output", create = true)
public class SetSingleAnnotation extends JIPipeSimpleIteratingAlgorithm {

    private StringQueryExpression annotationValue = new StringQueryExpression("\"Annotation value\"");
    private StringQueryExpression annotationName = new StringQueryExpression("\"Annotation name\"");
    private JIPipeTextAnnotationMergeMode annotationMergeStrategy = JIPipeTextAnnotationMergeMode.OverwriteExisting;

    /**
     * @param info the info
     */
    public SetSingleAnnotation(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Copies the algorithm
     *
     * @param other the original
     */
    public SetSingleAnnotation(SetSingleAnnotation other) {
        super(other);
        this.annotationMergeStrategy = other.annotationMergeStrategy;
        this.annotationName = new StringQueryExpression(other.annotationName);
        this.annotationValue = new StringQueryExpression(other.annotationValue);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        JIPipeExpressionVariablesMap variableSet = new JIPipeExpressionVariablesMap();
        for (JIPipeTextAnnotation annotation : iterationStep.getMergedTextAnnotations().values()) {
            variableSet.set(annotation.getName(), annotation.getValue());
        }
        variableSet.putProjectDirectories(getProjectDirectory(), getProjectDataDirs());
        variableSet.set("data_string", getFirstInputSlot().getDataItemStore(iterationStep.getInputSlotRows().get(getFirstInputSlot())).getStringRepresentation());
        variableSet.set("data_type", JIPipe.getDataTypes().getIdOf(getFirstInputSlot().getDataItemStore(iterationStep.getInputSlotRows().get(getFirstInputSlot())).getDataClass()));
        variableSet.set("row", iterationStep.getInputSlotRows().get(getFirstInputSlot()));
        variableSet.set("num_rows", getFirstInputSlot().getRowCount());
        String name = StringUtils.nullToEmpty(annotationName.generate(variableSet));
        String value = StringUtils.nullToEmpty(annotationValue.generate(variableSet));
        iterationStep.addMergedTextAnnotation(new JIPipeTextAnnotation(name, value), annotationMergeStrategy);
        iterationStep.addOutputData(getFirstOutputSlot(), iterationStep.getInputData(getFirstInputSlot(), JIPipeData.class, progressInfo), progressInfo);
    }

    @SetJIPipeDocumentation(name = "Annotation value", description = "The value of the generated annotation. ")
    @JIPipeParameter("annotation-value")
    @JIPipeExpressionParameterSettings(variableSource = VariablesInfo.class)
    @AddJIPipeExpressionParameterVariable(fromClass = JIPipeProjectDirectoriesVariablesInfo.class)
    public StringQueryExpression getAnnotationValue() {
        return annotationValue;
    }

    @JIPipeParameter("annotation-value")
    public void setAnnotationValue(StringQueryExpression annotationValue) {
        this.annotationValue = annotationValue;
    }

    @SetJIPipeDocumentation(name = "Annotation name", description = "The name of the generated annotation. ")
    @JIPipeParameter("annotation-name")
    @JIPipeExpressionParameterSettings(variableSource = VariablesInfo.class)
    public StringQueryExpression getAnnotationName() {
        return annotationName;
    }

    @JIPipeParameter("annotation-name")
    public void setAnnotationName(StringQueryExpression annotationName) {
        this.annotationName = annotationName;
    }

    public static class VariablesInfo implements ExpressionParameterVariablesInfo {

        public static final Set<JIPipeExpressionParameterVariableInfo> VARIABLES;

        static {
            VARIABLES = new HashSet<>();
            VARIABLES.add(JIPipeExpressionParameterVariableInfo.ANNOTATIONS_VARIABLE);
            VARIABLES.add(new JIPipeExpressionParameterVariableInfo("data_string", "Data string",
                    "The data stored as string"
            ));
            VARIABLES.add(new JIPipeExpressionParameterVariableInfo("data_type", "Data type ID",
                    "The data type ID"
            ));
            VARIABLES.add(new JIPipeExpressionParameterVariableInfo("row", "Row",
                    "The row inside the data table"
            ));
            VARIABLES.add(new JIPipeExpressionParameterVariableInfo("num_rows", "Number of rows",
                    "The number of rows in the data table"
            ));
        }

        @Override
        public Set<JIPipeExpressionParameterVariableInfo> getVariables(JIPipeParameterTree parameterTree, JIPipeParameterAccess parameterAccess) {
            return VARIABLES;
        }
    }
}
