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

package org.hkijena.jipipe.plugins.imagejalgorithms.nodes.calibration;

import ij.ImagePlus;
import ij.measure.Calibration;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImageJNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameterSettings;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.expressions.ImagePlusPropertiesExpressionParameterVariablesInfo;
import org.hkijena.jipipe.plugins.parameters.library.primitives.optional.OptionalTextAnnotationNameParameter;
import org.hkijena.jipipe.plugins.parameters.library.quantities.Quantity;

@SetJIPipeDocumentation(name = "Set physical dimensions from annotations", description = "Allows to set the physical dimensions of the incoming images. This node allows " +
        "extracts the properties from the annotations.")
@ConfigureJIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Calibration")
@AddJIPipeInputSlot(value = ImagePlusData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = ImagePlusData.class, name = "Output", create = true)
@AddJIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "Image\nProperties")
public class SetPhysicalDimensionsByAnnotationsAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private OptionalTextAnnotationNameParameter physicalDimensionXAnnotation = new OptionalTextAnnotationNameParameter("Physical dimension (X)", true);
    private OptionalTextAnnotationNameParameter physicalDimensionYAnnotation = new OptionalTextAnnotationNameParameter("Physical dimension (Y)", true);
    private OptionalTextAnnotationNameParameter physicalDimensionZAnnotation = new OptionalTextAnnotationNameParameter("Physical dimension (Z)", true);
    private OptionalTextAnnotationNameParameter physicalDimensionTAnnotation = new OptionalTextAnnotationNameParameter("Physical dimension (Time)", false);
    private OptionalTextAnnotationNameParameter physicalDimensionValueAnnotation = new OptionalTextAnnotationNameParameter("Physical dimension (Value)", false);

    public SetPhysicalDimensionsByAnnotationsAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public SetPhysicalDimensionsByAnnotationsAlgorithm(SetPhysicalDimensionsByAnnotationsAlgorithm other) {
        super(other);
        this.physicalDimensionXAnnotation = new OptionalTextAnnotationNameParameter(other.physicalDimensionXAnnotation);
        this.physicalDimensionYAnnotation = new OptionalTextAnnotationNameParameter(other.physicalDimensionYAnnotation);
        this.physicalDimensionZAnnotation = new OptionalTextAnnotationNameParameter(other.physicalDimensionZAnnotation);
        this.physicalDimensionTAnnotation = new OptionalTextAnnotationNameParameter(other.physicalDimensionTAnnotation);
        this.physicalDimensionValueAnnotation = new OptionalTextAnnotationNameParameter(other.physicalDimensionValueAnnotation);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImagePlus img = iterationStep.getInputData(getFirstInputSlot(), ImagePlusData.class, progressInfo).getDuplicateImage();
        Calibration calibration = img.getCalibration();
        if (calibration == null) {
            calibration = new Calibration(img);
            img.setCalibration(calibration);
        }
        JIPipeExpressionVariablesMap variables = new JIPipeExpressionVariablesMap(iterationStep);
        ImagePlusPropertiesExpressionParameterVariablesInfo.extractValues(variables, img, iterationStep.getMergedTextAnnotations().values());
        if (physicalDimensionXAnnotation.isEnabled()) {
            Quantity quantity = Quantity.parse(iterationStep.getMergedTextAnnotation(physicalDimensionXAnnotation.getContent()).getValue());
            calibration.setXUnit(quantity.getUnit());
            calibration.pixelWidth = quantity.getValue();
        }
        if (physicalDimensionYAnnotation.isEnabled()) {
            Quantity quantity = Quantity.parse(iterationStep.getMergedTextAnnotation(physicalDimensionYAnnotation.getContent()).getValue());
            calibration.setYUnit(quantity.getUnit());
            calibration.pixelHeight = quantity.getValue();
        }
        if (physicalDimensionZAnnotation.isEnabled()) {
            Quantity quantity = Quantity.parse(iterationStep.getMergedTextAnnotation(physicalDimensionZAnnotation.getContent()).getValue());
            calibration.setZUnit(quantity.getUnit());
            calibration.pixelDepth = quantity.getValue();
        }
        if (physicalDimensionTAnnotation.isEnabled()) {
            Quantity quantity = Quantity.parse(iterationStep.getMergedTextAnnotation(physicalDimensionTAnnotation.getContent()).getValue());
            calibration.setTimeUnit(quantity.getUnit());
        }
        if (physicalDimensionValueAnnotation.isEnabled()) {
            Quantity quantity = Quantity.parse(iterationStep.getMergedTextAnnotation(physicalDimensionValueAnnotation.getContent()).getValue());
            calibration.setValueUnit(quantity.getUnit());
        }

        iterationStep.addOutputData(getFirstOutputSlot(), new ImagePlusData(img), progressInfo);
    }

    @SetJIPipeDocumentation(name = "Physical dimension (X)", description = "If enabled, sets the physical dimension of the image")
    @JIPipeParameter("physical-dimension-x")
    @JIPipeExpressionParameterSettings(variableSource = ImagePlusPropertiesExpressionParameterVariablesInfo.class)
    public OptionalTextAnnotationNameParameter getPhysicalDimensionX() {
        return physicalDimensionXAnnotation;
    }

    @JIPipeParameter("physical-dimension-x")
    public void setPhysicalDimensionX(OptionalTextAnnotationNameParameter physicalDimensionX) {
        this.physicalDimensionXAnnotation = physicalDimensionX;
    }

    @SetJIPipeDocumentation(name = "Physical dimension (Y)", description = "If enabled, sets the physical dimension of the image")
    @JIPipeParameter("physical-dimension-y")
    @JIPipeExpressionParameterSettings(variableSource = ImagePlusPropertiesExpressionParameterVariablesInfo.class)
    public OptionalTextAnnotationNameParameter getPhysicalDimensionY() {
        return physicalDimensionYAnnotation;
    }

    @JIPipeParameter("physical-dimension-y")
    public void setPhysicalDimensionY(OptionalTextAnnotationNameParameter physicalDimensionY) {
        this.physicalDimensionYAnnotation = physicalDimensionY;
    }

    @SetJIPipeDocumentation(name = "Physical dimension (Z)", description = "If enabled, sets the physical dimension of the image")
    @JIPipeParameter("physical-dimension-z")
    @JIPipeExpressionParameterSettings(variableSource = ImagePlusPropertiesExpressionParameterVariablesInfo.class)
    public OptionalTextAnnotationNameParameter getPhysicalDimensionZ() {
        return physicalDimensionZAnnotation;
    }

    @JIPipeParameter("physical-dimension-z")
    public void setPhysicalDimensionZ(OptionalTextAnnotationNameParameter physicalDimensionZ) {
        this.physicalDimensionZAnnotation = physicalDimensionZ;
    }

    @SetJIPipeDocumentation(name = "Physical dimension (T)", description = "If enabled, sets the physical dimension of the image. Please note that only the unit is supported.")
    @JIPipeParameter("physical-dimension-t")
    @JIPipeExpressionParameterSettings(variableSource = ImagePlusPropertiesExpressionParameterVariablesInfo.class)
    public OptionalTextAnnotationNameParameter getPhysicalDimensionT() {
        return physicalDimensionTAnnotation;
    }

    @JIPipeParameter("physical-dimension-t")
    public void setPhysicalDimensionT(OptionalTextAnnotationNameParameter physicalDimensionT) {
        this.physicalDimensionTAnnotation = physicalDimensionT;
    }

    @SetJIPipeDocumentation(name = "Physical dimension (Value)", description = "If enabled, sets the physical dimension of the image. Please note that only the unit is supported.")
    @JIPipeParameter("physical-dimension-value")
    @JIPipeExpressionParameterSettings(variableSource = ImagePlusPropertiesExpressionParameterVariablesInfo.class)
    public OptionalTextAnnotationNameParameter getPhysicalDimensionValue() {
        return physicalDimensionValueAnnotation;
    }

    @JIPipeParameter("physical-dimension-value")
    public void setPhysicalDimensionValue(OptionalTextAnnotationNameParameter physicalDimensionValue) {
        this.physicalDimensionValueAnnotation = physicalDimensionValue;
    }
}
