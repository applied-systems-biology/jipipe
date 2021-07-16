package org.hkijena.jipipe.extensions.imagejalgorithms.ij1.segment;

import ij.ImagePlus;
import ij.process.ImageProcessor;
import inra.ijpb.watershed.Watershed;
import org.hkijena.jipipe.api.JIPipeCitation;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.Neighborhood2D3D;
import org.hkijena.jipipe.extensions.imagejalgorithms.utils.ImageJUtils2;
import org.hkijena.jipipe.extensions.imagejalgorithms.utils.ImageROITargetArea;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d3.greyscale.ImagePlus3DGreyscaleData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.parameters.primitives.OptionalDoubleParameter;

@JIPipeDocumentation(name = "Classic watershed", description = "Performs segmentation via watershed on a 2D or 3D image using flooding simulations. Please note that this node returns labels instead of masks.")
@JIPipeCitation("\"Determining watersheds in digital pictures via flooding simulations.\" Lausanne-DL tentative. International Society for Optics and Photonics, 1990")
@JIPipeOrganization(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Segment")
@JIPipeInputSlot(value = ImagePlus3DGreyscaleData.class, slotName = "Image", autoCreate = true)
@JIPipeOutputSlot(value = ImagePlus3DGreyscaleData.class, slotName = "Labels", autoCreate = true)
public class ClassicWatershedSegmentationAlgorithm extends JIPipeIteratingAlgorithm {

    private Neighborhood2D3D connectivity = Neighborhood2D3D.NoDiagonals;
    private OptionalDoubleParameter customHMin = new OptionalDoubleParameter(0, false);
    private OptionalDoubleParameter customHMax = new OptionalDoubleParameter(255, false);
    private ImageROITargetArea targetArea = ImageROITargetArea.WholeImage;
    private boolean applyPerSlice = false;

    public ClassicWatershedSegmentationAlgorithm(JIPipeNodeInfo info) {
        super(info);
        ImageJUtils2.updateROIOrMaskSlot(targetArea, getSlotConfiguration());
    }

    public ClassicWatershedSegmentationAlgorithm(ClassicWatershedSegmentationAlgorithm other) {
        super(other);
        this.connectivity = other.connectivity;
        this.customHMax = new OptionalDoubleParameter(other.customHMax);
        this.customHMin = new OptionalDoubleParameter(other.customHMin);
        this.targetArea = other.targetArea;
        this.applyPerSlice = other.applyPerSlice;
        ImageJUtils2.updateROIOrMaskSlot(targetArea, getSlotConfiguration());
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ImagePlus inputImage = dataBatch.getInputData("Image", ImagePlus3DGreyscaleData.class, progressInfo).getImage();
        double hMin, hMax;
        if (customHMin.isEnabled()) {
            hMin = customHMin.getContent();
        } else {
            hMin = 0;
        }
        if (customHMax.isEnabled()) {
            hMax = customHMax.getContent();
        } else {
            switch (inputImage.getBitDepth()) {
                case 8:
                    hMax = 255;
                    break;
                case 16:
                    hMax = 65535;
                    break;
                default:
                    hMax = Double.MAX_VALUE;
                    break;
            }
        }
        if (applyPerSlice) {
            ImagePlus resultImage = ImageJUtils.generateForEachIndexedZCTSlice(inputImage, (ip, index) -> {
                ImageProcessor mask = ImageJUtils2.getMaskProcessorFromMaskOrROI(targetArea, dataBatch, index, progressInfo);
                return Watershed.computeWatershed(new ImagePlus("raw", ip), new ImagePlus("mask", mask), connectivity.getNativeValue2D(), hMin, hMax).getProcessor();
            }, progressInfo);
            resultImage.copyScale(resultImage);
            dataBatch.addOutputData(getFirstOutputSlot(), new ImagePlus3DGreyscaleData(resultImage), progressInfo);
        } else {
            ImagePlus mask = ImageJUtils2.getMaskFromMaskOrROI(targetArea, dataBatch, "Input", progressInfo);
            ImagePlus resultImage = Watershed.computeWatershed(inputImage, mask, inputImage.getStackSize() == 1 ? connectivity.getNativeValue2D() : connectivity.getNativeValue3D(), hMin, hMax);
            resultImage.copyScale(inputImage);
            dataBatch.addOutputData(getFirstOutputSlot(), new ImagePlus3DGreyscaleData(resultImage), progressInfo);
        }
    }

    @JIPipeDocumentation(name = "Connectivity", description = "Determines the pixel neighborhood for the flood fill algorithm.")
    @JIPipeParameter("connectivity")
    public Neighborhood2D3D getConnectivity() {
        return connectivity;
    }

    @JIPipeParameter("connectivity")
    public void setConnectivity(Neighborhood2D3D connectivity) {
        this.connectivity = connectivity;
    }

    @JIPipeDocumentation(name = "Custom H-Min", description = "Allows to customize the H-Min parameter of the watershed")
    @JIPipeParameter("custom-h-min")
    public OptionalDoubleParameter getCustomHMin() {
        return customHMin;
    }

    @JIPipeParameter("custom-h-min")
    public void setCustomHMin(OptionalDoubleParameter customHMin) {
        this.customHMin = customHMin;
    }

    @JIPipeDocumentation(name = "Custom H-Max", description = "Allows to customize the H-Max parameter of the watershed")
    @JIPipeParameter("custom-h-max")
    public OptionalDoubleParameter getCustomHMax() {
        return customHMax;
    }

    @JIPipeParameter("custom-h-max")
    public void setCustomHMax(OptionalDoubleParameter customHMax) {
        this.customHMax = customHMax;
    }

    @JIPipeDocumentation(name = "Only apply to ...", description = "Determines where the watershed is applied.")
    @JIPipeParameter("target-area")
    public ImageROITargetArea getTargetArea() {
        return targetArea;
    }

    @JIPipeParameter("target-area")
    public void setTargetArea(ImageROITargetArea targetArea) {
        this.targetArea = targetArea;
        ImageJUtils2.updateROIOrMaskSlot(targetArea, getSlotConfiguration());
    }

    @JIPipeDocumentation(name = "Apply per slice", description = "If enabled, 3D data is split into 2D slices and the watershed algorithm is applied per slice.")
    @JIPipeParameter("apply-per-slice")
    public boolean isApplyPerSlice() {
        return applyPerSlice;
    }

    @JIPipeParameter("apply-per-slice")
    public void setApplyPerSlice(boolean applyPerSlice) {
        this.applyPerSlice = applyPerSlice;
    }
}
