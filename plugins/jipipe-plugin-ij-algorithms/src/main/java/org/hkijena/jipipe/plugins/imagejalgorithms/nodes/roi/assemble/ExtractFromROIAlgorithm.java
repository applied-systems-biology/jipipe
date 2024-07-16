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

package org.hkijena.jipipe.plugins.imagejalgorithms.nodes.roi.assemble;

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
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.d2.ImagePlus2DData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.plugins.parameters.library.colors.OptionalColorParameter;
import org.hkijena.jipipe.plugins.parameters.library.primitives.optional.OptionalTextAnnotationNameParameter;
import org.hkijena.jipipe.plugins.parameters.library.roi.Anchor;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

@SetJIPipeDocumentation(name = "Extract from 2D ROI", description = "Extracts parts of the incoming image within the given ROI by extracting the bounding area.")
@ConfigureJIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "ROI")
@AddJIPipeInputSlot(value = ImagePlusData.class, name = "Image", create = true)
@AddJIPipeInputSlot(value = ROIListData.class, name = "ROI", create = true)
@AddJIPipeOutputSlot(value = ImagePlus2DData.class, name = "Extracted", create = true)
public class ExtractFromROIAlgorithm extends JIPipeIteratingAlgorithm {

    private OptionalColorParameter outsideColor = new OptionalColorParameter(Color.BLACK, true);
    private OptionalTextAnnotationNameParameter annotationX = new OptionalTextAnnotationNameParameter("X", true);
    private OptionalTextAnnotationNameParameter annotationY = new OptionalTextAnnotationNameParameter("Y", true);
    private OptionalTextAnnotationNameParameter annotationZ = new OptionalTextAnnotationNameParameter("Z", true);
    private OptionalTextAnnotationNameParameter annotationC = new OptionalTextAnnotationNameParameter("C", true);
    private OptionalTextAnnotationNameParameter annotationT = new OptionalTextAnnotationNameParameter("T", true);
    private OptionalTextAnnotationNameParameter annotationBoundingWidth = new OptionalTextAnnotationNameParameter("Width", true);
    private OptionalTextAnnotationNameParameter annotationBoundingHeight = new OptionalTextAnnotationNameParameter("Height", true);
    private Anchor xyAnchor = Anchor.TopLeft;
    private JIPipeTextAnnotationMergeMode annotationMergeStrategy = JIPipeTextAnnotationMergeMode.OverwriteExisting;

    public ExtractFromROIAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public ExtractFromROIAlgorithm(ExtractFromROIAlgorithm other) {
        super(other);
        this.outsideColor = new OptionalColorParameter(other.outsideColor);
        this.annotationX = new OptionalTextAnnotationNameParameter(other.annotationX);
        this.annotationY = new OptionalTextAnnotationNameParameter(other.annotationY);
        this.annotationZ = new OptionalTextAnnotationNameParameter(other.annotationZ);
        this.annotationC = new OptionalTextAnnotationNameParameter(other.annotationC);
        this.annotationT = new OptionalTextAnnotationNameParameter(other.annotationT);
        this.xyAnchor = other.xyAnchor;
        this.annotationBoundingWidth = new OptionalTextAnnotationNameParameter(other.annotationBoundingWidth);
        this.annotationBoundingHeight = new OptionalTextAnnotationNameParameter(other.annotationBoundingHeight);
        this.annotationMergeStrategy = other.annotationMergeStrategy;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImagePlusData image = iterationStep.getInputData("Image", ImagePlusData.class, progressInfo);
        ROIListData rois = iterationStep.getInputData("ROI", ROIListData.class, progressInfo);

        ImageJUtils.forEachIndexedZCTSlice(image.getImage(), (processor, index) -> {
            for (Roi roi : rois) {
                if (ROIListData.isVisibleIn(roi, index, false, false, false)) {
                    ImageProcessor resultProcessor;
                    if (outsideColor.isEnabled()) {
                        resultProcessor = processor.duplicate();
                        resultProcessor.setColor(outsideColor.getContent());
                        resultProcessor.fillOutside(roi);
                        resultProcessor.setRoi(roi);
                        resultProcessor = resultProcessor.crop();
                    } else {
                        processor.setRoi(roi);
                        resultProcessor = processor.crop();
                        processor.setRoi((Roi) null);
                    }
                    ImagePlus resultImage = new ImagePlus(image.getImage().getTitle() + " cropped", resultProcessor);
                    resultImage.copyScale(image.getImage());
                    List<JIPipeTextAnnotation> annotations = new ArrayList<>();
                    Rectangle bounds = roi.getBounds();
                    Point coords = xyAnchor.getRectangleCoordinates(bounds);
                    annotationX.addAnnotationIfEnabled(annotations, "" + coords.x);
                    annotationY.addAnnotationIfEnabled(annotations, "" + coords.y);
                    annotationBoundingWidth.addAnnotationIfEnabled(annotations, "" + bounds.getWidth());
                    annotationBoundingHeight.addAnnotationIfEnabled(annotations, "" + bounds.getHeight());
                    annotationZ.addAnnotationIfEnabled(annotations, "" + roi.getZPosition());
                    annotationC.addAnnotationIfEnabled(annotations, "" + roi.getCPosition());
                    annotationT.addAnnotationIfEnabled(annotations, "" + roi.getTPosition());
                    iterationStep.addOutputData(getFirstOutputSlot(), new ImagePlusData(resultImage), annotations, annotationMergeStrategy, progressInfo);
                }
            }

        }, progressInfo);

    }

    @SetJIPipeDocumentation(name = "Set color outside ROI", description = "If enabled, the area outside the ROI (e.g. if you have a circular ROI) is set to this color. Otherwise, the bounding area of the ROI is taken as-is.")
    @JIPipeParameter("outside-color")
    public OptionalColorParameter getOutsideColor() {
        return outsideColor;
    }

    @JIPipeParameter("outside-color")
    public void setOutsideColor(OptionalColorParameter outsideColor) {
        this.outsideColor = outsideColor;
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

    @SetJIPipeDocumentation(name = "X/Y location anchor", description = "Determines which X and Y locations are extracted as location.")
    @JIPipeParameter("xy-anchor")
    public Anchor getXyAnchor() {
        return xyAnchor;
    }

    @JIPipeParameter("xy-anchor")
    public void setXyAnchor(Anchor xyAnchor) {
        this.xyAnchor = xyAnchor;
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
