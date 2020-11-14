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
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.JIPipeValidityReport;
import org.hkijena.jipipe.api.data.JIPipeAnnotation;
import org.hkijena.jipipe.api.data.JIPipeAnnotationMergeStrategy;
import org.hkijena.jipipe.api.nodes.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.AnnotationsNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.parameters.primitives.OptionalAnnotationNameParameter;

import java.util.ArrayList;
import java.util.List;

/**
 * Adds data annotations that contain the image properties
 */
@JIPipeDocumentation(name = "Annotate with image properties", description = "Adds data annotations that contain the image properties.")
@JIPipeOrganization(nodeTypeCategory = AnnotationsNodeTypeCategory.class, menuPath = "Generate")
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
    }

    @Override
    public void reportValidity(JIPipeValidityReport report) {
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
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progress) {
        ImagePlusData inputData = dataBatch.getInputData(getFirstInputSlot(), ImagePlusData.class);
        List<JIPipeAnnotation> annotations = new ArrayList<>();

        if (getTitleAnnotation().isEnabled()) {
            annotations.add(new JIPipeAnnotation(getTitleAnnotation().getContent(), "" + inputData.getImage().getTitle()));
        }
        if (getWidthAnnotation().isEnabled()) {
            annotations.add(new JIPipeAnnotation(getWidthAnnotation().getContent(), "" + inputData.getImage().getWidth()));
        }
        if (getHeightAnnotation().isEnabled()) {
            annotations.add(new JIPipeAnnotation(getHeightAnnotation().getContent(), "" + inputData.getImage().getHeight()));
        }
        if (getStackSizeAnnotation().isEnabled()) {
            annotations.add(new JIPipeAnnotation(getStackSizeAnnotation().getContent(), "" + inputData.getImage().getNSlices()));
        }
        if (getPlaneNumberAnnotation().isEnabled()) {
            annotations.add(new JIPipeAnnotation(getPlaneNumberAnnotation().getContent(), "" + inputData.getImage().getStackSize()));
        }
        if (getChannelSizeAnnotation().isEnabled()) {
            annotations.add(new JIPipeAnnotation(getChannelSizeAnnotation().getContent(), "" + inputData.getImage().getNChannels()));
        }
        if (getFramesSizeAnnotation().isEnabled()) {
            annotations.add(new JIPipeAnnotation(getFramesSizeAnnotation().getContent(), "" + inputData.getImage().getNFrames()));
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
            annotations.add(new JIPipeAnnotation(getImageTypeAnnotation().getContent(), type));
        }
        if (getBitDepthAnnotation().isEnabled()) {
            annotations.add(new JIPipeAnnotation(getBitDepthAnnotation().getContent(), "" + inputData.getImage().getBitDepth()));
        }

        dataBatch.addOutputData(getFirstOutputSlot(), inputData, annotations, JIPipeAnnotationMergeStrategy.Merge);
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
}
