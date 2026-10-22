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
import org.hkijena.jipipe.api.nodes.taxonomy.JIPipeNodeTaxonomyFunction;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import java.util.Collections;
import java.util.Set;

/**
 * Top-level branch for graph/project-level nodes (the legacy
 * {@code org.hkijena.jipipe:internal} category).
 * <p>
 * This branch is <em>transparent</em>: {@link #getColorHue()} returns
 * {@code null} and {@link #isVisibleInCompartments()} returns {@code false},
 * matching the legacy {@code InternalNodeTypeCategory} behavior (its negative
 * hue case). Consumers render it using the theme's panel background instead of
 * a resolved hue.
 */
public final class GraphFunction implements JIPipeNodeTaxonomyFunction {

    /** The stable registry id of this function (a string literal per the stable-ID rule). */
    public static final String ID = "jipipe:function:graph";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getName() {
        return "Graph / Project";
    }

    @Override
    @Nullable
    public JIPipeNodeTaxonomyFunction getParent() {
        return null;
    }

    @Override
    @Nullable
    public Float getColorHue() {
        return null;
    }

    @Override
    public String getDescription() {
        return "Nodes that operate on the graph or project";
    }

    @Override
    @Nullable
    public Icon getIcon() {
        return JIPipe.RESOURCES.getIcon16("actions/configure.png");
    }

    @Override
    public Set<String> getTags() {
        return Collections.emptySet();
    }

    @Override
    public boolean isVisibleInPipeline() {
        return false;
    }

    @Override
    public boolean isVisibleInCompartments() {
        return false;
    }
}
