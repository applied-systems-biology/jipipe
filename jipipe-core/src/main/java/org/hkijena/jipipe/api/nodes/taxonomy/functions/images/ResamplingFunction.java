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
 * Seed leaf of the Images branch: Resampling images in space.
 */
public final class ResamplingFunction implements JIPipeNodeTaxonomyFunction {

    @Override
    public String getId() {
        return "jipipe:function:resampling";
    }

    @Override
    public String getName() {
        return "Resampling";
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
        return "Resampling images in space";
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
