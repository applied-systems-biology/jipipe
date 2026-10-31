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

import org.hkijena.jipipe.api.nodes.database.entries.TemplateScope;

/**
 * The {@code userObject} marker of the synthetic, UI-only
 * {@code Templates / <scope>} branch of the node browser's function tree
 * (spec §7.2).
 * <p>
 * This is deliberately <em>not</em> a taxonomy function: the synthetic
 * branch is pure UI grouping keyed on the entry type + its
 * {@link TemplateScope}, so it must never be registered with, or pruned by,
 * the function registry / validation / dumper. The node browser's
 * {@link NodeBrowserTreeController#attachNodeLeaves} special-cases this
 * marker to list the matching template entries under it (the "scope
 * placement"), in addition to their free taxonomy-function placement.
 * <p>
 * The class is free of any Swing and {@code JIPipe} instance dependency so
 * it can be reasoned about (and, if needed, tested) in isolation.
 */
public final class TemplateScopeTreeNode {

    /**
     * The sentinel {@code userObject} of the synthetic branch's header row
     * ("Templates"). Its scope is {@code null} - the header only forwards
     * the leaf-attachment walk to its {@link TemplateScope children}.
     */
    public static final TemplateScopeTreeNode ROOT = new TemplateScopeTreeNode(null);

    private final TemplateScope scope;

    /**
     * @param scope the template scope this row groups ({@code null} for the
     *              {@link #ROOT} header)
     */
    private TemplateScopeTreeNode(TemplateScope scope) {
        this.scope = scope;
    }

    /**
     * @return a fresh row grouping the given template scope
     */
    public static TemplateScopeTreeNode forScope(TemplateScope scope) {
        return new TemplateScopeTreeNode(scope);
    }

    /**
     * @return the branch header sentinel (scope {@code null})
     */
    public static TemplateScopeTreeNode getRoot() {
        return ROOT;
    }

    /**
     * @return the template scope this row groups, or {@code null} for the
     * {@link #ROOT} header
     */
    public TemplateScope getScope() {
        return scope;
    }

    /**
     * @return {@code true} for the {@link #ROOT} header (scope {@code null}),
     * which forwards to its scope children rather than listing
     * entries itself
     */
    public boolean isRoot() {
        return scope == null;
    }
}
