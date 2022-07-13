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

package org.hkijena.jipipe.extensions.imagejalgorithms.ij1.features;

import ij.ImagePlus;
import ij.ImageStack;
import ij.plugin.filter.MaximumFinder;
import ij.process.ByteProcessor;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeDefaultMutableSlotConfiguration;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImageJNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleMaskData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;

/**
 * Wrapper around {@link ij.plugin.filter.MaximumFinder}
 */
@JIPipeDocumentation(name = "Local maxima 2D", description = "Finds the local maxima of each image. " +
        "If higher-dimensional data is provided, the filter is applied to each 2D slice.")
@JIPipeNode(menuPath = "Features", nodeTypeCategory = ImagesNodeTypeCategory.class)
@JIPipeInputSlot(value = ImagePlusData.class, slotName = "Input")
@JIPipeOutputSlot(value = ImagePlusData.class, slotName = "Output")
@JIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "Process", aliasName = "Find Maxima...")
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
        super(info, JIPipeDefaultMutableSlotConfiguration.builder().addInputSlot("Input", "", ImagePlusGreyscaleData.class)
                .addOutputSlot("Output", "", ImagePlusGreyscaleMaskData.class, null)
                .allowOutputSlotInheritance(true)
                .seal()
                .build());
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
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ImagePlusData inputData = dataBatch.getInputData(getFirstInputSlot(), ImagePlusData.class, progressInfo);
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
        dataBatch.addOutputData(getFirstOutputSlot(), new ImagePlusData(resultImage), progressInfo);
    }

    @JIPipeDocumentation(name = "Height tolerance", description = "Maxima are accepted only if protruding more than this value from the ridge to a higher maximum")
    @JIPipeParameter("height-tolerance")
    public double getHeightTolerance() {
        return heightTolerance;
    }

    @JIPipeParameter("height-tolerance")
    public void setHeightTolerance(double heightTolerance) {
        this.heightTolerance = heightTolerance;
    }

    @JIPipeDocumentation(name = "Is strict", description = "When off, the global maximum is accepted even if all other pixel are less than " +
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

    @JIPipeDocumentation(name = "Threshold", description = "Minimum height of a maximum (uncalibrated)")
    @JIPipeParameter("threshold")
    public double getThreshold() {
        return threshold;
    }

    @JIPipeParameter("threshold")
    public void setThreshold(double threshold) {
        this.threshold = threshold;
    }

    @JIPipeDocumentation(name = "Output type", description = "Available output types: <table>" +
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

    @JIPipeDocumentation(name = "Exclude on edges", description = "Whether to exclude edge maxima")
    @JIPipeParameter("exclude-on-edges")
    public boolean isExcludeOnEdges() {
        return excludeOnEdges;
    }

    @JIPipeParameter("exclude-on-edges")
    public void setExcludeOnEdges(boolean excludeOnEdges) {
        this.excludeOnEdges = excludeOnEdges;
    }

    @JIPipeDocumentation(name = "Input is EDM", description = "Whether the input image is a float Euclidean Distance Map (EDM).")
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
