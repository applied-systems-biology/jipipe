package org.hkijena.jipipe.extensions.imagejalgorithms.nodes.calibration;

import ij.ImagePlus;
import ij.measure.Calibration;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImageJNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionParameterSettings;
import org.hkijena.jipipe.extensions.expressions.ExpressionVariables;
import org.hkijena.jipipe.extensions.expressions.OptionalJIPipeExpressionParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImagePlusPropertiesExpressionParameterVariablesInfo;
import org.hkijena.jipipe.extensions.parameters.library.quantities.Quantity;
import org.hkijena.jipipe.utils.StringUtils;

@JIPipeDocumentation(name = "Set physical dimensions from expressions", description = "Allows to set the physical dimensions of the incoming images. This node allows " +
        "to utilize expressions that have access to annotations and image properties.")
@JIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Calibration")
@JIPipeInputSlot(value = ImagePlusData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ImagePlusData.class, slotName = "Output", autoCreate = true)
@JIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "Image\nProperties")
public class SetPhysicalDimensionsByExpressionsAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private OptionalJIPipeExpressionParameter physicalDimensionX = new OptionalJIPipeExpressionParameter();
    private OptionalJIPipeExpressionParameter physicalDimensionY = new OptionalJIPipeExpressionParameter();
    private OptionalJIPipeExpressionParameter physicalDimensionZ = new OptionalJIPipeExpressionParameter();

    private OptionalJIPipeExpressionParameter physicalDimensionT = new OptionalJIPipeExpressionParameter();

    private OptionalJIPipeExpressionParameter physicalDimensionValue = new OptionalJIPipeExpressionParameter();

    public SetPhysicalDimensionsByExpressionsAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public SetPhysicalDimensionsByExpressionsAlgorithm(SetPhysicalDimensionsByExpressionsAlgorithm other) {
        super(other);
        this.physicalDimensionX = new OptionalJIPipeExpressionParameter(other.physicalDimensionX);
        this.physicalDimensionY = new OptionalJIPipeExpressionParameter(other.physicalDimensionY);
        this.physicalDimensionZ = new OptionalJIPipeExpressionParameter(other.physicalDimensionZ);
        this.physicalDimensionT = new OptionalJIPipeExpressionParameter(other.physicalDimensionT);
        this.physicalDimensionValue = new OptionalJIPipeExpressionParameter(other.physicalDimensionValue);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeProgressInfo progressInfo) {
        ImagePlus img = iterationStep.getInputData(getFirstInputSlot(), ImagePlusData.class, progressInfo).getDuplicateImage();
        Calibration calibration = img.getCalibration();
        if (calibration == null) {
            calibration = new Calibration(img);
            img.setCalibration(calibration);
        }
        ExpressionVariables variables = new ExpressionVariables();
        ImagePlusPropertiesExpressionParameterVariablesInfo.extractValues(variables, img, iterationStep.getMergedTextAnnotations().values());
        if (physicalDimensionX.isEnabled()) {
            Quantity quantity = Quantity.parse(StringUtils.nullToEmpty(physicalDimensionX.getContent().evaluate(variables)));
            calibration.setXUnit(quantity.getUnit());
            calibration.pixelWidth = quantity.getValue();
        }
        if (physicalDimensionY.isEnabled()) {
            Quantity quantity = Quantity.parse(StringUtils.nullToEmpty(physicalDimensionY.getContent().evaluate(variables)));
            calibration.setYUnit(quantity.getUnit());
            calibration.pixelHeight = quantity.getValue();
        }
        if (physicalDimensionZ.isEnabled()) {
            Quantity quantity = Quantity.parse(StringUtils.nullToEmpty(physicalDimensionZ.getContent().evaluate(variables)));
            calibration.setZUnit(quantity.getUnit());
            calibration.pixelDepth = quantity.getValue();
        }
        if (physicalDimensionT.isEnabled()) {
            Quantity quantity = Quantity.parse(StringUtils.nullToEmpty(physicalDimensionT.getContent().evaluate(variables)));
            calibration.setTimeUnit(quantity.getUnit());
        }
        if (physicalDimensionValue.isEnabled()) {
            Quantity quantity = Quantity.parse(StringUtils.nullToEmpty(physicalDimensionValue.getContent().evaluate(variables)));
            calibration.setValueUnit(quantity.getUnit());
        }

        iterationStep.addOutputData(getFirstOutputSlot(), new ImagePlusData(img), progressInfo);
    }

    @JIPipeDocumentation(name = "Physical dimension (X)", description = "If enabled, sets the physical dimension of the image")
    @JIPipeParameter("physical-dimension-x")
    @JIPipeExpressionParameterSettings(variableSource = ImagePlusPropertiesExpressionParameterVariablesInfo.class)
    public OptionalJIPipeExpressionParameter getPhysicalDimensionX() {
        return physicalDimensionX;
    }

    @JIPipeParameter("physical-dimension-x")
    public void setPhysicalDimensionX(OptionalJIPipeExpressionParameter physicalDimensionX) {
        this.physicalDimensionX = physicalDimensionX;
    }

    @JIPipeDocumentation(name = "Physical dimension (Y)", description = "If enabled, sets the physical dimension of the image")
    @JIPipeParameter("physical-dimension-y")
    @JIPipeExpressionParameterSettings(variableSource = ImagePlusPropertiesExpressionParameterVariablesInfo.class)
    public OptionalJIPipeExpressionParameter getPhysicalDimensionY() {
        return physicalDimensionY;
    }

    @JIPipeParameter("physical-dimension-y")
    public void setPhysicalDimensionY(OptionalJIPipeExpressionParameter physicalDimensionY) {
        this.physicalDimensionY = physicalDimensionY;
    }

    @JIPipeDocumentation(name = "Physical dimension (Z)", description = "If enabled, sets the physical dimension of the image")
    @JIPipeParameter("physical-dimension-z")
    @JIPipeExpressionParameterSettings(variableSource = ImagePlusPropertiesExpressionParameterVariablesInfo.class)
    public OptionalJIPipeExpressionParameter getPhysicalDimensionZ() {
        return physicalDimensionZ;
    }

    @JIPipeParameter("physical-dimension-z")
    public void setPhysicalDimensionZ(OptionalJIPipeExpressionParameter physicalDimensionZ) {
        this.physicalDimensionZ = physicalDimensionZ;
    }

    @JIPipeDocumentation(name = "Physical dimension (T)", description = "If enabled, sets the physical dimension of the image. Please note that only the unit is supported.")
    @JIPipeParameter("physical-dimension-t")
    @JIPipeExpressionParameterSettings(variableSource = ImagePlusPropertiesExpressionParameterVariablesInfo.class)
    public OptionalJIPipeExpressionParameter getPhysicalDimensionT() {
        return physicalDimensionT;
    }

    @JIPipeParameter("physical-dimension-t")
    public void setPhysicalDimensionT(OptionalJIPipeExpressionParameter physicalDimensionT) {
        this.physicalDimensionT = physicalDimensionT;
    }

    @JIPipeDocumentation(name = "Physical dimension (Value)", description = "If enabled, sets the physical dimension of the image. Please note that only the unit is supported.")
    @JIPipeParameter("physical-dimension-value")
    @JIPipeExpressionParameterSettings(variableSource = ImagePlusPropertiesExpressionParameterVariablesInfo.class)
    public OptionalJIPipeExpressionParameter getPhysicalDimensionValue() {
        return physicalDimensionValue;
    }

    @JIPipeParameter("physical-dimension-value")
    public void setPhysicalDimensionValue(OptionalJIPipeExpressionParameter physicalDimensionValue) {
        this.physicalDimensionValue = physicalDimensionValue;
    }
}
