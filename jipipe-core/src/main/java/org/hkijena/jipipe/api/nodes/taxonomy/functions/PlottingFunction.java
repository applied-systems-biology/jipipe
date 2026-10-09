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
 * Top-level branch for plotting operations.
 */
public final class PlottingFunction implements JIPipeNodeTaxonomyFunction {

    @Override
    public String getId() {
        return "jipipe:function:plotting";
    }

    @Override
    public String getName() {
        return "Plotting";
    }

    @Override
    @Nullable
    public JIPipeNodeTaxonomyFunction getParent() {
        return null;
    }

    @Override
    public Float getColorHue() {
        return 155.0f / 360.0f;
    }

    @Override
    public String getDescription() {
        return "Plotting operations";
    }

    @Override
    @Nullable
    public Icon getIcon() {
        return JIPipe.RESOURCES.getIcon16("actions/chart-line.png");
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
