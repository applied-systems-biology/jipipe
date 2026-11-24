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

package org.hkijena.jipipe.plugins.ij3d.nodes.segmentation;

import mcib3d.image3d.segment.LocalThresholder;
import mcib3d.image3d.segment.LocalThresholderGaussFit;
import org.hkijena.jipipe.api.AddJIPipeCitation;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.taxonomy.frameworks.IJ3DFramework;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.images.ThresholdingFunction;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleMaskData;

import static org.hkijena.jipipe.api.nodes.taxonomy.JIPipeNodeTaxonomySpatialDimension.SPATIAL_2D;
import static org.hkijena.jipipe.api.nodes.taxonomy.JIPipeNodeTaxonomySpatialDimension.SPATIAL_3D;

@SetJIPipeDocumentation(name = "IJ3D spot segmentation (Gaussian fit)", description = "Performs spot segmentation in the ImageJ 3D suite (IJ3D) by fitting a Gaussian to each object's radial distribution. It works with two images: one with the object seeds (local maxima), the other with signal data. The fit's standard deviation sets the threshold via a user factor; 1.17 reaches the full width at half maximum, 2 and 3 cover about 90% and 99% of the curve surface. Groups incoming data by annotations (one data item per slot).")
@AddJIPipeCitation("https://mcib3d.frama.io/3d-suite-imagej/plugins/Segmentation/Custom/3D-Spots-Segmentation/")
@ConfigureJIPipeNode(function = ThresholdingFunction.class, framework = IJ3DFramework.class, spatialDimensions = {SPATIAL_2D, SPATIAL_3D})
@AddJIPipeInputSlot(value = ImagePlusGreyscaleData.class, name = "Spots", create = true)
@AddJIPipeInputSlot(value = ImagePlusGreyscaleMaskData.class, name = "Seeds", create = true, optional = true, description = "Optional seeds")
@AddJIPipeOutputSlot(value = ImagePlusGreyscaleData.class, name = "Labels", create = true)
public class GaussianFitSpotSegmentation3DAlgorithm extends SpotSegmentation3DAlgorithm {
    private int maxRadius = 10;
    private double gaussPC = 1.0;

    public GaussianFitSpotSegmentation3DAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public GaussianFitSpotSegmentation3DAlgorithm(GaussianFitSpotSegmentation3DAlgorithm other) {
        super(other);
        this.maxRadius = other.maxRadius;
        this.gaussPC = other.gaussPC;
    }

    @Override
    protected LocalThresholder createThresholder() {
        return new LocalThresholderGaussFit(maxRadius, gaussPC);
    }

    @SetJIPipeDocumentation(name = "Maximum radius", description = "A gaussian fit of the radial distribution is computed in a given radius around the seed (Radius Max, in pixels))")
    @JIPipeParameter("max-radius")
    public int getMaxRadius() {
        return maxRadius;
    }

    @JIPipeParameter("max-radius")
    public void setMaxRadius(int maxRadius) {
        this.maxRadius = maxRadius;
    }

    @SetJIPipeDocumentation(name = "SD Value", description = "A factor\n" +
            "which is applied to the standard deviation to define the value of the\n" +
            "threshold (sd value). As a rule of the thumb, a factor 1.17 will bring the\n" +
            "threshold to the full width at half maximum while factors 2 and 3 will fill\n" +
            "about 90% and 99% of the curve surface, respectively.")
    @JIPipeParameter("gauss-pc")
    public double getGaussPC() {
        return gaussPC;
    }

    @JIPipeParameter("gauss-pc")
    public void setGaussPC(double gaussPC) {
        this.gaussPC = gaussPC;
    }
}
