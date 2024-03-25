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

package org.hkijena.jipipe.plugins.imagejalgorithms.nodes.roi.convert;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.RoiNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleMaskData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJUtils;

/**
 * Wrapper around {@link ij.plugin.frame.RoiManager}
 */
@SetJIPipeDocumentation(name = "Convert ROI to mask", description = "Converts ROI lists to masks. " +
        "This algorithm needs a reference image that provides the output sizes. If you do not have a reference image, you can use the unreferenced variant.")
@ConfigureJIPipeNode(nodeTypeCategory = RoiNodeTypeCategory.class, menuPath = "Convert")
@AddJIPipeInputSlot(value = ROIListData.class, slotName = "ROI", create = true)
@AddJIPipeInputSlot(value = ImagePlusData.class, slotName = "Image", create = true)
@AddJIPipeOutputSlot(value = ImagePlusGreyscaleMaskData.class, slotName = "Output", create = true)
public class RoiToMaskAlgorithm extends JIPipeIteratingAlgorithm {

    private boolean drawOutline = false;
    private boolean drawFilledOutline = true;
    private int lineThickness = 1;
    private boolean drawOver = false;

    /**
     * Instantiates a new node type.
     *
     * @param info the info
     */
    public RoiToMaskAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Instantiates a new node type.
     *
     * @param other the other
     */
    public RoiToMaskAlgorithm(RoiToMaskAlgorithm other) {
        super(other);
        this.drawOutline = other.drawOutline;
        this.drawFilledOutline = other.drawFilledOutline;
        this.lineThickness = other.lineThickness;
        this.drawOver = other.drawOver;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ROIListData inputData = (ROIListData) iterationStep.getInputData("ROI", ROIListData.class, progressInfo).duplicate(progressInfo);
        ImagePlus reference = iterationStep.getInputData("Image", ImagePlusData.class, progressInfo).getImage();

        // Find the bounds and future stack position
        int sx = reference.getWidth();
        int sy = reference.getHeight();
        int sz = reference.getNSlices();
        int sc = reference.getNChannels();
        int st = reference.getNFrames();

        ImagePlus result;
        if (drawOver) {
            result = ImageJUtils.convertToGreyscale8UIfNeeded(ImageJUtils.duplicate(reference));
            result.setTitle("Reference+ROIs");
        } else {
            result = IJ.createImage("ROIs", "8-bit", sx, sy, sc, sz, st);
        }
        for (int z = 0; z < sz; z++) {
            for (int c = 0; c < sc; c++) {
                for (int t = 0; t < st; t++) {
                    int stackIndex = result.getStackIndex(c + 1, z + 1, t + 1);
                    ImageProcessor processor = result.getStack().getProcessor(stackIndex);
                    processor.setLineWidth(lineThickness);
                    processor.setColor(255);

                    for (Roi roi : inputData) {
                        int rz = roi.getZPosition();
                        int rc = roi.getCPosition();
                        int rt = roi.getTPosition();
                        if (rz != 0 && rz != (z + 1))
                            continue;
                        if (rc != 0 && rc != (c + 1))
                            continue;
                        if (rt != 0 && rt != (t + 1))
                            continue;
                        if (drawFilledOutline)
                            processor.fill(roi);
                        if (drawOutline)
                            roi.drawPixels(processor);
                    }
                }
            }
        }

        iterationStep.addOutputData(getFirstOutputSlot(), new ImagePlusData(result), progressInfo);
    }

    @SetJIPipeDocumentation(name = "Draw outline", description = "If enabled, draw a white outline of the ROI")
    @JIPipeParameter("draw-outline")
    public boolean isDrawOutline() {
        return drawOutline;
    }

    @JIPipeParameter("draw-outline")
    public void setDrawOutline(boolean drawOutline) {
        this.drawOutline = drawOutline;
    }

    @SetJIPipeDocumentation(name = "Draw filled outline", description = "If enabled, fill the ROI areas")
    @JIPipeParameter("fill-outline")
    public boolean isDrawFilledOutline() {
        return drawFilledOutline;
    }

    @JIPipeParameter("fill-outline")
    public void setDrawFilledOutline(boolean drawFilledOutline) {
        this.drawFilledOutline = drawFilledOutline;
    }

    @SetJIPipeDocumentation(name = "Line thickness", description = "Only relevant if 'Draw outline' is enabled. Sets the outline thickness.")
    @JIPipeParameter("line-thickness")
    public int getLineThickness() {
        return lineThickness;
    }

    @JIPipeParameter("line-thickness")
    public void setLineThickness(int lineThickness) {
        this.lineThickness = lineThickness;
    }

    @SetJIPipeDocumentation(name = "Draw over reference", description = "If enabled, draw the ROI over the reference image.")
    @JIPipeParameter("draw-over")
    public boolean isDrawOver() {
        return drawOver;
    }

    @JIPipeParameter("draw-over")
    public void setDrawOver(boolean drawOver) {
        this.drawOver = drawOver;
    }
}
