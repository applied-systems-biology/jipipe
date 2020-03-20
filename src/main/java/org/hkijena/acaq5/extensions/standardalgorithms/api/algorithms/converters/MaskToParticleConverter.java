package org.hkijena.acaq5.extensions.standardalgorithms.api.algorithms.converters;

import ij.measure.ResultsTable;
import ij.plugin.filter.ParticleAnalyzer;
import ij.plugin.frame.RoiManager;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.*;
import org.hkijena.acaq5.api.events.ParameterChangedEvent;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.ACAQROIData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.ACAQResultsTableData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d2.greyscale.ImagePlus2DGreyscaleMaskData;

import java.util.Arrays;

// Algorithm metadata
@ACAQDocumentation(name = "Convert mask to particles")
@AlgorithmMetadata(category = ACAQAlgorithmCategory.Converter)

// Algorithm data flow
@AlgorithmInputSlot(value = ImagePlus2DGreyscaleMaskData.class, slotName = "Mask", autoCreate = true)
@AlgorithmOutputSlot(value = ACAQROIData.class, slotName = "ROI", autoCreate = true)
@AlgorithmOutputSlot(value = ACAQResultsTableData.class, slotName = "Measurements", autoCreate = true)

// Algorithm traits
public class MaskToParticleConverter extends ACAQIteratingAlgorithm {
    private double minParticleSize = 0;
    private double maxParticleSize = Double.MAX_VALUE;
    private double minParticleCircularity = 0;
    private double maxParticleCircularity = 1;
    private boolean excludeEdges = false;

    public MaskToParticleConverter(ACAQAlgorithmDeclaration declaration) {
        super(declaration, null, null);
    }

    public MaskToParticleConverter(MaskToParticleConverter other) {
        super(other);
        this.minParticleSize = other.minParticleSize;
        this.maxParticleSize = other.maxParticleSize;
        this.minParticleCircularity = other.minParticleCircularity;
        this.maxParticleCircularity = other.maxParticleCircularity;
        this.excludeEdges = other.excludeEdges;
    }

    @Override
    protected void runIteration(ACAQDataInterface dataInterface) {
        ImagePlus2DGreyscaleMaskData inputData = dataInterface.getInputData(getFirstInputSlot());

        ResultsTable resultsTable = new ResultsTable();
        RoiManager manager = new RoiManager(true);
        ResultsTable table = new ResultsTable();
        ParticleAnalyzer.setRoiManager(manager);
        ParticleAnalyzer.setResultsTable(table);
        ParticleAnalyzer analyzer = new ParticleAnalyzer(0, 0, resultsTable, minParticleSize, maxParticleSize, minParticleCircularity, maxParticleCircularity);
        analyzer.analyze(inputData.getImage());

        dataInterface.addOutputData("ROI", new ACAQROIData(Arrays.asList(manager.getRoisAsArray())));
        dataInterface.addOutputData("Measurements", new ACAQResultsTableData(table));
    }

    @ACAQParameter("min-particle-size")
    @ACAQDocumentation(name = "Min particle size")
    public double getMinParticleSize() {
        return minParticleSize;
    }

    @ACAQParameter("min-particle-size")
    public void setMinParticleSize(double minParticleSize) {
        this.minParticleSize = minParticleSize;
        getEventBus().post(new ParameterChangedEvent(this, "min-particle-size"));
    }

    @ACAQParameter("max-particle-size")
    @ACAQDocumentation(name = "Max particle size")
    public double getMaxParticleSize() {
        return maxParticleSize;
    }

    @ACAQParameter("max-particle-size")
    public void setMaxParticleSize(double maxParticleSize) {
        this.maxParticleSize = maxParticleSize;
        getEventBus().post(new ParameterChangedEvent(this, "max-particle-size"));
    }

    @ACAQParameter("min-particle-circularity")
    @ACAQDocumentation(name = "Min particle circularity")
    public double getMinParticleCircularity() {
        return minParticleCircularity;
    }

    @ACAQParameter("min-particle-circularity")
    public boolean setMinParticleCircularity(double minParticleCircularity) {
        if (minParticleCircularity < 0 || minParticleCircularity > 1)
            return false;
        this.minParticleCircularity = minParticleCircularity;
        getEventBus().post(new ParameterChangedEvent(this, "min-particle-circularity"));
        return true;
    }

    @ACAQParameter("max-particle-circularity")
    @ACAQDocumentation(name = "Max particle circularity")
    public double getMaxParticleCircularity() {
        return maxParticleCircularity;
    }

    @ACAQParameter("max-particle-circularity")
    public boolean setMaxParticleCircularity(double maxParticleCircularity) {
        if (maxParticleCircularity < 0 || maxParticleCircularity > 1)
            return false;
        this.maxParticleCircularity = maxParticleCircularity;
        getEventBus().post(new ParameterChangedEvent(this, "max-particle-circularity"));
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
        getEventBus().post(new ParameterChangedEvent(this, "exclude-edges"));
    }

    @Override
    public void reportValidity(ACAQValidityReport report) {

    }
}
