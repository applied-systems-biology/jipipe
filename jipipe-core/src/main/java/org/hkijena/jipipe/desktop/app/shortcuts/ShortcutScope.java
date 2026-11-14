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
import java.awt.KeyboardFocusManager;
import java.awt.Window;
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
 * <p>
 * Containment is descending by default: the focus owner must be the scope
 * root or one of its descendants. Scopes whose actions are window-level UI
 * (dock toggles, the command bar trigger) are created through the
 * {@link #windowLevel(JComponent, Resolver)} factory instead: they contain
 * the focus wherever inside the hosting window it is, including the
 * transient states in which the focus owner is the window itself or
 * {@code null} (e.g. right after a tab switch), where no component descends
 * from any in-window root. Only scopes whose actions are window-level may
 * opt in: a scope whose actions depend on which part of the window is
 * visible (the graph editor's context actions and tools) must stay
 * descending, because hidden editor tabs share their window with the
 * visible one and a window-level scope of a hidden tab would also contain
 * the focus and could hijack the visible tab's canvas actions.
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
    private final boolean windowLevel;

    /**
     * @param root the top-level component of this scope (the dispatcher
     *            considers a focus owner inside it)
     * @param resolver resolves action ids to runnables available in this scope
     */
    public ShortcutScope(JComponent root, Resolver resolver) {
        this(root, resolver, false);
    }

    /**
     * @param root the top-level component of this scope
     * @param resolver resolves action ids to runnables available in this scope
     * @param windowLevel whether containment is window-level instead of
     *                   descending; production code uses the
     *                   {@link #windowLevel(JComponent, Resolver)} factory
     */
    private ShortcutScope(JComponent root, Resolver resolver, boolean windowLevel) {
        this.root = Objects.requireNonNull(root, "root");
        this.resolver = Objects.requireNonNull(resolver, "resolver");
        this.windowLevel = windowLevel;
    }

    /**
     * Creates a scope with window-level containment (opt-in): instead of
     * requiring the focus owner to descend from {@code root}, the scope
     * contains the focus whenever it is anywhere inside the window that
     * hosts {@code root}.
     * <p>
     * The hosting window is resolved per containment check via
     * {@link SwingUtilities#getWindowAncestor(Component)} of {@code root};
     * the target window is the window ancestor of the focus owner or, when
     * there is no focus owner (the window itself holds the keyboard focus,
     * e.g. right after a tab switch), the currently focused window of the
     * {@link KeyboardFocusManager}. The focus is contained iff both windows
     * resolve and are the same object; a root outside any window (e.g. a
     * headless test hierarchy) never contains.
     * <p>
     * A dialog's focus owner has a different window ancestor, so a modal
     * dialog stays excluded exactly as with descending containment. Opt in
     * only for scopes whose actions are window-level UI (dock toggles, the
     * command bar trigger); see the class javadoc for why a scope whose
     * actions depend on the visible part of the window (the graph editor)
     * must not.
     *
     * @param root the top-level component of this scope (resolves the
     *            hosting window)
     * @param resolver resolves action ids to runnables available in this scope
     * @return the scope with window-level containment
     */
    public static ShortcutScope windowLevel(JComponent root, Resolver resolver) {
        return new ShortcutScope(root, resolver, true);
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
     * @param focusOwner the component currently holding the keyboard focus,
     *                   may be {@code null}
     * @return descending containment (the default): true when
     * {@code focusOwner} is the scope root or one of its descendants;
     * window-level containment ({@link #windowLevel(JComponent, Resolver)}):
     * true when the keyboard focus is anywhere inside the window that hosts
     * the scope root, with a {@code null} focus owner resolved against the
     * currently focused window
     */
    public boolean contains(Component focusOwner) {
        if (windowLevel) {
            return containsWindowLevel(focusOwner);
        }
        return focusOwner != null && SwingUtilities.isDescendingFrom(focusOwner, root);
    }

    /**
     * The window-level containment check: compares the window hosting the
     * scope root with the window holding the keyboard focus.
     */
    private boolean containsWindowLevel(Component focusOwner) {
        Window scopeWindow = SwingUtilities.getWindowAncestor(root);
        if (scopeWindow == null) {
            return false;
        }
        if (focusOwner != null) {
            return SwingUtilities.getWindowAncestor(focusOwner) == scopeWindow;
        }
        // No focus owner (e.g. right after a tab switch the window itself
        // holds the keyboard focus): compare the focused window
        try {
            return KeyboardFocusManager.getCurrentKeyboardFocusManager()
                    .getFocusedWindow() == scopeWindow;
        } catch (Throwable t) {
            // Headless or a focus manager that cannot be consulted: no
            // window to compare, so not contained
            return false;
        }
    }
}
