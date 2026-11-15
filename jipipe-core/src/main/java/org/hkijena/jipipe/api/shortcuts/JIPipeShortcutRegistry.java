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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
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
     * Clears the application-wide singleton so the next
     * {@link #getInstance()} call creates a fresh registry.
     * <p>
     * Intended for tests that need an isolated registry (including tests
     * outside this package that pin dispatcher behavior); production code
     * should always use {@link #getInstance()} and rely on the idempotent
     * registration helpers.
     */
    public static void resetInstance() {
        INSTANCE = null;
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
     * Resolves the effective binding of one action against a keymap state.
     * <p>
     * Resolution order (design Section 4): a user override in
     * {@code settings} wins; otherwise the action's non-empty default
     * variants are used; otherwise the action is unbound.
     *
     * @param actionId the id of the action to resolve
     * @param settings the keymap state carrying the user overrides, may be
     *                 {@code null} (treated as no overrides)
     * @return the effective binding, never {@code null}. An unknown
     *         {@code actionId} resolves to an {@link JIPipeShortcutBinding.Source#UNASSIGNED}
     *         binding rather than throwing, so callers may probe freely.
     */
    public JIPipeShortcutBinding bindingFor(String actionId, KeymapSettings settings) {
        Objects.requireNonNull(actionId, "actionId");
        JIPipeShortcutAction action = byId(actionId);
        if (action == null) {
            return new JIPipeShortcutBinding(actionId, List.of(), JIPipeShortcutBinding.Source.UNASSIGNED);
        }
        if (settings != null && settings.hasOverride(actionId)) {
            return new JIPipeShortcutBinding(actionId, settings.overrideVariants(actionId),
                    JIPipeShortcutBinding.Source.OVERRIDE);
        }
        if (!action.defaultVariants().isEmpty()) {
            return new JIPipeShortcutBinding(actionId, action.defaultVariants(),
                    JIPipeShortcutBinding.Source.DEFAULT);
        }
        return new JIPipeShortcutBinding(actionId, List.of(), JIPipeShortcutBinding.Source.UNASSIGNED);
    }

    /**
     * Finds the other registered actions whose effective binding collides with
     * a candidate set of variants.
     * <p>
     * A collision is reported only between actions of the <em>same scope
     * family</em>: a graph action never "conflicts" with a project menu action
     * on the same key, because their scopes cannot be active at once. Two
     * variants are equal chords when they carry the same strokes in the same
     * order, compared with {@link KeyStroke#equals} (which is insensitive to
     * modifier-key ordering). The candidate set is compared against each other
     * action's <em>effective</em> binding (override-aware), not its default.
     *
     * @param actionId the id of the action being (re)bound; it is never reported
     *                 as conflicting with itself and its scope family is used
     *                 to constrain the search
     * @param variants the candidate variants to check, may be {@code null} or
     *                 empty (then nothing conflicts)
     * @param settings the keymap state used to resolve effective bindings, may
     *                 be {@code null} (treated as no overrides)
     * @return the ids of the other colliding actions in the same scope family,
     *         empty when there is no collision
     */
    public List<String> findConflicts(String actionId, List<List<KeyStroke>> variants, KeymapSettings settings) {
        Objects.requireNonNull(actionId, "actionId");
        if (variants == null || variants.isEmpty()) {
            return List.of();
        }
        JIPipeShortcutAction action = byId(actionId);
        if (action == null) {
            return List.of();
        }
        String family = action.scopeFamily();
        List<String> conflicts = new ArrayList<>();
        for (JIPipeShortcutAction other : all()) {
            if (other.id().equals(actionId) || !other.scopeFamily().equals(family)) {
                continue;
            }
            List<List<KeyStroke>> effective = bindingFor(other.id(), settings).variants();
            for (List<KeyStroke> candidate : variants) {
                boolean clash = effective.stream().anyMatch(eff -> chordsEqual(candidate, eff));
                if (clash) {
                    conflicts.add(other.id());
                    break;
                }
            }
        }
        return conflicts;
    }

    /**
     * Collects the chord prefixes that the
     * {@link JIPipeChordStateMachine} must watch for: the first stroke of every
     * length-2 variant across the effective bindings of all registered actions.
     * <p>
     * Each prefix is wrapped as a one-element list, matching the state
     * machine's {@code prefixes} parameter and its
     * {@code prefixes.contains(List.of(stroke))} lookup. Plain (length-1)
     * variants contribute nothing: a single key is never a chord prefix.
     *
     * @param settings the keymap state used to resolve effective bindings, may
     *                 be {@code null} (treated as no overrides)
     * @return the set of chord prefix chords, one element per distinct prefix
     */
    public Set<List<KeyStroke>> chordPrefixes(KeymapSettings settings) {
        Set<List<KeyStroke>> prefixes = new LinkedHashSet<>();
        for (JIPipeShortcutAction action : all()) {
            for (List<KeyStroke> variant : bindingFor(action.id(), settings).variants()) {
                if (variant.size() == 2) {
                    prefixes.add(List.of(variant.get(0)));
                }
            }
        }
        return prefixes;
    }

    /**
     * Order-sensitive chord equality over two strokes lists, delegating each
     * stroke to {@link KeyStroke#equals}. Two chords are equal when they have
     * the same length and the same strokes at the same positions.
     */
    private static boolean chordsEqual(List<KeyStroke> a, List<KeyStroke> b) {
        if (a.size() != b.size()) {
            return false;
        }
        for (int i = 0; i < a.size(); i++) {
            if (!a.get(i).equals(b.get(i))) {
                return false;
            }
        }
        return true;
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
     * Renders a single key stroke for display, e.g. "Ctrl+T". The per-stroke
     * text of {@link #formatChord(List)}.
     */
    public static String formatKeyStroke(KeyStroke stroke) {
        return UIUtils.keyStrokeToString(stroke);
    }

    /**
     * Renders a chord (an ordered list of key strokes) for display: the
     * strokes are joined with " then " and each stroke is rendered with
     * {@link #formatKeyStroke(KeyStroke)}, e.g. "Ctrl+T then M". An empty
     * chord renders as "-".
     */
    public static String formatChord(List<KeyStroke> chord) {
        if (chord.isEmpty()) {
            return "-";
        }
        List<String> parts = new ArrayList<>(chord.size());
        for (KeyStroke stroke : chord) {
            parts.add(formatKeyStroke(stroke));
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
