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
import ij.gui.Roi;
import ij.process.ImageProcessor;
import org.apache.commons.lang3.math.NumberUtils;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeAnnotation;
import org.hkijena.jipipe.api.data.JIPipeAnnotationMergeStrategy;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeMergingAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeMergingDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d2.ImagePlus2DData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.parameters.primitives.OptionalStringParameter;
import org.hkijena.jipipe.extensions.parameters.roi.Anchor;

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.Map;

@JIPipeDocumentation(name = "Overlay with extracted ROI", description = "Assembles/overlays extracted ROI onto the target images.")
@JIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "ROI")
@JIPipeInputSlot(value = ImagePlusData.class, slotName = "Target", autoCreate = true)
@JIPipeInputSlot(value = ImagePlus2DData.class, slotName = "ROI", autoCreate = true)
@JIPipeOutputSlot(value = ImagePlusData.class, slotName = "Output", autoCreate = true, inheritedSlot = "Target")
public class AssembleExtractedROIAlgorithm extends JIPipeMergingAlgorithm {

    private String annotationX = "X";
    private String annotationY = "Y";
    private OptionalStringParameter annotationZ = new OptionalStringParameter("Z", true);
    private OptionalStringParameter annotationC = new OptionalStringParameter("C", true);
    private OptionalStringParameter annotationT = new OptionalStringParameter("T", true);
    private Anchor xyAnchor = Anchor.TopLeft;
    private boolean doScaling = true;

    public AssembleExtractedROIAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public AssembleExtractedROIAlgorithm(AssembleExtractedROIAlgorithm other) {
        super(other);
        this.annotationX = other.annotationX;
        this.annotationY = other.annotationY;
        this.annotationZ = new OptionalStringParameter(other.annotationZ);
        this.annotationC = new OptionalStringParameter(other.annotationC);
        this.annotationT = new OptionalStringParameter(other.annotationT);
        this.xyAnchor = other.xyAnchor;
        this.doScaling = other.doScaling;
    }

    @Override
    protected void runIteration(JIPipeMergingDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        JIPipeDataSlot targetImageSlot = getInputSlot("Target");
        JIPipeDataSlot roiSlot = getInputSlot("ROI");
        for (Integer targetImageRow : dataBatch.getInputRows(targetImageSlot)) {
            ImagePlusData targetImage = (ImagePlusData) targetImageSlot.getData(targetImageRow, ImagePlusData.class, progressInfo).duplicate();
            for (Integer roiImageRow : dataBatch.getInputRows(roiSlot)) {
                ImagePlusData roiImage = roiSlot.getData(roiImageRow, ImagePlusData.class, progressInfo);
                ImagePlus convertedRoiImage = ImageJUtils.convertToSameTypeIfNeeded(roiImage.getImage(), targetImage.getImage(), doScaling);
                Map<String, String> annotationMap = JIPipeAnnotation.annotationListToMap(roiSlot.getAnnotations(roiImageRow), JIPipeAnnotationMergeStrategy.OverwriteExisting);
                int x = NumberUtils.createDouble(annotationMap.get(annotationX)).intValue();
                int y = NumberUtils.createDouble(annotationMap.get(annotationY)).intValue();
                int z = 0;
                int c = 0;
                int t = 0;
                if (annotationZ.isEnabled())
                    z = NumberUtils.createDouble(annotationMap.get(annotationZ.getContent())).intValue();
                if (annotationC.isEnabled())
                    c = NumberUtils.createDouble(annotationMap.get(annotationC.getContent())).intValue();
                if (annotationT.isEnabled())
                    t = NumberUtils.createDouble(annotationMap.get(annotationT.getContent())).intValue();
                int finalZ = z;
                int finalC = c;
                int finalT = t;
                Rectangle bounds = xyAnchor.getRectangle(new Point(x, y), roiImage.getImage().getWidth(), roiImage.getImage().getHeight());
                ImageProcessor roiProcessor = convertedRoiImage.getProcessor();
                ImageJUtils.forEachIndexedZCTSlice(targetImage.getImage(), (processor, index) -> {
                    if (finalZ != 0 && finalZ != index.getZ())
                        return;
                    if (finalC != 0 && finalC != index.getC())
                        return;
                    if (finalT != 0 && finalT != index.getT())
                        return;
                    processor.setColor(Color.WHITE);
                    processor.setRoi(bounds);
                    processor.fill();
                    processor.setRoi((Roi) null);
                    processor.insert(roiProcessor, bounds.x, bounds.y);
                }, progressInfo);
            }
            dataBatch.addOutputData(getFirstOutputSlot(), targetImage, progressInfo);
        }
    }

    @JIPipeDocumentation(name = "X location", description = "If enabled, the X location of each target image is extracted from its annotations. If you disable this, you can enable extraction of the width and center X as alternative.")
    @JIPipeParameter(value = "annotation-x", uiOrder = -50)
    public String getAnnotationX() {
        return annotationX;
    }

    @JIPipeParameter("annotation-x")
    public void setAnnotationX(String annotationX) {
        this.annotationX = annotationX;
    }

    @JIPipeDocumentation(name = "Y location", description = "If enabled, the Y location of each target image is extracted from its annotation. If you disable this, you can enable extraction of the width and center Y as alternative.")
    @JIPipeParameter(value = "annotation-y", uiOrder = -45)
    public String getAnnotationY() {
        return annotationY;
    }

    @JIPipeParameter("annotation-y")
    public void setAnnotationY(String annotationY) {
        this.annotationY = annotationY;
    }

    @JIPipeDocumentation(name = "Get Z location", description = "If enabled, the Z location is extracted from the given annotation column. The first index is one. Zero indicates that the target is present in all Z slices.")
    @JIPipeParameter("annotation-z")
    public OptionalStringParameter getAnnotationZ() {
        return annotationZ;
    }

    @JIPipeParameter("annotation-z")
    public void setAnnotationZ(OptionalStringParameter annotationZ) {
        this.annotationZ = annotationZ;
    }

    @JIPipeDocumentation(name = "Get C location", description = "If enabled, the channel location is extracted from the given annotation column. The first index is one. Zero indicates that the target is present in all C slices.")
    @JIPipeParameter("annotation-c")
    public OptionalStringParameter getAnnotationC() {
        return annotationC;
    }

    @JIPipeParameter("annotation-c")
    public void setAnnotationC(OptionalStringParameter annotationC) {
        this.annotationC = annotationC;
    }

    @JIPipeDocumentation(name = "Get T location", description = "If enabled, the frame location is extracted from the given annotation column. The first index is one. Zero indicates that the target is present in all T slices.")
    @JIPipeParameter("annotation-t")
    public OptionalStringParameter getAnnotationT() {
        return annotationT;
    }

    @JIPipeParameter("annotation-t")
    public void setAnnotationT(OptionalStringParameter annotationT) {
        this.annotationT = annotationT;
    }

    @JIPipeDocumentation(name = "X/Y location anchor", description = "Determines how the X and Y locations are interpreted.")
    @JIPipeParameter("xy-anchor")
    public Anchor getXyAnchor() {
        return xyAnchor;
    }

    @JIPipeParameter("xy-anchor")
    public void setXyAnchor(Anchor xyAnchor) {
        this.xyAnchor = xyAnchor;
    }

    @JIPipeDocumentation(name = "Apply scaling for greyscale conversions", description = "If enabled, the appropriate scaling is applied when converting between 16 and 8 bit images.")
    @JIPipeParameter("do-scaling")
    public boolean isDoScaling() {
        return doScaling;
    }

    @JIPipeParameter("do-scaling")
    public void setDoScaling(boolean doScaling) {
        this.doScaling = doScaling;
    }
}
