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

package org.hkijena.jipipe.extensions.imagejalgorithms.ij1.dimensions;

import ij.ImagePlus;
import ij.plugin.ZProjector;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeIssueReport;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeDataSlotInfo;
import org.hkijena.jipipe.api.data.JIPipeDefaultMutableSlotConfiguration;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImageJNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;

import static org.hkijena.jipipe.extensions.imagejalgorithms.ImageJAlgorithmsExtension.DECREASE_DIMENSION_CONVERSION;
import static org.hkijena.jipipe.extensions.imagejalgorithms.ImageJAlgorithmsExtension.REMOVE_MASK_QUALIFIER;

/**
 * Wrapper around {@link ij.plugin.ZProjector}
 */
@JIPipeDocumentation(name = "Z-Project (classic)", description = "Performs a Z-Projection. This version of the Z-project algorithm wraps around the native ImageJ function and can only handle 3D images. If you have a hyperstack (4D+), use the other Z-project algorithm.")
@JIPipeNode(menuPath = "Dimensions", nodeTypeCategory = ImagesNodeTypeCategory.class)
@JIPipeInputSlot(value = ImagePlusData.class, slotName = "Input")
@JIPipeOutputSlot(value = ImagePlusData.class, slotName = "Output")
@JIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "Image\nStacks", aliasName = "Z Project...")
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
        super(info, JIPipeDefaultMutableSlotConfiguration.builder().addInputSlot("Input", "", ImagePlusData.class)
                .addOutputSlot("Output",
                        "", ImagePlusData.class,
                        "Input",
                        JIPipeDataSlotInfo.composeRawInheritanceConversions(REMOVE_MASK_QUALIFIER, DECREASE_DIMENSION_CONVERSION))
                .allowOutputSlotInheritance(true)
                .seal()
                .build());
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
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ImagePlusData inputData = dataBatch.getInputData(getFirstInputSlot(), ImagePlusData.class, progressInfo);
        ImagePlus img = inputData.getDuplicateImage();

        ImagePlus result;
        if (img.getStackSize() > 1) {
            int start = startSlice >= 0 ? startSlice + 1 : 1;
            int end = stopSlice >= 0 ? Math.min(img.getStackSize(), stopSlice + 1) : img.getStackSize();
            result = ZProjector.run(img, method.toString() + (projectAllHyperstackTimePoints ? " all" : ""), start, end);
        } else {
            result = img;
        }

        dataBatch.addOutputData(getFirstOutputSlot(), new ImagePlusData(result), progressInfo);
    }


    @Override
    public void reportValidity(JIPipeIssueReport report) {
        report.resolve("Start slice").checkIfWithin(this, startSlice, -1, Double.POSITIVE_INFINITY, true, true);
        report.resolve("Stop slice").checkIfWithin(this, stopSlice, -1, Double.POSITIVE_INFINITY, true, true);
    }

    @JIPipeDocumentation(name = "Method", description = "The function that is applied to each stack of pixels.")
    @JIPipeParameter("method")
    public Method getMethod() {
        return method;
    }

    @JIPipeParameter("method")
    public void setMethod(Method method) {
        this.method = method;

    }

    @JIPipeParameter("start-slice")
    @JIPipeDocumentation(name = "Start slice", description = "The slice number to start from. The minimum number is zero.")
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
    @JIPipeDocumentation(name = "Stop slice", description = "Slice index that is included last. This is inclusive. Set to -1 to always include all slices.")
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

    @JIPipeDocumentation(name = "Project all hyper stack time points", description = "If true, all time frames are projected")
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
        AverageIntensity, MaxIntensity, MinIntensity, SumSlices, StandardDeviation, Median
    }
}
