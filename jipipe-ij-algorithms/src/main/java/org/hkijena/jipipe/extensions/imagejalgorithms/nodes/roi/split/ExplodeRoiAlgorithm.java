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

package org.hkijena.jipipe.extensions.imagejalgorithms.nodes.roi.split;

import ij.gui.Roi;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.RoiNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterPersistence;
import org.hkijena.jipipe.extensions.expressions.*;
import org.hkijena.jipipe.extensions.expressions.variables.TextAnnotationsExpressionParameterVariableSource;
import org.hkijena.jipipe.extensions.imagejalgorithms.nodes.roi.modify.ChangeRoiPropertiesFromExpressionsAlgorithm;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.parameters.library.primitives.StringParameterSettings;
import org.hkijena.jipipe.extensions.parameters.library.primitives.optional.OptionalStringParameter;
import org.hkijena.jipipe.utils.ColorUtils;
import org.hkijena.jipipe.utils.ResourceUtils;
import org.hkijena.jipipe.utils.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Wrapper around {@link ij.plugin.frame.RoiManager}
 */
@JIPipeDocumentation(name = "Split into individual ROI lists", description = "Splits the ROI in a ROI list into individual ROI lists.")
@JIPipeNode(nodeTypeCategory = RoiNodeTypeCategory.class, menuPath = "Split")
@JIPipeInputSlot(value = ROIListData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ROIListData.class, slotName = "Output", autoCreate = true)
public class ExplodeRoiAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private final CustomExpressionVariablesParameter customExpressionVariables;
    private OptionalStringParameter generatedAnnotation = new OptionalStringParameter();
    private DefaultExpressionParameter annotationValue = new DefaultExpressionParameter("\"index=\" + index + \";name=\" + name");

    /**
     * Instantiates a new node type.
     *
     * @param info the info
     */
    public ExplodeRoiAlgorithm(JIPipeNodeInfo info) {
        super(info);
        this.customExpressionVariables = new CustomExpressionVariablesParameter(this);
        generatedAnnotation.setContent("ROI index");
    }

    /**
     * Instantiates a new node type.
     *
     * @param other the other
     */
    public ExplodeRoiAlgorithm(ExplodeRoiAlgorithm other) {
        super(other);
        this.customExpressionVariables = new CustomExpressionVariablesParameter(other.customExpressionVariables, this);
        this.generatedAnnotation = other.generatedAnnotation;
        this.annotationValue = new DefaultExpressionParameter(other.annotationValue);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeProgressInfo progressInfo) {
        ROIListData data = iterationStep.getInputData(getFirstInputSlot(), ROIListData.class, progressInfo);

        ExpressionVariables variables = new ExpressionVariables();
        variables.putAnnotations(iterationStep.getMergedTextAnnotations());
        customExpressionVariables.writeToVariables(variables, true, "custom.", true, "custom");

        for (int i = 0; i < data.size(); i++) {
            Roi roi = data.get(i);
            List<JIPipeTextAnnotation> annotations = new ArrayList<>();
            if (generatedAnnotation.isEnabled() && !StringUtils.isNullOrEmpty(generatedAnnotation.getContent())) {

                // Make metadata accessible
                Map<String, String> roiProperties = ImageJUtils.getRoiProperties(roi);
                variables.set("metadata", roiProperties);
                for (Map.Entry<String, String> entry : roiProperties.entrySet()) {
                    variables.set("metadata." + entry.getKey(), entry.getValue());
                }

                variables.set("index", i);
                variables.set("num_roi", data.size());
                variables.set("x", roi.getXBase());
                variables.set("y", roi.getYBase());
                variables.set("z", roi.getZPosition());
                variables.set("c", roi.getCPosition());
                variables.set("t", roi.getTPosition());
                variables.set("fill_color", roi.getFillColor() != null ? ColorUtils.colorToHexString(roi.getFillColor()) : null);
                variables.set("line_color", roi.getStrokeColor() != null ? ColorUtils.colorToHexString(roi.getStrokeColor()) : null);
                variables.set("line_width", roi.getStrokeWidth());
                variables.set("name", StringUtils.orElse(roi.getName(), "unnamed"));

                annotations.add(new JIPipeTextAnnotation(generatedAnnotation.getContent(), annotationValue.evaluateToString(variables)));
            }
            ROIListData output = new ROIListData();
            output.add(roi);
            iterationStep.addOutputData(getFirstOutputSlot(), output, annotations, JIPipeTextAnnotationMergeMode.Merge, progressInfo);
        }
    }

    @JIPipeDocumentation(name = "Generated annotation name", description = "Optional. Annotation that is added to each individual ROI list. Contains the value index=[index];name=[name].")
    @JIPipeParameter("generated-annotation")
    @StringParameterSettings(monospace = true, icon = ResourceUtils.RESOURCE_BASE_PATH + "/icons/data-types/annotation.png")
    public OptionalStringParameter getGeneratedAnnotation() {
        return generatedAnnotation;
    }

    @JIPipeParameter("generated-annotation")
    public void setGeneratedAnnotation(OptionalStringParameter generatedAnnotation) {
        this.generatedAnnotation = generatedAnnotation;
    }

    @JIPipeDocumentation(name = "Annotation value", description = "If an annotation is generated, sets the value")
    @JIPipeParameter("roi-name")
    @ExpressionParameterSettings(variableSource = ChangeRoiPropertiesFromExpressionsAlgorithm.VariableSource.class, hint = "per ROI")
    @ExpressionParameterSettingsVariable(fromClass = TextAnnotationsExpressionParameterVariableSource.class)
    @ExpressionParameterSettingsVariable(key = "custom", name = "Custom variables", description = "A map containing custom expression variables (keys are the parameter keys)")
    @ExpressionParameterSettingsVariable(name = "custom.<Custom variable key>", description = "Custom variable parameters are added with a prefix 'custom.'")
    @ExpressionParameterSettingsVariable(key = "metadata", name = "ROI metadata", description = "A map containing the ROI metadata/properties (string keys, string values)")
    @ExpressionParameterSettingsVariable(name = "metadata.<Metadata key>", description = "ROI metadata/properties accessible via their string keys")
    public DefaultExpressionParameter getAnnotationValue() {
        return annotationValue;
    }

    @JIPipeParameter("roi-name")
    public void setAnnotationValue(DefaultExpressionParameter annotationValue) {
        this.annotationValue = annotationValue;
    }

    @JIPipeDocumentation(name = "Custom expression variables", description = "Here you can add parameters that will be included into the expression as variables <code>custom.[key]</code>. Alternatively, you can access them via <code>GET_ITEM(\"custom\", \"[key]\")</code>.")
    @JIPipeParameter(value = "custom-filter-variables", iconURL = ResourceUtils.RESOURCE_BASE_PATH + "/icons/actions/insert-math-expression.png",
            iconDarkURL = ResourceUtils.RESOURCE_BASE_PATH + "/dark/icons/actions/insert-math-expression.png", persistence = JIPipeParameterPersistence.NestedCollection)
    public CustomExpressionVariablesParameter getCustomExpressionVariables() {
        return customExpressionVariables;
    }
}
