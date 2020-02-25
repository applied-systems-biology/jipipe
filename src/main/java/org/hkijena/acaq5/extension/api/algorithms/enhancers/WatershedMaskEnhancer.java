package org.hkijena.acaq5.extension.api.algorithms.enhancers;

import ij.ImagePlus;
import ij.plugin.filter.Binary;
import ij.plugin.filter.EDM;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.*;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.api.traits.AutoTransferTraits;
import org.hkijena.acaq5.api.traits.BadForTrait;
import org.hkijena.acaq5.api.traits.GoodForTrait;
import org.hkijena.acaq5.api.traits.RemovesTrait;
import org.hkijena.acaq5.extension.api.dataslots.ACAQMaskDataSlot;
import org.hkijena.acaq5.extension.api.datatypes.ACAQMaskData;
import org.hkijena.acaq5.extension.api.traits.bioobject.count.ClusterBioObjects;
import org.hkijena.acaq5.extension.api.traits.bioobject.morphology.RoundBioObjects;
import org.hkijena.acaq5.extension.api.traits.bioobject.preparations.labeling.BioObjectsLabeling;
import org.hkijena.acaq5.extension.api.traits.bioobject.preparations.labeling.UnlabeledBioObjects;

@ACAQDocumentation(name = "Watershed enhancer")
@AlgorithmMetadata(category = ACAQAlgorithmCategory.Enhancer)

// Algorithm flow
@AlgorithmInputSlot(value = ACAQMaskDataSlot.class, slotName = "Input image", autoCreate = true)
@AlgorithmOutputSlot(value = ACAQMaskDataSlot.class, slotName = "Output image", autoCreate = true)

// Trait matching
@GoodForTrait(RoundBioObjects.class)
@GoodForTrait(BioObjectsLabeling.class)
@BadForTrait(UnlabeledBioObjects.class)
@GoodForTrait(ClusterBioObjects.class)

// Trait configuration
@AutoTransferTraits
@RemovesTrait(ClusterBioObjects.class)
public class WatershedMaskEnhancer extends ACAQSimpleAlgorithm<ACAQMaskData, ACAQMaskData> {

    private int erosionIterations = 0;

    public WatershedMaskEnhancer(ACAQAlgorithmDeclaration declaration) {
        super(declaration);
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

    @Override
    public void reportValidity(ACAQValidityReport report) {

    }
}