package org.hkijena.jipipe.extensions.ijfilaments.nodes.modify;

import ij.ImagePlus;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.extensions.ijfilaments.FilamentsNodeTypeCategory;
import org.hkijena.jipipe.extensions.ijfilaments.datatypes.Filaments3DData;
import org.hkijena.jipipe.extensions.ijfilaments.util.FilamentVertex;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;

@JIPipeDocumentation(name = "Set filament vertex intensity from image", description = "Sets the intensity of each vertex from the given input image. Please note that if the C/T coordinates are set to zero, the value is extracted from the 0/0 slice.")
@JIPipeNode(nodeTypeCategory = FilamentsNodeTypeCategory.class, menuPath = "Modify")
@JIPipeInputSlot(value = Filaments3DData.class, slotName = "Filaments", autoCreate = true)
@JIPipeInputSlot(value = ImagePlusGreyscaleData.class, slotName = "Intensity", description = "The intensity is sourced from the pixels in this image", autoCreate = true)
@JIPipeOutputSlot(value = Filaments3DData.class, slotName = "Output", autoCreate = true)
public class SetVertexIntensityFromImageAlgorithm extends JIPipeIteratingAlgorithm {
    public SetVertexIntensityFromImageAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public SetVertexIntensityFromImageAlgorithm(SetVertexIntensityFromImageAlgorithm other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeProgressInfo progressInfo) {
        Filaments3DData filaments = new Filaments3DData(iterationStep.getInputData("Filaments", Filaments3DData.class, progressInfo));
        ImagePlus thickness = iterationStep.getInputData("Intensity", ImagePlusGreyscaleData.class, progressInfo).getImage();

        for (FilamentVertex vertex : filaments.vertexSet()) {
            int z = (int) Math.max(0, vertex.getSpatialLocation().getZ());
            int c = Math.max(0, vertex.getNonSpatialLocation().getChannel());
            int t = Math.max(0, vertex.getNonSpatialLocation().getFrame());
            ImageProcessor ip = ImageJUtils.getSliceZero(thickness, c, z, t);
            float d = ip.getf((int) vertex.getSpatialLocation().getX(), (int) vertex.getSpatialLocation().getY());
            vertex.setValue(d);
        }

        iterationStep.addOutputData(getFirstOutputSlot(), filaments, progressInfo);
    }
}
