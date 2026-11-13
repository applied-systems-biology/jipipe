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

import javax.swing.KeyStroke;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * An immutable description of one keyboard shortcut action: what it does,
 * where it belongs, and what keys are bound to it by default.
 * <p>
 * Actions are created through {@link #builder(String, String, String)} and
 * registered in a {@link JIPipeShortcutRegistry}. The {@code id} is a stable
 * string literal that is persisted in saved profiles: it must never be
 * derived from code. Equality and hashing depend on the {@code id} only, so
 * two actions with the same id are considered the same action even if their
 * display fields or default bindings differ.
 * <p>
 * A binding variant is a chord: an ordered list of {@link KeyStroke}s. A
 * chord of length 1 is a plain binding; a longer chord is pressed in
 * sequence (Microsoft-style multi-keybinds, e.g. {@code Ctrl+T} then {@code M}).
 * An action may offer several variants that are interchangeable.
 */
public final class JIPipeShortcutAction {
    /**
     * Scope family of actions that operate on the graph editor (nodes,
     * connections, tools).
     */
    public static final String FAMILY_GRAPH = "graph";
    /**
     * Scope family of actions that operate on a project or pipeline as a whole.
     */
    public static final String FAMILY_PROJECT = "project";
    /**
     * Scope family of actions that operate on a dock panel (open/close,
     * navigation within the dock).
     */
    public static final String FAMILY_DOCK = "dock";

    private final String id;
    private final String name;
    private final String description;
    private final String category;
    private final String scopeFamily;
    private final boolean textInputSafe;
    private final List<List<KeyStroke>> defaultVariants;

    private JIPipeShortcutAction(String id, String name, String description, String category, String scopeFamily,
                                 boolean textInputSafe, List<List<KeyStroke>> defaultVariants) {
        this.id = Objects.requireNonNull(id, "id");
        this.name = Objects.requireNonNull(name, "name");
        this.description = Objects.requireNonNull(description, "description");
        this.category = Objects.requireNonNull(category, "category");
        this.scopeFamily = Objects.requireNonNull(scopeFamily, "scopeFamily");
        this.textInputSafe = textInputSafe;
        this.defaultVariants = Collections.unmodifiableList(
                defaultVariants.stream().map(v -> Collections.unmodifiableList(new ArrayList<>(v))).toList());
    }

    /**
     * Starts building an action.
     *
     * @param id the stable registration id (a string literal, never code-derived)
     * @param name the display name, e.g. "Duplicate"
     * @param category the settings-table group, e.g. "Selection"
     */
    public static Builder builder(String id, String name, String category) {
        return new Builder(id, name, category);
    }

    public String id() {
        return id;
    }

    public String name() {
        return name;
    }

    public String description() {
        return description;
    }

    public String category() {
        return category;
    }

    public String scopeFamily() {
        return scopeFamily;
    }

    /**
     * @return true when this action may be triggered while a text input has
     * the focus, e.g. inside a node parameter editor
     */
    public boolean isTextInputSafe() {
        return textInputSafe;
    }

    /**
     * @return the default binding variants, each an ordered list of key
     * strokes (a chord); empty when the action has no default binding
     */
    public List<List<KeyStroke>> defaultVariants() {
        return defaultVariants;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof JIPipeShortcutAction other)) {
            return false;
        }
        return id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    public static final class Builder {
        private final String id;
        private final String name;
        private final String category;
        private String description = "";
        private String scopeFamily = FAMILY_GRAPH;
        private boolean textInputSafe;
        private List<List<KeyStroke>> defaultVariants = List.of();

        private Builder(String id, String name, String category) {
            this.id = Objects.requireNonNull(id, "id");
            this.name = Objects.requireNonNull(name, "name");
            this.category = Objects.requireNonNull(category, "category");
        }

        /**
         * Sets the human-readable description.
         */
        public Builder description(String description) {
            this.description = Objects.requireNonNull(description, "description");
            return this;
        }

        /**
         * Sets the scope family, e.g. {@link #FAMILY_GRAPH}.
         */
        public Builder scopeFamily(String scopeFamily) {
            this.scopeFamily = Objects.requireNonNull(scopeFamily, "scopeFamily");
            return this;
        }

        /**
         * Marks this action as safe to trigger while a text input has focus.
         */
        public Builder textInputSafe(boolean textInputSafe) {
            this.textInputSafe = textInputSafe;
            return this;
        }

        /**
         * Sets the default binding variants, each an ordered list of key
         * strokes.
         */
        public Builder defaultVariants(List<List<KeyStroke>> defaultVariants) {
            this.defaultVariants = Objects.requireNonNull(defaultVariants, "defaultVariants");
            return this;
        }

        /**
         * Sets a single default chord from its strokes. With one stroke this
         * is a plain binding; with several strokes the keys are pressed in
         * sequence.
         */
        public Builder defaultChord(KeyStroke... stroke) {
            this.defaultVariants = List.of(List.of(stroke));
            return this;
        }

        public JIPipeShortcutAction build() {
            return new JIPipeShortcutAction(id, name, description, category, scopeFamily, textInputSafe, defaultVariants);
        }
    }
}
