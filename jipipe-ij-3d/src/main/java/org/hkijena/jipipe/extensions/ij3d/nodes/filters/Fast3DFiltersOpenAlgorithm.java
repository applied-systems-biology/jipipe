package org.hkijena.jipipe.extensions.ij3d.nodes.filters;

import mcib3d.image3d.processing.FastFilters3D;
import org.hkijena.jipipe.api.AddJIPipeCitation;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;

@SetJIPipeDocumentation(name = "Fast 3D morphological opening", description = "Calculates the morphological opening using an ellipsoidal neighbourhood.")
@ConfigureJIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Morphology")
@AddJIPipeInputSlot(value = ImagePlusGreyscaleData.class, slotName = "Input", create = true)
@AddJIPipeOutputSlot(value = ImagePlusGreyscaleData.class, slotName = "Output", create = true)
@AddJIPipeCitation("https://mcib3d.frama.io/3d-suite-imagej/plugins/Filters/3D-Fast-Filters/")
public class Fast3DFiltersOpenAlgorithm extends Fast3DFiltersAlgorithm {
    public Fast3DFiltersOpenAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public Fast3DFiltersOpenAlgorithm(Fast3DFiltersAlgorithm other) {
        super(other);
    }

    @Override
    protected int getFilterIndex() {
        return FastFilters3D.OPENGRAY;
    }
}
