/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.extensions.annotation.algorithms;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.DefineJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.AnnotationsNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.extensions.expressions.*;
import org.hkijena.jipipe.extensions.expressions.custom.JIPipeCustomExpressionVariablesParameterVariablesInfo;
import org.hkijena.jipipe.extensions.parameters.api.pairs.PairParameterSettings;
import org.hkijena.jipipe.extensions.parameters.library.primitives.StringParameterSettings;

import java.util.HashSet;
import java.util.Set;

/**
 * Algorithm that annotates all data with the same annotation
 */
@SetJIPipeDocumentation(name = "Set/Edit annotations", description = "Modifies the specified annotations to the specified values. Supports expressions to combine existing annotations or generate new values.")
@DefineJIPipeNode(nodeTypeCategory = AnnotationsNodeTypeCategory.class, menuPath = "Modify")
@AddJIPipeNodeAlias(aliasName = "Set annotations")
@AddJIPipeNodeAlias(aliasName = "Modify annotations")
@AddJIPipeNodeAlias(aliasName = "Edit annotations")
@AddJIPipeInputSlot(value = JIPipeData.class, slotName = "Input", create = true)
@AddJIPipeOutputSlot(value = JIPipeData.class, slotName = "Output", create = true)
public class AnnotateByExpression extends JIPipeSimpleIteratingAlgorithm {
    private NamedTextAnnotationGeneratorExpression.List annotations = new NamedTextAnnotationGeneratorExpression.List();
    private JIPipeTextAnnotationMergeMode annotationMergeStrategy = JIPipeTextAnnotationMergeMode.OverwriteExisting;

    /**
     * @param info the info
     */
    public AnnotateByExpression(JIPipeNodeInfo info) {
        super(info);
        annotations.addNewInstance();
    }

    /**
     * Copies the algorithm
     *
     * @param other the original
     */
    public AnnotateByExpression(AnnotateByExpression other) {
        super(other);
        this.annotations = new NamedTextAnnotationGeneratorExpression.List(other.annotations);
        this.annotationMergeStrategy = other.annotationMergeStrategy;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        for (NamedTextAnnotationGeneratorExpression expression : annotations) {
            JIPipeExpressionVariablesMap variableSet = new JIPipeExpressionVariablesMap()
                    .putAnnotations(iterationStep.getMergedTextAnnotations())
                    .putProjectDirectories(getProjectDirectory(), getProjectDataDirs())
                    .putCustomVariables(getDefaultCustomExpressionVariables());
            variableSet.set("data_string", getFirstInputSlot().getDataItemStore(iterationStep.getInputSlotRows().get(getFirstInputSlot())).getStringRepresentation());
            variableSet.set("data_type", JIPipe.getDataTypes().getIdOf(getFirstInputSlot().getDataItemStore(iterationStep.getInputSlotRows().get(getFirstInputSlot())).getDataClass()));
            variableSet.set("row", iterationStep.getInputSlotRows().get(getFirstInputSlot()));
            variableSet.set("num_rows", getFirstInputSlot().getRowCount());
            iterationStep.addMergedTextAnnotation(expression.generateTextAnnotation(iterationStep.getMergedTextAnnotations().values(), variableSet),
                    annotationMergeStrategy
            );
        }
        iterationStep.addOutputData(getFirstOutputSlot(), iterationStep.getInputData(getFirstInputSlot(), JIPipeData.class, progressInfo), progressInfo);
    }

    @SetJIPipeDocumentation(name = "Annotations", description = "Allows you to set the annotation to add/modify. ")
    @JIPipeParameter("generated-annotation")
    @PairParameterSettings(keyLabel = "Value", valueLabel = "Name")
    @StringParameterSettings(monospace = true)
    @JIPipeExpressionParameterVariable(fromClass = VariablesInfo.class)
    @JIPipeExpressionParameterVariable(fromClass = JIPipeCustomExpressionVariablesParameterVariablesInfo.class)
    @JIPipeExpressionParameterVariable(fromClass = JIPipeProjectDirectoriesVariablesInfo.class)
    public NamedTextAnnotationGeneratorExpression.List getAnnotations() {
        return annotations;
    }

    @JIPipeParameter("generated-annotation")
    public void setAnnotations(NamedTextAnnotationGeneratorExpression.List annotations) {
        this.annotations = annotations;
    }

    @SetJIPipeDocumentation(name = "Merge same annotation values", description = "Determines which strategy is applied if an annotation already exists.")
    @JIPipeParameter("annotation-merge-strategy")
    public JIPipeTextAnnotationMergeMode getAnnotationMergeStrategy() {
        return annotationMergeStrategy;
    }

    @JIPipeParameter("annotation-merge-strategy")
    public void setAnnotationMergeStrategy(JIPipeTextAnnotationMergeMode annotationMergeStrategy) {
        this.annotationMergeStrategy = annotationMergeStrategy;
    }

    @Override
    public boolean isEnableDefaultCustomExpressionVariables() {
        return true;
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
