package org.hkijena.jipipe.extensions.ij3d.nodes.roi3d.convert;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.extensions.ij3d.datatypes.ROI3DListData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;

@JIPipeDocumentation(name = "3D ROI to mask", description = "Converts 3D ROI lists to a mask")
@JIPipeInputSlot(value = ROI3DListData.class, slotName = "ROI", autoCreate = true)
@JIPipeInputSlot(value = ImagePlusData.class, slotName = "Reference", autoCreate = true, optional = true, description = "Optional reference image that determines the size of the output")
public class Roi3DToMaskAlgorithm extends JIPipeIteratingAlgorithm {
    public Roi3DToMaskAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public Roi3DToMaskAlgorithm(Roi3DToMaskAlgorithm other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {

    }
}
