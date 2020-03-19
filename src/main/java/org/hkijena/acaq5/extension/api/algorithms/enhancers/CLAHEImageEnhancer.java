package org.hkijena.acaq5.extension.api.algorithms.enhancers;

import ij.ImagePlus;
import mpicbg.ij.clahe.Flat;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.*;
import org.hkijena.acaq5.api.data.traits.GoodForTrait;
import org.hkijena.acaq5.api.data.traits.RemovesTrait;
import org.hkijena.acaq5.api.events.ParameterChangedEvent;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.extension.api.datatypes.ACAQGreyscaleImageData;
import org.hkijena.acaq5.extension.api.traits.bioobject.preparations.labeling.MembraneLabeledBioObjects;
import org.hkijena.acaq5.extension.api.traits.bioobject.preparations.labeling.UniformlyLabeledBioObjects;
import org.hkijena.acaq5.extension.api.traits.quality.LowBrightnessQuality;
import org.hkijena.acaq5.extension.api.traits.quality.NonUniformBrightnessQuality;

@ACAQDocumentation(name = "CLAHE enhancer")
@AlgorithmMetadata(category = ACAQAlgorithmCategory.Enhancer)

// Algorithm flow
@AlgorithmInputSlot(value = ACAQGreyscaleImageData.class, slotName = "Input image", autoCreate = true)
@AlgorithmOutputSlot(value = ACAQGreyscaleImageData.class, slotName = "Output image", autoCreate = true)

// Trait matching
@GoodForTrait(UniformlyLabeledBioObjects.class)
@GoodForTrait(MembraneLabeledBioObjects.class)
@GoodForTrait(LowBrightnessQuality.class)
@GoodForTrait(NonUniformBrightnessQuality.class)

// Trait configuration
@RemovesTrait(LowBrightnessQuality.class)
@RemovesTrait(NonUniformBrightnessQuality.class)
public class CLAHEImageEnhancer extends ACAQIteratingAlgorithm {

    private int blocks = 127;
    private int bins = 256;
    private float maxSlope = 3.0f;
    private boolean fastMode = false;

    public CLAHEImageEnhancer(ACAQAlgorithmDeclaration declaration) {
        super(declaration);
    }

    public CLAHEImageEnhancer(CLAHEImageEnhancer other) {
        super(other);
        this.blocks = other.blocks;
        this.bins = other.bins;
        this.maxSlope = other.maxSlope;
        this.fastMode = other.fastMode;
    }

    @Override
    protected void runIteration(ACAQDataInterface dataInterface) {
        ACAQGreyscaleImageData inputData = dataInterface.getInputData(getFirstInputSlot());
        ImagePlus result = inputData.getImage().duplicate();
        Flat clahe = fastMode ? Flat.getFastInstance() : Flat.getInstance();
        clahe.run(result, blocks, bins, maxSlope, null, true);
        dataInterface.addOutputData(getFirstOutputSlot(), new ACAQGreyscaleImageData(result));
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