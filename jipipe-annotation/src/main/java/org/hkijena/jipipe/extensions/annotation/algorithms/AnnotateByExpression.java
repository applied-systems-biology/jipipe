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
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeIssueReport;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.AnnotationsNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeContextAction;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.extensions.expressions.*;
import org.hkijena.jipipe.extensions.parameters.api.pairs.PairParameterSettings;
import org.hkijena.jipipe.extensions.parameters.library.primitives.StringParameterSettings;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.utils.ResourceUtils;
import org.hkijena.jipipe.utils.UIUtils;

import java.util.HashSet;
import java.util.Set;

/**
 * Algorithm that annotates all data with the same annotation
 */
@JIPipeDocumentation(name = "Set/Edit annotations", description = "Modifies the specified annotations to the specified values. Supports expressions to combine existing annotations or generate new values.")
@JIPipeNode(nodeTypeCategory = AnnotationsNodeTypeCategory.class, menuPath = "Modify")
@JIPipeInputSlot(value = JIPipeData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = JIPipeData.class, slotName = "Output", inheritedSlot = "Input", autoCreate = true)
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
    public void reportValidity(JIPipeIssueReport report) {
        for (int i = 0; i < annotations.size(); i++) {
            report.resolve("Annotations").resolve("Item #" + (i + 1)).resolve("Name").checkNonEmpty(annotations.get(i).getValue(), this);
        }
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        for (NamedTextAnnotationGeneratorExpression expression : annotations) {
            ExpressionVariables variableSet = new ExpressionVariables();
            variableSet.set("data_string", getFirstInputSlot().getVirtualData(dataBatch.getInputSlotRows().get(getFirstInputSlot())).getStringRepresentation());
            variableSet.set("data_type", JIPipe.getDataTypes().getIdOf(getFirstInputSlot().getVirtualData(dataBatch.getInputSlotRows().get(getFirstInputSlot())).getDataClass()));
            variableSet.set("row", dataBatch.getInputSlotRows().get(getFirstInputSlot()));
            dataBatch.addMergedTextAnnotation(expression.generateTextAnnotation(dataBatch.getMergedTextAnnotations().values(), variableSet),
                    annotationMergeStrategy
            );
        }
        dataBatch.addOutputData(getFirstOutputSlot(), dataBatch.getInputData(getFirstInputSlot(), JIPipeData.class, progressInfo), progressInfo);
    }

    @JIPipeDocumentation(name = "Annotations", description = "Allows you to set the annotation to add/modify. " + AnnotationGeneratorExpression.DOCUMENTATION_DESCRIPTION)
    @JIPipeParameter("generated-annotation")
    @PairParameterSettings(keyLabel = "Value", valueLabel = "Name", singleRow = false)
    @StringParameterSettings(monospace = true)
    @ExpressionParameterSettings(variableSource = VariableSource.class)
    public NamedTextAnnotationGeneratorExpression.List getAnnotations() {
        return annotations;
    }

    @JIPipeParameter("generated-annotation")
    public void setAnnotations(NamedTextAnnotationGeneratorExpression.List annotations) {
        this.annotations = annotations;
    }

    @JIPipeDocumentation(name = "Merge same annotation values", description = "Determines which strategy is applied if an annotation already exists.")
    @JIPipeParameter("annotation-merge-strategy")
    public JIPipeTextAnnotationMergeMode getAnnotationMergeStrategy() {
        return annotationMergeStrategy;
    }

    @JIPipeParameter("annotation-merge-strategy")
    public void setAnnotationMergeStrategy(JIPipeTextAnnotationMergeMode annotationMergeStrategy) {
        this.annotationMergeStrategy = annotationMergeStrategy;
    }

    @JIPipeDocumentation(name = "Load example", description = "Loads example parameters that showcase how to use this algorithm.")
    @JIPipeContextAction(iconURL = ResourceUtils.RESOURCE_BASE_PATH + "/icons/actions/graduation-cap.png", iconDarkURL = ResourceUtils.RESOURCE_BASE_PATH + "/dark/icons/actions/graduation-cap.png")
    public void setToExample(JIPipeWorkbench parent) {
        if (UIUtils.confirmResetParameters(parent, "Load example")) {
            annotations.clear();
            NamedTextAnnotationGeneratorExpression expression = annotations.addNewInstance();
            expression.setKey(new AnnotationGeneratorExpression("\"My value\""));
            expression.setValue("My annotation");
            getEventBus().post(new ParameterChangedEvent(this, "generated-annotation"));
        }
    }

    public static class VariableSource implements ExpressionParameterVariableSource {

        public static final Set<ExpressionParameterVariable> VARIABLES;

        static {
            VARIABLES = new HashSet<>();
            VARIABLES.add(ExpressionParameterVariable.ANNOTATIONS_VARIABLE);
            VARIABLES.add(new ExpressionParameterVariable("Data string",
                    "The data stored as string",
                    "data_string"));
            VARIABLES.add(new ExpressionParameterVariable("Data type ID",
                    "The data type ID",
                    "data_type"));
            VARIABLES.add(new ExpressionParameterVariable("Row",
                    "The row inside the data table",
                    "row"));
        }

        @Override
        public Set<ExpressionParameterVariable> getVariables(JIPipeParameterAccess parameterAccess) {
            return VARIABLES;
        }
    }
}
