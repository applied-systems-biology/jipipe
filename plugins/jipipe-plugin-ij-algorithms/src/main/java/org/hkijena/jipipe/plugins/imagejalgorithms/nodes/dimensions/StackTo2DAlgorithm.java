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

package org.hkijena.jipipe.plugins.imagejalgorithms.nodes.dimensions;

import ij.ImagePlus;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImageJNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.d2.ImagePlus2DData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.plugins.parameters.library.primitives.optional.OptionalTextAnnotationNameParameter;

import java.util.ArrayList;
import java.util.List;


/**
 * Wrapper around {@link ImageProcessor}
 */
@SetJIPipeDocumentation(name = "Split stack into 2D", description = "Splits high-dimensional image stacks into 2D planes")
@ConfigureJIPipeNode(menuPath = "Dimensions", nodeTypeCategory = ImagesNodeTypeCategory.class)
@AddJIPipeInputSlot(value = ImagePlusData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = ImagePlus2DData.class, name = "Output", create = true)
@AddJIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "Image\nStacks", aliasName = "Stacks to Image")
public class StackTo2DAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private OptionalTextAnnotationNameParameter annotationIndex = new OptionalTextAnnotationNameParameter("Index", false);
    private OptionalTextAnnotationNameParameter annotationZ = new OptionalTextAnnotationNameParameter("Z", true);
    private OptionalTextAnnotationNameParameter annotationC = new OptionalTextAnnotationNameParameter("C", true);
    private OptionalTextAnnotationNameParameter annotationT = new OptionalTextAnnotationNameParameter("T", true);

    /**
     * Instantiates a new node type.
     *
     * @param info the info
     */
    public StackTo2DAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Instantiates a new node type.
     *
     * @param other the other
     */
    public StackTo2DAlgorithm(StackTo2DAlgorithm other) {
        super(other);
        this.annotationC = new OptionalTextAnnotationNameParameter(other.annotationC);
        this.annotationZ = new OptionalTextAnnotationNameParameter(other.annotationZ);
        this.annotationT = new OptionalTextAnnotationNameParameter(other.annotationT);
        this.annotationIndex = new OptionalTextAnnotationNameParameter(other.annotationIndex);
    }

    @Override
    public boolean supportsParallelization() {
        return true;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImagePlusData inputData = iterationStep.getInputData(getFirstInputSlot(), ImagePlusData.class, progressInfo);
        ImagePlus img = inputData.getDuplicateImage();
        ImageJUtils.forEachIndexedZCTSlice(img, (ip, index) -> {
            List<JIPipeTextAnnotation> annotationList = new ArrayList<>();
            annotationIndex.addAnnotationIfEnabled(annotationList, "" + (img.getStackIndex(index.getC() + 1, index.getZ() + 1, index.getT() + 1) - 1));
            annotationT.addAnnotationIfEnabled(annotationList, "" + index.getT());
            annotationC.addAnnotationIfEnabled(annotationList, "" + index.getC());
            annotationZ.addAnnotationIfEnabled(annotationList, "" + index.getZ());
            ImagePlus resultImage = new ImagePlus("slice=" + index, ip);
            resultImage.copyScale(img);
            iterationStep.addOutputData(getFirstOutputSlot(),
                    new ImagePlus2DData(resultImage),
                    annotationList,
                    JIPipeTextAnnotationMergeMode.OverwriteExisting,
                    progressInfo);
        }, progressInfo);
    }

    @SetJIPipeDocumentation(name = "Annotate with index", description = "If enabled, annotate the output plane with the index inside the image.")
    @JIPipeParameter("annotation-index")
    public OptionalTextAnnotationNameParameter getAnnotationIndex() {
        return annotationIndex;
    }

    @JIPipeParameter("annotation-index")
    public void setAnnotationIndex(OptionalTextAnnotationNameParameter annotationIndex) {
        this.annotationIndex = annotationIndex;
    }

    @SetJIPipeDocumentation(name = "Annotate with Z position", description = "If enabled, annotate the output plane with the Z position of the plane (first is zero).")
    @JIPipeParameter("annotation-z")
    public OptionalTextAnnotationNameParameter getAnnotationZ() {
        return annotationZ;
    }

    @JIPipeParameter("annotation-z")
    public void setAnnotationZ(OptionalTextAnnotationNameParameter annotationZ) {
        this.annotationZ = annotationZ;
    }

    @SetJIPipeDocumentation(name = "Annotate with channel", description = "If enabled, annotate the output plane with the channel (C) position of the plane (first is zero).")
    @JIPipeParameter("annotation-c")
    public OptionalTextAnnotationNameParameter getAnnotationC() {
        return annotationC;
    }

    @JIPipeParameter("annotation-c")
    public void setAnnotationC(OptionalTextAnnotationNameParameter annotationC) {
        this.annotationC = annotationC;
    }

    @SetJIPipeDocumentation(name = "Annotate with frame", description = "If enabled, annotate the output plane with the frame (T) position of the plane (first is zero).")
    @JIPipeParameter("annotation-t")
    public OptionalTextAnnotationNameParameter getAnnotationT() {
        return annotationT;
    }

    @JIPipeParameter("annotation-t")
    public void setAnnotationT(OptionalTextAnnotationNameParameter annotationT) {
        this.annotationT = annotationT;
    }
}

