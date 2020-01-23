package org.hkijena.acaq5.algorithms.converters;

import ij.measure.ResultsTable;
import ij.plugin.filter.ParticleAnalyzer;
import org.hkijena.acaq5.ACAQInputDataSlot;
import org.hkijena.acaq5.ACAQOutputDataSlot;
import org.hkijena.acaq5.ACAQSimpleAlgorithm;
import org.hkijena.acaq5.datatypes.ACAQMaskData;
import org.hkijena.acaq5.datatypes.ACAQROIData;

public class MaskToParticleConverter extends ACAQSimpleAlgorithm<ACAQInputDataSlot<ACAQMaskData>,
        ACAQOutputDataSlot<ACAQROIData>> {
    private double minParticleSize = 0;
    private double maxParticleSize = Double.MAX_VALUE;
    private double minParticleCircularity = 0;
    private double maxParticleCircularity = 1;
    private boolean excludeEdges = false;

    public MaskToParticleConverter() {
        super(new ACAQInputDataSlot<>("Mask", ACAQMaskData.class),
                new ACAQOutputDataSlot<>("ROI", ACAQROIData.class));
    }

    @Override
    public void run() {
        ResultsTable resultsTable = new ResultsTable();
        ParticleAnalyzer analyzer = new ParticleAnalyzer(0, 0, resultsTable, minParticleSize, maxParticleSize, minParticleCircularity, maxParticleCircularity);
    }
}
