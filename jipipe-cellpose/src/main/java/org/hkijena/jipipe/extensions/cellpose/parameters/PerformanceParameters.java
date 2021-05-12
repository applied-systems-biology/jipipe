package org.hkijena.jipipe.extensions.cellpose.parameters;

import com.google.common.eventbus.EventBus;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.extensions.cellpose.CellPoseModel;

public class PerformanceParameters implements JIPipeParameterCollection {
    private final EventBus eventBus = new EventBus();

    private int batchSize = 8;
    private boolean tile = true;
    private double tileOverlap = 0.1;
    private boolean resample = false;

    public PerformanceParameters() {
    }

    public PerformanceParameters(PerformanceParameters other) {
        this.batchSize = other.batchSize;
        this.tile = other.tile;
        this.tileOverlap = other.tileOverlap;
        this.resample = other.resample;
    }

    @Override
    public EventBus getEventBus() {
        return eventBus;
    }

    @JIPipeDocumentation(name = "Batch size", description = "Number of 224x224 patches to run simultaneously on the GPU " +
            "(can make smaller or bigger depending on GPU memory usage)")
    @JIPipeParameter("batch-size")
    public int getBatchSize() {
        return batchSize;
    }

    @JIPipeParameter("batch-size")
    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    @JIPipeDocumentation(name = "Tile images", description = "Tiles image to ensure GPU/CPU memory usage limited (recommended)")
    @JIPipeParameter("tile")
    public boolean isTile() {
        return tile;
    }

    @JIPipeParameter("tile")
    public void setTile(boolean tile) {
        this.tile = tile;
    }

    @JIPipeDocumentation(name = "Tile overlap", description = "Fraction of overlap of tiles when computing flows")
    @JIPipeParameter("tile-overlap")
    public double getTileOverlap() {
        return tileOverlap;
    }

    @JIPipeParameter("tile-overlap")
    public void setTileOverlap(double tileOverlap) {
        this.tileOverlap = tileOverlap;
    }

    @JIPipeDocumentation(name = "Resample", description = "If enabled, run dynamics at original image size " +
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
