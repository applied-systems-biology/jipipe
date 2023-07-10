package org.hkijena.jipipe.extensions.imagejalgorithms.nodes.calibration;

import ij.ImagePlus;
import ij.measure.Calibration;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImageJNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameterSettings;
import org.hkijena.jipipe.extensions.expressions.ExpressionVariables;
import org.hkijena.jipipe.extensions.expressions.OptionalDefaultExpressionParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImagePlusPropertiesExpressionParameterVariableSource;
import org.hkijena.jipipe.extensions.parameters.library.quantities.Quantity;
import org.hkijena.jipipe.utils.StringUtils;

@JIPipeDocumentation(name = "Set physical dimensions from expressions", description = "Allows to set the physical dimensions of the incoming images. This node allows " +
        "to utilize expressions that have access to annotations and image properties.")
@JIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Calibration")
@JIPipeInputSlot(value = ImagePlusData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ImagePlusData.class, slotName = "Output", inheritedSlot = "Input", autoCreate = true)
@JIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "Image\nProperties")
public class SetPhysicalDimensionsByExpressionsAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private OptionalDefaultExpressionParameter physicalDimensionX = new OptionalDefaultExpressionParameter();
    private OptionalDefaultExpressionParameter physicalDimensionY = new OptionalDefaultExpressionParameter();
    private OptionalDefaultExpressionParameter physicalDimensionZ = new OptionalDefaultExpressionParameter();

    private OptionalDefaultExpressionParameter physicalDimensionT = new OptionalDefaultExpressionParameter();

    private OptionalDefaultExpressionParameter physicalDimensionValue = new OptionalDefaultExpressionParameter();

    public SetPhysicalDimensionsByExpressionsAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public SetPhysicalDimensionsByExpressionsAlgorithm(SetPhysicalDimensionsByExpressionsAlgorithm other) {
        super(other);
        this.physicalDimensionX = new OptionalDefaultExpressionParameter(other.physicalDimensionX);
        this.physicalDimensionY = new OptionalDefaultExpressionParameter(other.physicalDimensionY);
        this.physicalDimensionZ = new OptionalDefaultExpressionParameter(other.physicalDimensionZ);
        this.physicalDimensionT = new OptionalDefaultExpressionParameter(other.physicalDimensionT);
        this.physicalDimensionValue = new OptionalDefaultExpressionParameter(other.physicalDimensionValue);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ImagePlus img = dataBatch.getInputData(getFirstInputSlot(), ImagePlusData.class, progressInfo).getDuplicateImage();
        Calibration calibration = img.getCalibration();
        if (calibration == null) {
            calibration = new Calibration(img);
            img.setCalibration(calibration);
        }
        ExpressionVariables variables = new ExpressionVariables();
        ImagePlusPropertiesExpressionParameterVariableSource.extractValues(variables, img, dataBatch.getMergedTextAnnotations().values());
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

        dataBatch.addOutputData(getFirstOutputSlot(), new ImagePlusData(img), progressInfo);
    }

    @JIPipeDocumentation(name = "Physical dimension (X)", description = "If enabled, sets the physical dimension of the image")
    @JIPipeParameter("physical-dimension-x")
    @ExpressionParameterSettings(variableSource = ImagePlusPropertiesExpressionParameterVariableSource.class)
    public OptionalDefaultExpressionParameter getPhysicalDimensionX() {
        return physicalDimensionX;
    }

    @JIPipeParameter("physical-dimension-x")
    public void setPhysicalDimensionX(OptionalDefaultExpressionParameter physicalDimensionX) {
        this.physicalDimensionX = physicalDimensionX;
    }

    @JIPipeDocumentation(name = "Physical dimension (Y)", description = "If enabled, sets the physical dimension of the image")
    @JIPipeParameter("physical-dimension-y")
    @ExpressionParameterSettings(variableSource = ImagePlusPropertiesExpressionParameterVariableSource.class)
    public OptionalDefaultExpressionParameter getPhysicalDimensionY() {
        return physicalDimensionY;
    }

    @JIPipeParameter("physical-dimension-y")
    public void setPhysicalDimensionY(OptionalDefaultExpressionParameter physicalDimensionY) {
        this.physicalDimensionY = physicalDimensionY;
    }

    @JIPipeDocumentation(name = "Physical dimension (Z)", description = "If enabled, sets the physical dimension of the image")
    @JIPipeParameter("physical-dimension-z")
    @ExpressionParameterSettings(variableSource = ImagePlusPropertiesExpressionParameterVariableSource.class)
    public OptionalDefaultExpressionParameter getPhysicalDimensionZ() {
        return physicalDimensionZ;
    }

    @JIPipeParameter("physical-dimension-z")
    public void setPhysicalDimensionZ(OptionalDefaultExpressionParameter physicalDimensionZ) {
        this.physicalDimensionZ = physicalDimensionZ;
    }

    @JIPipeDocumentation(name = "Physical dimension (T)", description = "If enabled, sets the physical dimension of the image. Please note that only the unit is supported.")
    @JIPipeParameter("physical-dimension-t")
    @ExpressionParameterSettings(variableSource = ImagePlusPropertiesExpressionParameterVariableSource.class)
    public OptionalDefaultExpressionParameter getPhysicalDimensionT() {
        return physicalDimensionT;
    }

    @JIPipeParameter("physical-dimension-t")
    public void setPhysicalDimensionT(OptionalDefaultExpressionParameter physicalDimensionT) {
        this.physicalDimensionT = physicalDimensionT;
    }

    @JIPipeDocumentation(name = "Physical dimension (Value)", description = "If enabled, sets the physical dimension of the image. Please note that only the unit is supported.")
    @JIPipeParameter("physical-dimension-value")
    @ExpressionParameterSettings(variableSource = ImagePlusPropertiesExpressionParameterVariableSource.class)
    public OptionalDefaultExpressionParameter getPhysicalDimensionValue() {
        return physicalDimensionValue;
    }

    @JIPipeParameter("physical-dimension-value")
    public void setPhysicalDimensionValue(OptionalDefaultExpressionParameter physicalDimensionValue) {
        this.physicalDimensionValue = physicalDimensionValue;
    }
}
