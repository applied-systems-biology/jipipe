package org.hkijena.acaq5.extension.api.algorithms.converters;

import ij.ImagePlus;
import ij.measure.ResultsTable;
import ij.plugin.filter.ParticleAnalyzer;
import ij.plugin.frame.RoiManager;
import org.hkijena.acaq5.api.*;
import org.hkijena.acaq5.extension.api.dataslots.ACAQMaskDataSlot;
import org.hkijena.acaq5.extension.api.dataslots.ACAQROIDataSlot;
import org.hkijena.acaq5.extension.api.datatypes.ACAQMaskData;
import org.hkijena.acaq5.extension.api.datatypes.ACAQROIData;

import java.util.Arrays;

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

    public MaskToParticleConverter(MaskToParticleConverter other) {
        this();
        this.getSlotConfiguration().setTo(other.getSlotConfiguration());
        this.minParticleSize = other.minParticleSize;
        this.maxParticleSize = other.maxParticleSize;
        this.minParticleCircularity = other.minParticleCircularity;
        this.maxParticleCircularity = other.maxParticleCircularity;
        this.excludeEdges = other.excludeEdges;
    }

    @Override
    public void run() {
        ImagePlus inputImage = getInputData().getImage();

        ResultsTable resultsTable = new ResultsTable();
        RoiManager manager = new RoiManager(true);
        ParticleAnalyzer.setRoiManager(manager);
        ParticleAnalyzer analyzer = new ParticleAnalyzer(0, 0, resultsTable, minParticleSize, maxParticleSize, minParticleCircularity, maxParticleCircularity);
        analyzer.analyze(inputImage);

        setOutputData(new ACAQROIData(Arrays.asList(manager.getRoisAsArray())));
    }

    @ACAQParameter("min-particle-size")
    @ACAQDocumentation(name = "Min particle size")
    public double getMinParticleSize() {
        return minParticleSize;
    }

    @ACAQParameter("min-particle-size")
    public void setMinParticleSize(double minParticleSize) {
        this.minParticleSize = minParticleSize;
    }

    @ACAQParameter("max-particle-size")
    @ACAQDocumentation(name = "Max particle size")
    public double getMaxParticleSize() {
        return maxParticleSize;
    }

    @ACAQParameter("max-particle-size")
    public void setMaxParticleSize(double maxParticleSize) {
        this.maxParticleSize = maxParticleSize;
    }

    @ACAQParameter("min-particle-circularity")
    @ACAQDocumentation(name = "Min particle circularity")
    public double getMinParticleCircularity() {
        return minParticleCircularity;
    }

    @ACAQParameter("min-particle-circularity")
    public boolean setMinParticleCircularity(double minParticleCircularity) {
        if(minParticleCircularity < 0 || minParticleCircularity > 1)
            return false;
        this.minParticleCircularity = minParticleCircularity;
        return true;
    }

    @ACAQParameter("max-particle-circularity")
    @ACAQDocumentation(name = "Max particle circularity")
    public double getMaxParticleCircularity() {
        return maxParticleCircularity;
    }

    @ACAQParameter("max-particle-circularity")
    public boolean setMaxParticleCircularity(double maxParticleCircularity) {
        if(maxParticleCircularity < 0 || maxParticleCircularity > 1)
            return false;
        this.maxParticleCircularity = maxParticleCircularity;
        return true;
    }

    @ACAQParameter("exclude-edges")
    @ACAQDocumentation(name = "Exclude edges")
    public boolean isExcludeEdges() {
        return excludeEdges;
    }

    @ACAQParameter("exclude-edges")
    public void setExcludeEdges(boolean excludeEdges) {
        this.excludeEdges = excludeEdges;
    }
}
