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

package org.hkijena.jipipe.extensions.imagejdatatypes.algorithms;

import ij.ImagePlus;
import ij.measure.Calibration;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.AnnotationsNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntryCause;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.parameters.library.primitives.optional.OptionalAnnotationNameParameter;

import java.util.ArrayList;
import java.util.List;

/**
 * Adds data annotations that contain the image properties
 */
@JIPipeDocumentation(name = "Annotate with image properties", description = "Adds data annotations that contain the image properties.")
@JIPipeNode(nodeTypeCategory = AnnotationsNodeTypeCategory.class, menuPath = "For images")
@JIPipeInputSlot(value = ImagePlusData.class, slotName = "Image", autoCreate = true)
@JIPipeOutputSlot(value = ImagePlusData.class, slotName = "Annotated image", inheritedSlot = "Image", autoCreate = true)
public class ImagePropertiesToAnnotationAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private OptionalAnnotationNameParameter titleAnnotation = new OptionalAnnotationNameParameter();
    private OptionalAnnotationNameParameter widthAnnotation = new OptionalAnnotationNameParameter();
    private OptionalAnnotationNameParameter heightAnnotation = new OptionalAnnotationNameParameter();
    private OptionalAnnotationNameParameter stackSizeAnnotation = new OptionalAnnotationNameParameter();
    private OptionalAnnotationNameParameter planeNumberAnnotation = new OptionalAnnotationNameParameter();
    private OptionalAnnotationNameParameter channelSizeAnnotation = new OptionalAnnotationNameParameter();
    private OptionalAnnotationNameParameter framesSizeAnnotation = new OptionalAnnotationNameParameter();
    private OptionalAnnotationNameParameter imageTypeAnnotation = new OptionalAnnotationNameParameter();
    private OptionalAnnotationNameParameter bitDepthAnnotation = new OptionalAnnotationNameParameter();
    private OptionalAnnotationNameParameter colorSpaceAnnotation = new OptionalAnnotationNameParameter();
    private OptionalAnnotationNameParameter physicalDimensionXAnnotation = new OptionalAnnotationNameParameter();
    private OptionalAnnotationNameParameter physicalDimensionYAnnotation = new OptionalAnnotationNameParameter();
    private OptionalAnnotationNameParameter physicalDimensionZAnnotation = new OptionalAnnotationNameParameter();
    private OptionalAnnotationNameParameter physicalDimensionTAnnotation = new OptionalAnnotationNameParameter();
    private OptionalAnnotationNameParameter physicalDimensionValueAnnotation = new OptionalAnnotationNameParameter();

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
        this.titleAnnotation = new OptionalAnnotationNameParameter(other.titleAnnotation);
        this.widthAnnotation = new OptionalAnnotationNameParameter(other.widthAnnotation);
        this.heightAnnotation = new OptionalAnnotationNameParameter(other.heightAnnotation);
        this.stackSizeAnnotation = new OptionalAnnotationNameParameter(other.stackSizeAnnotation);
        this.planeNumberAnnotation = new OptionalAnnotationNameParameter(other.planeNumberAnnotation);
        this.channelSizeAnnotation = new OptionalAnnotationNameParameter(other.channelSizeAnnotation);
        this.framesSizeAnnotation = new OptionalAnnotationNameParameter(other.framesSizeAnnotation);
        this.imageTypeAnnotation = new OptionalAnnotationNameParameter(other.imageTypeAnnotation);
        this.bitDepthAnnotation = new OptionalAnnotationNameParameter(other.bitDepthAnnotation);
        this.colorSpaceAnnotation = new OptionalAnnotationNameParameter(other.colorSpaceAnnotation);
        this.physicalDimensionXAnnotation = new OptionalAnnotationNameParameter(other.physicalDimensionXAnnotation);
        this.physicalDimensionYAnnotation = new OptionalAnnotationNameParameter(other.physicalDimensionYAnnotation);
        this.physicalDimensionZAnnotation = new OptionalAnnotationNameParameter(other.physicalDimensionZAnnotation);
        this.physicalDimensionTAnnotation = new OptionalAnnotationNameParameter(other.physicalDimensionTAnnotation);
        this.physicalDimensionValueAnnotation = new OptionalAnnotationNameParameter(other.physicalDimensionValueAnnotation);
    }

    @Override
    public void reportValidity(JIPipeValidationReportEntryCause parentCause, JIPipeValidationReport report) {
        super.reportValidity(parentCause, report);
        if (getTitleAnnotation().isEnabled())
            report.resolve("Annotate with title").checkNonEmpty(getTitleAnnotation().getContent(), this);
        if (getWidthAnnotation().isEnabled())
            report.resolve("Annotate with image width").checkNonEmpty(getWidthAnnotation().getContent(), this);
        if (getHeightAnnotation().isEnabled())
            report.resolve("Annotate with image height").checkNonEmpty(getHeightAnnotation().getContent(), this);
        if (getStackSizeAnnotation().isEnabled())
            report.resolve("Annotate with stack size (Z)").checkNonEmpty(getStackSizeAnnotation().getContent(), this);
        if (getPlaneNumberAnnotation().isEnabled())
            report.resolve("Annotate with number of planes").checkNonEmpty(getPlaneNumberAnnotation().getContent(), this);
        if (getChannelSizeAnnotation().isEnabled())
            report.resolve("Annotate with channel size (C)").checkNonEmpty(getChannelSizeAnnotation().getContent(), this);
        if (getFramesSizeAnnotation().isEnabled())
            report.resolve("Annotate with number of frames (T)").checkNonEmpty(getFramesSizeAnnotation().getContent(), this);
        if (getImageTypeAnnotation().isEnabled())
            report.resolve("Annotate with image type").checkNonEmpty(getImageTypeAnnotation().getContent(), this);
        if (getBitDepthAnnotation().isEnabled())
            report.resolve("Annotate with bit depth").checkNonEmpty(getBitDepthAnnotation().getContent(), this);
        if (getBitDepthAnnotation().isEnabled())
            report.resolve("Annotate with color space").checkNonEmpty(getColorSpaceAnnotation().getContent(), this);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ImagePlusData inputData = dataBatch.getInputData(getFirstInputSlot(), ImagePlusData.class, progressInfo);
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

        dataBatch.addOutputData(getFirstOutputSlot(), inputData, annotations, JIPipeTextAnnotationMergeMode.Merge, progressInfo);
    }

    @JIPipeDocumentation(name = "Annotate with physical dimension (X)", description = "If enabled, the physical size of one pixel (including the unit) is annotated to the image.")
    @JIPipeParameter("physical-dimension-x-annotation")
    public OptionalAnnotationNameParameter getPhysicalDimensionXAnnotation() {
        return physicalDimensionXAnnotation;
    }

    @JIPipeParameter("physical-dimension-x-annotation")
    public void setPhysicalDimensionXAnnotation(OptionalAnnotationNameParameter physicalDimensionXAnnotation) {
        this.physicalDimensionXAnnotation = physicalDimensionXAnnotation;
    }

    @JIPipeDocumentation(name = "Annotate with physical dimension (Y)", description = "If enabled, the physical size of one pixel (including the unit) is annotated to the image.")
    @JIPipeParameter("physical-dimension-y-annotation")
    public OptionalAnnotationNameParameter getPhysicalDimensionYAnnotation() {
        return physicalDimensionYAnnotation;
    }

    @JIPipeParameter("physical-dimension-y-annotation")

    public void setPhysicalDimensionYAnnotation(OptionalAnnotationNameParameter physicalDimensionYAnnotation) {
        this.physicalDimensionYAnnotation = physicalDimensionYAnnotation;
    }

    @JIPipeDocumentation(name = "Annotate with physical dimension (Z)", description = "If enabled, the physical size of one pixel (including the unit) is annotated to the image.")
    @JIPipeParameter("physical-dimension-z-annotation")
    public OptionalAnnotationNameParameter getPhysicalDimensionZAnnotation() {
        return physicalDimensionZAnnotation;
    }

    @JIPipeParameter("physical-dimension-z-annotation")
    public void setPhysicalDimensionZAnnotation(OptionalAnnotationNameParameter physicalDimensionZAnnotation) {
        this.physicalDimensionZAnnotation = physicalDimensionZAnnotation;
    }

    @JIPipeDocumentation(name = "Annotate with physical dimension (Time)", description = "If enabled, the time unit for multi-frame images is annotated to the image.")
    @JIPipeParameter("physical-dimension-t-annotation")
    public OptionalAnnotationNameParameter getPhysicalDimensionTAnnotation() {
        return physicalDimensionTAnnotation;
    }

    @JIPipeParameter("physical-dimension-t-annotation")
    public void setPhysicalDimensionTAnnotation(OptionalAnnotationNameParameter physicalDimensionTAnnotation) {
        this.physicalDimensionTAnnotation = physicalDimensionTAnnotation;
    }

    @JIPipeDocumentation(name = "Annotate with physical dimension (Value)", description = "If enabled, the physical size of greyscale pixel values is annotated to the image.")
    @JIPipeParameter("physical-dimension-value-annotation")
    public OptionalAnnotationNameParameter getPhysicalDimensionValueAnnotation() {
        return physicalDimensionValueAnnotation;
    }

    @JIPipeParameter("physical-dimension-value-annotation")
    public void setPhysicalDimensionValueAnnotation(OptionalAnnotationNameParameter physicalDimensionValueAnnotation) {
        this.physicalDimensionValueAnnotation = physicalDimensionValueAnnotation;
    }

    @JIPipeDocumentation(name = "Annotate with title", description = "If enabled, an annotation with provided name is created. The annotation " +
            "contains the ImageJ image name.")
    @JIPipeParameter("title-annotation")
    public OptionalAnnotationNameParameter getTitleAnnotation() {
        return titleAnnotation;
    }

    @JIPipeParameter("title-annotation")
    public void setTitleAnnotation(OptionalAnnotationNameParameter titleAnnotation) {
        this.titleAnnotation = titleAnnotation;
    }

    @JIPipeDocumentation(name = "Annotate with image width", description = "If enabled, an annotation with provided name is created. The annotation " +
            "contains image width.")
    @JIPipeParameter(value = "width-annotation")
    public OptionalAnnotationNameParameter getWidthAnnotation() {
        return widthAnnotation;
    }

    @JIPipeParameter("width-annotation")
    public void setWidthAnnotation(OptionalAnnotationNameParameter widthAnnotation) {
        this.widthAnnotation = widthAnnotation;
    }

    @JIPipeDocumentation(name = "Annotate with image height", description = "If enabled, an annotation with provided name is created. The annotation " +
            "contains image height.")
    @JIPipeParameter("height-annotation")
    public OptionalAnnotationNameParameter getHeightAnnotation() {
        return heightAnnotation;
    }

    @JIPipeParameter("height-annotation")
    public void setHeightAnnotation(OptionalAnnotationNameParameter heightAnnotation) {
        this.heightAnnotation = heightAnnotation;
    }

    @JIPipeDocumentation(name = "Annotate with stack size (Z)", description = "If enabled, an annotation with provided name is created. The annotation " +
            "contains the image stack size (number of Z slices).")
    @JIPipeParameter(value = "stack-size-annotation", uiOrder = 100)
    public OptionalAnnotationNameParameter getStackSizeAnnotation() {
        return stackSizeAnnotation;
    }

    @JIPipeParameter("stack-size-annotation")
    public void setStackSizeAnnotation(OptionalAnnotationNameParameter stackSizeAnnotation) {
        this.stackSizeAnnotation = stackSizeAnnotation;
    }

    @JIPipeDocumentation(name = "Annotate with channel size (C)", description = "If enabled, an annotation with provided name is created. The annotation " +
            "contains the composite image channel size (number of C slices). Please note that is is different from the pixel channels like RGB.")
    @JIPipeParameter(value = "channel-size-annotation", uiOrder = 101)
    public OptionalAnnotationNameParameter getChannelSizeAnnotation() {
        return channelSizeAnnotation;
    }

    @JIPipeParameter("channel-size-annotation")
    public void setChannelSizeAnnotation(OptionalAnnotationNameParameter channelSizeAnnotation) {
        this.channelSizeAnnotation = channelSizeAnnotation;
    }

    @JIPipeDocumentation(name = "Annotate with number of frames (T)", description = "If enabled, an annotation with provided name is created. The annotation " +
            "contains the number of frames (number of T slices).")
    @JIPipeParameter(value = "frames-size-annotation", uiOrder = 102)
    public OptionalAnnotationNameParameter getFramesSizeAnnotation() {
        return framesSizeAnnotation;
    }

    @JIPipeParameter("frames-size-annotation")
    public void setFramesSizeAnnotation(OptionalAnnotationNameParameter framesSizeAnnotation) {
        this.framesSizeAnnotation = framesSizeAnnotation;
    }

    @JIPipeDocumentation(name = "Annotate with image type", description = "If enabled, an annotation with provided name is created. The annotation " +
            "contains one of the following values: GRAY8, GRAY16, GRAY32, COLOR_256, COLOR_RGB, or UNKNOWN")
    @JIPipeParameter("image-type-annotation")
    public OptionalAnnotationNameParameter getImageTypeAnnotation() {
        return imageTypeAnnotation;
    }

    @JIPipeParameter("image-type-annotation")
    public void setImageTypeAnnotation(OptionalAnnotationNameParameter imageTypeAnnotation) {
        this.imageTypeAnnotation = imageTypeAnnotation;
    }

    @JIPipeDocumentation(name = "Annotate with bit depth", description = "If enabled, an annotation with provided name is created. The annotation " +
            "contains one of the following values: 0, 8, 16, 24 or 32")
    @JIPipeParameter("image-bit-depth-annotation")
    public OptionalAnnotationNameParameter getBitDepthAnnotation() {
        return bitDepthAnnotation;
    }

    @JIPipeParameter("image-bit-depth-annotation")
    public void setBitDepthAnnotation(OptionalAnnotationNameParameter bitDepthAnnotation) {
        this.bitDepthAnnotation = bitDepthAnnotation;
    }

    @JIPipeDocumentation(name = "Annotate with number of planes", description = "If enabled, an annotation with provided name is created. The annotation " +
            "contains the number of 2D image planes (also referred as 'Stack size'). Please note that this value might be different from the number " +
            "of Z slices. This number is size(Z) * size(C) * size(T).")
    @JIPipeParameter("image-plane-number-annotation")
    public OptionalAnnotationNameParameter getPlaneNumberAnnotation() {
        return planeNumberAnnotation;
    }

    @JIPipeParameter("image-plane-number-annotation")
    public void setPlaneNumberAnnotation(OptionalAnnotationNameParameter planeNumberAnnotation) {
        this.planeNumberAnnotation = planeNumberAnnotation;
    }

    @JIPipeDocumentation(name = "Annotate with color space", description = "If enabled, the color space is stored in an annotation. If a greyscale image is " +
            "provided, the color space will be 'Greyscale'.")
    @JIPipeParameter("color-space-annotation")
    public OptionalAnnotationNameParameter getColorSpaceAnnotation() {
        return colorSpaceAnnotation;
    }

    @JIPipeParameter("color-space-annotation")
    public void setColorSpaceAnnotation(OptionalAnnotationNameParameter colorSpaceAnnotation) {
        this.colorSpaceAnnotation = colorSpaceAnnotation;
    }
}
