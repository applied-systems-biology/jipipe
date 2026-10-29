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

package org.hkijena.jipipe.api.nodes.taxonomy.functions;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.nodes.taxonomy.HueCategory;
import org.hkijena.jipipe.api.nodes.taxonomy.JIPipeNodeTaxonomyFunction;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Collections;
import java.util.Set;

/**
 * Top-level branch for machine learning operations (deep learning and classical classifiers).
 * <p>
 * Its hue is auto-picked deterministically from the
 * {@link HueCategory#INTELLIGENCE} family (purple cell), per the spec B.5
 * migration: the historic 200° explicit hue was dropped in favor of the
 * family cell.
 */
public final class MachineLearningFunction implements JIPipeNodeTaxonomyFunction {

    /**
     * The stable registry id of this function (a string literal per the stable-ID rule).
     */
    public static final String ID = "jipipe:function:machine-learning";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getName() {
        return "Machine learning";
    }

    @Override
    @Nullable
    public JIPipeNodeTaxonomyFunction getParent() {
        return null;
    }

    @Override
    public HueCategory getHueCategory() {
        return HueCategory.INTELLIGENCE;
    }

    @Override
    public String getDescription() {
        return "Machine learning operations: classifying and segmenting images with neural networks (deep learning) or classical methods (random forests)";
    }

    @Override
    @Nullable
    public Icon getIcon() {
        return JIPipe.RESOURCES.getIcon16("actions/brain.png");
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
