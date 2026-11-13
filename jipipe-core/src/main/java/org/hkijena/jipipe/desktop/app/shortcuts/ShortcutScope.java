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

package org.hkijena.jipipe.desktop.app.shortcuts;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.util.Objects;

/**
 * A context scope for keyboard shortcut dispatch: a rooted UI region whose
 * {@link Resolver} decides which shortcut actions are available inside it.
 * <p>
 * Scopes are registered with the {@link JIPipeDesktopShortcutDispatcher};
 * when a key press resolves to an action id, the dispatcher walks all scopes
 * that contain the current focus owner, innermost first, and fires the first
 * non-null result of their resolvers. Typical scopes are a graph editor (its
 * context actions and tools), a dock panel (open / close / switch), and the
 * project window (menu accelerators).
 */
public final class ShortcutScope {
    /**
     * Resolves a shortcut action id to the runnable that performs it in this
     * scope, or {@code null} when the action is not available here (no
     * matching context action, nothing selected, ...). The dispatcher falls
     * through to the next scope on a {@code null} result.
     */
    public interface Resolver {
        /**
         * @param actionId the id of the resolved shortcut action
         * @return the runnable to fire, or {@code null} when unavailable
         */
        Runnable resolve(String actionId);
    }

    private final JComponent root;
    private final Resolver resolver;

    /**
     * @param root the top-level component of this scope (the dispatcher
     *            considers a focus owner inside it)
     * @param resolver resolves action ids to runnables available in this scope
     */
    public ShortcutScope(JComponent root, Resolver resolver) {
        this.root = Objects.requireNonNull(root, "root");
        this.resolver = Objects.requireNonNull(resolver, "resolver");
    }

    /**
     * @return the root component of this scope
     */
    public JComponent root() {
        return root;
    }

    /**
     * @return the resolver of this scope
     */
    public Resolver resolver() {
        return resolver;
    }

    /**
     * @param focusOwner the component currently holding the keyboard focus
     * @return true when {@code focusOwner} is the scope root or one of its
     * descendants
     */
    public boolean contains(Component focusOwner) {
        return focusOwner != null && SwingUtilities.isDescendingFrom(focusOwner, root);
    }
}
