package org.hkijena.jipipe.extensions.ij3d.nodes.segmentation;

import mcib3d.image3d.segment.LocalThresholder;
import mcib3d.image3d.segment.LocalThresholderConstant;
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

@JIPipeDocumentation(name = "3D spot segmentation (constant)", description = "The node works with two images, one containing the seeds of the objects, " +
        "that can be obtained from local maxima (see 3D Filters or 3D Maxima Finder), the other image containing signal data. " +
        "The program computes a local threshold around each seeds and cluster voxels with values higher than the local threshold computed. " +
"The same threshold will be applied to all the objects. This threshold is defined by the user as Local Background")
@JIPipeCitation("https://mcib3d.frama.io/3d-suite-imagej/plugins/Segmentation/Custom/3D-Spots-Segmentation/")
@JIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Threshold")
@JIPipeInputSlot(value = ImagePlusGreyscaleData.class, slotName = "Spots", autoCreate = true)
@JIPipeInputSlot(value = ImagePlusGreyscaleMaskData.class, slotName = "Seeds", autoCreate = true, optional = true, description = "Optional seeds")
@JIPipeOutputSlot(value = ImagePlusGreyscaleData.class, slotName = "Labels", autoCreate = true)
public class ConstantSpotSegmentation3DAlgorithm extends SpotSegmentation3DAlgorithm {

    private int localBackground = 65;

    public ConstantSpotSegmentation3DAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public ConstantSpotSegmentation3DAlgorithm(ConstantSpotSegmentation3DAlgorithm other) {
        super(other);
        this.localBackground = other.localBackground;
    }

    @JIPipeDocumentation(name = "Local background", description = "The threshold that is applied to all objects")
    @JIPipeParameter(value = "local-background", important = true)
    public int getLocalBackground() {
        return localBackground;
    }

    @JIPipeParameter("local-background")
    public void setLocalBackground(int localBackground) {
        this.localBackground = localBackground;
    }

    @Override
    protected LocalThresholder createThresholder() {
        return new LocalThresholderConstant(localBackground);
    }
}
