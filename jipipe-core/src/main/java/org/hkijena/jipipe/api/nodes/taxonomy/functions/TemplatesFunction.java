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
 * Leaf of the Miscellaneous branch: the catch-all placement for node
 * templates whose taxonomy function is unset or stale (the id no longer
 * resolves).
 * <p>
 * Added by the template-taxonomy integration (6.0.0, work item 1324): a
 * template's database entry reports {@link #ID} whenever its
 * {@code taxonomy-function} reference is not resolved, so every template
 * always has a defined function-tree placement. This is a real registered
 * function leaf (a stable literal id) that users can also pick explicitly in
 * the taxonomy-function picker.
 * <p>
 * Note: this leaf is distinct from the synthetic top-level
 * <em>Templates</em> branch of the node browser (a UI-only scope grouping
 * with System/Global/Project children) — see the template-taxonomy
 * integration design spec, §7.2.
 */
public final class TemplatesFunction implements JIPipeNodeTaxonomyFunction {

    /** The stable registry id of this function (a string literal per the stable-ID rule). */
    public static final String ID = "jipipe:function:templates";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getName() {
        return "Templates";
    }

    @Override
    public JIPipeNodeTaxonomyFunction getParent() {
        return new MiscellaneousFunction();
    }

    @Override
    public String getDescription() {
        return "Node templates without an explicit taxonomy function (the catch-all placement)";
    }

    @Override
    @Nullable
    public Icon getIcon() {
        return JIPipe.RESOURCES.getIcon16("actions/template.png");
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
