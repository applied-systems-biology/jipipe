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

import mcib3d.image3d.segment.LocalThresholder;
import mcib3d.image3d.segment.LocalThresholderGaussFit;
import org.hkijena.jipipe.api.AddJIPipeCitation;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleMaskData;

@SetJIPipeDocumentation(name = "3D spot segmentation (Gaussian fit)", description = "The node works with two images, one containing the seeds of the objects, " +
        "that can be obtained from local maxima (see 3D Filters or 3D Maxima Finder), the other image containing signal data. " +
        "The program computes a local threshold around each seeds and cluster voxels with values higher than the local threshold computed. " +
        "First the radial distribution of the object is computed (see plugin 3D Radial\n" +
        "Distribution), as concentric circles centred on the seed define growing\n" +
        "regions of interest in which mean intensity values are measured. A\n" +
        "gaussian fit of the radial distribution is computed in a given radius around the seed (Radius Max, in pixels)). The standard deviation of the fitted\n" +
        "gaussian curve is used to define the threshold. The user enters a factor\n" +
        "which is applied to the standard deviation to define the value of the\n" +
        "threshold (sd value). As a rule of the thumb, a factor 1.17 will bring the\n" +
        "threshold to the full width at half maximum while factors 2 and 3 will fill\n" +
        "about 90% and 99% of the curve surface, respectively.")
@AddJIPipeCitation("https://mcib3d.frama.io/3d-suite-imagej/plugins/Segmentation/Custom/3D-Spots-Segmentation/")
@ConfigureJIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Threshold")
@AddJIPipeInputSlot(value = ImagePlusGreyscaleData.class, slotName = "Spots", create = true)
@AddJIPipeInputSlot(value = ImagePlusGreyscaleMaskData.class, slotName = "Seeds", create = true, optional = true, description = "Optional seeds")
@AddJIPipeOutputSlot(value = ImagePlusGreyscaleData.class, slotName = "Labels", create = true)
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
