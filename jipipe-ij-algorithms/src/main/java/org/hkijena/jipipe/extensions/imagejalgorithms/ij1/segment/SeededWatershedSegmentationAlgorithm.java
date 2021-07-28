package org.hkijena.jipipe.extensions.imagejalgorithms.ij1.segment;

import ij.ImagePlus;
import ij.process.ImageProcessor;
import inra.ijpb.watershed.Watershed;
import org.hkijena.jipipe.api.JIPipeCitation;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
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

@JIPipeDocumentation(name = "Seeded watershed", description = "Performs segmentation via watershed on a 2D or 3D image using flooding simulations. Please note that this node returns labels instead of masks. " +
        "The markers need to be labels, so apply a connected components labeling if you only have masks.")
@JIPipeCitation("\"Determining watersheds in digital pictures via flooding simulations.\" Lausanne-DL tentative. International Society for Optics and Photonics, 1990")
@JIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Segment")
@JIPipeInputSlot(value = ImagePlus3DGreyscaleData.class, slotName = "Image", autoCreate = true)
@JIPipeInputSlot(value = ImagePlus3DGreyscaleData.class, slotName = "Markers", autoCreate = true)
@JIPipeOutputSlot(value = ImagePlus3DGreyscaleData.class, slotName = "Labels", autoCreate = true)
public class SeededWatershedSegmentationAlgorithm extends JIPipeIteratingAlgorithm {

    private Neighborhood2D3D connectivity = Neighborhood2D3D.NoDiagonals;
    private ImageROITargetArea targetArea = ImageROITargetArea.WholeImage;
    private boolean applyPerSlice = false;
    private boolean getDams = false;

    public SeededWatershedSegmentationAlgorithm(JIPipeNodeInfo info) {
        super(info);
        ImageJUtils2.updateROIOrMaskSlot(targetArea, getSlotConfiguration());
    }

    public SeededWatershedSegmentationAlgorithm(SeededWatershedSegmentationAlgorithm other) {
        super(other);
        this.connectivity = other.connectivity;
        this.getDams = other.getDams;
        this.targetArea = other.targetArea;
        this.applyPerSlice = other.applyPerSlice;
        ImageJUtils2.updateROIOrMaskSlot(targetArea, getSlotConfiguration());
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ImagePlus inputImage = dataBatch.getInputData("Image", ImagePlus3DGreyscaleData.class, progressInfo).getImage();
        ImagePlus seedImage = dataBatch.getInputData("Markers", ImagePlus3DGreyscaleData.class, progressInfo).getImage();
        if (applyPerSlice) {
            ImagePlus resultImage = ImageJUtils.generateForEachIndexedZCTSlice(inputImage, (ip, index) -> {
                ImageProcessor mask = ImageJUtils2.getMaskProcessorFromMaskOrROI(targetArea, dataBatch, index, progressInfo);
                ImageProcessor seeds = ImageJUtils.getSliceZero(seedImage, index);
                return Watershed.computeWatershed(new ImagePlus("raw", ip), new ImagePlus("marker", seeds), new ImagePlus("mask", mask), connectivity.getNativeValue2D(), getDams).getProcessor();
            }, progressInfo);
            resultImage.copyScale(inputImage);
            dataBatch.addOutputData(getFirstOutputSlot(), new ImagePlus3DGreyscaleData(resultImage), progressInfo);
        } else {
            ImagePlus mask = ImageJUtils2.getMaskFromMaskOrROI(targetArea, dataBatch, "Input", progressInfo);
            ImagePlus resultImage = Watershed.computeWatershed(inputImage, seedImage, mask, inputImage.getStackSize() == 1 ? connectivity.getNativeValue2D() : connectivity.getNativeValue3D(), getDams);
            resultImage.copyScale(inputImage);
            dataBatch.addOutputData(getFirstOutputSlot(), new ImagePlus3DGreyscaleData(resultImage), progressInfo);
        }
    }

    @JIPipeDocumentation(name = "Get dams", description = "If enabled, the dams instead of the wells are returned")
    @JIPipeParameter("get-dams")
    public boolean isGetDams() {
        return getDams;
    }

    @JIPipeParameter("get-dams")
    public void setGetDams(boolean getDams) {
        this.getDams = getDams;
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
