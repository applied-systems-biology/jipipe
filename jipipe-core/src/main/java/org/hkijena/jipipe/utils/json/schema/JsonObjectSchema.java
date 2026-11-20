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

package org.hkijena.jipipe.utils.json.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.hkijena.jipipe.utils.json.JsonUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A JSON Schema {@code object} node.
 * Properties and required names keep their declaration order.
 * {@code additionalProperties} is always {@code false}.
 */
public final class JsonObjectSchema implements JsonSchema {

    private final String description;
    private final Map<String, JsonSchema> properties;
    private final List<String> required;

    private JsonObjectSchema(String description, Map<String, JsonSchema> properties, List<String> required) {
        this.description = description;
        this.properties = properties;
        this.required = required;
    }

    /**
     * Starts building an object schema.
     *
     * @return a new builder
     */
    public static Builder object() {
        return new Builder();
    }

    @Override
    public JsonNode toJson() {
        ObjectNode node = JsonUtils.getObjectMapper().createObjectNode();
        node.put("type", "object");
        if (description != null) {
            node.put("description", description);
        }
        if (!required.isEmpty()) {
            ArrayNode requiredNode = node.putArray("required");
            for (String name : required) {
                requiredNode.add(name);
            }
        }
        ObjectNode propertiesNode = node.putObject("properties");
        for (Map.Entry<String, JsonSchema> entry : properties.entrySet()) {
            propertiesNode.set(entry.getKey(), entry.getValue().toJson());
        }
        node.put("additionalProperties", false);
        return node;
    }

    /**
     * Builds a frozen {@link JsonObjectSchema}.
     * Properties keep their declaration order; the required list is the
     * subset of property names that were declared required, in declaration order.
     */
    public static final class Builder {

        private final LinkedHashMap<String, Property> properties = new LinkedHashMap<>();
        private String description;

        private Builder() {

        }

        /**
         * Sets the description of the object schema.
         *
         * @param description the description
         * @return this builder
         */
        public Builder description(String description) {
            this.description = description;
            return this;
        }

        /**
         * Adds an optional property.
         *
         * @param name   the property name
         * @param schema the property schema
         * @return this builder
         */
        public Builder property(String name, JsonSchema schema) {
            return property(name, schema, false);
        }

        /**
         * Adds a property.
         *
         * @param name     the property name
         * @param schema   the property schema
         * @param required whether the property is required
         * @return this builder
         */
        public Builder property(String name, JsonSchema schema, boolean required) {
            properties.put(name, new Property(schema, required));
            return this;
        }

        /**
         * Freezes the builder into an immutable {@link JsonObjectSchema}.
         *
         * @return the frozen object schema
         */
        public JsonObjectSchema build() {
            LinkedHashMap<String, JsonSchema> frozenProperties = new LinkedHashMap<>();
            List<String> frozenRequired = new ArrayList<>();
            for (Map.Entry<String, Property> entry : properties.entrySet()) {
                frozenProperties.put(entry.getKey(), entry.getValue().schema());
                if (entry.getValue().required()) {
                    frozenRequired.add(entry.getKey());
                }
            }
            return new JsonObjectSchema(description, frozenProperties, List.copyOf(frozenRequired));
        }
    }

    private record Property(JsonSchema schema, boolean required) {
    }
}
