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

import org.hkijena.jipipe.api.nodes.taxonomy.HueCategory;
import org.hkijena.jipipe.api.nodes.taxonomy.JIPipeNodeTaxonomyFunction;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import java.util.Collections;
import java.util.Set;

/**
 * Top-level branch for running scripts and processes: the top-level member of
 * the {@link HueCategory#INTELLIGENCE} family.
 * <p>
 * Promoted from a leaf of the Miscellaneous branch (full-registry
 * classification audit, Phase 3) to a first-class top-level branch ("Scripting
 * is turned into a proper function", user ruling): its script/process leaves
 * re-parented to it, and it is the first in-tree user of the deterministic
 * hue auto-picker — its hue is not overridden but derived from the
 * Intelligence family cell ({@link JIPipeNodeTaxonomyFunction#getColorHue()}
 * default).
 */
public final class ScriptingFunction implements JIPipeNodeTaxonomyFunction {

    /** The stable registry id of this function (a string literal per the stable-ID rule). */
    public static final String ID = "jipipe:function:scripting";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getName() {
        return "Scripting";
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
        return "Nodes that run scripts and processes: Python, Jython and R scripts, ImageJ macros, expressions";
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
