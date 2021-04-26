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

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeAnnotation;
import org.hkijena.jipipe.api.data.JIPipeAnnotationMergeStrategy;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.exceptions.UserFriendlyRuntimeException;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.AnnotationsNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.extensions.parameters.expressions.*;
import org.hkijena.jipipe.utils.StringUtils;

import java.util.HashSet;
import java.util.Set;

/**
 * Algorithm that annotates all data with the same annotation
 */
@JIPipeDocumentation(name = "Set single annotation", description = "Sets a single annotation")
@JIPipeOrganization(nodeTypeCategory = AnnotationsNodeTypeCategory.class, menuPath = "Modify")
@JIPipeInputSlot(value = JIPipeData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = JIPipeData.class, slotName = "Output", inheritedSlot = "Input", autoCreate = true)
public class SetSingleAnnotation extends JIPipeSimpleIteratingAlgorithm {

    private StringQueryExpression annotationValue = new StringQueryExpression();
    private StringQueryExpression annotationName = new StringQueryExpression();
    private JIPipeAnnotationMergeStrategy annotationMergeStrategy = JIPipeAnnotationMergeStrategy.OverwriteExisting;

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
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ExpressionParameters variableSet = new ExpressionParameters();
        for (JIPipeAnnotation annotation : dataBatch.getAnnotations().values()) {
            variableSet.set(annotation.getName(), annotation.getValue());
        }
        variableSet.set("data_string", getFirstInputSlot().getVirtualData(dataBatch.getInputSlotRows().get(getFirstInputSlot())).getStringRepresentation());
        String name = StringUtils.nullToEmpty(annotationName.generate(variableSet));
        String value = StringUtils.nullToEmpty(annotationValue.generate(variableSet));
        if (StringUtils.isNullOrEmpty(name)) {
            throw new UserFriendlyRuntimeException("Generated annotation name is empty!",
                    "Generated annotation name is empty!",
                    getName(),
                    "You wanted to set the name of an annotation, but the expression generated an empty name. This is not allowed.",
                    "Check if the expression is correct.");
        }
        dataBatch.addGlobalAnnotation(new JIPipeAnnotation(name, value), annotationMergeStrategy);
        dataBatch.addOutputData(getFirstOutputSlot(), dataBatch.getInputData(getFirstInputSlot(), JIPipeData.class, progressInfo), progressInfo);
    }

    @JIPipeDocumentation(name = "Annotation value", description = "The value of the generated annotation. " + StringQueryExpression.DOCUMENTATION_DESCRIPTION)
    @JIPipeParameter("annotation-value")
    @ExpressionParameterSettings(variableSource = VariableSource.class)
    public StringQueryExpression getAnnotationValue() {
        return annotationValue;
    }

    @JIPipeParameter("annotation-value")
    public void setAnnotationValue(StringQueryExpression annotationValue) {
        this.annotationValue = annotationValue;
    }

    @JIPipeDocumentation(name = "Annotation name", description = "The name of the generated annotation. " + StringQueryExpression.DOCUMENTATION_DESCRIPTION)
    @JIPipeParameter("annotation-name")
    @ExpressionParameterSettings(variableSource = VariableSource.class)
    public StringQueryExpression getAnnotationName() {
        return annotationName;
    }

    @JIPipeParameter("annotation-name")
    public void setAnnotationName(StringQueryExpression annotationName) {
        this.annotationName = annotationName;
    }

    public static class VariableSource implements ExpressionParameterVariableSource {

        public static final Set<ExpressionParameterVariable> VARIABLES;

        static {
            VARIABLES = new HashSet<>();
            VARIABLES.add(new ExpressionParameterVariable("<Annotations>",
                    "Annotations of the source ROI list are available (use Update Cache to find the list of annotations)",
                    ""));
        }

        @Override
        public Set<ExpressionParameterVariable> getVariables(JIPipeParameterAccess parameterAccess) {
            return VARIABLES;
        }
    }
}
