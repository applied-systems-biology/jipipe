package org.hkijena.acaq5.extensions.imagejalgorithms.ij1.features;

import ij.ImagePlus;
import ij.ImageStack;
import ij.plugin.filter.MaximumFinder;
import ij.process.ByteProcessor;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;
import org.hkijena.acaq5.api.ACAQRunnerSubStatus;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.*;
import org.hkijena.acaq5.api.data.ACAQMutableSlotConfiguration;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleMaskData;
import org.hkijena.acaq5.utils.ImageJUtils;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Wrapper around {@link ij.plugin.filter.MaximumFinder}
 */
@ACAQDocumentation(name = "Local maxima 2D", description = "Finds the local maxima of each image. " +
        "If higher-dimensional data is provided, the filter is applied to each 2D slice.")
@ACAQOrganization(menuPath = "Features", algorithmCategory = ACAQAlgorithmCategory.Processor)
@AlgorithmInputSlot(value = ImagePlusData.class, slotName = "Input")
@AlgorithmOutputSlot(value = ImagePlusData.class, slotName = "Output")
public class LocalMaxima2DAlgorithm extends ACAQSimpleIteratingAlgorithm {

    private double heightTolerance = 10;
    private boolean strict = false;
    private double threshold = 0;
    private OutputType outputType = OutputType.SinglePoints;
    private boolean excludeOnEdges = false;
    private boolean inputIsEDM = false;

    /**
     * Instantiates a new algorithm.
     *
     * @param declaration the declaration
     */
    public LocalMaxima2DAlgorithm(ACAQAlgorithmDeclaration declaration) {
        super(declaration, ACAQMutableSlotConfiguration.builder().addInputSlot("Input", ImagePlusGreyscaleData.class)
                .addOutputSlot("Output", ImagePlusGreyscaleMaskData.class, null)
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
    protected void runIteration(ACAQDataInterface dataInterface, ACAQRunnerSubStatus subProgress, Consumer<ACAQRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        ImagePlusData inputData = dataInterface.getInputData(getFirstInputSlot(), ImagePlusData.class);
        ImagePlus img = inputData.getImage().duplicate();

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
        });

        ImagePlus resultImage = new ImagePlus("Output", resultStack);
        resultImage.setDimensions(img.getNChannels(), img.getNSlices(), img.getNFrames());

        dataInterface.addOutputData(getFirstOutputSlot(), new ImagePlusData(resultImage));
    }


    @Override
    public void reportValidity(ACAQValidityReport report) {
    }

    @ACAQDocumentation(name = "Height tolerance", description = "Maxima are accepted only if protruding more than this value from the ridge to a higher maximum")
    @ACAQParameter("height-tolerance")
    public double getHeightTolerance() {
        return heightTolerance;
    }

    @ACAQParameter("height-tolerance")
    public void setHeightTolerance(double heightTolerance) {
        this.heightTolerance = heightTolerance;
    }

    @ACAQDocumentation(name = "Is strict", description = "When off, the global maximum is accepted even if all other pixel are less than " +
            "'tolerance' below this level. With 'Exclude on Edges' enabled, 'strict' also " +
            "means that the surrounding of a maximum within 'tolerance' must not include an edge pixel " +
            "(otherwise, it is enough that there is no edge pixel with the maximum value).")
    @ACAQParameter("strict")
    public boolean isStrict() {
        return strict;
    }

    @ACAQParameter("strict")
    public void setStrict(boolean strict) {
        this.strict = strict;
    }

    @ACAQDocumentation(name = "Threshold", description = "Minimum height of a maximum (uncalibrated)")
    @ACAQParameter("threshold")
    public double getThreshold() {
        return threshold;
    }

    @ACAQParameter("threshold")
    public void setThreshold(double threshold) {
        this.threshold = threshold;
    }

    @ACAQDocumentation(name = "Output type", description = "Available output types: <table>" +
            "<tr><td>SinglePoints</td><td>Outputs single points</td></tr>" +
            "<tr><td>InTolerance</td><td>Outputs all points around the maximum within the tolerance</td></tr>" +
            "<tr><td>Segmented</td><td>Outputs a watershed-segmented image</td></tr></table>")
    @ACAQParameter("output-type")
    public OutputType getOutputType() {
        return outputType;
    }

    @ACAQParameter("output-type")
    public void setOutputType(OutputType outputType) {
        this.outputType = outputType;
    }

    @ACAQDocumentation(name = "Exclude on edges", description = "Whether to exclude edge maxima")
    @ACAQParameter("exclude-on-edges")
    public boolean isExcludeOnEdges() {
        return excludeOnEdges;
    }

    @ACAQParameter("exclude-on-edges")
    public void setExcludeOnEdges(boolean excludeOnEdges) {
        this.excludeOnEdges = excludeOnEdges;
    }

    @ACAQDocumentation(name = "Input is EDM", description = "Whether the input image is a float Euclidean Distance Map (EDM).")
    @ACAQParameter("input-is-edm")
    public boolean isInputIsEDM() {
        return inputIsEDM;
    }

    @ACAQParameter("input-is-edm")
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
