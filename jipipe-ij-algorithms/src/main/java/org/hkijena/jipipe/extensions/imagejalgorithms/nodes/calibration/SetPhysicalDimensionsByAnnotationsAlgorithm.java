package org.hkijena.jipipe.extensions.imagejalgorithms.nodes.calibration;

import ij.ImagePlus;
import ij.measure.Calibration;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImageJNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.databatch.JIPipeSingleDataBatch;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameterSettings;
import org.hkijena.jipipe.extensions.expressions.ExpressionVariables;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImagePlusPropertiesExpressionParameterVariableSource;
import org.hkijena.jipipe.extensions.parameters.library.primitives.optional.OptionalAnnotationNameParameter;
import org.hkijena.jipipe.extensions.parameters.library.quantities.Quantity;

@JIPipeDocumentation(name = "Set physical dimensions from annotations", description = "Allows to set the physical dimensions of the incoming images. This node allows " +
        "extracts the properties from the annotations.")
@JIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Calibration")
@JIPipeInputSlot(value = ImagePlusData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ImagePlusData.class, slotName = "Output", autoCreate = true)
@JIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "Image\nProperties")
public class SetPhysicalDimensionsByAnnotationsAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private OptionalAnnotationNameParameter physicalDimensionXAnnotation = new OptionalAnnotationNameParameter("Physical dimension (X)", true);
    private OptionalAnnotationNameParameter physicalDimensionYAnnotation = new OptionalAnnotationNameParameter("Physical dimension (Y)", true);
    private OptionalAnnotationNameParameter physicalDimensionZAnnotation = new OptionalAnnotationNameParameter("Physical dimension (Z)", true);
    private OptionalAnnotationNameParameter physicalDimensionTAnnotation = new OptionalAnnotationNameParameter("Physical dimension (Time)", false);
    private OptionalAnnotationNameParameter physicalDimensionValueAnnotation = new OptionalAnnotationNameParameter("Physical dimension (Value)", false);

    public SetPhysicalDimensionsByAnnotationsAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public SetPhysicalDimensionsByAnnotationsAlgorithm(SetPhysicalDimensionsByAnnotationsAlgorithm other) {
        super(other);
        this.physicalDimensionXAnnotation = new OptionalAnnotationNameParameter(other.physicalDimensionXAnnotation);
        this.physicalDimensionYAnnotation = new OptionalAnnotationNameParameter(other.physicalDimensionYAnnotation);
        this.physicalDimensionZAnnotation = new OptionalAnnotationNameParameter(other.physicalDimensionZAnnotation);
        this.physicalDimensionTAnnotation = new OptionalAnnotationNameParameter(other.physicalDimensionTAnnotation);
        this.physicalDimensionValueAnnotation = new OptionalAnnotationNameParameter(other.physicalDimensionValueAnnotation);
    }

    @Override
    protected void runIteration(JIPipeSingleDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ImagePlus img = dataBatch.getInputData(getFirstInputSlot(), ImagePlusData.class, progressInfo).getDuplicateImage();
        Calibration calibration = img.getCalibration();
        if (calibration == null) {
            calibration = new Calibration(img);
            img.setCalibration(calibration);
        }
        ExpressionVariables variables = new ExpressionVariables();
        ImagePlusPropertiesExpressionParameterVariableSource.extractValues(variables, img, dataBatch.getMergedTextAnnotations().values());
        if (physicalDimensionXAnnotation.isEnabled()) {
            Quantity quantity = Quantity.parse(dataBatch.getMergedTextAnnotation(physicalDimensionXAnnotation.getContent()).getValue());
            calibration.setXUnit(quantity.getUnit());
            calibration.pixelWidth = quantity.getValue();
        }
        if (physicalDimensionYAnnotation.isEnabled()) {
            Quantity quantity = Quantity.parse(dataBatch.getMergedTextAnnotation(physicalDimensionYAnnotation.getContent()).getValue());
            calibration.setYUnit(quantity.getUnit());
            calibration.pixelHeight = quantity.getValue();
        }
        if (physicalDimensionZAnnotation.isEnabled()) {
            Quantity quantity = Quantity.parse(dataBatch.getMergedTextAnnotation(physicalDimensionZAnnotation.getContent()).getValue());
            calibration.setZUnit(quantity.getUnit());
            calibration.pixelDepth = quantity.getValue();
        }
        if (physicalDimensionTAnnotation.isEnabled()) {
            Quantity quantity = Quantity.parse(dataBatch.getMergedTextAnnotation(physicalDimensionTAnnotation.getContent()).getValue());
            calibration.setTimeUnit(quantity.getUnit());
        }
        if (physicalDimensionValueAnnotation.isEnabled()) {
            Quantity quantity = Quantity.parse(dataBatch.getMergedTextAnnotation(physicalDimensionValueAnnotation.getContent()).getValue());
            calibration.setValueUnit(quantity.getUnit());
        }

        dataBatch.addOutputData(getFirstOutputSlot(), new ImagePlusData(img), progressInfo);
    }

    @JIPipeDocumentation(name = "Physical dimension (X)", description = "If enabled, sets the physical dimension of the image")
    @JIPipeParameter("physical-dimension-x")
    @ExpressionParameterSettings(variableSource = ImagePlusPropertiesExpressionParameterVariableSource.class)
    public OptionalAnnotationNameParameter getPhysicalDimensionX() {
        return physicalDimensionXAnnotation;
    }

    @JIPipeParameter("physical-dimension-x")
    public void setPhysicalDimensionX(OptionalAnnotationNameParameter physicalDimensionX) {
        this.physicalDimensionXAnnotation = physicalDimensionX;
    }

    @JIPipeDocumentation(name = "Physical dimension (Y)", description = "If enabled, sets the physical dimension of the image")
    @JIPipeParameter("physical-dimension-y")
    @ExpressionParameterSettings(variableSource = ImagePlusPropertiesExpressionParameterVariableSource.class)
    public OptionalAnnotationNameParameter getPhysicalDimensionY() {
        return physicalDimensionYAnnotation;
    }

    @JIPipeParameter("physical-dimension-y")
    public void setPhysicalDimensionY(OptionalAnnotationNameParameter physicalDimensionY) {
        this.physicalDimensionYAnnotation = physicalDimensionY;
    }

    @JIPipeDocumentation(name = "Physical dimension (Z)", description = "If enabled, sets the physical dimension of the image")
    @JIPipeParameter("physical-dimension-z")
    @ExpressionParameterSettings(variableSource = ImagePlusPropertiesExpressionParameterVariableSource.class)
    public OptionalAnnotationNameParameter getPhysicalDimensionZ() {
        return physicalDimensionZAnnotation;
    }

    @JIPipeParameter("physical-dimension-z")
    public void setPhysicalDimensionZ(OptionalAnnotationNameParameter physicalDimensionZ) {
        this.physicalDimensionZAnnotation = physicalDimensionZ;
    }

    @JIPipeDocumentation(name = "Physical dimension (T)", description = "If enabled, sets the physical dimension of the image. Please note that only the unit is supported.")
    @JIPipeParameter("physical-dimension-t")
    @ExpressionParameterSettings(variableSource = ImagePlusPropertiesExpressionParameterVariableSource.class)
    public OptionalAnnotationNameParameter getPhysicalDimensionT() {
        return physicalDimensionTAnnotation;
    }

    @JIPipeParameter("physical-dimension-t")
    public void setPhysicalDimensionT(OptionalAnnotationNameParameter physicalDimensionT) {
        this.physicalDimensionTAnnotation = physicalDimensionT;
    }

    @JIPipeDocumentation(name = "Physical dimension (Value)", description = "If enabled, sets the physical dimension of the image. Please note that only the unit is supported.")
    @JIPipeParameter("physical-dimension-value")
    @ExpressionParameterSettings(variableSource = ImagePlusPropertiesExpressionParameterVariableSource.class)
    public OptionalAnnotationNameParameter getPhysicalDimensionValue() {
        return physicalDimensionValueAnnotation;
    }

    @JIPipeParameter("physical-dimension-value")
    public void setPhysicalDimensionValue(OptionalAnnotationNameParameter physicalDimensionValue) {
        this.physicalDimensionValueAnnotation = physicalDimensionValue;
    }
}
