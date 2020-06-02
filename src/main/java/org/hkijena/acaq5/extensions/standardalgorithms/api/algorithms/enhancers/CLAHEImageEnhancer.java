package org.hkijena.acaq5.extensions.standardalgorithms.api.algorithms.enhancers;

import ij.ImagePlus;
import mpicbg.ij.clahe.Flat;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;
import org.hkijena.acaq5.api.ACAQRunnerSubStatus;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmCategory;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmDeclaration;
import org.hkijena.acaq5.api.algorithm.ACAQDataInterface;
import org.hkijena.acaq5.api.algorithm.ACAQIteratingAlgorithm;
import org.hkijena.acaq5.api.algorithm.AlgorithmInputSlot;
import org.hkijena.acaq5.api.algorithm.AlgorithmOutputSlot;
import org.hkijena.acaq5.api.data.traits.GoodForTrait;
import org.hkijena.acaq5.api.data.traits.RemovesTrait;
import org.hkijena.acaq5.api.events.ParameterChangedEvent;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d2.greyscale.ImagePlus2DGreyscaleData;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Applies CLAHE image enhancing
 */
@ACAQDocumentation(name = "CLAHE enhancer (deprecated)")
@ACAQOrganization(algorithmCategory = ACAQAlgorithmCategory.Processor, menuPath = "Contrast")

// Algorithm flow
@AlgorithmInputSlot(value = ImagePlus2DGreyscaleData.class, slotName = "Input image", autoCreate = true)
@AlgorithmOutputSlot(value = ImagePlus2DGreyscaleData.class, slotName = "Output image", autoCreate = true)

// Trait matching
@GoodForTrait("bioobject-preparations-labeling-uniform")
@GoodForTrait("bioobject-preparations-labeling-membrane")
@GoodForTrait("image-quality-brightness-low")
@GoodForTrait("image-quality-brightness-nonuniform")

// Trait configuration
@RemovesTrait("image-quality-brightness-low")
@RemovesTrait("image-quality-brightness-nonuniform")
public class CLAHEImageEnhancer extends ACAQIteratingAlgorithm {

    private int blocks = 127;
    private int bins = 256;
    private float maxSlope = 3.0f;
    private boolean fastMode = false;

    /**
     * @param declaration the declaration
     */
    public CLAHEImageEnhancer(ACAQAlgorithmDeclaration declaration) {
        super(declaration);
    }

    /**
     * Copies the algorithm
     *
     * @param other the original
     */
    public CLAHEImageEnhancer(CLAHEImageEnhancer other) {
        super(other);
        this.blocks = other.blocks;
        this.bins = other.bins;
        this.maxSlope = other.maxSlope;
        this.fastMode = other.fastMode;
    }

    @Override
    protected void runIteration(ACAQDataInterface dataInterface, ACAQRunnerSubStatus subProgress, Consumer<ACAQRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        ImagePlus2DGreyscaleData inputData = dataInterface.getInputData(getFirstInputSlot(), ImagePlus2DGreyscaleData.class);
        ImagePlus result = inputData.getImage().duplicate();
        Flat clahe = fastMode ? Flat.getFastInstance() : Flat.getInstance();
        clahe.run(result, blocks, bins, maxSlope, null, true);
        dataInterface.addOutputData(getFirstOutputSlot(), new ImagePlus2DGreyscaleData(result));
    }

    @ACAQParameter("blocks")
    @ACAQDocumentation(name = "Blocks")
    public int getBlocks() {
        return blocks;
    }

    @ACAQParameter("blocks")
    public void setBlocks(int blocks) {
        this.blocks = blocks;
        getEventBus().post(new ParameterChangedEvent(this, "blocks"));
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