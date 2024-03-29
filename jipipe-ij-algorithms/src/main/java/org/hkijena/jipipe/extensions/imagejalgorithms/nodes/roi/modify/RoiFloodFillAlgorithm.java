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

package org.hkijena.jipipe.extensions.imagejalgorithms.nodes.roi.modify;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.Roi;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.RoiNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;

import java.awt.*;

/**
 * Wrapper around {@link ij.plugin.frame.RoiManager}
 */
@SetJIPipeDocumentation(name = "Flood fill", description = "Starts a flood fill (magic wand) operation starting at the location of the ROI. Multiple operations are applied for each point within the ROI. Returns the generated outlines.")
@ConfigureJIPipeNode(nodeTypeCategory = RoiNodeTypeCategory.class)
@AddJIPipeInputSlot(value = ROIListData.class, slotName = "ROI", create = true)
@AddJIPipeInputSlot(value = ImagePlusData.class, slotName = "Image", create = true)
@AddJIPipeOutputSlot(value = ROIListData.class, slotName = "ROI", create = true)
public class RoiFloodFillAlgorithm extends JIPipeIteratingAlgorithm {

    private double tolerance = 0;

    private Mode mode = Mode.Connect4;

    private boolean smooth = true;


    /**
     * Instantiates a new node type.
     *
     * @param info the info
     */
    public RoiFloodFillAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Instantiates a new node type.
     *
     * @param other the other
     */
    public RoiFloodFillAlgorithm(RoiFloodFillAlgorithm other) {
        super(other);
        this.tolerance = other.tolerance;
        this.mode = other.mode;
        this.smooth = other.smooth;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ROIListData inputRoi = iterationStep.getInputData("ROI", ROIListData.class, progressInfo);
        ImagePlus inputImage = iterationStep.getInputData("Image", ImagePlusData.class, progressInfo).getImage();

        ROIListData outputRoi = new ROIListData();
        ImageJUtils.forEachIndexedZCTSlice(inputImage, (ip, index) -> {
            for (Roi roi : inputRoi) {
                ImagePlus wrapper = new ImagePlus("slice", ip);
                ROIListData collection = new ROIListData();
                for (Point point : roi.getContainedPoints()) {
                    IJ.doWand(wrapper, point.x, point.y, tolerance, mode.getNativeValue() + (smooth ? " smooth" : ""));
                    Roi wand = wrapper.getRoi();
                    wrapper.setRoi((Roi) null);
                    collection.add(wand);
                }
                if (collection.size() > 1) {
                    collection.logicalOr();
                }
                Roi output = collection.get(0);
                output.setPosition(index.getC() + 1, index.getZ() + 1, index.getT() + 1);
                outputRoi.add(output);
            }
        }, progressInfo);


        iterationStep.addOutputData(getFirstOutputSlot(), outputRoi, progressInfo);
    }

    @SetJIPipeDocumentation(name = "Tolerance")
    @JIPipeParameter("tolerance")
    public double getTolerance() {
        return tolerance;
    }

    @JIPipeParameter("tolerance")
    public void setTolerance(double tolerance) {
        this.tolerance = tolerance;
    }

    @SetJIPipeDocumentation(name = "Mode")
    @JIPipeParameter("mode")
    public Mode getMode() {
        return mode;
    }

    @JIPipeParameter("mode")
    public void setMode(Mode mode) {
        this.mode = mode;
    }

    @SetJIPipeDocumentation(name = "Smooth")
    @JIPipeParameter("smooth")
    public boolean isSmooth() {
        return smooth;
    }

    @JIPipeParameter("smooth")
    public void setSmooth(boolean smooth) {
        this.smooth = smooth;
    }

    public enum Mode {
        Connect4("4-connected"),
        Connect8("8-connected"),
        Legacy("Legacy");

        private final String nativeValue;

        Mode(String nativeValue) {
            this.nativeValue = nativeValue;
        }

        public String getNativeValue() {
            return nativeValue;
        }
    }
}
