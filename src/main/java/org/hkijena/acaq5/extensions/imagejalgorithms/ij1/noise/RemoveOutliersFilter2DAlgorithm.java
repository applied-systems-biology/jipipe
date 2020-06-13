package org.hkijena.acaq5.extensions.imagejalgorithms.ij1.noise;

import ij.ImagePlus;
import ij.plugin.filter.RankFilters;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;
import org.hkijena.acaq5.api.ACAQRunnerSubStatus;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.*;
import org.hkijena.acaq5.api.data.ACAQMutableSlotConfiguration;
import org.hkijena.acaq5.api.events.ParameterChangedEvent;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.acaq5.utils.ImageJUtils;

import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.hkijena.acaq5.extensions.imagejalgorithms.ImageJAlgorithmsExtension.REMOVE_MASK_QUALIFIER;

/**
 * Wrapper around {@link RankFilters}
 */
@ACAQDocumentation(name = "Remove outliers 2D", description = "Filter that replaces pixel values by the median if they deviate too much from it. " +
        "If a multi-channel image is provided, the operation is applied to each channel. " +
        "If higher-dimensional data is provided, the filter is applied to each 2D slice.")
@ACAQOrganization(menuPath = "Math", algorithmCategory = ACAQAlgorithmCategory.Processor)
@AlgorithmInputSlot(value = ImagePlusData.class, slotName = "Input")
@AlgorithmOutputSlot(value = ImagePlusData.class, slotName = "Output")
public class RemoveOutliersFilter2DAlgorithm extends ACAQSimpleIteratingAlgorithm {

    private double radius = 1;
    private float threshold = 50;
    private Mode mode = Mode.RemoveSmallerThanMedian;

    /**
     * Instantiates a new algorithm.
     *
     * @param declaration the declaration
     */
    public RemoveOutliersFilter2DAlgorithm(ACAQAlgorithmDeclaration declaration) {
        super(declaration, ACAQMutableSlotConfiguration.builder().addInputSlot("Input", ImagePlusData.class)
                .addOutputSlot("Output", ImagePlusData.class, "Input", REMOVE_MASK_QUALIFIER)
                .allowOutputSlotInheritance(true)
                .seal()
                .build());
    }

    /**
     * Instantiates a new algorithm.
     *
     * @param other the other
     */
    public RemoveOutliersFilter2DAlgorithm(RemoveOutliersFilter2DAlgorithm other) {
        super(other);
        this.radius = other.radius;
        this.mode = other.mode;
        this.threshold = other.threshold;
    }

    @Override
    public boolean supportsParallelization() {
        return true;
    }

    @Override
    protected void runIteration(ACAQDataInterface dataInterface, ACAQRunnerSubStatus subProgress, Consumer<ACAQRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        ImagePlusData inputData = dataInterface.getInputData(getFirstInputSlot(), ImagePlusData.class);
        ImagePlus img = inputData.getImage().duplicate();
        RankFilters rankFilters = new RankFilters();
        ImageJUtils.forEachSlice(img, ip -> rankFilters.rank(ip,
                radius,
                RankFilters.OUTLIERS,
                mode == Mode.RemoveLargerThanMedian ? RankFilters.BRIGHT_OUTLIERS : RankFilters.DARK_OUTLIERS,
                threshold));
        dataInterface.addOutputData(getFirstOutputSlot(), new ImagePlusData(img));
    }


    @Override
    public void reportValidity(ACAQValidityReport report) {
        report.forCategory("Radius").checkIfWithin(this, radius, 0, Double.POSITIVE_INFINITY, false, true);
    }

    @ACAQDocumentation(name = "Radius", description = "Radius of the filter kernel. See ImageJ>Process>Filters>Show Circular Masks for a reference.")
    @ACAQParameter("radius")
    public double getRadius() {
        return radius;
    }

    @ACAQParameter("radius")
    public boolean setRadius(double radius) {
        if (radius <= 0) {
            return false;
        }
        this.radius = radius;
        getEventBus().post(new ParameterChangedEvent(this, "radius"));
        return true;
    }

    @ACAQDocumentation(name = "Mode", description = "Determines whether to modify pixels smaller or greater than the local median.")
    @ACAQParameter("mode")
    public Mode getMode() {
        return mode;
    }

    @ACAQParameter("mode")
    public void setMode(Mode mode) {
        this.mode = mode;
        getEventBus().post(new ParameterChangedEvent(this, "mode"));
    }

    @ACAQDocumentation(name = "Threshold", description = "Determines by how much a pixel has to deviate from the local median to be replaced by it.")
    @ACAQParameter("threshold")
    public float getThreshold() {
        return threshold;
    }

    @ACAQParameter("threshold")
    public boolean setThreshold(float threshold) {
        if (threshold < 0) {
            return false;
        }
        this.threshold = threshold;
        getEventBus().post(new ParameterChangedEvent(this, "threshold"));
        return true;
    }

    /**
     * The two different modes of this filter
     */
    public enum Mode {
        RemoveSmallerThanMedian,
        RemoveLargerThanMedian
    }
}
