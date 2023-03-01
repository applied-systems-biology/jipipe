package org.hkijena.jipipe.extensions.ij3d.nodes.roi3d.convert;

import ij.IJ;
import ij.ImagePlus;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.RoiNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.ij3d.IJ3DUtils;
import org.hkijena.jipipe.extensions.ij3d.datatypes.ROI3D;
import org.hkijena.jipipe.extensions.ij3d.datatypes.ROI3DListData;
import org.hkijena.jipipe.extensions.ij3d.utils.Roi3DDrawer;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.color.ImagePlusColorRGBData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageSliceIndex;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JIPipeDocumentation(name = "3D ROI to labels", description = "Converts 3D ROI lists to a label image. Depending on the number of objects, an 8-bit, 16-bit, or 32-bit label image is generated.")
@JIPipeNode(nodeTypeCategory = RoiNodeTypeCategory.class, menuPath = "Convert")
@JIPipeInputSlot(value = ROI3DListData.class, slotName = "ROI", autoCreate = true)
@JIPipeInputSlot(value = ImagePlusData.class, slotName = "Image", autoCreate = true, optional = true, description = "Optional image where the objects are drawn on")
@JIPipeOutputSlot(value = ImagePlusColorRGBData.class, slotName = "Output", autoCreate = true)
public class Roi3DToRGBAlgorithm extends JIPipeIteratingAlgorithm {

    private final Roi3DDrawer drawer;

    public Roi3DToRGBAlgorithm(JIPipeNodeInfo info) {
        super(info);
        drawer = new Roi3DDrawer();
        registerSubParameter(drawer);
    }

    public Roi3DToRGBAlgorithm(Roi3DToRGBAlgorithm other) {
        super(other);
        this.drawer = new Roi3DDrawer(other.drawer);
        registerSubParameter(drawer);
    }

    @JIPipeDocumentation(name = "ROI rendering settings", description = "The following settings determine how the 3D ROI are rendered")
    @JIPipeParameter("drawer-settings")
    public Roi3DDrawer getDrawer() {
        return drawer;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ROI3DListData roi3DListData = dataBatch.getInputData("ROI", ROI3DListData.class, progressInfo);
        ImagePlusData referenceImage = dataBatch.getInputData("Reference", ImagePlusData.class, progressInfo);

        ImagePlus outputImage = drawer.renderToRGB(roi3DListData, referenceImage != null ? referenceImage.getImage() : null, progressInfo);
        dataBatch.addOutputData(getFirstOutputSlot(), new ImagePlusColorRGBData(outputImage), progressInfo);
    }
}