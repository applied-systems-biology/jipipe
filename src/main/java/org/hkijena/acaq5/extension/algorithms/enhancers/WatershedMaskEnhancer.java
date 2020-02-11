package org.hkijena.acaq5.extension.algorithms.enhancers;

import ij.ImagePlus;
import ij.plugin.filter.Binary;
import ij.plugin.filter.EDM;
import org.hkijena.acaq5.api.ACAQAlgorithmCategory;
import org.hkijena.acaq5.api.ACAQAlgorithmMetadata;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQSimpleAlgorithm;
import org.hkijena.acaq5.extension.dataslots.ACAQMaskDataSlot;
import org.hkijena.acaq5.extension.datatypes.ACAQMaskData;

@ACAQDocumentation(name = "Watershed enhancer")
@ACAQAlgorithmMetadata(category = ACAQAlgorithmCategory.Enhancer)
public class WatershedMaskEnhancer extends ACAQSimpleAlgorithm<ACAQMaskData, ACAQMaskData> {

    private int erosionIterations = 0;

    public WatershedMaskEnhancer() {
        super("Input image", ACAQMaskDataSlot.class,
            "Output image", ACAQMaskDataSlot.class);
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
}