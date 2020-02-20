package org.hkijena.acaq5.extension.api.algorithms.enhancers;

import ij.ImagePlus;
import ij.plugin.filter.Binary;
import ij.plugin.filter.EDM;
import org.hkijena.acaq5.api.*;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmCategory;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmMetadata;
import org.hkijena.acaq5.api.algorithm.ACAQSimpleAlgorithm;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.api.traits.AutoTransferTraits;
import org.hkijena.acaq5.api.traits.BadForTrait;
import org.hkijena.acaq5.api.traits.GoodForTrait;
import org.hkijena.acaq5.api.traits.RemovesTrait;
import org.hkijena.acaq5.extension.api.dataslots.ACAQMaskDataSlot;
import org.hkijena.acaq5.extension.api.datatypes.ACAQMaskData;
import org.hkijena.acaq5.extension.api.traits.bioobject.morphology.ClusterBioObjects;
import org.hkijena.acaq5.extension.api.traits.bioobject.preparations.labeling.LabeledBioObjects;
import org.hkijena.acaq5.extension.api.traits.bioobject.morphology.RoundBioObjects;
import org.hkijena.acaq5.extension.api.traits.bioobject.preparations.labeling.UnlabeledBioObjects;

@ACAQDocumentation(name = "Watershed enhancer")
@ACAQAlgorithmMetadata(category = ACAQAlgorithmCategory.Enhancer)

// Trait matching
@GoodForTrait(RoundBioObjects.class)
@GoodForTrait(LabeledBioObjects.class)
@BadForTrait(UnlabeledBioObjects.class)
@GoodForTrait(ClusterBioObjects.class)

// Trait configuration
@AutoTransferTraits
@RemovesTrait(ClusterBioObjects.class)
public class WatershedMaskEnhancer extends ACAQSimpleAlgorithm<ACAQMaskData, ACAQMaskData> {

    private int erosionIterations = 0;

    public WatershedMaskEnhancer() {
        super("Input image", ACAQMaskDataSlot.class,
            "Output image", ACAQMaskDataSlot.class);
    }

    public WatershedMaskEnhancer(WatershedMaskEnhancer other) {
        super(other);
        this.erosionIterations = other.erosionIterations;
    }

    @Override
    public void run() {
        ImagePlus img = getInputSlot().getData().getImage();

        EDM watershed = new EDM();
        ImagePlus result = img.duplicate();
        watershed.toWatershed(result.getProcessor());

        // Optional erosion steps
        Binary binaryFilter = new Binary();
        binaryFilter.setup("erode", null);
        for(int i = 0; i < erosionIterations; ++i) {
            binaryFilter.run(result.getProcessor());
        }

        getOutputSlot().setData(new ACAQMaskData(result));
    }

    @ACAQParameter("erosion-iterations")
    @ACAQDocumentation(name = "Erosion iterations")
    public int getErosionIterations() {
        return erosionIterations;
    }

    @ACAQParameter("erosion-iterations")
    public void setErosionIterations(int erosionIterations) {
        this.erosionIterations = erosionIterations;
    }
}