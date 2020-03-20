package org.hkijena.acaq5.extensions.standardalgorithms.api.algorithms.enhancers;

import ij.ImagePlus;
import ij.plugin.filter.Binary;
import ij.plugin.filter.EDM;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.*;
import org.hkijena.acaq5.api.data.traits.BadForTrait;
import org.hkijena.acaq5.api.data.traits.GoodForTrait;
import org.hkijena.acaq5.api.data.traits.RemovesTrait;
import org.hkijena.acaq5.api.events.ParameterChangedEvent;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.extensions.biooobjects.api.traits.bioobject.count.ClusterBioObjects;
import org.hkijena.acaq5.extensions.biooobjects.api.traits.bioobject.morphology.RoundBioObjects;
import org.hkijena.acaq5.extensions.biooobjects.api.traits.bioobject.preparations.labeling.BioObjectsLabeling;
import org.hkijena.acaq5.extensions.biooobjects.api.traits.bioobject.preparations.labeling.UnlabeledBioObjects;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d2.greyscale.ImagePlus2DGreyscaleMaskData;

@ACAQDocumentation(name = "Watershed enhancer")
@AlgorithmMetadata(category = ACAQAlgorithmCategory.Enhancer)

// Algorithm flow
@AlgorithmInputSlot(value = ImagePlus2DGreyscaleMaskData.class, slotName = "Input image", autoCreate = true)
@AlgorithmOutputSlot(value = ImagePlus2DGreyscaleMaskData.class, slotName = "Output image", autoCreate = true)

// Trait matching
@GoodForTrait(RoundBioObjects.class)
@GoodForTrait(BioObjectsLabeling.class)
@BadForTrait(UnlabeledBioObjects.class)
@GoodForTrait(ClusterBioObjects.class)

// Trait configuration
@RemovesTrait(ClusterBioObjects.class)
public class WatershedMaskEnhancer extends ACAQIteratingAlgorithm {

    private int erosionIterations = 0;

    public WatershedMaskEnhancer(ACAQAlgorithmDeclaration declaration) {
        super(declaration);
    }

    public WatershedMaskEnhancer(WatershedMaskEnhancer other) {
        super(other);
        this.erosionIterations = other.erosionIterations;
    }

    @Override
    protected void runIteration(ACAQDataInterface dataInterface) {
        ImagePlus2DGreyscaleMaskData inputData = dataInterface.getInputData(getFirstInputSlot());
        ImagePlus img = inputData.getImage();

        EDM watershed = new EDM();
        ImagePlus result = img.duplicate();
        watershed.toWatershed(result.getProcessor());

        // Optional erosion steps
        Binary binaryFilter = new Binary();
        binaryFilter.setup("erode", null);
        for (int i = 0; i < erosionIterations; ++i) {
            binaryFilter.run(result.getProcessor());
        }

        dataInterface.addOutputData(getFirstOutputSlot(), new ImagePlus2DGreyscaleMaskData(result));
    }

    @ACAQParameter("erosion-iterations")
    @ACAQDocumentation(name = "Erosion iterations")
    public int getErosionIterations() {
        return erosionIterations;
    }

    @ACAQParameter("erosion-iterations")
    public void setErosionIterations(int erosionIterations) {
        this.erosionIterations = erosionIterations;
        getEventBus().post(new ParameterChangedEvent(this, "erosion-iterations"));
    }

    @Override
    public void reportValidity(ACAQValidityReport report) {

    }
}