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

package org.hkijena.jipipe.plugins.imagejalgorithms.nodes.roi.outline;

import ij.gui.Roi;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.RoiNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ROI2DListData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.InvalidRoiOutlineBehavior;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.RoiOutline;

/**
 * Wrapper around {@link ij.plugin.frame.RoiManager}
 */
@SetJIPipeDocumentation(name = "Interpolate 2D ROI", description = "Converts the ROI into a sub-pixel resolution ROI of floating-point coordinates spaced interval pixels apart.")
@ConfigureJIPipeNode(nodeTypeCategory = RoiNodeTypeCategory.class, menuPath = "Outline")
@AddJIPipeInputSlot(value = ROI2DListData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = ROI2DListData.class, name = "Output", create = true)
public class InterpolateRoiAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private double interval = 1;
    private boolean smooth = false;
    private boolean adjust = false;
    private InvalidRoiOutlineBehavior errorBehavior = InvalidRoiOutlineBehavior.Error;

    public InterpolateRoiAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public InterpolateRoiAlgorithm(InterpolateRoiAlgorithm other) {
        super(other);
        this.interval = other.interval;
        this.smooth = other.smooth;
        this.adjust = other.adjust;
        this.errorBehavior = other.errorBehavior;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ROI2DListData inputData = iterationStep.getInputData(getFirstInputSlot(), ROI2DListData.class, progressInfo);
        ROI2DListData outputData = new ROI2DListData();
        for (Roi roi : inputData) {
            Roi outlined = null;
            try {
                outlined =  ImageJUtils.interpolateRoi(roi, interval, smooth, adjust);
            } catch (Exception e) {
                if(errorBehavior == InvalidRoiOutlineBehavior.Error) {
                    throw new RuntimeException(e);
                }
            }
            if(outlined != null) {
                outputData.add(outlined);
            }
            else if(errorBehavior == InvalidRoiOutlineBehavior.Skip) {
                // Do nothing
            }
            else if(errorBehavior == InvalidRoiOutlineBehavior.KeepOriginal) {
                outputData.add(roi);
            }
            else {
                throw new NullPointerException("Unable to interpolate ROI " + roi);
            }
        }
        iterationStep.addOutputData(getFirstOutputSlot(), outputData, progressInfo);
    }

    @SetJIPipeDocumentation(name = "Adjust interval to match")
    @JIPipeParameter("adjust")
    public boolean isAdjust() {
        return adjust;
    }

    @JIPipeParameter("adjust")
    public void setAdjust(boolean adjust) {
        this.adjust = adjust;
    }

     @SetJIPipeDocumentation(name = "Interval (px)", description = "Interval between sub-pixel resolution coordinates")
     @JIPipeParameter("interval")
    public double getInterval() {
        return interval;
    }

    @JIPipeParameter("interval")
    public void setInterval(double interval) {
        this.interval = interval;
    }

    @SetJIPipeDocumentation(name = "Smooth", description = "If enabled, smooth ROI using a 3-point running average")
    @JIPipeParameter("smooth")
    public boolean isSmooth() {
        return smooth;
    }

    @JIPipeParameter("smooth")
    public void setSmooth(boolean smooth) {
        this.smooth = smooth;
    }

    @SetJIPipeDocumentation(name = "Error handling", description = "What to do if a ROI could not be processed")
    @JIPipeParameter("error-behavior")
    public InvalidRoiOutlineBehavior getErrorBehavior() {
        return errorBehavior;
    }

    @JIPipeParameter("error-behavior")
    public void setErrorBehavior(InvalidRoiOutlineBehavior errorBehavior) {
        this.errorBehavior = errorBehavior;
    }
}
