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

package org.hkijena.jipipe.desktop.app.grapheditor.nodebrowser;

import org.hkijena.jipipe.api.nodes.taxonomy.JIPipeNodeTaxonomyFunction;

/**
 * The {@code userObject} marker of the synthetic, UI-only
 * {@code <function> / Patterns} row of the node browser's function tree.
 * <p>
 * System/Global/Project/Downloaded templates are filed under their taxonomy
 * function via {@code getFunctionIds()}, which mixed them into the same tree
 * level as the function's real nodes (the "templates spill into the
 * functions" clutter). This row groups a function's template leaves one
 * level below the function — the function level then shows only real nodes
 * plus (when it has templates) the {@code Patterns} child.
 * <p>
 * This is deliberately <em>not</em> a taxonomy function: the row is pure UI
 * grouping keyed on the entry type, so it must never be registered with, or
 * pruned by, the function registry / validation / dumper. It is created per
 * function by {@link NodeBrowserTreeController#attachNodeLeaves} only when
 * that function carries at least one template entry, and is removed again on
 * the next rebuild.
 * <p>
 * The class is free of any Swing and {@code JIPipe} instance dependency so
 * it can be reasoned about (and, if needed, tested) in isolation.
 */
public final class PatternsTreeNode {

    private final JIPipeNodeTaxonomyFunction function;

    /**
     * @param function the taxonomy function this row is a child of (the
     *                 templates listed under the row are the entries filed
     *                 under {@code function})
     */
    public PatternsTreeNode(JIPipeNodeTaxonomyFunction function) {
        this.function = function;
    }

    /**
     * @return the taxonomy function this row groups the templates of
     */
    public JIPipeNodeTaxonomyFunction getFunction() {
        return function;
    }
}
