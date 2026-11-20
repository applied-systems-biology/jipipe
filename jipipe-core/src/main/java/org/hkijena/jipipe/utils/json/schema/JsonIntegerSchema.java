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
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.hkijena.jipipe.utils.json.JsonUtils;

/**
 * A JSON Schema {@code integer} node.
 * Carries exactly the integer keywords: {@code minimum} and {@code default}.
 */
public final class JsonIntegerSchema implements JsonSchema {

    private final String description;
    private final Integer minimum;
    private final Integer defaultValue;

    private JsonIntegerSchema(String description, Integer minimum, Integer defaultValue) {
        this.description = description;
        this.minimum = minimum;
        this.defaultValue = defaultValue;
    }

    /**
     * Creates an empty integer schema.
     *
     * @return the frozen integer schema
     */
    public static JsonIntegerSchema integer() {
        return new JsonIntegerSchema(null, null, null);
    }

    /**
     * Sets the description.
     *
     * @param description the description
     * @return a new frozen integer schema with the description
     */
    public JsonIntegerSchema description(String description) {
        return new JsonIntegerSchema(description, minimum, defaultValue);
    }

    /**
     * Sets the inclusive lower bound.
     *
     * @param minimum the minimum
     * @return a new frozen integer schema with the minimum
     */
    public JsonIntegerSchema minimum(int minimum) {
        return new JsonIntegerSchema(description, minimum, defaultValue);
    }

    /**
     * Sets the default value.
     *
     * @param value the default value
     * @return a new frozen integer schema with the default
     */
    public JsonIntegerSchema defaultsTo(int value) {
        return new JsonIntegerSchema(description, minimum, value);
    }

    @Override
    public JsonNode toJson() {
        ObjectNode node = JsonUtils.getObjectMapper().createObjectNode();
        node.put("type", "integer");
        if (description != null) {
            node.put("description", description);
        }
        if (defaultValue != null) {
            node.put("default", defaultValue);
        }
        if (minimum != null) {
            node.put("minimum", minimum);
        }
        return node;
    }
}
