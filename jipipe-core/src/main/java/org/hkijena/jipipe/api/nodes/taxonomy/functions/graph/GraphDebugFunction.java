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
 * Seed leaf of the Graph / Project branch: Debugging graph execution.
 */
public final class GraphDebugFunction implements JIPipeNodeTaxonomyFunction {

    @Override
    public String getId() {
        return "jipipe:function:graph-debug";
    }

    @Override
    public String getName() {
        return "Graph debugging";
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
        return "Debugging graph execution";
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
}
