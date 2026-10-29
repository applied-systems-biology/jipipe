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
import org.hkijena.jipipe.api.nodes.taxonomy.JIPipeNodeTaxonomyInferencePattern;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.ImagesFunction;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Seed leaf of the Images branch: Applying filters to images.
 */
public final class FilteringFunction implements JIPipeNodeTaxonomyFunction {

    /**
     * The stable registry id of this function (a string literal per the stable-ID rule).
     */
    public static final String ID = "jipipe:function:filtering";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getName() {
        return "Filtering";
    }

    @Override
    public JIPipeNodeTaxonomyFunction getParent() {
        return new ImagesFunction();
    }

    @Override
    public String getDescription() {
        return "Applying filters to images";
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

    @Override
    public List<JIPipeNodeTaxonomyInferencePattern> getInferencePatterns() {
        return List.of(
                new JIPipeNodeTaxonomyInferencePattern("filter", 8f),
                new JIPipeNodeTaxonomyInferencePattern("blur", 1f),
                new JIPipeNodeTaxonomyInferencePattern("smooth", 1f),
                new JIPipeNodeTaxonomyInferencePattern("erode", 1f),
                new JIPipeNodeTaxonomyInferencePattern("dilate", 1f),
                new JIPipeNodeTaxonomyInferencePattern("morpholog", 1f),
                new JIPipeNodeTaxonomyInferencePattern("gaussian", 1f),
                new JIPipeNodeTaxonomyInferencePattern("median", 1f));
    }
}
