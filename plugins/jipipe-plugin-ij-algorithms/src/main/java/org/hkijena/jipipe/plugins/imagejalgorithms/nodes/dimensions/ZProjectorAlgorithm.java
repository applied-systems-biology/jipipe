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
import ij.plugin.ZProjector;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImageJNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJUtils;


/**
 * Wrapper around {@link ij.plugin.ZProjector}
 */
@SetJIPipeDocumentation(name = "Z-Project (classic)", description = "Performs a Z-Projection. This version of the Z-project algorithm wraps around the native ImageJ function and can only handle 3D images. If you have a hyperstack (4D+), use the other Z-project algorithm.")
@ConfigureJIPipeNode(menuPath = "Dimensions", nodeTypeCategory = ImagesNodeTypeCategory.class)
@AddJIPipeInputSlot(value = ImagePlusData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = ImagePlusData.class, name = "Output", create = true)
@AddJIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "Image\nStacks", aliasName = "Z Project...")
public class ZProjectorAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private Method method = Method.MaxIntensity;
    private int startSlice = 0;
    private int stopSlice = -1;
    private boolean projectAllHyperstackTimePoints = true;

    /**
     * Instantiates a new node type.
     *
     * @param info the info
     */
    public ZProjectorAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Instantiates a new node type.
     *
     * @param other the other
     */
    public ZProjectorAlgorithm(ZProjectorAlgorithm other) {
        super(other);
        this.method = other.method;
        this.startSlice = other.startSlice;
        this.stopSlice = other.stopSlice;
        this.projectAllHyperstackTimePoints = other.projectAllHyperstackTimePoints;
    }

    @Override
    public boolean supportsParallelization() {
        return true;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImagePlusData inputData = iterationStep.getInputData(getFirstInputSlot(), ImagePlusData.class, progressInfo);
        ImagePlus img = inputData.getDuplicateImage();

        ImagePlus result;
        if (img.getStackSize() > 1) {
            int start = startSlice >= 0 ? startSlice + 1 : 1;
            int end = stopSlice >= 0 ? Math.min(img.getStackSize(), stopSlice + 1) : img.getStackSize();
            result = ZProjector.run(img, method.toString() + (projectAllHyperstackTimePoints ? " all" : ""), start, end);
            result = ImageJUtils.copyLUTsIfNeeded(img, result);
        } else {
            result = img;
        }

        iterationStep.addOutputData(getFirstOutputSlot(), new ImagePlusData(result), progressInfo);
    }

    @SetJIPipeDocumentation(name = "Method", description = "The function that is applied to each stack of pixels.")
    @JIPipeParameter("method")
    public Method getMethod() {
        return method;
    }

    @JIPipeParameter("method")
    public void setMethod(Method method) {
        this.method = method;

    }

    @JIPipeParameter("start-slice")
    @SetJIPipeDocumentation(name = "Start slice", description = "The slice number to start from. The minimum number is zero.")
    public int getStartSlice() {
        return startSlice;
    }

    @JIPipeParameter("start-slice")
    public boolean setStartSlice(int startSlice) {
        if (startSlice < 0) {
            this.startSlice = 0;
            return false;
        }
        this.startSlice = startSlice;

        return true;
    }

    @JIPipeParameter("stop-slice")
    @SetJIPipeDocumentation(name = "Stop slice", description = "Slice index that is included last. This is inclusive. Set to -1 to always include all slices.")
    public int getStopSlice() {
        return stopSlice;
    }

    @JIPipeParameter("stop-slice")
    public boolean setStopSlice(int stopSlice) {
        if (stopSlice < -1) {
            this.startSlice = -1;
            return false;
        }
        this.stopSlice = stopSlice;

        return true;
    }

    @SetJIPipeDocumentation(name = "Project all hyper stack time points", description = "If true, all time frames are projected")
    @JIPipeParameter("all-hyperstack-timepoints")
    public boolean isProjectAllHyperstackTimePoints() {
        return projectAllHyperstackTimePoints;
    }

    @JIPipeParameter("all-hyperstack-timepoints")
    public void setProjectAllHyperstackTimePoints(boolean projectAllHyperstackTimePoints) {
        this.projectAllHyperstackTimePoints = projectAllHyperstackTimePoints;

    }

    /**
     * Available transformation functions
     */
    public enum Method {
        AverageIntensity("av"), MaxIntensity("max"), MinIntensity("min"), SumSlices("sum"), StandardDeviation("sd"), Median("median");

        private final String nativeValue;

        Method(String nativeValue) {
            this.nativeValue = nativeValue;
        }

        public String getNativeValue() {
            return nativeValue;
        }
    }
}
