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

package org.hkijena.jipipe.extensions.imagejalgorithms.nodes.roi.assemble;

import ij.ImagePlus;
import ij.gui.Roi;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.databatch.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.utils.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d2.ImagePlus2DData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.parameters.library.colors.OptionalColorParameter;
import org.hkijena.jipipe.extensions.parameters.library.primitives.optional.OptionalAnnotationNameParameter;
import org.hkijena.jipipe.extensions.parameters.library.roi.Anchor;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

@JIPipeDocumentation(name = "Extract from ROI", description = "Extracts parts of the incoming image within the given ROI by extracting the bounding area.")
@JIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "ROI")
@JIPipeInputSlot(value = ImagePlusData.class, slotName = "Image", autoCreate = true)
@JIPipeInputSlot(value = ROIListData.class, slotName = "ROI", autoCreate = true)
@JIPipeOutputSlot(value = ImagePlus2DData.class, slotName = "Extracted", autoCreate = true)
public class ExtractFromROIAlgorithm extends JIPipeIteratingAlgorithm {

    private OptionalColorParameter outsideColor = new OptionalColorParameter(Color.BLACK, true);
    private OptionalAnnotationNameParameter annotationX = new OptionalAnnotationNameParameter("X", true);
    private OptionalAnnotationNameParameter annotationY = new OptionalAnnotationNameParameter("Y", true);
    private OptionalAnnotationNameParameter annotationZ = new OptionalAnnotationNameParameter("Z", true);
    private OptionalAnnotationNameParameter annotationC = new OptionalAnnotationNameParameter("C", true);
    private OptionalAnnotationNameParameter annotationT = new OptionalAnnotationNameParameter("T", true);
    private OptionalAnnotationNameParameter annotationBoundingWidth = new OptionalAnnotationNameParameter("Width", true);
    private OptionalAnnotationNameParameter annotationBoundingHeight = new OptionalAnnotationNameParameter("Height", true);
    private Anchor xyAnchor = Anchor.TopLeft;
    private JIPipeTextAnnotationMergeMode annotationMergeStrategy = JIPipeTextAnnotationMergeMode.OverwriteExisting;

    public ExtractFromROIAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public ExtractFromROIAlgorithm(ExtractFromROIAlgorithm other) {
        super(other);
        this.outsideColor = new OptionalColorParameter(other.outsideColor);
        this.annotationX = new OptionalAnnotationNameParameter(other.annotationX);
        this.annotationY = new OptionalAnnotationNameParameter(other.annotationY);
        this.annotationZ = new OptionalAnnotationNameParameter(other.annotationZ);
        this.annotationC = new OptionalAnnotationNameParameter(other.annotationC);
        this.annotationT = new OptionalAnnotationNameParameter(other.annotationT);
        this.xyAnchor = other.xyAnchor;
        this.annotationBoundingWidth = new OptionalAnnotationNameParameter(other.annotationBoundingWidth);
        this.annotationBoundingHeight = new OptionalAnnotationNameParameter(other.annotationBoundingHeight);
        this.annotationMergeStrategy = other.annotationMergeStrategy;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ImagePlusData image = dataBatch.getInputData("Image", ImagePlusData.class, progressInfo);
        ROIListData rois = dataBatch.getInputData("ROI", ROIListData.class, progressInfo);

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
                    dataBatch.addOutputData(getFirstOutputSlot(), new ImagePlusData(resultImage), annotations, annotationMergeStrategy, progressInfo);
                }
            }

        }, progressInfo);

    }

    @JIPipeDocumentation(name = "Set color outside ROI", description = "If enabled, the area outside the ROI (e.g. if you have a circular ROI) is set to this color. Otherwise, the bounding area of the ROI is taken as-is.")
    @JIPipeParameter("outside-color")
    public OptionalColorParameter getOutsideColor() {
        return outsideColor;
    }

    @JIPipeParameter("outside-color")
    public void setOutsideColor(OptionalColorParameter outsideColor) {
        this.outsideColor = outsideColor;
    }

    @JIPipeDocumentation(name = "Annotate with X location", description = "If enabled, the generated image is annotated with the top-left X coordinate of the ROI bounding box.")
    @JIPipeParameter(value = "annotation-x", uiOrder = -50)
    public OptionalAnnotationNameParameter getAnnotationX() {
        return annotationX;
    }

    @JIPipeParameter("annotation-x")
    public void setAnnotationX(OptionalAnnotationNameParameter annotationX) {
        this.annotationX = annotationX;
    }

    @JIPipeDocumentation(name = "Annotate with Y location", description = "If enabled, the generated image is annotated with the top-left Y coordinate of the ROI bounding box.")
    @JIPipeParameter(value = "annotation-y", uiOrder = -45)
    public OptionalAnnotationNameParameter getAnnotationY() {
        return annotationY;
    }

    @JIPipeParameter("annotation-y")
    public void setAnnotationY(OptionalAnnotationNameParameter annotationY) {
        this.annotationY = annotationY;
    }

    @JIPipeDocumentation(name = "Annotate with Z location", description = "If enabled, the generated image is annotated with the Z slice of the ROI. The first index is 1. A value of zero indicates that the ROI is located on all Z slices.")
    @JIPipeParameter("annotation-z")
    public OptionalAnnotationNameParameter getAnnotationZ() {
        return annotationZ;
    }

    @JIPipeParameter("annotation-z")
    public void setAnnotationZ(OptionalAnnotationNameParameter annotationZ) {
        this.annotationZ = annotationZ;
    }

    @JIPipeDocumentation(name = "Annotate with C location", description = "If enabled, the generated image is annotated with the channel slice of the ROI. The first index is 1. A value of zero indicates that the ROI is located on all C slices.")
    @JIPipeParameter("annotation-c")
    public OptionalAnnotationNameParameter getAnnotationC() {
        return annotationC;
    }

    @JIPipeParameter("annotation-c")
    public void setAnnotationC(OptionalAnnotationNameParameter annotationC) {
        this.annotationC = annotationC;
    }

    @JIPipeDocumentation(name = "Annotate with T location", description = "If enabled, the generated image is annotated with the frame slice of the ROI. The first index is 1. A value of zero indicates that the ROI is located on all T slices.")
    @JIPipeParameter("annotation-t")
    public OptionalAnnotationNameParameter getAnnotationT() {
        return annotationT;
    }

    @JIPipeParameter("annotation-t")
    public void setAnnotationT(OptionalAnnotationNameParameter annotationT) {
        this.annotationT = annotationT;
    }

    @JIPipeDocumentation(name = "X/Y location anchor", description = "Determines which X and Y locations are extracted as location.")
    @JIPipeParameter("xy-anchor")
    public Anchor getXyAnchor() {
        return xyAnchor;
    }

    @JIPipeParameter("xy-anchor")
    public void setXyAnchor(Anchor xyAnchor) {
        this.xyAnchor = xyAnchor;
    }

    @JIPipeDocumentation(name = "Annotate with ROI width", description = "If enabled, the generated image is annotated with the width of the ROI")
    @JIPipeParameter("annotation-width")
    public OptionalAnnotationNameParameter getAnnotationBoundingWidth() {
        return annotationBoundingWidth;
    }

    @JIPipeParameter("annotation-width")
    public void setAnnotationBoundingWidth(OptionalAnnotationNameParameter annotationBoundingWidth) {
        this.annotationBoundingWidth = annotationBoundingWidth;
    }

    @JIPipeDocumentation(name = "Annotate with ROI height", description = "If enabled, the generated image is annotated with the height of the ROI")
    @JIPipeParameter("annotation-height")
    public OptionalAnnotationNameParameter getAnnotationBoundingHeight() {
        return annotationBoundingHeight;
    }

    @JIPipeParameter("annotation-height")
    public void setAnnotationBoundingHeight(OptionalAnnotationNameParameter annotationBoundingHeight) {
        this.annotationBoundingHeight = annotationBoundingHeight;
    }

    @JIPipeDocumentation(name = "Annotation merging", description = "Determines how generated annotations are merged with existing annotations")
    @JIPipeParameter("annotation-merging")
    public JIPipeTextAnnotationMergeMode getAnnotationMergeStrategy() {
        return annotationMergeStrategy;
    }

    @JIPipeParameter("annotation-merging")
    public void setAnnotationMergeStrategy(JIPipeTextAnnotationMergeMode annotationMergeStrategy) {
        this.annotationMergeStrategy = annotationMergeStrategy;
    }
}
