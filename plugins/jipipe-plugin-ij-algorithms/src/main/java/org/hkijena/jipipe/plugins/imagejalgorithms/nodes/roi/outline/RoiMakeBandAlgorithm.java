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

@SetJIPipeDocumentation(name = "Create band around 2D ROI", description = "Create a band-shaped selection around an existing polygon, freehand, or composite ROI")
@ConfigureJIPipeNode(nodeTypeCategory = RoiNodeTypeCategory.class, menuPath = "Outline")
@AddJIPipeInputSlot(value = ROI2DListData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = ROI2DListData.class, name = "Output", create = true)
public class RoiMakeBandAlgorithm extends JIPipeSimpleIteratingAlgorithm {

  private int iterations = 1;
  private double size = 15;
    private InvalidRoiOutlineBehavior errorBehavior = InvalidRoiOutlineBehavior.Error;

    public RoiMakeBandAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public RoiMakeBandAlgorithm(RoiMakeBandAlgorithm other) {
        super(other);
        this.iterations = other.iterations;
        this.size = other.size;
        this.errorBehavior = other.errorBehavior;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
//        ROI2DListData inputData = iterationStep.getInputData(getFirstInputSlot(), ROI2DListData.class, progressInfo);
//        ROI2DListData outputData = new ROI2DListData();
//        for (Roi roi : inputData) {
//            Roi currentRoi = roi;
//            for (int i = 0; i < iterations; i++) {
//                Roi outlined = null;
//                try {
//                    outlined =  ImageJUtils.makeBand(roi, size);
//                    currentRoi = outlined;
//                } catch (Exception e) {
//                    if(errorBehavior == InvalidRoiOutlineBehavior.Error) {
//                        throw new RuntimeException(e);
//                    }
//                }
//                if(outlined != null) {
//                    currentRoi = outlined;
//                    outputData.add(outlined);
//                }
//                else if(errorBehavior == InvalidRoiOutlineBehavior.Skip) {
//                    // Do nothing
//                    break;
//                }
//                else if(errorBehavior == InvalidRoiOutlineBehavior.KeepOriginal) {
//                    outputData.add(roi);
//                    break;
//                }
//                else {
//                    throw new NullPointerException("Unable to interpolate ROI " + roi);
//                }
//            }
//        }
//        iterationStep.addOutputData(getFirstOutputSlot(), outputData, progressInfo);
    }

    @SetJIPipeDocumentation(name = "Iterations", description = "The number of iterations")
    @JIPipeParameter("num-iterations")
    public int getIterations() {
        return iterations;
    }

    @JIPipeParameter("num-iterations")
    public void setIterations(int iterations) {
        this.iterations = iterations;
    }

    @SetJIPipeDocumentation(name = "Size (px)", description = "The size of the band in pixels")
    @JIPipeParameter("size")
    public double getSize() {
        return size;
    }

    @JIPipeParameter("size")
    public void setSize(double size) {
        this.size = size;
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
