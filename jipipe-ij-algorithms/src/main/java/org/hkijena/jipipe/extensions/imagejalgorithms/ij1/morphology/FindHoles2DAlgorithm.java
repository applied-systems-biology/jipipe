package org.hkijena.jipipe.extensions.imagejalgorithms.ij1.morphology;

import ij.ImagePlus;
import ij.gui.Roi;
import ij.plugin.filter.ThresholdToSelection;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleMaskData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;

@JIPipeDocumentation(name = "Find holes 2D", description = "Find holes by extracting the ROI located in the image and utilizing the fact that ROI cannot have holes. " + "If a multi-channel image is provided, the operation is applied to each channel. " +
        "If higher-dimensional data is provided, the filter is applied to each 2D slice.")
@JIPipeNode(menuPath = "Morphology", nodeTypeCategory = ImagesNodeTypeCategory.class)
@JIPipeInputSlot(value = ImagePlusGreyscaleMaskData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ImagePlusGreyscaleMaskData.class, slotName = "Output", autoCreate = true)
public class FindHoles2DAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private boolean blackBackground = true;

    public FindHoles2DAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public FindHoles2DAlgorithm(FindHoles2DAlgorithm other) {
        super(other);
        this.blackBackground = other.blackBackground;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ImagePlus img = dataBatch.getInputData(getFirstInputSlot(), ImagePlusGreyscaleMaskData.class, progressInfo).getDuplicateImage();
        // Convert mask to ROI
        ROIListData roiList = new ROIListData();
        ImageJUtils.forEachIndexedZCTSlice(img, (ip, index) -> {
            ImageProcessor ip2 = ip.duplicate();
            int threshold = ip2.isInvertedLut() ? 255 : 0;
            if (isBlackBackground())
                threshold = (threshold == 255) ? 0 : 255;
            ip2.setThreshold(threshold, threshold, ImageProcessor.NO_LUT_UPDATE);
            Roi roi = ThresholdToSelection.run(new ImagePlus("slice", ip2));
            if (roi != null) {
                roi.setPosition(index.getC() + 1, index.getZ() + 1, index.getT() + 1);
                roiList.add(roi);
            }
        }, progressInfo);

        // Split into their simple ROI
        roiList.splitAll();

        // Create a mask
        ImagePlus roiMask = roiList.toMask(img, true, false, 1);

        // Subtract mask from roi mask
        ImageJUtils.forEachIndexedZCTSlice(img, (ip, index) -> {
            ImageProcessor ip2 = ImageJUtils.getSliceZero(roiMask, index);
            byte[] roiMaskPixels = (byte[]) ip2.getPixels();
            byte[] maskPixels = (byte[]) ip.getPixels();
            for (int i = 0; i < roiMaskPixels.length; i++) {
                maskPixels[i] = (byte) (Math.max(0, Byte.toUnsignedInt(roiMaskPixels[i]) - Byte.toUnsignedInt(maskPixels[i])));
            }
        }, progressInfo);

        dataBatch.addOutputData(getFirstOutputSlot(), new ImagePlusGreyscaleMaskData(img), progressInfo);
    }

    @JIPipeDocumentation(name = "Black background", description = "If enabled, the background is assumed to be black. Otherwise, white pixels are recognized as background.")
    @JIPipeParameter("black-background")
    public boolean isBlackBackground() {
        return blackBackground;
    }

    @JIPipeParameter("black-background")
    public void setBlackBackground(boolean blackBackground) {
        this.blackBackground = blackBackground;
    }
}
