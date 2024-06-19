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
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.plugins.parameters.library.quantities.OptionalQuantity;
import org.hkijena.jipipe.plugins.parameters.library.quantities.QuantityParameterSettings;

@SetJIPipeDocumentation(name = "Set physical dimensions", description = "Allows to set the physical dimensions of the incoming images")
@ConfigureJIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Calibration")
@AddJIPipeInputSlot(value = ImagePlusData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = ImagePlusData.class, slotName = "Output", create = true)
@AddJIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "Image\nProperties")
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
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImagePlus img = iterationStep.getInputData(getFirstInputSlot(), ImagePlusData.class, progressInfo).getDuplicateImage();
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
        iterationStep.addOutputData(getFirstOutputSlot(), new ImagePlusData(img), progressInfo);
    }

    @SetJIPipeDocumentation(name = "Physical dimension (X)", description = "If enabled, sets the physical dimension of the image")
    @JIPipeParameter("physical-dimension-x")
    @QuantityParameterSettings(predefinedUnits = {"pixel", "nm", "µm", "microns", "mm", "cm", "dm", "m"})
    public OptionalQuantity getPhysicalDimensionX() {
        return physicalDimensionX;
    }

    @JIPipeParameter("physical-dimension-x")
    public void setPhysicalDimensionX(OptionalQuantity physicalDimensionX) {
        this.physicalDimensionX = physicalDimensionX;
    }

    @SetJIPipeDocumentation(name = "Physical dimension (Y)", description = "If enabled, sets the physical dimension of the image")
    @JIPipeParameter("physical-dimension-y")
    @QuantityParameterSettings(predefinedUnits = {"pixel", "nm", "µm", "microns", "mm", "cm", "dm", "m"})
    public OptionalQuantity getPhysicalDimensionY() {
        return physicalDimensionY;
    }

    @JIPipeParameter("physical-dimension-y")
    public void setPhysicalDimensionY(OptionalQuantity physicalDimensionY) {
        this.physicalDimensionY = physicalDimensionY;
    }

    @SetJIPipeDocumentation(name = "Physical dimension (Z)", description = "If enabled, sets the physical dimension of the image")
    @JIPipeParameter("physical-dimension-z")
    @QuantityParameterSettings(predefinedUnits = {"pixel", "nm", "µm", "microns", "mm", "cm", "dm", "m"})
    public OptionalQuantity getPhysicalDimensionZ() {
        return physicalDimensionZ;
    }

    @JIPipeParameter("physical-dimension-z")
    public void setPhysicalDimensionZ(OptionalQuantity physicalDimensionZ) {
        this.physicalDimensionZ = physicalDimensionZ;
    }

    @SetJIPipeDocumentation(name = "Physical dimension (Time)", description = "If enabled, sets the physical dimension of the image. Please note that only the unit is supported.")
    @JIPipeParameter("physical-dimension-t")
    @QuantityParameterSettings(predefinedUnits = {"ns", "µs", "ms", "s", "min", "h", "d"})
    public OptionalQuantity getPhysicalDimensionT() {
        return physicalDimensionT;
    }

    @JIPipeParameter("physical-dimension-t")
    public void setPhysicalDimensionT(OptionalQuantity physicalDimensionT) {
        this.physicalDimensionT = physicalDimensionT;
    }

    @SetJIPipeDocumentation(name = "Physical dimension (Value)", description = "If enabled, sets the physical dimension of the image. Please note that only the unit is supported.")
    @JIPipeParameter("physical-dimension-value")
    public OptionalQuantity getPhysicalDimensionValue() {
        return physicalDimensionValue;
    }

    @JIPipeParameter("physical-dimension-value")
    public void setPhysicalDimensionValue(OptionalQuantity physicalDimensionValue) {
        this.physicalDimensionValue = physicalDimensionValue;
    }
}
