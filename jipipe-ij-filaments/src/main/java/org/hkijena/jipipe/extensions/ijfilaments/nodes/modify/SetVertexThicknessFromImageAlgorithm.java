package org.hkijena.jipipe.extensions.ijfilaments.nodes.modify;

import ij.ImagePlus;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.RoiNodeTypeCategory;
import org.hkijena.jipipe.extensions.ijfilaments.FilamentsNodeTypeCategory;
import org.hkijena.jipipe.extensions.ijfilaments.datatypes.FilamentsData;
import org.hkijena.jipipe.extensions.ijfilaments.util.FilamentVertex;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;

@JIPipeDocumentation(name = "Set thickness from image", description = "Sets the thickness of each vertex from the given input image. Please note that if the Z/C/T coordinates are set to zero, the value is extracted from the 0/0/0 slice.")
@JIPipeNode(nodeTypeCategory = FilamentsNodeTypeCategory.class, menuPath = "Modify")
@JIPipeInputSlot(value = FilamentsData.class, slotName = "Filaments", autoCreate = true)
@JIPipeInputSlot(value = ImagePlusGreyscaleData.class, slotName = "Thickness", description = "The thickness is sources from the pixels in this image", autoCreate = true)
@JIPipeOutputSlot(value = FilamentsData.class, slotName = "Output", autoCreate = true)
public class SetVertexThicknessFromImageAlgorithm extends JIPipeIteratingAlgorithm {
    public SetVertexThicknessFromImageAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public SetVertexThicknessFromImageAlgorithm(SetVertexThicknessFromImageAlgorithm other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        FilamentsData filaments = new FilamentsData(dataBatch.getInputData("Filaments", FilamentsData.class, progressInfo));
        ImagePlus thickness = dataBatch.getInputData("Thickness", ImagePlusGreyscaleData.class, progressInfo).getImage();

        for (FilamentVertex vertex : filaments.vertexSet()) {
            int z = Math.max(0, vertex.getCentroid().getZ());
            int c = Math.max(0, vertex.getCentroid().getC());
            int t = Math.max(0, vertex.getCentroid().getT());
            ImageProcessor ip = ImageJUtils.getSliceZero(thickness, c, z, t);
            float d = ip.getf(vertex.getCentroid().getX(), vertex.getCentroid().getY());
            vertex.setThickness(d);
        }

        dataBatch.addOutputData(getFirstOutputSlot(), filaments, progressInfo);
    }
}