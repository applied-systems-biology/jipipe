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

import java.util.List;

/**
 * A JSON Schema {@code string} node.
 * Carries exactly the string keywords: {@code enum} and {@code default}.
 */
public final class JsonStringSchema implements JsonSchema {

    private final String description;
    private final List<String> enumValues;
    private final String defaultValue;

    private JsonStringSchema(String description, List<String> enumValues, String defaultValue) {
        this.description = description;
        this.enumValues = enumValues;
        this.defaultValue = defaultValue;
    }

    /**
     * Creates an empty string schema.
     *
     * @return the frozen string schema
     */
    public static JsonStringSchema string() {
        return new JsonStringSchema(null, null, null);
    }

    /**
     * Sets the description.
     *
     * @param description the description
     * @return a new frozen string schema with the description
     */
    public JsonStringSchema description(String description) {
        return new JsonStringSchema(description, enumValues, defaultValue);
    }

    /**
     * Restricts the string to the given values.
     *
     * @param values the allowed values
     * @return a new frozen string schema with the enumeration
     */
    public JsonStringSchema enumeration(String... values) {
        return new JsonStringSchema(description, List.of(values), defaultValue);
    }

    /**
     * Sets the default value.
     *
     * @param value the default value
     * @return a new frozen string schema with the default
     */
    public JsonStringSchema defaultsTo(String value) {
        return new JsonStringSchema(description, enumValues, value);
    }

    /**
     * Returns the allowed values.
     *
     * @return the allowed values, or null when no enumeration is declared
     */
    public List<String> getEnumValues() {
        return enumValues;
    }

    @Override
    public JsonNode toJson() {
        ObjectNode node = JsonUtils.getObjectMapper().createObjectNode();
        node.put("type", "string");
        if (description != null) {
            node.put("description", description);
        }
        if (enumValues != null && !enumValues.isEmpty()) {
            ArrayNode enumNode = node.putArray("enum");
            for (String value : enumValues) {
                enumNode.add(value);
            }
        }
        if (defaultValue != null) {
            node.put("default", defaultValue);
        }
        return node;
    }
}
