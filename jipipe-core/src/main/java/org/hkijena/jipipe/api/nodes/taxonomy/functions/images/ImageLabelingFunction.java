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
import org.hkijena.jipipe.api.nodes.taxonomy.functions.ImagesFunction;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import java.util.Collections;
import java.util.Set;

/**
 * Leaf of the Images branch: images: Label images (connected components, Voronoi, label maps, label spots).
 * <p>
 * Added by the full-registry classification audit: the auto-discovered
 * ImageJ2/CLIJ2 nodes (and some curated ones) previously fell through the
 * name-keyword heuristic into the Miscellaneous catch-all.
 */
public final class ImageLabelingFunction implements JIPipeNodeTaxonomyFunction {

    /** The stable registry id of this function (a string literal per the stable-ID rule). */
    public static final String ID = "jipipe:function:image-labeling";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getName() {
        return "Image labeling";
    }

    @Override
    public JIPipeNodeTaxonomyFunction getParent() {
        return new ImagesFunction();
    }

    @Override
    @Nullable
    public Float getColorHue() {
        return null;
    }

    @Override
    public String getDescription() {
        return "Labeling of images: connected components, Voronoi labeling, label maps, and label spots";
    }

    @Override
    @Nullable
    public Icon getIcon() {
        return null;
    }

    @Override
    public Set<String> getTags() {
        return Collections.emptySet();
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
