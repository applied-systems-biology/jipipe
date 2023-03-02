package org.hkijena.jipipe.extensions.ij3d.nodes.segmentation;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;

public class SeedSegmentationSettings extends AbstractJIPipeParameterCollection {
    private float seedSegmentationRadius = 2;

    public SeedSegmentationSettings() {
    }

    public SeedSegmentationSettings(SeedSegmentationSettings other) {
        this.seedSegmentationRadius = other.seedSegmentationRadius;
    }

    @JIPipeDocumentation(name = "Seed radius", description = "The radius for the automatically detected seeds")
    @JIPipeParameter("radius")
    public float getRadius() {
        return seedSegmentationRadius;
    }

    @JIPipeParameter("radius")
    public void setRadius(float radius) {
        this.seedSegmentationRadius = radius;
    }
}
