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
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.parameters.library.quantities.OptionalQuantity;
import org.hkijena.jipipe.extensions.parameters.library.quantities.QuantityParameterSettings;

@JIPipeDocumentation(name = "Set physical dimensions", description = "Allows to set the physical dimensions of the incoming images")
@JIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Calibration")
@JIPipeInputSlot(value = ImagePlusData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ImagePlusData.class, slotName = "Output", autoCreate = true)
@JIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "Image\nProperties")
public class SetPhysicalDimensionsAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private OptionalQuantity physicalDimensionX = new OptionalQuantity();
    private OptionalQuantity physicalDimensionY = new OptionalQuantity();
    private OptionalQuantity physicalDimensionZ = new OptionalQuantity();

    private OptionalQuantity physicalDimensionT = new OptionalQuantity();
    private OptionalQuantity physicalDimensionValue = new OptionalQuantity();

    public SetPhysicalDimensionsAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public SetPhysicalDimensionsAlgorithm(SetPhysicalDimensionsAlgorithm other) {
        super(other);
        this.physicalDimensionX = new OptionalQuantity(other.physicalDimensionX);
        this.physicalDimensionY = new OptionalQuantity(other.physicalDimensionY);
        this.physicalDimensionZ = new OptionalQuantity(other.physicalDimensionZ);
        this.physicalDimensionT = new OptionalQuantity(other.physicalDimensionT);
        this.physicalDimensionValue = new OptionalQuantity(other.physicalDimensionValue);
    }

    @Override
    protected void runIteration(JIPipeSingleDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ImagePlus img = dataBatch.getInputData(getFirstInputSlot(), ImagePlusData.class, progressInfo).getDuplicateImage();
        Calibration calibration = img.getCalibration();
        if (calibration == null) {
            calibration = new Calibration(img);
            img.setCalibration(calibration);
        }
        if (physicalDimensionX.isEnabled()) {
            calibration.setXUnit(physicalDimensionX.getContent().getUnit());
            calibration.pixelWidth = physicalDimensionX.getContent().getValue();
        }
        if (physicalDimensionY.isEnabled()) {
            calibration.setYUnit(physicalDimensionY.getContent().getUnit());
            calibration.pixelHeight = physicalDimensionY.getContent().getValue();
        }
        if (physicalDimensionZ.isEnabled()) {
            calibration.setZUnit(physicalDimensionZ.getContent().getUnit());
            calibration.pixelDepth = physicalDimensionZ.getContent().getValue();
        }
        if (physicalDimensionT.isEnabled()) {
            calibration.setTimeUnit(physicalDimensionT.getContent().getUnit());
        }
        if (physicalDimensionValue.isEnabled()) {
            calibration.setValueUnit(physicalDimensionValue.getContent().getUnit());
        }
        dataBatch.addOutputData(getFirstOutputSlot(), new ImagePlusData(img), progressInfo);
    }

    @JIPipeDocumentation(name = "Physical dimension (X)", description = "If enabled, sets the physical dimension of the image")
    @JIPipeParameter("physical-dimension-x")
    @QuantityParameterSettings(predefinedUnits = {"pixel", "nm", "µm", "microns", "mm", "cm", "dm", "m"})
    public OptionalQuantity getPhysicalDimensionX() {
        return physicalDimensionX;
    }

    @JIPipeParameter("physical-dimension-x")
    public void setPhysicalDimensionX(OptionalQuantity physicalDimensionX) {
        this.physicalDimensionX = physicalDimensionX;
    }

    @JIPipeDocumentation(name = "Physical dimension (Y)", description = "If enabled, sets the physical dimension of the image")
    @JIPipeParameter("physical-dimension-y")
    @QuantityParameterSettings(predefinedUnits = {"pixel", "nm", "µm", "microns", "mm", "cm", "dm", "m"})
    public OptionalQuantity getPhysicalDimensionY() {
        return physicalDimensionY;
    }

    @JIPipeParameter("physical-dimension-y")
    public void setPhysicalDimensionY(OptionalQuantity physicalDimensionY) {
        this.physicalDimensionY = physicalDimensionY;
    }

    @JIPipeDocumentation(name = "Physical dimension (Z)", description = "If enabled, sets the physical dimension of the image")
    @JIPipeParameter("physical-dimension-z")
    @QuantityParameterSettings(predefinedUnits = {"pixel", "nm", "µm", "microns", "mm", "cm", "dm", "m"})
    public OptionalQuantity getPhysicalDimensionZ() {
        return physicalDimensionZ;
    }

    @JIPipeParameter("physical-dimension-z")
    public void setPhysicalDimensionZ(OptionalQuantity physicalDimensionZ) {
        this.physicalDimensionZ = physicalDimensionZ;
    }

    @JIPipeDocumentation(name = "Physical dimension (Time)", description = "If enabled, sets the physical dimension of the image. Please note that only the unit is supported.")
    @JIPipeParameter("physical-dimension-t")
    @QuantityParameterSettings(predefinedUnits = {"ns", "µs", "ms", "s", "min", "h", "d"})
    public OptionalQuantity getPhysicalDimensionT() {
        return physicalDimensionT;
    }

    @JIPipeParameter("physical-dimension-t")
    public void setPhysicalDimensionT(OptionalQuantity physicalDimensionT) {
        this.physicalDimensionT = physicalDimensionT;
    }

    @JIPipeDocumentation(name = "Physical dimension (Value)", description = "If enabled, sets the physical dimension of the image. Please note that only the unit is supported.")
    @JIPipeParameter("physical-dimension-value")
    public OptionalQuantity getPhysicalDimensionValue() {
        return physicalDimensionValue;
    }

    @JIPipeParameter("physical-dimension-value")
    public void setPhysicalDimensionValue(OptionalQuantity physicalDimensionValue) {
        this.physicalDimensionValue = physicalDimensionValue;
    }
}
