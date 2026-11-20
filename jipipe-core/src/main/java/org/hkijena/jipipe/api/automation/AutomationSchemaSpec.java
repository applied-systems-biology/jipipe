package org.hkijena.jipipe.api.automation;

import com.fasterxml.jackson.databind.JsonNode;
import org.hkijena.jipipe.utils.json.schema.JsonArraySchema;
import org.hkijena.jipipe.utils.json.schema.JsonBooleanSchema;
import org.hkijena.jipipe.utils.json.schema.JsonIntegerSchema;
import org.hkijena.jipipe.utils.json.schema.JsonNumberSchema;
import org.hkijena.jipipe.utils.json.schema.JsonObjectSchema;
import org.hkijena.jipipe.utils.json.schema.JsonSchema;
import org.hkijena.jipipe.utils.json.schema.JsonStringSchema;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The complete input schema of an operation: the typed source of truth.
 * <p>
 * The automation consumer layer over the general JSON Schema AST
 * ({@code org.hkijena.jipipe.utils.json.schema}): {@link #toJsonSchema()} (the wire schema
 * the registry validates against and the broker forwards) and {@link #getParameters()}
 * (the flattened machine-readable parameter metadata) are two projections of the same
 * builder calls and cannot diverge.
 * <p>
 * Usage (the single override point {@link AutomationOperation#getSchemaSpec()}):
 * <pre>{@code
 * static final AutomationSchemaSpec SPEC = AutomationSchemaSpec.object()
 *         .string("compartment", "Compartment whose nodes are arranged").required()
 *         .bool("apply", "true commits the computed positions").defaultsTo(false)
 *         .integer("limit", "Maximum results").defaultsTo(8).minimum(1)
 *         .build();
 * }</pre>
 */
public final class AutomationSchemaSpec {

    private final JsonObjectSchema schema;
    private final List<AutomationParameterSpec> parameters;

    private AutomationSchemaSpec(JsonObjectSchema schema, List<AutomationParameterSpec> parameters) {
        this.schema = schema;
        this.parameters = parameters;
    }

    /**
     * Starts building an object spec.
     *
     * @return a new builder
     */
    public static Builder object() {
        return new Builder();
    }

    /**
     * Renders the JSON schema (the wire contract). Builds a fresh node per call (no caching;
     * the registry validates and the broker forwards per registration, both cheap).
     *
     * @return the JSON schema
     */
    public JsonNode toJsonSchema() {
        return schema.toJson();
    }

    /**
     * Returns the flattened top-level parameter metadata.
     * Returns the same immutable list instance on every call (frozen at {@code build()}),
     * so consumers may cache it.
     *
     * @return the parameters
     */
    public List<AutomationParameterSpec> getParameters() {
        return parameters;
    }

    /**
     * Builds a frozen {@link AutomationSchemaSpec}.
     * <p>
     * The starters return a {@link ParameterBuilder} bound to the declared parameter; the
     * terminal methods ({@code required()}, {@code defaultsTo(...)}, {@code minimum(int)})
     * exist on both the spec builder and the parameter builder and return the spec builder,
     * so the terminals chain in any order after any starter. On the spec builder they apply
     * to the most recently declared parameter.
     * <p>
     * Programmer errors fail fast at construction: duplicate parameter names and
     * type-inappropriate keywords (e.g. {@code minimum(int)} on a string parameter) throw
     * immediately instead of producing a broken schema.
     */
    public static final class Builder {

        private final LinkedHashMap<String, Entry> entries = new LinkedHashMap<>();
        private Entry last;

        private Builder() {
        }

        /**
         * Declares a string parameter.
         *
         * @param name        the parameter name (stable; kebab-case when multi-word)
         * @param description the parameter description
         * @return the per-parameter builder
         */
        public ParameterBuilder string(String name, String description) {
            return add(new Entry(name, JsonStringSchema.string().description(description)));
        }

        /**
         * Declares a boolean parameter.
         *
         * @param name        the parameter name
         * @param description the parameter description
         * @return the per-parameter builder
         */
        public ParameterBuilder bool(String name, String description) {
            return add(new Entry(name, JsonBooleanSchema.bool().description(description)));
        }

        /**
         * Declares an integer parameter.
         *
         * @param name        the parameter name
         * @param description the parameter description
         * @return the per-parameter builder
         */
        public ParameterBuilder integer(String name, String description) {
            return add(new Entry(name, JsonIntegerSchema.integer().description(description)));
        }

        /**
         * Declares a number parameter.
         *
         * @param name        the parameter name
         * @param description the parameter description
         * @return the per-parameter builder
         */
        public ParameterBuilder number(String name, String description) {
            return add(new Entry(name, JsonNumberSchema.number().description(description)));
        }

        /**
         * Declares a string parameter restricted to the given values.
         *
         * @param name        the parameter name
         * @param description the parameter description
         * @param values      the allowed values
         * @return the per-parameter builder
         */
        public ParameterBuilder enumeration(String name, String description, String... values) {
            return add(new Entry(name,
                    JsonStringSchema.string().description(description).enumeration(values)));
        }

        /**
         * Declares an array-of-strings parameter.
         *
         * @param name        the parameter name
         * @param description the parameter description
         * @return the per-parameter builder
         */
        public ParameterBuilder arrayOfStrings(String name, String description) {
            return add(new Entry(name,
                    JsonArraySchema.array(JsonStringSchema.string()).description(description)));
        }

        /**
         * Declares an array-of-objects parameter whose items follow the given object schema.
         *
         * @param name        the parameter name
         * @param description the parameter description
         * @param items       the schema of the array items
         * @return the per-parameter builder
         */
        public ParameterBuilder arrayOfObjects(String name, String description, JsonObjectSchema items) {
            return add(new Entry(name,
                    JsonArraySchema.array(items).description(description)));
        }

        /**
         * Marks the most recently declared parameter required.
         *
         * @return this builder
         */
        public Builder required() {
            last().required = true;
            return this;
        }

        /**
         * Sets the default of the most recently declared string parameter.
         *
         * @param value the default value
         * @return this builder
         */
        public Builder defaultsTo(String value) {
            Entry entry = last();
            entry.schema = as(entry.schema, JsonStringSchema.class, "defaultsTo(String)").defaultsTo(value);
            entry.defaultValue = value;
            return this;
        }

        /**
         * Sets the default of the most recently declared integer parameter.
         *
         * @param value the default value
         * @return this builder
         */
        public Builder defaultsTo(int value) {
            Entry entry = last();
            entry.schema = as(entry.schema, JsonIntegerSchema.class, "defaultsTo(int)").defaultsTo(value);
            entry.defaultValue = value;
            return this;
        }

        /**
         * Sets the default of the most recently declared boolean parameter.
         *
         * @param value the default value
         * @return this builder
         */
        public Builder defaultsTo(boolean value) {
            Entry entry = last();
            entry.schema = as(entry.schema, JsonBooleanSchema.class, "defaultsTo(boolean)").defaultsTo(value);
            entry.defaultValue = value;
            return this;
        }

        /**
         * Sets the default of the most recently declared number parameter.
         *
         * @param value the default value
         * @return this builder
         */
        public Builder defaultsTo(double value) {
            Entry entry = last();
            entry.schema = as(entry.schema, JsonNumberSchema.class, "defaultsTo(double)").defaultsTo(value);
            entry.defaultValue = value;
            return this;
        }

        /**
         * Sets the inclusive lower bound of the most recently declared integer parameter.
         *
         * @param value the minimum
         * @return this builder
         */
        public Builder minimum(int value) {
            Entry entry = last();
            entry.schema = as(entry.schema, JsonIntegerSchema.class, "minimum(int)").minimum(value);
            return this;
        }

        /**
         * Freezes the builder: constructs the object schema and the parameter view from the
         * same accumulated data, so the two projections cannot diverge.
         *
         * @return the frozen spec
         */
        public AutomationSchemaSpec build() {
            JsonObjectSchema.Builder objectBuilder = JsonObjectSchema.object();
            List<AutomationParameterSpec> parameters = new ArrayList<>();
            for (Map.Entry<String, Entry> entry : entries.entrySet()) {
                Entry e = entry.getValue();
                objectBuilder.property(e.name, e.schema, e.required);
                parameters.add(new AutomationParameterSpec(e.name, e.schema, e.required, e.defaultValue));
            }
            return new AutomationSchemaSpec(objectBuilder.build(), List.copyOf(parameters));
        }

        private ParameterBuilder add(Entry entry) {
            if (entries.containsKey(entry.name)) {
                throw new IllegalArgumentException("Duplicate parameter name: " + entry.name);
            }
            entries.put(entry.name, entry);
            last = entry;
            return new ParameterBuilder(this, entry);
        }

        private Entry last() {
            if (last == null) {
                throw new IllegalStateException("No parameter was declared: required(), defaultsTo(...), "
                        + "and minimum(...) apply to the most recently declared parameter");
            }
            return last;
        }

        private static <T extends JsonSchema> T as(JsonSchema schema, Class<T> type, String keyword) {
            if (!type.isInstance(schema)) {
                throw new IllegalArgumentException("The keyword " + keyword
                        + " does not apply to this parameter type");
            }
            return type.cast(schema);
        }

        /**
         * The accumulation of one parameter: name, schema (withered in place by the terminal
         * methods), required flag, and the convenience default mirror.
         */
        private static final class Entry {
            final String name;
            JsonSchema schema;
            boolean required;
            Object defaultValue;

            Entry(String name, JsonSchema schema) {
                this.name = name;
                this.schema = schema;
            }
        }
    }

    /**
     * The per-parameter builder returned by the {@link Builder} starters. Bound to the
     * parameter declared by the starter that created it: its terminal methods apply to THAT
     * parameter (immune to later declarations) and return the spec builder. The starters and
     * {@code build()} are forwarded to the spec builder, so any declaration order chains.
     */
    public static final class ParameterBuilder {

        private final Builder outer;
        private final Builder.Entry entry;

        private ParameterBuilder(Builder outer, Builder.Entry entry) {
            this.outer = outer;
            this.entry = entry;
        }

        /**
         * Marks this parameter required.
         *
         * @return the spec builder, to declare the next parameter
         */
        public Builder required() {
            entry.required = true;
            return outer;
        }

        /**
         * Sets the default value of this string parameter.
         *
         * @param value the default value
         * @return the spec builder, to declare the next parameter
         */
        public Builder defaultsTo(String value) {
            entry.schema = Builder.as(entry.schema, JsonStringSchema.class, "defaultsTo(String)").defaultsTo(value);
            entry.defaultValue = value;
            return outer;
        }

        /**
         * Sets the default value of this integer parameter.
         *
         * @param value the default value
         * @return the spec builder, to declare the next parameter
         */
        public Builder defaultsTo(int value) {
            entry.schema = Builder.as(entry.schema, JsonIntegerSchema.class, "defaultsTo(int)").defaultsTo(value);
            entry.defaultValue = value;
            return outer;
        }

        /**
         * Sets the default value of this boolean parameter.
         *
         * @param value the default value
         * @return the spec builder, to declare the next parameter
         */
        public Builder defaultsTo(boolean value) {
            entry.schema = Builder.as(entry.schema, JsonBooleanSchema.class, "defaultsTo(boolean)").defaultsTo(value);
            entry.defaultValue = value;
            return outer;
        }

        /**
         * Sets the default value of this number parameter.
         *
         * @param value the default value
         * @return the spec builder, to declare the next parameter
         */
        public Builder defaultsTo(double value) {
            entry.schema = Builder.as(entry.schema, JsonNumberSchema.class, "defaultsTo(double)").defaultsTo(value);
            entry.defaultValue = value;
            return outer;
        }

        /**
         * Sets the inclusive lower bound of this integer parameter.
         *
         * @param value the minimum
         * @return the spec builder, to declare the next parameter
         */
        public Builder minimum(int value) {
            entry.schema = Builder.as(entry.schema, JsonIntegerSchema.class, "minimum(int)").minimum(value);
            return outer;
        }

        /**
         * Declares a string parameter (forwarded to the spec builder).
         */
        public ParameterBuilder string(String name, String description) {
            return outer.string(name, description);
        }

        /**
         * Declares a boolean parameter (forwarded to the spec builder).
         */
        public ParameterBuilder bool(String name, String description) {
            return outer.bool(name, description);
        }

        /**
         * Declares an integer parameter (forwarded to the spec builder).
         */
        public ParameterBuilder integer(String name, String description) {
            return outer.integer(name, description);
        }

        /**
         * Declares a number parameter (forwarded to the spec builder).
         */
        public ParameterBuilder number(String name, String description) {
            return outer.number(name, description);
        }

        /**
         * Declares a string parameter restricted to the given values (forwarded to the spec builder).
         */
        public ParameterBuilder enumeration(String name, String description, String... values) {
            return outer.enumeration(name, description, values);
        }

        /**
         * Declares an array-of-strings parameter (forwarded to the spec builder).
         */
        public ParameterBuilder arrayOfStrings(String name, String description) {
            return outer.arrayOfStrings(name, description);
        }

        /**
         * Declares an array-of-objects parameter (forwarded to the spec builder).
         */
        public ParameterBuilder arrayOfObjects(String name, String description, JsonObjectSchema items) {
            return outer.arrayOfObjects(name, description, items);
        }

        /**
         * Freezes the spec (forwarded to the spec builder).
         */
        public AutomationSchemaSpec build() {
            return outer.build();
        }
    }
}
