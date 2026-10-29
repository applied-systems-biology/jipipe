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

package org.hkijena.jipipe.api.nodes.taxonomy.functions.images;

import org.hkijena.jipipe.api.nodes.taxonomy.JIPipeNodeTaxonomyFunction;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Set;

/**
 * Seed leaf of the Images branch: Segmenting cells in an image.
 */
public final class SegmentationCellBasedFunction implements JIPipeNodeTaxonomyFunction {

    /**
     * The stable registry id of this function (a string literal per the stable-ID rule).
     */
    public static final String ID = "jipipe:function:segmentation-cell-based";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getName() {
        return "Cell-based segmentation";
    }

    @Override
    public JIPipeNodeTaxonomyFunction getParent() {
        return new SegmentationFunction();
    }

    @Override
    public String getDescription() {
        return "Segmenting cells in an image";
    }

    @Override
    @Nullable
    public Icon getIcon() {
        return null;
    }

    @Override
    public Set<String> getTags() {
        return Set.of("cell-segmentation");
    }

    @Override
    public boolean isVisibleInPipeline() {
        return true;
    }

    @Override
    public boolean isVisibleInCompartments() {
        return false;
    }
}
