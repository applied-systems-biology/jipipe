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
import mcib3d.image3d.segment.LocalThresholderMean;
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

@SetJIPipeDocumentation(name = "3D spot segmentation (local mean)", description = "The node works with two images, one containing the seeds of the objects, " +
        "that can be obtained from local maxima (see 3D Filters or 3D Maxima Finder), the other image containing signal data. " +
        "The program computes a local threshold around each seeds and cluster voxels with values higher than the local threshold computed. " +
        "Three circles are drawn. The user defines the radius (in pixels) of each\n" +
        "circle, given that the first circle should be located within the object, while\n" +
        "the two other circles should be located outside of the object. The mean\n" +
        "intensities of the object (within the first circle), and of the background (in\n" +
        "between the two other circles) are measured and the threshold calculated.\n" +
        "By default, the threshold is the mean of the two mean intensity values\n" +
        "(weight=0.5). The user can however shift this parameter: for a weight\n" +
        "value of 0.75, the threshold will be closer to the background value.")
@AddJIPipeCitation("https://mcib3d.frama.io/3d-suite-imagej/plugins/Segmentation/Custom/3D-Spots-Segmentation/")
@ConfigureJIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Threshold")
@AddJIPipeInputSlot(value = ImagePlusGreyscaleData.class, slotName = "Spots", create = true)
@AddJIPipeInputSlot(value = ImagePlusGreyscaleMaskData.class, slotName = "Seeds", create = true, optional = true, description = "Optional seeds")
@AddJIPipeOutputSlot(value = ImagePlusGreyscaleData.class, slotName = "Labels", create = true)
public class LocalMeanSpotSegmentation3DAlgorithm extends SpotSegmentation3DAlgorithm {

    private float radius0 = 2;

    private float radius1 = 4;

    private float radius2 = 6;

    private float weight = 0.5f;

    public LocalMeanSpotSegmentation3DAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public LocalMeanSpotSegmentation3DAlgorithm(LocalMeanSpotSegmentation3DAlgorithm other) {
        super(other);
        this.radius0 = other.radius0;
        this.radius1 = other.radius1;
        this.radius2 = other.radius2;
        this.weight = other.weight;
    }

    @Override
    protected LocalThresholder createThresholder() {
        return new LocalThresholderMean(radius0, radius1, radius2, weight);
    }

    @SetJIPipeDocumentation(name = "Radius 0 (inside object)", description = "The first circle (should be located within the object)")
    @JIPipeParameter(value = "radius0", important = true)
    public float getRadius0() {
        return radius0;
    }

    @JIPipeParameter("radius0")
    public void setRadius0(float radius0) {
        this.radius0 = radius0;
    }

    @SetJIPipeDocumentation(name = "Radius 1 (outside object)", description = "The second circle (should be located outside the object)")
    @JIPipeParameter(value = "radius1", important = true)
    public float getRadius1() {
        return radius1;
    }

    @JIPipeParameter("radius1")
    public void setRadius1(float radius1) {
        this.radius1 = radius1;
    }

    @SetJIPipeDocumentation(name = "Radius 2 (outside object)", description = "The third circle (should be located outside the object)")
    @JIPipeParameter(value = "radius2", important = true)
    public float getRadius2() {
        return radius2;
    }

    @JIPipeParameter("radius2")
    public void setRadius2(float radius2) {
        this.radius2 = radius2;
    }

    @SetJIPipeDocumentation(name = "Weight", description = "Allows to weight the two mean intensity values (0.5 is the default)")
    @JIPipeParameter(value = "weight", important = true)
    public float getWeight() {
        return weight;
    }

    @JIPipeParameter("weight")
    public void setWeight(float weight) {
        this.weight = weight;
    }
}
