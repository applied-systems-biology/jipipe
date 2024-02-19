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

package org.hkijena.jipipe.extensions.imagejalgorithms.nodes.features;

import ij.ImagePlus;
import ij.ImageStack;
import ij.plugin.filter.MaximumFinder;
import ij.process.ByteProcessor;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.DefineJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImageJNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;

/**
 * Wrapper around {@link ij.plugin.filter.MaximumFinder}
 */
@SetJIPipeDocumentation(name = "Local maxima 2D", description = "Finds the local maxima of each image. " +
        "If higher-dimensional data is provided, the filter is applied to each 2D slice.")
@DefineJIPipeNode(menuPath = "Features", nodeTypeCategory = ImagesNodeTypeCategory.class)
@AddJIPipeInputSlot(value = ImagePlusData.class, slotName = "Input", create = true)
@AddJIPipeOutputSlot(value = ImagePlusData.class, slotName = "Output", create = true)
@AddJIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "Process", aliasName = "Find Maxima...")
public class LocalMaxima2DAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private double heightTolerance = 10;
    private boolean strict = false;
    private double threshold = 0;
    private OutputType outputType = OutputType.SinglePoints;
    private boolean excludeOnEdges = false;
    private boolean inputIsEDM = false;

    /**
     * Instantiates a new node type.
     *
     * @param info the info
     */
    public LocalMaxima2DAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public LocalMaxima2DAlgorithm(LocalMaxima2DAlgorithm other) {
        super(other);
        this.heightTolerance = other.heightTolerance;
        this.strict = other.strict;
        this.threshold = other.threshold;
        this.outputType = other.outputType;
        this.excludeOnEdges = other.excludeOnEdges;
        this.inputIsEDM = other.inputIsEDM;
    }

    @Override
    public boolean supportsParallelization() {
        return true;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImagePlusData inputData = iterationStep.getInputData(getFirstInputSlot(), ImagePlusData.class, progressInfo);
        ImagePlus img = inputData.getDuplicateImage();

        ImageStack resultStack = new ImageStack(img.getWidth(), img.getHeight(), img.getStackSize());
        MaximumFinder maximumFinder = new MaximumFinder();
        ImageJUtils.forEachIndexedSlice(img, (imp, index) -> {
            ByteProcessor maxima = maximumFinder.findMaxima(imp,
                    heightTolerance,
                    strict,
                    threshold,
                    outputType.getConstant(),
                    excludeOnEdges,
                    inputIsEDM);
            resultStack.setProcessor(maxima, index + 1);
            resultStack.setSliceLabel("slice=" + index, index + 1);
        }, progressInfo);

        ImagePlus resultImage = new ImagePlus("Output", resultStack);
        resultImage.setDimensions(img.getNChannels(), img.getNSlices(), img.getNFrames());
        resultImage.copyScale(img);
        iterationStep.addOutputData(getFirstOutputSlot(), new ImagePlusData(resultImage), progressInfo);
    }

    @SetJIPipeDocumentation(name = "Height tolerance", description = "Maxima are accepted only if protruding more than this value from the ridge to a higher maximum")
    @JIPipeParameter("height-tolerance")
    public double getHeightTolerance() {
        return heightTolerance;
    }

    @JIPipeParameter("height-tolerance")
    public void setHeightTolerance(double heightTolerance) {
        this.heightTolerance = heightTolerance;
    }

    @SetJIPipeDocumentation(name = "Is strict", description = "When off, the global maximum is accepted even if all other pixel are less than " +
            "'tolerance' below this level. With 'Exclude on Edges' enabled, 'strict' also " +
            "means that the surrounding of a maximum within 'tolerance' must not include an edge pixel " +
            "(otherwise, it is enough that there is no edge pixel with the maximum value).")
    @JIPipeParameter("strict")
    public boolean isStrict() {
        return strict;
    }

    @JIPipeParameter("strict")
    public void setStrict(boolean strict) {
        this.strict = strict;
    }

    @SetJIPipeDocumentation(name = "Threshold", description = "Minimum height of a maximum (uncalibrated)")
    @JIPipeParameter("threshold")
    public double getThreshold() {
        return threshold;
    }

    @JIPipeParameter("threshold")
    public void setThreshold(double threshold) {
        this.threshold = threshold;
    }

    @SetJIPipeDocumentation(name = "Output type", description = "Available output types: <table>" +
            "<tr><td>SinglePoints</td><td>Outputs single points</td></tr>" +
            "<tr><td>InTolerance</td><td>Outputs all points around the maximum within the tolerance</td></tr>" +
            "<tr><td>Segmented</td><td>Outputs a watershed-segmented image</td></tr></table>")
    @JIPipeParameter("output-type")
    public OutputType getOutputType() {
        return outputType;
    }

    @JIPipeParameter("output-type")
    public void setOutputType(OutputType outputType) {
        this.outputType = outputType;
    }

    @SetJIPipeDocumentation(name = "Exclude on edges", description = "Whether to exclude edge maxima")
    @JIPipeParameter("exclude-on-edges")
    public boolean isExcludeOnEdges() {
        return excludeOnEdges;
    }

    @JIPipeParameter("exclude-on-edges")
    public void setExcludeOnEdges(boolean excludeOnEdges) {
        this.excludeOnEdges = excludeOnEdges;
    }

    @SetJIPipeDocumentation(name = "Input is EDM", description = "Whether the input image is a float Euclidean Distance Map (EDM).")
    @JIPipeParameter("input-is-edm")
    public boolean isInputIsEDM() {
        return inputIsEDM;
    }

    @JIPipeParameter("input-is-edm")
    public void setInputIsEDM(boolean inputIsEDM) {
        this.inputIsEDM = inputIsEDM;
    }

    /**
     * Available output types
     */
    public enum OutputType {
        SinglePoints(MaximumFinder.SINGLE_POINTS),
        InTolerance(MaximumFinder.IN_TOLERANCE),
        Segmented(MaximumFinder.SEGMENTED);

        private final int constant;

        OutputType(int constant) {
            this.constant = constant;
        }

        public int getConstant() {
            return constant;
        }
    }
}
