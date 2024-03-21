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
import mcib3d.image3d.segment.LocalThresholderDiff;
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

@SetJIPipeDocumentation(name = "3D spot segmentation (difference)", description = "The node works with two images, one containing the seeds of the objects, " +
        "that can be obtained from local maxima (see 3D Filters or 3D Maxima Finder), the other image containing signal data. " +
        "The program computes a local threshold around each seeds and cluster voxels with values higher than the local threshold computed. " +
        "A constant difference is used to compute local threshold. The local threshold value is then computed as ValueOfSeed – diff.")
@AddJIPipeCitation("https://mcib3d.frama.io/3d-suite-imagej/plugins/Segmentation/Custom/3D-Spots-Segmentation/")
@ConfigureJIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Threshold")
@AddJIPipeInputSlot(value = ImagePlusGreyscaleData.class, slotName = "Spots", create = true)
@AddJIPipeInputSlot(value = ImagePlusGreyscaleMaskData.class, slotName = "Seeds", create = true, optional = true, description = "Optional seeds")
@AddJIPipeOutputSlot(value = ImagePlusGreyscaleData.class, slotName = "Labels", create = true)
public class DifferenceSpotSegmentation3DAlgorithm extends SpotSegmentation3DAlgorithm {

    private float difference = 65;

    public DifferenceSpotSegmentation3DAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public DifferenceSpotSegmentation3DAlgorithm(DifferenceSpotSegmentation3DAlgorithm other) {
        super(other);
        this.difference = other.difference;
    }

    @SetJIPipeDocumentation(name = "Difference", description = "A constant difference is used to compute local threshold. " +
            "The local threshold value is then computed as ValueOfSeed – diff.")
    @JIPipeParameter(value = "difference", important = true)
    public float getDifference() {
        return difference;
    }

    @JIPipeParameter("difference")
    public void setDifference(float difference) {
        this.difference = difference;
    }

    @Override
    protected LocalThresholder createThresholder() {
        return new LocalThresholderDiff(difference);
    }
}
