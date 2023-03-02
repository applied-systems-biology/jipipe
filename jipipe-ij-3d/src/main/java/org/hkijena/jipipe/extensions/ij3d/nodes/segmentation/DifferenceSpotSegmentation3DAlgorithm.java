package org.hkijena.jipipe.extensions.ij3d.nodes.segmentation;

import mcib3d.image3d.segment.LocalThresholder;
import mcib3d.image3d.segment.LocalThresholderDiff;
import org.hkijena.jipipe.api.JIPipeCitation;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleMaskData;

@JIPipeDocumentation(name = "3D spot segmentation (difference)", description = "The node works with two images, one containing the seeds of the objects, " +
        "that can be obtained from local maxima (see 3D Filters or 3D Maxima Finder), the other image containing signal data. " +
        "The program computes a local threshold around each seeds and cluster voxels with values higher than the local threshold computed. " +
"A constant difference is used to compute local threshold. The local threshold value is then computed as ValueOfSeed – diff.")
@JIPipeCitation("https://mcib3d.frama.io/3d-suite-imagej/plugins/Segmentation/Custom/3D-Spots-Segmentation/")
@JIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Threshold")
@JIPipeInputSlot(value = ImagePlusGreyscaleData.class, slotName = "Spots", autoCreate = true)
@JIPipeInputSlot(value = ImagePlusGreyscaleMaskData.class, slotName = "Seeds", autoCreate = true, optional = true, description = "Optional seeds")
@JIPipeOutputSlot(value = ImagePlusGreyscaleData.class, slotName = "Labels", autoCreate = true)
public class DifferenceSpotSegmentation3DAlgorithm extends SpotSegmentation3DAlgorithm {

    private float difference = 65;

    public DifferenceSpotSegmentation3DAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public DifferenceSpotSegmentation3DAlgorithm(DifferenceSpotSegmentation3DAlgorithm other) {
        super(other);
        this.difference = other.difference;
    }

    @JIPipeDocumentation(name = "Difference", description = "A constant difference is used to compute local threshold. " +
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
