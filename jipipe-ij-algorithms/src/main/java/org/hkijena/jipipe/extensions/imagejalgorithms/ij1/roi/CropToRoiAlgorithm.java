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

package org.hkijena.jipipe.extensions.imagejalgorithms.ij1.roi;

import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.Roi;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeRunnableInfo;
import org.hkijena.jipipe.api.nodes.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejalgorithms.utils.ImageJUtils;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;

import java.awt.Rectangle;

@JIPipeDocumentation(name = "Crop to ROI", description = "Crops the incoming images to fit into the boundaries defined by the ROI.")
@JIPipeOrganization(menuPath = "Transform", nodeTypeCategory = ImagesNodeTypeCategory.class)
@JIPipeInputSlot(value = ImagePlusData.class, slotName = "Image", autoCreate = true)
@JIPipeInputSlot(value = ROIListData.class, slotName = "ROI", autoCreate = true)
@JIPipeOutputSlot(value = ImagePlusData.class, slotName = "Cropped", inheritedSlot = "Image", autoCreate = true)
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
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeRunnableInfo progress) {
        ImagePlus input = dataBatch.getInputData("Image", ImagePlusData.class).getImage();
        ROIListData rois = dataBatch.getInputData("ROI", ROIListData.class);
        Rectangle bounds = rois.getBounds();

        if (input.getStackSize() <= 1) {
            if (cropXY) {
                ImageProcessor ip = input.getProcessor();
                ip.setRoi(bounds);
                ImageProcessor cropped = ip.crop();
                ip.resetRoi();
                dataBatch.addOutputData(getFirstOutputSlot(), new ImagePlusData(new ImagePlus("Cropped " + bounds, cropped)));
            } else {
                dataBatch.addOutputData(getFirstOutputSlot(), new ImagePlusData(input));
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

        int targetWidth = input.getWidth();
        int targetHeight = input.getHeight();
        if (cropXY) {
            input.getProcessor().setRoi(bounds);
            ImageProcessor cropped = input.getProcessor().crop();
            input.getProcessor().setRoi((Roi) null);
            targetWidth = cropped.getWidth();
            targetHeight = cropped.getHeight();
        }

        ImageStack stack = new ImageStack(targetWidth, targetHeight, input.getProcessor().getColorModel());
        int finalMinZ = minZ;
        int finalMaxZ = maxZ;
        int finalMinC = minC;
        int finalMaxC = maxC;
        int finalMinT = minT;
        int finalMaxT = maxT;
        ImageJUtils.forEachIndexedSlice(input, (imp, index) -> {
            int[] pos = input.convertIndexToPosition(index + 1);
            int z = pos[0];
            int c = pos[1];
            int t = pos[2];
            if (z >= finalMinZ && z <= finalMaxZ && c >= finalMinC && c <= finalMaxC && t >= finalMinT && t <= finalMaxT) {
                imp.setRoi(bounds);
                ImageProcessor cropped = imp.crop();
                imp.setRoi((Roi) null);
                stack.addSlice(cropped);
            }
        });
        dataBatch.addOutputData(getFirstOutputSlot(), new ImagePlusData(new ImagePlus("Cropped", stack)));
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
