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

package org.hkijena.jipipe.extensions.imagejalgorithms.ij1.dimensions;

import ij.ImagePlus;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeAnnotation;
import org.hkijena.jipipe.api.data.JIPipeAnnotationMergeStrategy;
import org.hkijena.jipipe.api.data.JIPipeDefaultMutableSlotConfiguration;
import org.hkijena.jipipe.api.nodes.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d2.ImagePlus2DData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.parameters.primitives.OptionalAnnotationNameParameter;

import java.util.ArrayList;
import java.util.List;

import static org.hkijena.jipipe.extensions.imagejalgorithms.ImageJAlgorithmsExtension.TO_2D_CONVERSION;

/**
 * Wrapper around {@link ImageProcessor}
 */
@JIPipeDocumentation(name = "Split stack into 2D", description = "Splits high-dimensional image stacks into 2D planes")
@JIPipeOrganization(menuPath = "Dimensions", nodeTypeCategory = ImagesNodeTypeCategory.class)
@JIPipeInputSlot(value = ImagePlusData.class, slotName = "Input")
@JIPipeOutputSlot(value = ImagePlus2DData.class, slotName = "Output")
public class StackTo2DAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private OptionalAnnotationNameParameter annotationIndex = new OptionalAnnotationNameParameter("Index", false);
    private OptionalAnnotationNameParameter annotationZ = new OptionalAnnotationNameParameter("Z", true);
    private OptionalAnnotationNameParameter annotationC = new OptionalAnnotationNameParameter("C", true);
    private OptionalAnnotationNameParameter annotationT = new OptionalAnnotationNameParameter("T", true);

    /**
     * Instantiates a new node type.
     *
     * @param info the info
     */
    public StackTo2DAlgorithm(JIPipeNodeInfo info) {
        super(info, JIPipeDefaultMutableSlotConfiguration.builder().addInputSlot("Input", ImagePlusData.class)
                .addOutputSlot("Output", ImagePlus2DData.class, "Input", TO_2D_CONVERSION)
                .allowOutputSlotInheritance(true)
                .seal()
                .build());
    }

    /**
     * Instantiates a new node type.
     *
     * @param other the other
     */
    public StackTo2DAlgorithm(StackTo2DAlgorithm other) {
        super(other);
        this.annotationC = new OptionalAnnotationNameParameter(other.annotationC);
        this.annotationZ = new OptionalAnnotationNameParameter(other.annotationZ);
        this.annotationT = new OptionalAnnotationNameParameter(other.annotationT);
        this.annotationIndex = new OptionalAnnotationNameParameter(other.annotationIndex);
    }

    @Override
    public boolean supportsParallelization() {
        return true;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ImagePlusData inputData = dataBatch.getInputData(getFirstInputSlot(), ImagePlusData.class, progressInfo);
        ImagePlus img = inputData.getDuplicateImage();
        ImageJUtils.forEachIndexedZCTSlice(img, (ip, index) -> {
            List<JIPipeAnnotation> annotationList = new ArrayList<>();
            annotationIndex.addAnnotationIfEnabled(annotationList, "" + (img.getStackIndex(index.getC() + 1, index.getZ() + 1, index.getT() + 1) - 1));
            annotationT.addAnnotationIfEnabled(annotationList, "" + index.getT());
            annotationC.addAnnotationIfEnabled(annotationList, "" + index.getC());
            annotationZ.addAnnotationIfEnabled(annotationList, "" + index.getZ());
            ImagePlus resultImage = new ImagePlus("slice=" + index, ip);
            resultImage.copyScale(img);
            dataBatch.addOutputData(getFirstOutputSlot(),
                    new ImagePlus2DData(resultImage),
                    annotationList,
                    JIPipeAnnotationMergeStrategy.OverwriteExisting,
                    progressInfo);
        }, progressInfo);
    }

    @JIPipeDocumentation(name = "Annotate with index", description = "If enabled, annotate the output plane with the index inside the image.")
    @JIPipeParameter("annotation-index")
    public OptionalAnnotationNameParameter getAnnotationIndex() {
        return annotationIndex;
    }

    @JIPipeParameter("annotation-index")
    public void setAnnotationIndex(OptionalAnnotationNameParameter annotationIndex) {
        this.annotationIndex = annotationIndex;
    }

    @JIPipeDocumentation(name = "Annotate with Z position", description = "If enabled, annotate the output plane with the Z position of the plane (first is zero).")
    @JIPipeParameter("annotation-z")
    public OptionalAnnotationNameParameter getAnnotationZ() {
        return annotationZ;
    }

    @JIPipeParameter("annotation-z")
    public void setAnnotationZ(OptionalAnnotationNameParameter annotationZ) {
        this.annotationZ = annotationZ;
    }

    @JIPipeDocumentation(name = "Annotate with channel", description = "If enabled, annotate the output plane with the channel (C) position of the plane (first is zero).")
    @JIPipeParameter("annotation-c")
    public OptionalAnnotationNameParameter getAnnotationC() {
        return annotationC;
    }

    @JIPipeParameter("annotation-c")
    public void setAnnotationC(OptionalAnnotationNameParameter annotationC) {
        this.annotationC = annotationC;
    }

    @JIPipeDocumentation(name = "Annotate with frame", description = "If enabled, annotate the output plane with the frame (T) position of the plane (first is zero).")
    @JIPipeParameter("annotation-t")
    public OptionalAnnotationNameParameter getAnnotationT() {
        return annotationT;
    }

    @JIPipeParameter("annotation-t")
    public void setAnnotationT(OptionalAnnotationNameParameter annotationT) {
        this.annotationT = annotationT;
    }
}

