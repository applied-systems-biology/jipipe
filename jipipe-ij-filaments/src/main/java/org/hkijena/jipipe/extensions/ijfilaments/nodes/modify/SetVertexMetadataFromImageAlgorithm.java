package org.hkijena.jipipe.extensions.ijfilaments.nodes.modify;

import ij.ImagePlus;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.databatch.JIPipeSingleDataBatch;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.ijfilaments.FilamentsNodeTypeCategory;
import org.hkijena.jipipe.extensions.ijfilaments.datatypes.Filaments3DData;
import org.hkijena.jipipe.extensions.ijfilaments.util.FilamentVertex;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;

@JIPipeDocumentation(name = "Set filament vertex metadata from image", description = "Sets the vertex metadata of each vertex from the given input image. Please note that if the Z/C/T coordinates are set to zero, the value is extracted from the 0/0/0 slice.")
@JIPipeNode(nodeTypeCategory = FilamentsNodeTypeCategory.class, menuPath = "Modify")
@JIPipeInputSlot(value = Filaments3DData.class, slotName = "Filaments", autoCreate = true)
@JIPipeInputSlot(value = ImagePlusGreyscaleData.class, slotName = "Value", description = "The value is sourced from the pixels in this image", autoCreate = true)
@JIPipeOutputSlot(value = Filaments3DData.class, slotName = "Output", autoCreate = true)
public class SetVertexMetadataFromImageAlgorithm extends JIPipeIteratingAlgorithm {

    private String metadataKey = "key";

    public SetVertexMetadataFromImageAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public SetVertexMetadataFromImageAlgorithm(SetVertexMetadataFromImageAlgorithm other) {
        super(other);
        this.metadataKey = other.metadataKey;
    }

    @JIPipeDocumentation(name = "Metadata key", description = "The key/name of the metadata")
    @JIPipeParameter(value = "metadata-key", important = true)
    public String getMetadataKey() {
        return metadataKey;
    }

    @JIPipeParameter("metadata-key")
    public void setMetadataKey(String metadataKey) {
        this.metadataKey = metadataKey;
    }

    @Override
    protected void runIteration(JIPipeSingleDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        Filaments3DData filaments = new Filaments3DData(dataBatch.getInputData("Filaments", Filaments3DData.class, progressInfo));
        ImagePlus thickness = dataBatch.getInputData("Value", ImagePlusGreyscaleData.class, progressInfo).getImage();

        for (FilamentVertex vertex : filaments.vertexSet()) {
            int z = Math.max(0, vertex.getSpatialLocation().getZ());
            int c = Math.max(0, vertex.getNonSpatialLocation().getChannel());
            int t = Math.max(0, vertex.getNonSpatialLocation().getFrame());
            ImageProcessor ip = ImageJUtils.getSliceZero(thickness, c, z, t);
            float d = ip.getf(vertex.getSpatialLocation().getX(), vertex.getSpatialLocation().getY());
            vertex.setMetadata(metadataKey, d);
        }

        dataBatch.addOutputData(getFirstOutputSlot(), filaments, progressInfo);
    }
}
