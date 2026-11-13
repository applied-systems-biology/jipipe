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

package org.hkijena.jipipe.api.shortcuts;

import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.KeyStroke;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * The central registry of all keyboard shortcut actions of the application.
 * <p>
 * Every UI feature that can be triggered from the keyboard registers its
 * {@link JIPipeShortcutAction} here exactly once, with a stable string-literal
 * id. The registry is the single source of truth for the action table in the
 * settings and for the context-action dispatcher, which rebuilds its stroke
 * maps when a listener is notified after a registration (e.g. when a plugin
 * adds dock actions).
 * <p>
 * Use {@link #getInstance()} for the application-wide singleton. The
 * constructor is package-private so tests can work with fresh registries.
 */
public final class JIPipeShortcutRegistry {
    private static volatile JIPipeShortcutRegistry INSTANCE;

    private final ConcurrentHashMap<String, JIPipeShortcutAction> actions = new ConcurrentHashMap<>();
    private final List<Runnable> changeListeners = new CopyOnWriteArrayList<>();

    /**
     * Package-private so tests can create fresh registries;
     * {@link #getInstance()} holds the lazily created singleton.
     */
    JIPipeShortcutRegistry() {
    }

    /**
     * @return the application-wide singleton, created on first use
     */
    public static JIPipeShortcutRegistry getInstance() {
        JIPipeShortcutRegistry instance = INSTANCE;
        if (instance == null) {
            synchronized (JIPipeShortcutRegistry.class) {
                instance = INSTANCE;
                if (instance == null) {
                    instance = new JIPipeShortcutRegistry();
                    INSTANCE = instance;
                }
            }
        }
        return instance;
    }

    /**
     * Registers an action.
     *
     * @throws IllegalStateException when an action with the same id is already registered
     */
    public void register(JIPipeShortcutAction action) {
        if (action == null) {
            throw new NullPointerException("action");
        }
        JIPipeShortcutAction previous = actions.putIfAbsent(action.id(), action);
        if (previous != null) {
            throw new IllegalStateException("Shortcut action id '" + action.id() + "' is already registered");
        }
        fireChangeListeners();
    }

    /**
     * @return the registered action with the given id, or {@code null} when unknown
     */
    public JIPipeShortcutAction byId(String id) {
        return actions.get(id);
    }

    /**
     * @return true when an action with the given id is registered
     */
    public boolean isRegistered(String id) {
        return actions.containsKey(id);
    }

    /**
     * @return all registered actions, sorted by category and then by name
     */
    public List<JIPipeShortcutAction> all() {
        List<JIPipeShortcutAction> result = new ArrayList<>(actions.values());
        result.sort(Comparator.comparing(JIPipeShortcutAction::category)
                .thenComparing(JIPipeShortcutAction::name));
        return result;
    }

    /**
     * @return the registered actions of the given category, sorted by name
     */
    public List<JIPipeShortcutAction> byCategory(String category) {
        return all().stream()
                .filter(a -> a.category().equals(category))
                .toList();
    }

    /**
     * Registers a change listener, fired after every successful registration.
     * The dispatcher uses it to rebuild its stroke maps when plugins add
     * actions (e.g. dock actions).
     */
    public void addChangeListener(Runnable listener) {
        if (listener != null) {
            changeListeners.add(listener);
        }
    }

    /**
     * Removes a change listener previously added with
     * {@link #addChangeListener(Runnable)}. A listener that is not registered
     * is ignored.
     */
    public void removeChangeListener(Runnable listener) {
        changeListeners.remove(listener);
    }

    private void fireChangeListeners() {
        for (Runnable listener : changeListeners) {
            listener.run();
        }
    }

    /**
     * Renders a chord (an ordered list of key strokes) for display: the
     * strokes are joined with " then " and each stroke is rendered with
     * {@link UIUtils#keyStrokeToString(KeyStroke)}, e.g. "Ctrl+T then M". An
     * empty chord renders as "-".
     */
    public static String formatChord(List<KeyStroke> chord) {
        if (chord.isEmpty()) {
            return "-";
        }
        List<String> parts = new ArrayList<>(chord.size());
        for (KeyStroke stroke : chord) {
            parts.add(UIUtils.keyStrokeToString(stroke));
        }
        return String.join(" then ", parts);
    }

    /**
     * Renders all variants of a binding for display: each variant is rendered
     * with {@link #formatChord(List)} and the results are joined with " or ",
     * e.g. "Ctrl+= or Ctrl+Num+".
     */
    public static String formatVariants(List<List<KeyStroke>> variants) {
        List<String> parts = new ArrayList<>(variants.size());
        for (List<KeyStroke> variant : variants) {
            parts.add(formatChord(variant));
        }
        return String.join(" or ", parts);
    }
}
