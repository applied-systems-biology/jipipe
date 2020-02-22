package org.hkijena.acaq5.extension.api.algorithms.enhancers;

import ij.ImagePlus;
import mpicbg.ij.clahe.Flat;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.*;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.api.traits.AutoTransferTraits;
import org.hkijena.acaq5.api.traits.GoodForTrait;
import org.hkijena.acaq5.api.traits.RemovesTrait;
import org.hkijena.acaq5.extension.api.dataslots.ACAQGreyscaleImageDataSlot;
import org.hkijena.acaq5.extension.api.datatypes.ACAQGreyscaleImageData;
import org.hkijena.acaq5.extension.api.traits.bioobject.preparations.labeling.MembraneLabeledBioObjects;
import org.hkijena.acaq5.extension.api.traits.bioobject.preparations.labeling.UniformlyLabeledBioObjects;
import org.hkijena.acaq5.extension.api.traits.quality.LowBrightnessQuality;
import org.hkijena.acaq5.extension.api.traits.quality.NonUniformBrightnessQuality;

@ACAQDocumentation(name = "CLAHE enhancer")
@AlgorithmMetadata(category = ACAQAlgorithmCategory.Enhancer)

// Algorithm flow
@AlgorithmInputSlot(value = ACAQGreyscaleImageDataSlot.class, slotName = "Input image", autoCreate = true)
@AlgorithmOutputSlot(value = ACAQGreyscaleImageDataSlot.class, slotName = "Output image", autoCreate = true)

// Trait matching
@GoodForTrait(UniformlyLabeledBioObjects.class)
@GoodForTrait(MembraneLabeledBioObjects.class)
@GoodForTrait(LowBrightnessQuality.class)
@GoodForTrait(NonUniformBrightnessQuality.class)

// Trait configuration
@AutoTransferTraits
@RemovesTrait(LowBrightnessQuality.class)
@RemovesTrait(NonUniformBrightnessQuality.class)
public class CLAHEImageEnhancer extends ACAQSimpleAlgorithm<ACAQGreyscaleImageData,
        ACAQGreyscaleImageData> {

    private int blocks = 127;
    private int bins = 256;
    private float maxSlope = 3.0f;
    private boolean fastMode = false;

    public CLAHEImageEnhancer() {
    }

    public CLAHEImageEnhancer(CLAHEImageEnhancer other) {
        super(other);
        this.blocks = other.blocks;
        this.bins = other.bins;
        this.maxSlope = other.maxSlope;
        this.fastMode = other.fastMode;
    }

    @Override
    public void run() {
        ImagePlus img = getInputSlot().getData().getImage();

        ImagePlus result = img.duplicate();
        Flat clahe = fastMode ? Flat.getFastInstance() : Flat.getInstance();
        clahe.run(result, blocks, bins, maxSlope, null, true);
        getOutputSlot().setData(new ACAQGreyscaleImageData(result));
    }

    @ACAQParameter("blocks")
    @ACAQDocumentation(name = "Blocks")
    public int getBlocks() {
        return blocks;
    }

    @ACAQParameter("blocks")
    public void setBlocks(int blocks) {
        this.blocks = blocks;
    }

    @ACAQParameter("bins")
    @ACAQDocumentation(name = "Bins")
    public int getBins() {
        return bins;
    }

    @ACAQParameter("bins")
    public void setBins(int bins) {
        this.bins = bins;
    }

    @ACAQParameter("max-slope")
    @ACAQDocumentation(name = "Max slope")
    public float getMaxSlope() {
        return maxSlope;
    }

    @ACAQParameter("max-slope")
    public void setMaxSlope(float maxSlope) {
        this.maxSlope = maxSlope;
    }

    @ACAQParameter("fast-mode")
    @ACAQDocumentation(name = "Fast mode")
    public boolean isFastMode() {
        return fastMode;
    }

    @ACAQParameter("fast-mode")
    public void setFastMode(boolean fastMode) {
        this.fastMode = fastMode;
    }

    @Override
    public void reportValidity(ACAQValidityReport report) {

    }
}