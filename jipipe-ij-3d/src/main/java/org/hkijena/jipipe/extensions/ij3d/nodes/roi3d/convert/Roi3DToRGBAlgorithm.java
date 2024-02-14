package org.hkijena.jipipe.extensions.ij3d.nodes.roi3d.convert;

import ij.ImagePlus;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.RoiNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.ij3d.datatypes.ROI3DListData;
import org.hkijena.jipipe.extensions.ij3d.utils.Roi3DDrawer;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.color.ImagePlusColorRGBData;

@JIPipeDocumentation(name = "Convert 3D ROI to RGB", description = "Converts 3D ROI lists to a label image. Depending on the number of objects, an 8-bit, 16-bit, or 32-bit label image is generated.")
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
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ROI3DListData roi3DListData = iterationStep.getInputData("ROI", ROI3DListData.class, progressInfo);
        ImagePlusData referenceImage = iterationStep.getInputData("Image", ImagePlusData.class, progressInfo);

        ImagePlus outputImage = drawer.draw(roi3DListData, referenceImage != null ? referenceImage.getImage() : null, progressInfo);
        iterationStep.addOutputData(getFirstOutputSlot(), new ImagePlusColorRGBData(outputImage), progressInfo);
    }
}