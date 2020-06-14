package org.hkijena.acaq5.extensions.imagejdatatypes.algorithms;

import ij.ImagePlus;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;
import org.hkijena.acaq5.api.ACAQRunnerSubStatus;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.*;
import org.hkijena.acaq5.api.data.ACAQAnnotation;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.acaq5.extensions.parameters.primitives.OptionalStringParameter;
import org.hkijena.acaq5.extensions.parameters.primitives.StringParameterSettings;
import org.hkijena.acaq5.utils.ResourceUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Adds data annotations that contain the image properties
 */
@ACAQDocumentation(name = "Annotate with image properties", description = "Adds data annotations that contain the image properties.")
@ACAQOrganization(algorithmCategory = ACAQAlgorithmCategory.Annotation, menuPath = "Generate")
@AlgorithmInputSlot(value = ImagePlusData.class, slotName = "Image", autoCreate = true)
@AlgorithmOutputSlot(value = ImagePlusData.class, slotName = "Annotated image", inheritedSlot = "Image", autoCreate = true)
public class ImagePropertiesToAnnotationAlgorithm extends ACAQSimpleIteratingAlgorithm {

    private OptionalStringParameter titleAnnotation = new OptionalStringParameter();
    private OptionalStringParameter widthAnnotation = new OptionalStringParameter();
    private OptionalStringParameter heightAnnotation = new OptionalStringParameter();
    private OptionalStringParameter stackSizeAnnotation = new OptionalStringParameter();
    private OptionalStringParameter planeNumberAnnotation = new OptionalStringParameter();
    private OptionalStringParameter channelSizeAnnotation = new OptionalStringParameter();
    private OptionalStringParameter framesSizeAnnotation = new OptionalStringParameter();
    private OptionalStringParameter imageTypeAnnotation = new OptionalStringParameter();
    private OptionalStringParameter bitDepthAnnotation = new OptionalStringParameter();

    /**
     * Creates a new instance
     *
     * @param declaration algorithm declaration
     */
    public ImagePropertiesToAnnotationAlgorithm(ACAQAlgorithmDeclaration declaration) {
        super(declaration);
        titleAnnotation.setContent("Image title");
        widthAnnotation.setContent("Image width");
        heightAnnotation.setContent("Image height");
        stackSizeAnnotation.setContent("Image Z slices");
        planeNumberAnnotation.setContent("Image planes");
        channelSizeAnnotation.setContent("Image composite channel count");
        framesSizeAnnotation.setContent("Image frame count");
        imageTypeAnnotation.setContent("Image type");
        bitDepthAnnotation.setContent("Image bit depth");
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public ImagePropertiesToAnnotationAlgorithm(ImagePropertiesToAnnotationAlgorithm other) {
        super(other);
        this.titleAnnotation = new OptionalStringParameter(other.titleAnnotation);
        this.widthAnnotation = new OptionalStringParameter(other.widthAnnotation);
        this.heightAnnotation = new OptionalStringParameter(other.heightAnnotation);
        this.stackSizeAnnotation = new OptionalStringParameter(other.stackSizeAnnotation);
        this.planeNumberAnnotation = new OptionalStringParameter(other.planeNumberAnnotation);
        this.channelSizeAnnotation = new OptionalStringParameter(other.channelSizeAnnotation);
        this.framesSizeAnnotation = new OptionalStringParameter(other.framesSizeAnnotation);
        this.imageTypeAnnotation = new OptionalStringParameter(other.imageTypeAnnotation);
        this.bitDepthAnnotation = new OptionalStringParameter(other.bitDepthAnnotation);
    }

    @Override
    public void reportValidity(ACAQValidityReport report) {
        super.reportValidity(report);
        if (getTitleAnnotation().isEnabled())
            report.forCategory("Annotate with title").checkNonEmpty(getTitleAnnotation().getContent(), this);
        if (getWidthAnnotation().isEnabled())
            report.forCategory("Annotate with image width").checkNonEmpty(getWidthAnnotation().getContent(), this);
        if (getHeightAnnotation().isEnabled())
            report.forCategory("Annotate with image height").checkNonEmpty(getHeightAnnotation().getContent(), this);
        if (getStackSizeAnnotation().isEnabled())
            report.forCategory("Annotate with stack size (Z)").checkNonEmpty(getStackSizeAnnotation().getContent(), this);
        if (getPlaneNumberAnnotation().isEnabled())
            report.forCategory("Annotate with number of planes").checkNonEmpty(getPlaneNumberAnnotation().getContent(), this);
        if (getChannelSizeAnnotation().isEnabled())
            report.forCategory("Annotate with channel size (C)").checkNonEmpty(getChannelSizeAnnotation().getContent(), this);
        if (getFramesSizeAnnotation().isEnabled())
            report.forCategory("Annotate with number of frames (T)").checkNonEmpty(getFramesSizeAnnotation().getContent(), this);
        if (getImageTypeAnnotation().isEnabled())
            report.forCategory("Annotate with image type").checkNonEmpty(getImageTypeAnnotation().getContent(), this);
        if (getBitDepthAnnotation().isEnabled())
            report.forCategory("Annotate with bit depth").checkNonEmpty(getBitDepthAnnotation().getContent(), this);
    }

    @Override
    protected void runIteration(ACAQDataInterface dataInterface, ACAQRunnerSubStatus subProgress, Consumer<ACAQRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        ImagePlusData inputData = dataInterface.getInputData(getFirstInputSlot(), ImagePlusData.class);
        List<ACAQAnnotation> annotations = new ArrayList<>();

        if (getTitleAnnotation().isEnabled()) {
            annotations.add(new ACAQAnnotation(getTitleAnnotation().getContent(), "" + inputData.getImage().getTitle()));
        }
        if (getWidthAnnotation().isEnabled()) {
            annotations.add(new ACAQAnnotation(getWidthAnnotation().getContent(), "" + inputData.getImage().getWidth()));
        }
        if (getHeightAnnotation().isEnabled()) {
            annotations.add(new ACAQAnnotation(getHeightAnnotation().getContent(), "" + inputData.getImage().getHeight()));
        }
        if (getStackSizeAnnotation().isEnabled()) {
            annotations.add(new ACAQAnnotation(getStackSizeAnnotation().getContent(), "" + inputData.getImage().getNSlices()));
        }
        if (getPlaneNumberAnnotation().isEnabled()) {
            annotations.add(new ACAQAnnotation(getPlaneNumberAnnotation().getContent(), "" + inputData.getImage().getStackSize()));
        }
        if (getChannelSizeAnnotation().isEnabled()) {
            annotations.add(new ACAQAnnotation(getChannelSizeAnnotation().getContent(), "" + inputData.getImage().getNChannels()));
        }
        if (getFramesSizeAnnotation().isEnabled()) {
            annotations.add(new ACAQAnnotation(getFramesSizeAnnotation().getContent(), "" + inputData.getImage().getNFrames()));
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
            annotations.add(new ACAQAnnotation(getImageTypeAnnotation().getContent(), type));
        }
        if (getBitDepthAnnotation().isEnabled()) {
            annotations.add(new ACAQAnnotation(getBitDepthAnnotation().getContent(), "" + inputData.getImage().getBitDepth()));
        }

        dataInterface.addOutputData(getFirstOutputSlot(), inputData, annotations);
    }

    @ACAQDocumentation(name = "Annotate with title", description = "If enabled, an annotation with provided name is created. The annotation " +
            "contains the ImageJ image name.")
    @ACAQParameter("title-annotation")
    @StringParameterSettings(monospace = true, icon = ResourceUtils.RESOURCE_BASE_PATH + "/icons/annotation.png")
    public OptionalStringParameter getTitleAnnotation() {
        return titleAnnotation;
    }

    @ACAQParameter("title-annotation")
    public void setTitleAnnotation(OptionalStringParameter titleAnnotation) {
        this.titleAnnotation = titleAnnotation;
    }

    @ACAQDocumentation(name = "Annotate with image width", description = "If enabled, an annotation with provided name is created. The annotation " +
            "contains image width.")
    @ACAQParameter(value = "width-annotation")
    @StringParameterSettings(monospace = true, icon = ResourceUtils.RESOURCE_BASE_PATH + "/icons/annotation.png")
    public OptionalStringParameter getWidthAnnotation() {
        return widthAnnotation;
    }

    @ACAQParameter("width-annotation")
    public void setWidthAnnotation(OptionalStringParameter widthAnnotation) {
        this.widthAnnotation = widthAnnotation;
    }

    @ACAQDocumentation(name = "Annotate with image height", description = "If enabled, an annotation with provided name is created. The annotation " +
            "contains image height.")
    @ACAQParameter("height-annotation")
    @StringParameterSettings(monospace = true, icon = ResourceUtils.RESOURCE_BASE_PATH + "/icons/annotation.png")
    public OptionalStringParameter getHeightAnnotation() {
        return heightAnnotation;
    }

    @ACAQParameter("height-annotation")
    public void setHeightAnnotation(OptionalStringParameter heightAnnotation) {
        this.heightAnnotation = heightAnnotation;
    }

    @ACAQDocumentation(name = "Annotate with stack size (Z)", description = "If enabled, an annotation with provided name is created. The annotation " +
            "contains the image stack size (number of Z slices).")
    @ACAQParameter(value = "stack-size-annotation", uiOrder = 100)
    @StringParameterSettings(monospace = true, icon = ResourceUtils.RESOURCE_BASE_PATH + "/icons/annotation.png")
    public OptionalStringParameter getStackSizeAnnotation() {
        return stackSizeAnnotation;
    }

    @ACAQParameter("stack-size-annotation")
    public void setStackSizeAnnotation(OptionalStringParameter stackSizeAnnotation) {
        this.stackSizeAnnotation = stackSizeAnnotation;
    }

    @ACAQDocumentation(name = "Annotate with channel size (C)", description = "If enabled, an annotation with provided name is created. The annotation " +
            "contains the composite image channel size (number of C slices). Please note that is is different from the pixel channels like RGB.")
    @ACAQParameter(value = "channel-size-annotation", uiOrder = 101)
    @StringParameterSettings(monospace = true, icon = ResourceUtils.RESOURCE_BASE_PATH + "/icons/annotation.png")
    public OptionalStringParameter getChannelSizeAnnotation() {
        return channelSizeAnnotation;
    }

    @ACAQParameter("channel-size-annotation")
    public void setChannelSizeAnnotation(OptionalStringParameter channelSizeAnnotation) {
        this.channelSizeAnnotation = channelSizeAnnotation;
    }

    @ACAQDocumentation(name = "Annotate with number of frames (T)", description = "If enabled, an annotation with provided name is created. The annotation " +
            "contains the number of frames (number of T slices).")
    @ACAQParameter(value = "frames-size-annotation", uiOrder = 102)
    @StringParameterSettings(monospace = true, icon = ResourceUtils.RESOURCE_BASE_PATH + "/icons/annotation.png")
    public OptionalStringParameter getFramesSizeAnnotation() {
        return framesSizeAnnotation;
    }

    @ACAQParameter("frames-size-annotation")
    public void setFramesSizeAnnotation(OptionalStringParameter framesSizeAnnotation) {
        this.framesSizeAnnotation = framesSizeAnnotation;
    }

    @ACAQDocumentation(name = "Annotate with image type", description = "If enabled, an annotation with provided name is created. The annotation " +
            "contains one of the following values: GRAY8, GRAY16, GRAY32, COLOR_256, COLOR_RGB, or UNKNOWN")
    @ACAQParameter("image-type-annotation")
    @StringParameterSettings(monospace = true, icon = ResourceUtils.RESOURCE_BASE_PATH + "/icons/annotation.png")
    public OptionalStringParameter getImageTypeAnnotation() {
        return imageTypeAnnotation;
    }

    @ACAQParameter("image-type-annotation")
    public void setImageTypeAnnotation(OptionalStringParameter imageTypeAnnotation) {
        this.imageTypeAnnotation = imageTypeAnnotation;
    }

    @ACAQDocumentation(name = "Annotate with bit depth", description = "If enabled, an annotation with provided name is created. The annotation " +
            "contains one of the following values: 0, 8, 16, 24 or 32")
    @ACAQParameter("image-bit-depth-annotation")
    @StringParameterSettings(monospace = true, icon = ResourceUtils.RESOURCE_BASE_PATH + "/icons/annotation.png")
    public OptionalStringParameter getBitDepthAnnotation() {
        return bitDepthAnnotation;
    }

    @ACAQParameter("image-bit-depth-annotation")
    public void setBitDepthAnnotation(OptionalStringParameter bitDepthAnnotation) {
        this.bitDepthAnnotation = bitDepthAnnotation;
    }

    @ACAQDocumentation(name = "Annotate with number of planes", description = "If enabled, an annotation with provided name is created. The annotation " +
            "contains the number of 2D image planes (also referred as 'Stack size'). Please note that this value might be different from the number " +
            "of Z slices. This number is size(Z) * size(C) * size(T).")
    @ACAQParameter("image-plane-number-annotation")
    @StringParameterSettings(monospace = true, icon = ResourceUtils.RESOURCE_BASE_PATH + "/icons/annotation.png")
    public OptionalStringParameter getPlaneNumberAnnotation() {
        return planeNumberAnnotation;
    }

    @ACAQParameter("image-plane-number-annotation")
    public void setPlaneNumberAnnotation(OptionalStringParameter planeNumberAnnotation) {
        this.planeNumberAnnotation = planeNumberAnnotation;
    }
}
