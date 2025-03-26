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

package org.hkijena.jipipe.plugins.imagejalgorithms.nodes.roi;

import ij.ImagePlus;
import ij.gui.Roi;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ROI2DListData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJIterationUtils;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.dimensions.ImageSliceIndex;
import org.hkijena.jipipe.plugins.parameters.library.primitives.optional.OptionalTextAnnotationNameParameter;
import org.hkijena.jipipe.utils.json.JsonUtils;

import java.awt.*;
import java.util.*;
import java.util.List;

@SetJIPipeDocumentation(name = "Crop image to 2D ROI", description = "Crops the incoming images to fit into the boundaries defined by the ROI. " +
        "Alternative: Extract to ROI")
@ConfigureJIPipeNode(menuPath = "Transform", nodeTypeCategory = ImagesNodeTypeCategory.class)
@AddJIPipeInputSlot(value = ImagePlusData.class, name = "Image", create = true)
@AddJIPipeInputSlot(value = ROI2DListData.class, name = "ROI", create = true)
@AddJIPipeOutputSlot(value = ImagePlusData.class, name = "Cropped", create = true)
public class CropToRoiAlgorithm extends JIPipeIteratingAlgorithm {

    private boolean cropXY = true;
    private boolean cropZ = true;
    private boolean cropC = true;
    private boolean cropT = true;
    private OptionalTextAnnotationNameParameter annotationX = new OptionalTextAnnotationNameParameter("X", false);
    private OptionalTextAnnotationNameParameter annotationY = new OptionalTextAnnotationNameParameter("Y", false);
    private OptionalTextAnnotationNameParameter annotationZ = new OptionalTextAnnotationNameParameter("Z", false);
    private OptionalTextAnnotationNameParameter annotationC = new OptionalTextAnnotationNameParameter("C", false);
    private OptionalTextAnnotationNameParameter annotationT = new OptionalTextAnnotationNameParameter("T", false);
    private OptionalTextAnnotationNameParameter annotationBoundingWidth = new OptionalTextAnnotationNameParameter("Width", false);
    private OptionalTextAnnotationNameParameter annotationBoundingHeight = new OptionalTextAnnotationNameParameter("Height", false);
    private JIPipeTextAnnotationMergeMode annotationMergeStrategy = JIPipeTextAnnotationMergeMode.OverwriteExisting;

    public CropToRoiAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public CropToRoiAlgorithm(CropToRoiAlgorithm other) {
        super(other);
        this.cropXY = other.cropXY;
        this.cropZ = other.cropZ;
        this.cropC = other.cropC;
        this.cropT = other.cropT;
        this.annotationX = new OptionalTextAnnotationNameParameter(other.annotationX);
        this.annotationY = new OptionalTextAnnotationNameParameter(other.annotationY);
        this.annotationZ = new OptionalTextAnnotationNameParameter(other.annotationZ);
        this.annotationC = new OptionalTextAnnotationNameParameter(other.annotationC);
        this.annotationT = new OptionalTextAnnotationNameParameter(other.annotationT);
        this.annotationBoundingWidth = new OptionalTextAnnotationNameParameter(other.annotationBoundingWidth);
        this.annotationBoundingHeight = new OptionalTextAnnotationNameParameter(other.annotationBoundingHeight);
        this.annotationMergeStrategy = other.annotationMergeStrategy;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImagePlus input = iterationStep.getInputData("Image", ImagePlusData.class, progressInfo).getImage();
        ROI2DListData rois = iterationStep.getInputData("ROI", ROI2DListData.class, progressInfo);
        Rectangle bounds = rois.getBounds();

        List<JIPipeTextAnnotation> annotations = new ArrayList<>();
        if (cropXY) {
            annotationX.addAnnotationIfEnabled(annotations, String.valueOf(bounds.x));
            annotationY.addAnnotationIfEnabled(annotations, String.valueOf(bounds.y));
            annotationBoundingWidth.addAnnotationIfEnabled(annotations, String.valueOf(bounds.width));
            annotationBoundingHeight.addAnnotationIfEnabled(annotations, String.valueOf(bounds.height));
        }

        if (input.getStackSize() <= 1) {
            if (cropXY) {
                ImageProcessor ip = input.getProcessor();
                ip.setRoi(bounds);
                ImageProcessor cropped = ip.crop();
                ip.resetRoi();
                ImagePlus resultImage = new ImagePlus("Cropped " + bounds, cropped);
                resultImage.copyScale(input);
                iterationStep.addOutputData(getFirstOutputSlot(), new ImagePlusData(resultImage), annotations, annotationMergeStrategy, progressInfo);
            } else {
                iterationStep.addOutputData(getFirstOutputSlot(), new ImagePlusData(input), annotations, annotationMergeStrategy, progressInfo);
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

        if (cropC) {
            annotationC.addAnnotationIfEnabled(annotations, JsonUtils.toJsonString(Arrays.asList(minC, maxC)));
        }
        if (cropZ) {
            annotationZ.addAnnotationIfEnabled(annotations, JsonUtils.toJsonString(Arrays.asList(minZ, maxZ)));
        }
        if (cropT) {
            annotationT.addAnnotationIfEnabled(annotations, JsonUtils.toJsonString(Arrays.asList(minT, maxT)));
        }

        Map<ImageSliceIndex, ImageProcessor> sliceMap = new HashMap<>();
        int finalMinZ = minZ;
        int finalMaxZ = maxZ;
        int finalMinC = minC;
        int finalMaxC = maxC;
        int finalMinT = minT;
        int finalMaxT = maxT;
        ImageJIterationUtils.forEachIndexedZCTSlice(input, (imp, index) -> {
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
        iterationStep.addOutputData(getFirstOutputSlot(), new ImagePlusData(cropped), annotations, annotationMergeStrategy, progressInfo);
    }

    @SetJIPipeDocumentation(name = "Crop XY plane", description = "If enabled, images are cropped according to the boundaries in the XY plane.")
    @JIPipeParameter("crop-xy")
    public boolean isCropXY() {
        return cropXY;
    }

    @JIPipeParameter("crop-xy")
    public void setCropXY(boolean cropXY) {
        this.cropXY = cropXY;
    }

    @SetJIPipeDocumentation(name = "Crop Z plane", description = "If enabled, images are cropped in the Z plane.")
    @JIPipeParameter("crop-z")
    public boolean isCropZ() {
        return cropZ;
    }

    @JIPipeParameter("crop-z")
    public void setCropZ(boolean cropZ) {
        this.cropZ = cropZ;
    }

    @SetJIPipeDocumentation(name = "Crop channel plane", description = "If enabled, images are cropped in the channel plane.")
    @JIPipeParameter("crop-c")
    public boolean isCropC() {
        return cropC;
    }

    @JIPipeParameter("crop-c")
    public void setCropC(boolean cropC) {
        this.cropC = cropC;
    }

    @SetJIPipeDocumentation(name = "Crop frame plane", description = "If enabled, images are cropped in the time plane.")
    @JIPipeParameter("crop-t")
    public boolean isCropT() {
        return cropT;
    }

    @JIPipeParameter("crop-t")
    public void setCropT(boolean cropT) {
        this.cropT = cropT;
    }

    @SetJIPipeDocumentation(name = "Annotate with X location", description = "If enabled, the generated image is annotated with the top-left X coordinate of the ROI bounding box.")
    @JIPipeParameter(value = "annotation-x", uiOrder = -50)
    public OptionalTextAnnotationNameParameter getAnnotationX() {
        return annotationX;
    }

    @JIPipeParameter("annotation-x")
    public void setAnnotationX(OptionalTextAnnotationNameParameter annotationX) {
        this.annotationX = annotationX;
    }

    @SetJIPipeDocumentation(name = "Annotate with Y location", description = "If enabled, the generated image is annotated with the top-left Y coordinate of the ROI bounding box.")
    @JIPipeParameter(value = "annotation-y", uiOrder = -45)
    public OptionalTextAnnotationNameParameter getAnnotationY() {
        return annotationY;
    }

    @JIPipeParameter("annotation-y")
    public void setAnnotationY(OptionalTextAnnotationNameParameter annotationY) {
        this.annotationY = annotationY;
    }

    @SetJIPipeDocumentation(name = "Annotate with Z location", description = "If enabled, the generated image is annotated with the Z slice of the ROI. The first index is 1. A value of zero indicates that the ROI is located on all Z slices.")
    @JIPipeParameter("annotation-z")
    public OptionalTextAnnotationNameParameter getAnnotationZ() {
        return annotationZ;
    }

    @JIPipeParameter("annotation-z")
    public void setAnnotationZ(OptionalTextAnnotationNameParameter annotationZ) {
        this.annotationZ = annotationZ;
    }

    @SetJIPipeDocumentation(name = "Annotate with C location", description = "If enabled, the generated image is annotated with the channel slice of the ROI. The first index is 1. A value of zero indicates that the ROI is located on all C slices.")
    @JIPipeParameter("annotation-c")
    public OptionalTextAnnotationNameParameter getAnnotationC() {
        return annotationC;
    }

    @JIPipeParameter("annotation-c")
    public void setAnnotationC(OptionalTextAnnotationNameParameter annotationC) {
        this.annotationC = annotationC;
    }

    @SetJIPipeDocumentation(name = "Annotate with T location", description = "If enabled, the generated image is annotated with the frame slice of the ROI. The first index is 1. A value of zero indicates that the ROI is located on all T slices.")
    @JIPipeParameter("annotation-t")
    public OptionalTextAnnotationNameParameter getAnnotationT() {
        return annotationT;
    }

    @JIPipeParameter("annotation-t")
    public void setAnnotationT(OptionalTextAnnotationNameParameter annotationT) {
        this.annotationT = annotationT;
    }

    @SetJIPipeDocumentation(name = "Annotate with ROI width", description = "If enabled, the generated image is annotated with the width of the ROI")
    @JIPipeParameter("annotation-width")
    public OptionalTextAnnotationNameParameter getAnnotationBoundingWidth() {
        return annotationBoundingWidth;
    }

    @JIPipeParameter("annotation-width")
    public void setAnnotationBoundingWidth(OptionalTextAnnotationNameParameter annotationBoundingWidth) {
        this.annotationBoundingWidth = annotationBoundingWidth;
    }

    @SetJIPipeDocumentation(name = "Annotate with ROI height", description = "If enabled, the generated image is annotated with the height of the ROI")
    @JIPipeParameter("annotation-height")
    public OptionalTextAnnotationNameParameter getAnnotationBoundingHeight() {
        return annotationBoundingHeight;
    }

    @JIPipeParameter("annotation-height")
    public void setAnnotationBoundingHeight(OptionalTextAnnotationNameParameter annotationBoundingHeight) {
        this.annotationBoundingHeight = annotationBoundingHeight;
    }

    @SetJIPipeDocumentation(name = "Annotation merging", description = "Determines how generated annotations are merged with existing annotations")
    @JIPipeParameter("annotation-merging")
    public JIPipeTextAnnotationMergeMode getAnnotationMergeStrategy() {
        return annotationMergeStrategy;
    }

    @JIPipeParameter("annotation-merging")
    public void setAnnotationMergeStrategy(JIPipeTextAnnotationMergeMode annotationMergeStrategy) {
        this.annotationMergeStrategy = annotationMergeStrategy;
    }
}
