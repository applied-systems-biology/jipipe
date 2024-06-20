/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.plugins.ij3d.nodes.filters;

import mcib3d.image3d.processing.FastFilters3D;
import org.hkijena.jipipe.api.AddJIPipeCitation;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;

@SetJIPipeDocumentation(name = "Fast 3D sobel filter", description = "Applies the 3D sobel filter using an ellipsoidal neighbourhood.")
@ConfigureJIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Edges")
@AddJIPipeInputSlot(value = ImagePlusGreyscaleData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = ImagePlusGreyscaleData.class, name = "Output", create = true)
@AddJIPipeCitation("https://mcib3d.frama.io/3d-suite-imagej/plugins/Filters/3D-Fast-Filters/")
public class Fast3DFiltersSobelAlgorithm extends Fast3DFiltersAlgorithm {
    public Fast3DFiltersSobelAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public Fast3DFiltersSobelAlgorithm(Fast3DFiltersAlgorithm other) {
        super(other);
    }

    @Override
    protected int getFilterIndex() {
        return FastFilters3D.SOBEL;
    }
}
