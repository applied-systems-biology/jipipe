package org.hkijena.acaq5.extensions.imagejalgorithms.ij1.contrast;

import ij.ImagePlus;
import mpicbg.ij.clahe.Flat;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;
import org.hkijena.acaq5.api.ACAQRunnerSubStatus;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.*;
import org.hkijena.acaq5.api.data.ACAQMutableSlotConfiguration;
import org.hkijena.acaq5.api.events.ParameterChangedEvent;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.ImagePlusData;

import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.hkijena.acaq5.extensions.imagejalgorithms.ImageJAlgorithmsExtension.REMOVE_MASK_QUALIFIER;

/**
 * Applies CLAHE image enhancing
 */
@ACAQDocumentation(name = "Enhance local contrast (CLAHE)", description = "Applies 'Contrast Limited Adaptive Histogram Equalization' (CLAHE) to enhance contrast. " +
        "Composite color images are converted into their luminance.")
@ACAQOrganization(algorithmCategory = ACAQAlgorithmCategory.Processor, menuPath = "Contrast")

// Algorithm flow
@AlgorithmInputSlot(value = ImagePlusData.class, slotName = "Input")
@AlgorithmOutputSlot(value = ImagePlusData.class, slotName = "Output")

// Trait matching

// Trait configuration
public class CLAHEContrastEnhancer extends ACAQSimpleIteratingAlgorithm {

    private int blockRadius = 127;
    private int bins = 256;
    private float maxSlope = 3.0f;
    private boolean fastMode = false;

    /**
     * @param declaration the declaration
     */
    public CLAHEContrastEnhancer(ACAQAlgorithmDeclaration declaration) {
        super(declaration, ACAQMutableSlotConfiguration.builder().addInputSlot("Input", ImagePlusData.class)
                .addOutputSlot("Output", ImagePlusData.class, "Input", REMOVE_MASK_QUALIFIER)
                .allowOutputSlotInheritance(true)
                .seal()
                .build());
    }

    /**
     * Copies the algorithm
     *
     * @param other the original
     */
    public CLAHEContrastEnhancer(CLAHEContrastEnhancer other) {
        super(other);
        this.blockRadius = other.blockRadius;
        this.bins = other.bins;
        this.maxSlope = other.maxSlope;
        this.fastMode = other.fastMode;
    }

    @Override
    protected void runIteration(ACAQDataInterface dataInterface, ACAQRunnerSubStatus subProgress, Consumer<ACAQRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        ImagePlusData inputData = dataInterface.getInputData(getFirstInputSlot(), ImagePlusData.class);
        ImagePlus result = inputData.getImage().duplicate();
        Flat clahe = fastMode ? Flat.getFastInstance() : Flat.getInstance();
        clahe.run(result, blockRadius, bins, maxSlope, null, true);
        dataInterface.addOutputData(getFirstOutputSlot(), new ImagePlusData(result));
    }

    @ACAQParameter("block-radius")
    @ACAQDocumentation(name = "Blocks")
    public int getBlockRadius() {
        return blockRadius;
    }

    @ACAQParameter("block-radius")
    public void setBlockRadius(int blockRadius) {
        this.blockRadius = blockRadius;
        getEventBus().post(new ParameterChangedEvent(this, "block-radius"));
    }

    @ACAQParameter("bins")
    @ACAQDocumentation(name = "Bins")
    public int getBins() {
        return bins;
    }

    @ACAQParameter("bins")
    public void setBins(int bins) {
        this.bins = bins;
        getEventBus().post(new ParameterChangedEvent(this, "bins"));
    }

    @ACAQParameter("max-slope")
    @ACAQDocumentation(name = "Max slope")
    public float getMaxSlope() {
        return maxSlope;
    }

    @ACAQParameter("max-slope")
    public void setMaxSlope(float maxSlope) {
        this.maxSlope = maxSlope;
        getEventBus().post(new ParameterChangedEvent(this, "max-slope"));
    }

    @ACAQParameter("fast-mode")
    @ACAQDocumentation(name = "Fast mode")
    public boolean isFastMode() {
        return fastMode;
    }

    @ACAQParameter("fast-mode")
    public void setFastMode(boolean fastMode) {
        this.fastMode = fastMode;
        getEventBus().post(new ParameterChangedEvent(this, "fast-mode"));
    }

    @Override
    public void reportValidity(ACAQValidityReport report) {

    }
}