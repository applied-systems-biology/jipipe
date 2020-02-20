package org.hkijena.acaq5.extension.api.algorithms.enhancers;

import ij.ImagePlus;
import ij.plugin.filter.Binary;
import ij.plugin.filter.EDM;
import org.hkijena.acaq5.api.*;
import org.hkijena.acaq5.api.traits.BadForTrait;
import org.hkijena.acaq5.api.traits.GoodForTrait;
import org.hkijena.acaq5.extension.api.dataslots.ACAQMaskDataSlot;
import org.hkijena.acaq5.extension.api.datatypes.ACAQMaskData;
import org.hkijena.acaq5.extension.api.traits.bioobject.ClusterBioObjects;
import org.hkijena.acaq5.extension.api.traits.bioobject.LabeledBioObjects;
import org.hkijena.acaq5.extension.api.traits.bioobject.RoundBioObjects;
import org.hkijena.acaq5.extension.api.traits.bioobject.UnlabeledBioObjects;

@ACAQDocumentation(name = "Watershed enhancer")
@ACAQAlgorithmMetadata(category = ACAQAlgorithmCategory.Enhancer)
@GoodForTrait(RoundBioObjects.class)
@GoodForTrait(LabeledBioObjects.class)
@BadForTrait(UnlabeledBioObjects.class)
@GoodForTrait(ClusterBioObjects.class)
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