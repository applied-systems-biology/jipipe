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

package org.hkijena.jipipe.extensions.imagejalgorithms.ij1.roi.convert;

import ij.IJ;
import ij.ImagePlus;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeDefaultMutableSlotConfiguration;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.RoiNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleMaskData;
import org.hkijena.jipipe.extensions.parameters.library.roi.Margin;

import java.util.Map;
import java.util.Optional;

/**
 * Wrapper around {@link ij.plugin.frame.RoiManager}
 */
@JIPipeDocumentation(name = "Convert only ROI to mask", description = "Converts ROI lists to masks. " +
        "This algorithm does not need a reference image that determines the output size.")
@JIPipeNode(nodeTypeCategory = RoiNodeTypeCategory.class, menuPath = "Convert")
@JIPipeInputSlot(value = ROIListData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ImagePlusGreyscaleMaskData.class, slotName = "Output", autoCreate = true)
public class UnreferencedRoiToMaskAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private Margin imageArea = new Margin();
    private boolean drawOutline = false;
    private boolean drawFilledOutline = true;
    private int lineThickness = 1;
    private boolean preferAssociatedImage = true;

    /**
     * Instantiates a new node type.
     *
     * @param info the info
     */
    public UnreferencedRoiToMaskAlgorithm(JIPipeNodeInfo info) {
        super(info);
        imageArea.getWidth().ensureExactValue(false);
        imageArea.getHeight().ensureExactValue(false);
    }

    /**
     * Instantiates a new node type.
     *
     * @param other the other
     */
    public UnreferencedRoiToMaskAlgorithm(UnreferencedRoiToMaskAlgorithm other) {
        super(other);
        this.imageArea = new Margin(other.imageArea);
        this.drawOutline = other.drawOutline;
        this.drawFilledOutline = other.drawFilledOutline;
        this.lineThickness = other.lineThickness;
        this.preferAssociatedImage = other.preferAssociatedImage;
    }

    @JIPipeDocumentation(name = "Prefer ROI-associated images", description =
            "ROI can carry a reference to an image (e.g. the thresholding input). With this option enabled, this image is preferred to generating " +
                    "a mask based on the pure ROIs.")
    @JIPipeParameter("prefer-associated-image")
    public boolean isPreferAssociatedImage() {
        return preferAssociatedImage;
    }

    @JIPipeParameter("prefer-associated-image")
    public void setPreferAssociatedImage(boolean preferAssociatedImage) {
        this.preferAssociatedImage = preferAssociatedImage;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ROIListData inputData = (ROIListData) dataBatch.getInputData(getFirstInputSlot(), ROIListData.class, progressInfo).duplicate(progressInfo);
        if (preferAssociatedImage) {
            for (Map.Entry<Optional<ImagePlus>, ROIListData> entry : inputData.groupByReferenceImage().entrySet()) {
                if (entry.getKey().isPresent()) {
                    ImagePlus reference = entry.getKey().get();
                    ImagePlus target = IJ.createImage("ROIs",
                            "8-bit",
                            reference.getWidth(),
                            reference.getHeight(),
                            reference.getNChannels(),
                            reference.getNSlices(),
                            reference.getNFrames());
                    entry.getValue().drawMask(drawOutline, drawFilledOutline, lineThickness, target);
                    dataBatch.addOutputData(getFirstOutputSlot(), new ImagePlusData(target), progressInfo);
                } else {
                    ImagePlus result = entry.getValue().toMask(imageArea, drawOutline, drawFilledOutline, lineThickness);
                    dataBatch.addOutputData(getFirstOutputSlot(), new ImagePlusData(result), progressInfo);
                }
            }
        } else {
            ImagePlus result = inputData.toMask(imageArea, drawOutline, drawFilledOutline, lineThickness);
            dataBatch.addOutputData(getFirstOutputSlot(), new ImagePlusData(result), progressInfo);
        }
    }

    @JIPipeDocumentation(name = "Image area", description = "Allows modification of the output image width and height.")
    @JIPipeParameter("image-area")
    public Margin getImageArea() {
        return imageArea;
    }

    @JIPipeParameter("image-area")
    public void setImageArea(Margin imageArea) {
        this.imageArea = imageArea;
    }

    @JIPipeDocumentation(name = "Draw outline", description = "If enabled, draw a white outline of the ROI")
    @JIPipeParameter("draw-outline")
    public boolean isDrawOutline() {
        return drawOutline;
    }

    @JIPipeParameter("draw-outline")
    public void setDrawOutline(boolean drawOutline) {
        this.drawOutline = drawOutline;
    }

    @JIPipeDocumentation(name = "Draw filled outline", description = "If enabled, fill the ROI areas")
    @JIPipeParameter("fill-outline")
    public boolean isDrawFilledOutline() {
        return drawFilledOutline;
    }

    @JIPipeParameter("fill-outline")
    public void setDrawFilledOutline(boolean drawFilledOutline) {
        this.drawFilledOutline = drawFilledOutline;
    }

    @JIPipeDocumentation(name = "Line thickness", description = "Only relevant if 'Draw outline' is enabled. Sets the outline thickness.")
    @JIPipeParameter("line-thickness")
    public int getLineThickness() {
        return lineThickness;
    }

    @JIPipeParameter("line-thickness")
    public void setLineThickness(int lineThickness) {
        this.lineThickness = lineThickness;
    }
}
