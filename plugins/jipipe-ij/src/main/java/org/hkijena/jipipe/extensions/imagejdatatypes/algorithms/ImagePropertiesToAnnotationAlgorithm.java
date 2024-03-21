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

package org.hkijena.jipipe.extensions.imagejdatatypes.algorithms;

import ij.ImagePlus;
import ij.measure.Calibration;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.AnnotationsNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.parameters.library.primitives.optional.OptionalTextAnnotationNameParameter;

import java.util.ArrayList;
import java.util.List;

/**
 * Adds data annotations that contain the image properties
 */
@SetJIPipeDocumentation(name = "Annotate with image properties", description = "Adds data annotations that contain the image properties.")
@ConfigureJIPipeNode(nodeTypeCategory = AnnotationsNodeTypeCategory.class, menuPath = "For images")
@AddJIPipeInputSlot(value = ImagePlusData.class, slotName = "Image", create = true)
@AddJIPipeOutputSlot(value = ImagePlusData.class, slotName = "Annotated image", create = true)
public class ImagePropertiesToAnnotationAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private OptionalTextAnnotationNameParameter titleAnnotation = new OptionalTextAnnotationNameParameter();
    private OptionalTextAnnotationNameParameter widthAnnotation = new OptionalTextAnnotationNameParameter();
    private OptionalTextAnnotationNameParameter heightAnnotation = new OptionalTextAnnotationNameParameter();
    private OptionalTextAnnotationNameParameter stackSizeAnnotation = new OptionalTextAnnotationNameParameter();
    private OptionalTextAnnotationNameParameter planeNumberAnnotation = new OptionalTextAnnotationNameParameter();
    private OptionalTextAnnotationNameParameter channelSizeAnnotation = new OptionalTextAnnotationNameParameter();
    private OptionalTextAnnotationNameParameter framesSizeAnnotation = new OptionalTextAnnotationNameParameter();
    private OptionalTextAnnotationNameParameter imageTypeAnnotation = new OptionalTextAnnotationNameParameter();
    private OptionalTextAnnotationNameParameter bitDepthAnnotation = new OptionalTextAnnotationNameParameter();
    private OptionalTextAnnotationNameParameter colorSpaceAnnotation = new OptionalTextAnnotationNameParameter();
    private OptionalTextAnnotationNameParameter physicalDimensionXAnnotation = new OptionalTextAnnotationNameParameter();
    private OptionalTextAnnotationNameParameter physicalDimensionYAnnotation = new OptionalTextAnnotationNameParameter();
    private OptionalTextAnnotationNameParameter physicalDimensionZAnnotation = new OptionalTextAnnotationNameParameter();
    private OptionalTextAnnotationNameParameter physicalDimensionTAnnotation = new OptionalTextAnnotationNameParameter();
    private OptionalTextAnnotationNameParameter physicalDimensionValueAnnotation = new OptionalTextAnnotationNameParameter();

    /**
     * Creates a new instance
     *
     * @param info algorithm info
     */
    public ImagePropertiesToAnnotationAlgorithm(JIPipeNodeInfo info) {
        super(info);
        titleAnnotation.setContent("Image title");
        widthAnnotation.setContent("Image width");
        heightAnnotation.setContent("Image height");
        stackSizeAnnotation.setContent("Image Z slices");
        planeNumberAnnotation.setContent("Image planes");
        channelSizeAnnotation.setContent("Image composite channel count");
        framesSizeAnnotation.setContent("Image frame count");
        imageTypeAnnotation.setContent("Image type");
        bitDepthAnnotation.setContent("Image bit depth");
        colorSpaceAnnotation.setContent("Color space");
        physicalDimensionXAnnotation.setContent("Physical dimension (X)");
        physicalDimensionYAnnotation.setContent("Physical dimension (Y)");
        physicalDimensionZAnnotation.setContent("Physical dimension (Z)");
        physicalDimensionTAnnotation.setContent("Physical dimension (Time)");
        physicalDimensionValueAnnotation.setContent("Physical dimension (Value)");
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public ImagePropertiesToAnnotationAlgorithm(ImagePropertiesToAnnotationAlgorithm other) {
        super(other);
        this.titleAnnotation = new OptionalTextAnnotationNameParameter(other.titleAnnotation);
        this.widthAnnotation = new OptionalTextAnnotationNameParameter(other.widthAnnotation);
        this.heightAnnotation = new OptionalTextAnnotationNameParameter(other.heightAnnotation);
        this.stackSizeAnnotation = new OptionalTextAnnotationNameParameter(other.stackSizeAnnotation);
        this.planeNumberAnnotation = new OptionalTextAnnotationNameParameter(other.planeNumberAnnotation);
        this.channelSizeAnnotation = new OptionalTextAnnotationNameParameter(other.channelSizeAnnotation);
        this.framesSizeAnnotation = new OptionalTextAnnotationNameParameter(other.framesSizeAnnotation);
        this.imageTypeAnnotation = new OptionalTextAnnotationNameParameter(other.imageTypeAnnotation);
        this.bitDepthAnnotation = new OptionalTextAnnotationNameParameter(other.bitDepthAnnotation);
        this.colorSpaceAnnotation = new OptionalTextAnnotationNameParameter(other.colorSpaceAnnotation);
        this.physicalDimensionXAnnotation = new OptionalTextAnnotationNameParameter(other.physicalDimensionXAnnotation);
        this.physicalDimensionYAnnotation = new OptionalTextAnnotationNameParameter(other.physicalDimensionYAnnotation);
        this.physicalDimensionZAnnotation = new OptionalTextAnnotationNameParameter(other.physicalDimensionZAnnotation);
        this.physicalDimensionTAnnotation = new OptionalTextAnnotationNameParameter(other.physicalDimensionTAnnotation);
        this.physicalDimensionValueAnnotation = new OptionalTextAnnotationNameParameter(other.physicalDimensionValueAnnotation);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImagePlusData inputData = iterationStep.getInputData(getFirstInputSlot(), ImagePlusData.class, progressInfo);
        List<JIPipeTextAnnotation> annotations = new ArrayList<>();

        if (getTitleAnnotation().isEnabled()) {
            annotations.add(new JIPipeTextAnnotation(getTitleAnnotation().getContent(), "" + inputData.getImage().getTitle()));
        }
        if (getWidthAnnotation().isEnabled()) {
            annotations.add(new JIPipeTextAnnotation(getWidthAnnotation().getContent(), "" + inputData.getImage().getWidth()));
        }
        if (getHeightAnnotation().isEnabled()) {
            annotations.add(new JIPipeTextAnnotation(getHeightAnnotation().getContent(), "" + inputData.getImage().getHeight()));
        }
        if (getStackSizeAnnotation().isEnabled()) {
            annotations.add(new JIPipeTextAnnotation(getStackSizeAnnotation().getContent(), "" + inputData.getImage().getNSlices()));
        }
        if (getPlaneNumberAnnotation().isEnabled()) {
            annotations.add(new JIPipeTextAnnotation(getPlaneNumberAnnotation().getContent(), "" + inputData.getImage().getStackSize()));
        }
        if (getChannelSizeAnnotation().isEnabled()) {
            annotations.add(new JIPipeTextAnnotation(getChannelSizeAnnotation().getContent(), "" + inputData.getImage().getNChannels()));
        }
        if (getFramesSizeAnnotation().isEnabled()) {
            annotations.add(new JIPipeTextAnnotation(getFramesSizeAnnotation().getContent(), "" + inputData.getImage().getNFrames()));
        }
        if (physicalDimensionXAnnotation.isEnabled()) {
            Calibration calibration = inputData.getImage().getCalibration();
            double value = 0;
            String unit = "";
            if (calibration != null) {
                value = calibration.getX(1);
                unit = calibration.getXUnit();
            }
            physicalDimensionXAnnotation.addAnnotationIfEnabled(annotations, value + (!unit.isEmpty() ? " " + unit : ""));
        }
        if (physicalDimensionYAnnotation.isEnabled()) {
            Calibration calibration = inputData.getImage().getCalibration();
            double value = 0;
            String unit = "";
            if (calibration != null) {
                value = calibration.getY(1);
                unit = calibration.getYUnit();
            }
            physicalDimensionYAnnotation.addAnnotationIfEnabled(annotations, value + (!unit.isEmpty() ? " " + unit : ""));
        }
        if (physicalDimensionZAnnotation.isEnabled()) {
            Calibration calibration = inputData.getImage().getCalibration();
            double value = 0;
            String unit = "";
            if (calibration != null) {
                value = calibration.getZ(1);
                unit = calibration.getZUnit();
            }
            physicalDimensionZAnnotation.addAnnotationIfEnabled(annotations, value + (!unit.isEmpty() ? " " + unit : ""));
        }
        if (physicalDimensionTAnnotation.isEnabled()) {
            Calibration calibration = inputData.getImage().getCalibration();
            double value = 1;
            String unit = "";
            if (calibration != null) {
                unit = calibration.getTimeUnit();
            }
            physicalDimensionTAnnotation.addAnnotationIfEnabled(annotations, value + (!unit.isEmpty() ? " " + unit : ""));
        }
        if (physicalDimensionValueAnnotation.isEnabled()) {
            Calibration calibration = inputData.getImage().getCalibration();
            double value = 1;
            String unit = "";
            if (calibration != null) {
                unit = calibration.getValueUnit();
            }
            physicalDimensionValueAnnotation.addAnnotationIfEnabled(annotations, value + (!unit.isEmpty() ? " " + unit : ""));
        }
        if (getImageTypeAnnotation().isEnabled()) {
            String type;
            switch (inputData.getImage().getType()) {
                case ImagePlus.GRAY8:
                    type = "GRAY8";
                    break;
                case ImagePlus.GRAY16:
                    type = "GRAY16";
                    break;
                case ImagePlus.COLOR_RGB:
                    type = "COLOR_RGB";
                    break;
                case ImagePlus.COLOR_256:
                    type = "COLOR_256";
                    break;
                case ImagePlus.GRAY32:
                    type = "GRAY32";
                    break;
                default:
                    type = "UNKNOWN";
                    break;
            }
            annotations.add(new JIPipeTextAnnotation(getImageTypeAnnotation().getContent(), type));
        }
        if (getBitDepthAnnotation().isEnabled()) {
            annotations.add(new JIPipeTextAnnotation(getBitDepthAnnotation().getContent(), "" + inputData.getImage().getBitDepth()));
        }
        if (getColorSpaceAnnotation().isEnabled()) {
            String colorSpace = inputData.getColorSpace().toString();
            annotations.add(new JIPipeTextAnnotation(getColorSpaceAnnotation().getContent(), colorSpace));
        }

        iterationStep.addOutputData(getFirstOutputSlot(), inputData, annotations, JIPipeTextAnnotationMergeMode.Merge, progressInfo);
    }

    @SetJIPipeDocumentation(name = "Annotate with physical dimension (X)", description = "If enabled, the physical size of one pixel (including the unit) is annotated to the image.")
    @JIPipeParameter("physical-dimension-x-annotation")
    public OptionalTextAnnotationNameParameter getPhysicalDimensionXAnnotation() {
        return physicalDimensionXAnnotation;
    }

    @JIPipeParameter("physical-dimension-x-annotation")
    public void setPhysicalDimensionXAnnotation(OptionalTextAnnotationNameParameter physicalDimensionXAnnotation) {
        this.physicalDimensionXAnnotation = physicalDimensionXAnnotation;
    }

    @SetJIPipeDocumentation(name = "Annotate with physical dimension (Y)", description = "If enabled, the physical size of one pixel (including the unit) is annotated to the image.")
    @JIPipeParameter("physical-dimension-y-annotation")
    public OptionalTextAnnotationNameParameter getPhysicalDimensionYAnnotation() {
        return physicalDimensionYAnnotation;
    }

    @JIPipeParameter("physical-dimension-y-annotation")

    public void setPhysicalDimensionYAnnotation(OptionalTextAnnotationNameParameter physicalDimensionYAnnotation) {
        this.physicalDimensionYAnnotation = physicalDimensionYAnnotation;
    }

    @SetJIPipeDocumentation(name = "Annotate with physical dimension (Z)", description = "If enabled, the physical size of one pixel (including the unit) is annotated to the image.")
    @JIPipeParameter("physical-dimension-z-annotation")
    public OptionalTextAnnotationNameParameter getPhysicalDimensionZAnnotation() {
        return physicalDimensionZAnnotation;
    }

    @JIPipeParameter("physical-dimension-z-annotation")
    public void setPhysicalDimensionZAnnotation(OptionalTextAnnotationNameParameter physicalDimensionZAnnotation) {
        this.physicalDimensionZAnnotation = physicalDimensionZAnnotation;
    }

    @SetJIPipeDocumentation(name = "Annotate with physical dimension (Time)", description = "If enabled, the time unit for multi-frame images is annotated to the image.")
    @JIPipeParameter("physical-dimension-t-annotation")
    public OptionalTextAnnotationNameParameter getPhysicalDimensionTAnnotation() {
        return physicalDimensionTAnnotation;
    }

    @JIPipeParameter("physical-dimension-t-annotation")
    public void setPhysicalDimensionTAnnotation(OptionalTextAnnotationNameParameter physicalDimensionTAnnotation) {
        this.physicalDimensionTAnnotation = physicalDimensionTAnnotation;
    }

    @SetJIPipeDocumentation(name = "Annotate with physical dimension (Value)", description = "If enabled, the physical size of greyscale pixel values is annotated to the image.")
    @JIPipeParameter("physical-dimension-value-annotation")
    public OptionalTextAnnotationNameParameter getPhysicalDimensionValueAnnotation() {
        return physicalDimensionValueAnnotation;
    }

    @JIPipeParameter("physical-dimension-value-annotation")
    public void setPhysicalDimensionValueAnnotation(OptionalTextAnnotationNameParameter physicalDimensionValueAnnotation) {
        this.physicalDimensionValueAnnotation = physicalDimensionValueAnnotation;
    }

    @SetJIPipeDocumentation(name = "Annotate with title", description = "If enabled, an annotation with provided name is created. The annotation " +
            "contains the ImageJ image name.")
    @JIPipeParameter("title-annotation")
    public OptionalTextAnnotationNameParameter getTitleAnnotation() {
        return titleAnnotation;
    }

    @JIPipeParameter("title-annotation")
    public void setTitleAnnotation(OptionalTextAnnotationNameParameter titleAnnotation) {
        this.titleAnnotation = titleAnnotation;
    }

    @SetJIPipeDocumentation(name = "Annotate with image width", description = "If enabled, an annotation with provided name is created. The annotation " +
            "contains image width.")
    @JIPipeParameter(value = "width-annotation")
    public OptionalTextAnnotationNameParameter getWidthAnnotation() {
        return widthAnnotation;
    }

    @JIPipeParameter("width-annotation")
    public void setWidthAnnotation(OptionalTextAnnotationNameParameter widthAnnotation) {
        this.widthAnnotation = widthAnnotation;
    }

    @SetJIPipeDocumentation(name = "Annotate with image height", description = "If enabled, an annotation with provided name is created. The annotation " +
            "contains image height.")
    @JIPipeParameter("height-annotation")
    public OptionalTextAnnotationNameParameter getHeightAnnotation() {
        return heightAnnotation;
    }

    @JIPipeParameter("height-annotation")
    public void setHeightAnnotation(OptionalTextAnnotationNameParameter heightAnnotation) {
        this.heightAnnotation = heightAnnotation;
    }

    @SetJIPipeDocumentation(name = "Annotate with stack size (Z)", description = "If enabled, an annotation with provided name is created. The annotation " +
            "contains the image stack size (number of Z slices).")
    @JIPipeParameter(value = "stack-size-annotation", uiOrder = 100)
    public OptionalTextAnnotationNameParameter getStackSizeAnnotation() {
        return stackSizeAnnotation;
    }

    @JIPipeParameter("stack-size-annotation")
    public void setStackSizeAnnotation(OptionalTextAnnotationNameParameter stackSizeAnnotation) {
        this.stackSizeAnnotation = stackSizeAnnotation;
    }

    @SetJIPipeDocumentation(name = "Annotate with channel size (C)", description = "If enabled, an annotation with provided name is created. The annotation " +
            "contains the composite image channel size (number of C slices). Please note that is is different from the pixel channels like RGB.")
    @JIPipeParameter(value = "channel-size-annotation", uiOrder = 101)
    public OptionalTextAnnotationNameParameter getChannelSizeAnnotation() {
        return channelSizeAnnotation;
    }

    @JIPipeParameter("channel-size-annotation")
    public void setChannelSizeAnnotation(OptionalTextAnnotationNameParameter channelSizeAnnotation) {
        this.channelSizeAnnotation = channelSizeAnnotation;
    }

    @SetJIPipeDocumentation(name = "Annotate with number of frames (T)", description = "If enabled, an annotation with provided name is created. The annotation " +
            "contains the number of frames (number of T slices).")
    @JIPipeParameter(value = "frames-size-annotation", uiOrder = 102)
    public OptionalTextAnnotationNameParameter getFramesSizeAnnotation() {
        return framesSizeAnnotation;
    }

    @JIPipeParameter("frames-size-annotation")
    public void setFramesSizeAnnotation(OptionalTextAnnotationNameParameter framesSizeAnnotation) {
        this.framesSizeAnnotation = framesSizeAnnotation;
    }

    @SetJIPipeDocumentation(name = "Annotate with image type", description = "If enabled, an annotation with provided name is created. The annotation " +
            "contains one of the following values: GRAY8, GRAY16, GRAY32, COLOR_256, COLOR_RGB, or UNKNOWN")
    @JIPipeParameter("image-type-annotation")
    public OptionalTextAnnotationNameParameter getImageTypeAnnotation() {
        return imageTypeAnnotation;
    }

    @JIPipeParameter("image-type-annotation")
    public void setImageTypeAnnotation(OptionalTextAnnotationNameParameter imageTypeAnnotation) {
        this.imageTypeAnnotation = imageTypeAnnotation;
    }

    @SetJIPipeDocumentation(name = "Annotate with bit depth", description = "If enabled, an annotation with provided name is created. The annotation " +
            "contains one of the following values: 0, 8, 16, 24 or 32")
    @JIPipeParameter("image-bit-depth-annotation")
    public OptionalTextAnnotationNameParameter getBitDepthAnnotation() {
        return bitDepthAnnotation;
    }

    @JIPipeParameter("image-bit-depth-annotation")
    public void setBitDepthAnnotation(OptionalTextAnnotationNameParameter bitDepthAnnotation) {
        this.bitDepthAnnotation = bitDepthAnnotation;
    }

    @SetJIPipeDocumentation(name = "Annotate with number of planes", description = "If enabled, an annotation with provided name is created. The annotation " +
            "contains the number of 2D image planes (also referred as 'Stack size'). Please note that this value might be different from the number " +
            "of Z slices. This number is size(Z) * size(C) * size(T).")
    @JIPipeParameter("image-plane-number-annotation")
    public OptionalTextAnnotationNameParameter getPlaneNumberAnnotation() {
        return planeNumberAnnotation;
    }

    @JIPipeParameter("image-plane-number-annotation")
    public void setPlaneNumberAnnotation(OptionalTextAnnotationNameParameter planeNumberAnnotation) {
        this.planeNumberAnnotation = planeNumberAnnotation;
    }

    @SetJIPipeDocumentation(name = "Annotate with color space", description = "If enabled, the color space is stored in an annotation. If a greyscale image is " +
            "provided, the color space will be 'Greyscale'.")
    @JIPipeParameter("color-space-annotation")
    public OptionalTextAnnotationNameParameter getColorSpaceAnnotation() {
        return colorSpaceAnnotation;
    }

    @JIPipeParameter("color-space-annotation")
    public void setColorSpaceAnnotation(OptionalTextAnnotationNameParameter colorSpaceAnnotation) {
        this.colorSpaceAnnotation = colorSpaceAnnotation;
    }
}
