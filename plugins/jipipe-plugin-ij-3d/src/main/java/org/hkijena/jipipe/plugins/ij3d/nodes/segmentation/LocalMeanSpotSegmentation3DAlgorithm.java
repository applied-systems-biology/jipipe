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
import mcib3d.image3d.segment.LocalThresholderMean;
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

@SetJIPipeDocumentation(name = "IJ3D spot segmentation (local mean)", description = "Performs spot segmentation in the ImageJ 3D suite (IJ3D) from local means around each seed. It works with two images: one with the object seeds (local maxima), the other with signal data. Three circles are drawn: one inside the object, two outside it. The threshold is a weighted mean of the object mean (inside) and the background mean (outside); weight 0.5 by default, higher weights move it toward the background. Groups incoming data by annotations (one data item per slot).")
@AddJIPipeCitation("https://mcib3d.frama.io/3d-suite-imagej/plugins/Segmentation/Custom/3D-Spots-Segmentation/")
@ConfigureJIPipeNode(function = ThresholdingFunction.class, framework = IJ3DFramework.class, spatialDimensions = {SPATIAL_2D, SPATIAL_3D})
@AddJIPipeInputSlot(value = ImagePlusGreyscaleData.class, name = "Spots", create = true)
@AddJIPipeInputSlot(value = ImagePlusGreyscaleMaskData.class, name = "Seeds", create = true, optional = true, description = "Optional seeds")
@AddJIPipeOutputSlot(value = ImagePlusGreyscaleData.class, name = "Labels", create = true)
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
