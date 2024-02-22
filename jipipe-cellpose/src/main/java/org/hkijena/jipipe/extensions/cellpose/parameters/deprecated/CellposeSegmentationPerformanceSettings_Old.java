package org.hkijena.jipipe.extensions.cellpose.parameters.deprecated;

import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;

@Deprecated
public class CellposeSegmentationPerformanceSettings_Old extends AbstractJIPipeParameterCollection {
    private int batchSize = 8;
    private boolean tile = true;
    private double tileOverlap = 0.1;
    private boolean resample = false;

    public CellposeSegmentationPerformanceSettings_Old() {
    }

    public CellposeSegmentationPerformanceSettings_Old(CellposeSegmentationPerformanceSettings_Old other) {
        this.batchSize = other.batchSize;
        this.tile = other.tile;
        this.tileOverlap = other.tileOverlap;
        this.resample = other.resample;
    }

    @SetJIPipeDocumentation(name = "Batch size", description = "Number of 224x224 patches to run simultaneously on the GPU " +
            "(can make smaller or bigger depending on GPU memory usage)")
    @JIPipeParameter("batch-size")
    public int getBatchSize() {
        return batchSize;
    }

    @JIPipeParameter("batch-size")
    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    @SetJIPipeDocumentation(name = "Tile images", description = "Tiles image to ensure GPU/CPU memory usage limited (recommended)")
    @JIPipeParameter("tile")
    public boolean isTile() {
        return tile;
    }

    @JIPipeParameter("tile")
    public void setTile(boolean tile) {
        this.tile = tile;
    }

    @SetJIPipeDocumentation(name = "Tile overlap", description = "Fraction of overlap of tiles when computing flows")
    @JIPipeParameter("tile-overlap")
    public double getTileOverlap() {
        return tileOverlap;
    }

    @JIPipeParameter("tile-overlap")
    public void setTileOverlap(double tileOverlap) {
        this.tileOverlap = tileOverlap;
    }

    @SetJIPipeDocumentation(name = "Resample", description = "If enabled, run dynamics at original image size " +
            "(will be slower but create more accurate boundaries)")
    @JIPipeParameter("resample")
    public boolean isResample() {
        return resample;
    }

    @JIPipeParameter("resample")
    public void setResample(boolean resample) {
        this.resample = resample;
    }
}
