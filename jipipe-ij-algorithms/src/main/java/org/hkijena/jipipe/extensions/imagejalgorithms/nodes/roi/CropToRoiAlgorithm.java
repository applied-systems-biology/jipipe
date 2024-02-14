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

package org.hkijena.jipipe.extensions.imagejalgorithms.nodes.roi;

import ij.ImagePlus;
import ij.gui.Roi;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageSliceIndex;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

@JIPipeDocumentation(name = "Crop to ROI", description = "Crops the incoming images to fit into the boundaries defined by the ROI.")
@JIPipeNode(menuPath = "Transform", nodeTypeCategory = ImagesNodeTypeCategory.class)
@JIPipeInputSlot(value = ImagePlusData.class, slotName = "Image", autoCreate = true)
@JIPipeInputSlot(value = ROIListData.class, slotName = "ROI", autoCreate = true)
@JIPipeOutputSlot(value = ImagePlusData.class, slotName = "Cropped", autoCreate = true)
public class CropToRoiAlgorithm extends JIPipeIteratingAlgorithm {

    private boolean cropXY = true;
    private boolean cropZ = true;
    private boolean cropC = true;
    private boolean cropT = true;

    public CropToRoiAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public CropToRoiAlgorithm(CropToRoiAlgorithm other) {
        super(other);
        this.cropXY = other.cropXY;
        this.cropZ = other.cropZ;
        this.cropC = other.cropC;
        this.cropT = other.cropT;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImagePlus input = iterationStep.getInputData("Image", ImagePlusData.class, progressInfo).getImage();
        ROIListData rois = iterationStep.getInputData("ROI", ROIListData.class, progressInfo);
        Rectangle bounds = rois.getBounds();

        if (input.getStackSize() <= 1) {
            if (cropXY) {
                ImageProcessor ip = input.getProcessor();
                ip.setRoi(bounds);
                ImageProcessor cropped = ip.crop();
                ip.resetRoi();
                ImagePlus resultImage = new ImagePlus("Cropped " + bounds, cropped);
                resultImage.copyScale(input);
                iterationStep.addOutputData(getFirstOutputSlot(), new ImagePlusData(resultImage), progressInfo);
            } else {
                iterationStep.addOutputData(getFirstOutputSlot(), new ImagePlusData(input), progressInfo);
            }
            return;
        }

        int minZ = Integer.MAX_VALUE;
        int maxZ = 0;
        int minC = Integer.MAX_VALUE;
        int maxC = 0;
        int minT = Integer.MAX_VALUE;
        int maxT = 0;

        for (Roi roi : rois) {
            if (cropZ && roi.getZPosition() > 0) {
                minZ = Math.min(minZ, roi.getZPosition());
                maxZ = Math.max(maxZ, roi.getZPosition());
            }
            if (cropC && roi.getCPosition() > 0) {
                minC = Math.min(minC, roi.getCPosition());
                maxC = Math.max(maxC, roi.getCPosition());
            }
            if (cropT && roi.getTPosition() > 0) {
                minT = Math.min(minT, roi.getTPosition());
                maxT = Math.max(maxT, roi.getTPosition());
            }
        }

        if (!cropZ || minZ == Integer.MAX_VALUE)
            minZ = 1;
        if (!cropZ || maxZ == 0)
            maxZ = input.getNSlices();
        if (!cropC || minC == Integer.MAX_VALUE)
            minC = 1;
        if (!cropC || maxC == 0)
            maxC = input.getNChannels();
        if (!cropT || minT == Integer.MAX_VALUE)
            minT = 1;
        if (!cropT || maxT == 0)
            maxT = input.getNFrames();

//        int targetWidth = input.getWidth();
//        int targetHeight = input.getHeight();
//        if (cropXY) {
//            ImageProcessor imp = input.getProcessor();
//            imp.setRoi(bounds);
//            ImageProcessor cropped = imp.crop();
//            imp.setRoi((Roi) null);
//            targetWidth = cropped.getWidth();
//            targetHeight = cropped.getHeight();
//        }

        Map<ImageSliceIndex, ImageProcessor> sliceMap = new HashMap<>();
        int finalMinZ = minZ;
        int finalMaxZ = maxZ;
        int finalMinC = minC;
        int finalMaxC = maxC;
        int finalMinT = minT;
        int finalMaxT = maxT;
        ImageJUtils.forEachIndexedZCTSlice(input, (imp, index) -> {
            int z = index.getZ() + 1;
            int c = index.getC() + 1;
            int t = index.getT() + 1;
            if (z >= finalMinZ && z <= finalMaxZ && c >= finalMinC && c <= finalMaxC && t >= finalMinT && t <= finalMaxT) {
                imp.setRoi(bounds);
                ImageProcessor cropped = imp.crop();
                imp.setRoi((Roi) null);
                sliceMap.put(index, cropped);
            }
        }, progressInfo);
        ImagePlus cropped = ImageJUtils.combineSlices(sliceMap);
        cropped.setTitle("cropped");
        cropped.copyAttributes(input);
        iterationStep.addOutputData(getFirstOutputSlot(), new ImagePlusData(cropped), progressInfo);
    }

    @JIPipeDocumentation(name = "Crop XY plane", description = "If enabled, images are cropped according to the boundaries in the XY plane.")
    @JIPipeParameter("crop-xy")
    public boolean isCropXY() {
        return cropXY;
    }

    @JIPipeParameter("crop-xy")
    public void setCropXY(boolean cropXY) {
        this.cropXY = cropXY;
    }

    @JIPipeDocumentation(name = "Crop Z plane", description = "If enabled, images are cropped in the Z plane.")
    @JIPipeParameter("crop-z")
    public boolean isCropZ() {
        return cropZ;
    }

    @JIPipeParameter("crop-z")
    public void setCropZ(boolean cropZ) {
        this.cropZ = cropZ;
    }

    @JIPipeDocumentation(name = "Crop channel plane", description = "If enabled, images are cropped in the channel plane.")
    @JIPipeParameter("crop-c")
    public boolean isCropC() {
        return cropC;
    }

    @JIPipeParameter("crop-c")
    public void setCropC(boolean cropC) {
        this.cropC = cropC;
    }

    @JIPipeDocumentation(name = "Crop frame plane", description = "If enabled, images are cropped in the time plane.")
    @JIPipeParameter("crop-t")
    public boolean isCropT() {
        return cropT;
    }

    @JIPipeParameter("crop-t")
    public void setCropT(boolean cropT) {
        this.cropT = cropT;
    }
}
