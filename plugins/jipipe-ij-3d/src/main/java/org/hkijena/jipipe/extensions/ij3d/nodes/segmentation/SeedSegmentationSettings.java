/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.extensions.ij3d.nodes.segmentation;

import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;

public class SeedSegmentationSettings extends AbstractJIPipeParameterCollection {
    private float seedSegmentationRadius = 2;

    public SeedSegmentationSettings() {
    }

    public SeedSegmentationSettings(SeedSegmentationSettings other) {
        this.seedSegmentationRadius = other.seedSegmentationRadius;
    }

    @SetJIPipeDocumentation(name = "Seed radius", description = "The radius for the automatically detected seeds")
    @JIPipeParameter("radius")
    public float getRadius() {
        return seedSegmentationRadius;
    }

    @JIPipeParameter("radius")
    public void setRadius(float radius) {
        this.seedSegmentationRadius = radius;
    }
}
