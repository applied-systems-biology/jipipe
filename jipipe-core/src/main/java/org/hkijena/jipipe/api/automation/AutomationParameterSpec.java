package org.hkijena.jipipe.api.automation;

import org.hkijena.jipipe.utils.json.schema.JsonSchema;

/**
 * One parameter of an automation operation: the machine-readable metadata.
 * <p>
 * A typed view over one top-level property of an operation's {@link AutomationSchemaSpec};
 * not a second source of truth (both projections are built from the same builder call and
 * frozen at {@code build()}). The general AST node in {@link #getType()} carries the
 * description and the type-specific keywords ({@code enum}, {@code default}, {@code minimum},
 * {@code items}, nested {@code properties}), so nested types stay reachable without walking
 * the rendered JSON.
 */
public final class AutomationParameterSpec {

    private final String name;
    private final JsonSchema type;
    private final boolean required;
    private final Object defaultValue;

    AutomationParameterSpec(String name, JsonSchema type, boolean required, Object defaultValue) {
        this.name = name;
        this.type = type;
        this.required = required;
        this.defaultValue = defaultValue;
    }

    /**
     * Returns the parameter name. Stable; kebab-case when multi-word.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the general AST node of the parameter (its description is the parameter description).
     */
    public JsonSchema getType() {
        return type;
    }

    /**
     * Returns true when the parameter must be present.
     */
    public boolean isRequired() {
        return required;
    }

    /**
     * Returns the convenience mirror of the AST default; null = none.
     */
    public Object getDefaultValue() {
        return defaultValue;
    }
}
