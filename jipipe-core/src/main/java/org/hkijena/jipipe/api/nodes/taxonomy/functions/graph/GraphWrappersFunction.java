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

package org.hkijena.jipipe.api.nodes.taxonomy.functions.graph;

import org.hkijena.jipipe.api.nodes.taxonomy.JIPipeNodeTaxonomyFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.GraphFunction;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import java.util.Collections;
import java.util.Set;

/**
 * Seed leaf of the Graph / Project branch: Wrapping graphs.
 */
public final class GraphWrappersFunction implements JIPipeNodeTaxonomyFunction {

    /** The stable registry id of this function (a string literal per the stable-ID rule). */
    public static final String ID = "jipipe:function:graph-wrappers";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getName() {
        return "Graph wrappers";
    }

    @Override
    public JIPipeNodeTaxonomyFunction getParent() {
        return new GraphFunction();
    }

    @Override
    @Nullable
    public Float getColorHue() {
        return null;
    }

    @Override
    public String getDescription() {
        return "Wrapping graphs";
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
        return false;
    }

    @Override
    public boolean isVisibleInCompartments() {
        return false;
    }

    /**
     * Internal nodes are not user-extractable, mirroring the legacy
     * {@code InternalNodeTypeCategory.canExtract()} = {@code false}. This is the
     * third member of the internal trio alongside
     * {@link #isVisibleInPipeline()} / {@link #isVisibleInCompartments()} (both
     * {@code false} here), so that
     * {@code JIPipeGraph.extract(..., withInternal = false)} keeps excluding
     * these nodes once their flags are read from the taxonomy function.
     */
    @Override
    public boolean canExtract() {
        return false;
    }
}
