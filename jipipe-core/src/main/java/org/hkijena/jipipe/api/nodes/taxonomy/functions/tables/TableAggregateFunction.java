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

package org.hkijena.jipipe.api.nodes.taxonomy.functions.tables;

import org.hkijena.jipipe.api.nodes.taxonomy.JIPipeNodeTaxonomyFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.TablesFunction;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import java.util.Collections;
import java.util.Set;

/**
 * Seed leaf of the Tables branch: Aggregating table data.
 */
public final class TableAggregateFunction implements JIPipeNodeTaxonomyFunction {

    @Override
    public String getId() {
        return "jipipe:function:table-aggregate";
    }

    @Override
    public String getName() {
        return "Aggregate tables";
    }

    @Override
    public JIPipeNodeTaxonomyFunction getParent() {
        return new TablesFunction();
    }

    @Override
    @Nullable
    public Float getColorHue() {
        return null;
    }

    @Override
    public String getDescription() {
        return "Aggregating table data";
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
