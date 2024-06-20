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

package org.hkijena.jipipe.plugins.imagejalgorithms.nodes.transform;

import ij.ImagePlus;
import ij.gui.Roi;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.data.JIPipeDefaultMutableSlotConfiguration;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImageJNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.plugins.parameters.library.roi.Anchor;

import java.awt.*;
import java.util.Collections;

/**
 * Wrapper around {@link ImageProcessor}
 */
@SetJIPipeDocumentation(name = "Rotate 2D image (free)", description = "Rotates the image by any kind of angle. Expands the canvas if necessary. " +
        "If higher-dimensional data is provided, the filter is applied to each 2D slice.")
@ConfigureJIPipeNode(menuPath = "Transform", nodeTypeCategory = ImagesNodeTypeCategory.class)
@AddJIPipeInputSlot(value = ImagePlusData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = ImagePlusData.class, name = "Output", create = true)
@AddJIPipeOutputSlot(value = ROIListData.class, name = "Content")
@AddJIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "Image\nTransform", aliasName = "Rotate (free)")
public class TransformRotateFree2DAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private double angle = 0;
    private Color backgroundColor = Color.BLACK;
    private Anchor anchor = Anchor.CenterCenter;
    private boolean outputRoi = false;
    private boolean expandCanvas = true;

    /**
     * Instantiates a new node type.
     *
     * @param info the info
     */
    public TransformRotateFree2DAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public TransformRotateFree2DAlgorithm(TransformRotateFree2DAlgorithm other) {
        super(other);
        this.angle = other.angle;
        this.backgroundColor = other.backgroundColor;
        this.anchor = other.anchor;
        this.setOutputRoi(other.outputRoi);
        this.expandCanvas = other.expandCanvas;
    }

    @Override
    public boolean supportsParallelization() {
        return true;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImagePlusData inputData = iterationStep.getInputData(getFirstInputSlot(), ImagePlusData.class, progressInfo);
        ImagePlus imp = ImageJUtils.rotate(inputData.getImage(), angle, expandCanvas, backgroundColor, outputRoi, progressInfo);
        iterationStep.addOutputData("Output", new ImagePlusData(imp), progressInfo);
        if (outputRoi) {
            Roi roi = imp.getRoi();
            imp.setRoi((Roi) null);
            iterationStep.addOutputData("Content", new ROIListData(Collections.singletonList(roi)), progressInfo);
        }
    }

    @SetJIPipeDocumentation(name = "Angle (in degrees)", description = "Determines by which angle the image is rotated in clock-wise direction. To do counter-clockwise rotation, put in a negative angle.")
    @JIPipeParameter("angle")
    public double getAngle() {
        return angle;
    }

    @JIPipeParameter("angle")
    public void setAngle(double angle) {
        this.angle = angle;
    }

    @SetJIPipeDocumentation(name = "Background color", description = "The background color used if the canvas needs to be expanded.")
    @JIPipeParameter("background-color")
    public Color getBackgroundColor() {
        return backgroundColor;
    }

    @JIPipeParameter("background-color")
    public void setBackgroundColor(Color backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    @SetJIPipeDocumentation(name = "Anchor", description = "Anchor for the rotation. Please not that with the current implementation, only Center-Center will expand the canvas properly.")
    @JIPipeParameter("anchor")
    public Anchor getAnchor() {
        return anchor;
    }

    @JIPipeParameter("anchor")
    public void setAnchor(Anchor anchor) {
        this.anchor = anchor;
    }

    @SetJIPipeDocumentation(name = "Output ROI", description = "Also outputs ROI that separate background from the original image.")
    @JIPipeParameter("output-roi")
    public boolean isOutputRoi() {
        return outputRoi;
    }

    @JIPipeParameter("output-roi")
    public void setOutputRoi(boolean outputRoi) {
        this.outputRoi = outputRoi;
        updateSlots();
    }

    private void updateSlots() {
        if (outputRoi) {
            if (!hasOutputSlot("Content")) {
                JIPipeDefaultMutableSlotConfiguration slotConfiguration = (JIPipeDefaultMutableSlotConfiguration) getSlotConfiguration();
                slotConfiguration.addOutputSlot("Content", "ROI that separates background from the original image", ROIListData.class, null, false);
            }
        } else {
            if (hasOutputSlot("Content")) {
                JIPipeDefaultMutableSlotConfiguration slotConfiguration = (JIPipeDefaultMutableSlotConfiguration) getSlotConfiguration();
                slotConfiguration.removeOutputSlot("Content", false);
            }
        }
    }

    @SetJIPipeDocumentation(name = "Expand canvas", description = "If enabled, the canvas will be expanded, so it fits the rotated results.")
    @JIPipeParameter("expand-canvas")
    public boolean isExpandCanvas() {
        return expandCanvas;
    }

    @JIPipeParameter("expand-canvas")
    public void setExpandCanvas(boolean expandCanvas) {
        this.expandCanvas = expandCanvas;
    }
}
