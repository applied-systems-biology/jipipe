package org.hkijena.acaq5.extension.algorithms.converters;

import ij.measure.ResultsTable;
import ij.plugin.filter.ParticleAnalyzer;
import org.hkijena.acaq5.api.ACAQAlgorithmCategory;
import org.hkijena.acaq5.api.ACAQAlgorithmMetadata;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQSimpleAlgorithm;
import org.hkijena.acaq5.extension.dataslots.ACAQMaskDataSlot;
import org.hkijena.acaq5.extension.dataslots.ACAQROIDataSlot;
import org.hkijena.acaq5.extension.datatypes.ACAQMaskData;
import org.hkijena.acaq5.extension.datatypes.ACAQROIData;

@ACAQDocumentation(name = "Convert mask to particles")
@ACAQAlgorithmMetadata(category = ACAQAlgorithmCategory.Converter)
public class MaskToParticleConverter extends ACAQSimpleAlgorithm<ACAQMaskData, ACAQROIData> {
    private double minParticleSize = 0;
    private double maxParticleSize = Double.MAX_VALUE;
    private double minParticleCircularity = 0;
    private double maxParticleCircularity = 1;
    private boolean excludeEdges = false;

    public MaskToParticleConverter() {
        super("Mask", ACAQMaskDataSlot.class,
                "ROI", ACAQROIDataSlot.class);
    }

    @Override
    public void run() {
        ResultsTable resultsTable = new ResultsTable();
        ParticleAnalyzer analyzer = new ParticleAnalyzer(0, 0, resultsTable, minParticleSize, maxParticleSize, minParticleCircularity, maxParticleCircularity);
    }
}
