package org.hkijena.jipipe.extensions.ijfilaments.nodes.modify;

import ij.ImagePlus;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.DefineJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.ijfilaments.FilamentsNodeTypeCategory;
import org.hkijena.jipipe.extensions.ijfilaments.datatypes.Filaments3DData;
import org.hkijena.jipipe.extensions.ijfilaments.util.FilamentVertex;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;

@SetJIPipeDocumentation(name = "Set filament vertex metadata from image", description = "Sets the vertex metadata of each vertex from the given input image. Please note that if the Z/C/T coordinates are set to zero, the value is extracted from the 0/0/0 slice.")
@DefineJIPipeNode(nodeTypeCategory = FilamentsNodeTypeCategory.class, menuPath = "Modify")
@AddJIPipeInputSlot(value = Filaments3DData.class, slotName = "Filaments", create = true)
@AddJIPipeInputSlot(value = ImagePlusGreyscaleData.class, slotName = "Value", description = "The value is sourced from the pixels in this image", create = true)
@AddJIPipeOutputSlot(value = Filaments3DData.class, slotName = "Output", create = true)
public class SetVertexMetadataFromImageAlgorithm extends JIPipeIteratingAlgorithm {

    private String metadataKey = "key";

    public SetVertexMetadataFromImageAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public SetVertexMetadataFromImageAlgorithm(SetVertexMetadataFromImageAlgorithm other) {
        super(other);
        this.metadataKey = other.metadataKey;
    }

    @SetJIPipeDocumentation(name = "Metadata key", description = "The key/name of the metadata")
    @JIPipeParameter(value = "metadata-key", important = true)
    public String getMetadataKey() {
        return metadataKey;
    }

    @JIPipeParameter("metadata-key")
    public void setMetadataKey(String metadataKey) {
        this.metadataKey = metadataKey;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        Filaments3DData filaments = new Filaments3DData(iterationStep.getInputData("Filaments", Filaments3DData.class, progressInfo));
        ImagePlus thickness = iterationStep.getInputData("Value", ImagePlusGreyscaleData.class, progressInfo).getImage();

        for (FilamentVertex vertex : filaments.vertexSet()) {
            int z = (int) Math.max(0, vertex.getSpatialLocation().getZ());
            int c = Math.max(0, vertex.getNonSpatialLocation().getChannel());
            int t = Math.max(0, vertex.getNonSpatialLocation().getFrame());
            ImageProcessor ip = ImageJUtils.getSliceZero(thickness, c, z, t);
            float d = ip.getf((int) vertex.getSpatialLocation().getX(), (int) vertex.getSpatialLocation().getY());
            vertex.setMetadata(metadataKey, d);
        }

        iterationStep.addOutputData(getFirstOutputSlot(), filaments, progressInfo);
    }
}
