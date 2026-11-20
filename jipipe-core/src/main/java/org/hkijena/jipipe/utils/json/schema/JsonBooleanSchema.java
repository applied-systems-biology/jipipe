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
 * A JSON Schema {@code boolean} node.
 * Carries exactly the boolean keywords: {@code default}.
 */
public final class JsonBooleanSchema implements JsonSchema {

    private final String description;
    private final Boolean defaultValue;

    private JsonBooleanSchema(String description, Boolean defaultValue) {
        this.description = description;
        this.defaultValue = defaultValue;
    }

    /**
     * Creates an empty boolean schema.
     *
     * @return the frozen boolean schema
     */
    public static JsonBooleanSchema bool() {
        return new JsonBooleanSchema(null, null);
    }

    /**
     * Sets the description.
     *
     * @param description the description
     * @return a new frozen boolean schema with the description
     */
    public JsonBooleanSchema description(String description) {
        return new JsonBooleanSchema(description, defaultValue);
    }

    /**
     * Sets the default value.
     *
     * @param value the default value
     * @return a new frozen boolean schema with the default
     */
    public JsonBooleanSchema defaultsTo(boolean value) {
        return new JsonBooleanSchema(description, value);
    }

    @Override
    public JsonNode toJson() {
        ObjectNode node = JsonUtils.getObjectMapper().createObjectNode();
        node.put("type", "boolean");
        if (description != null) {
            node.put("description", description);
        }
        if (defaultValue != null) {
            node.put("default", (boolean) defaultValue);
        }
        return node;
    }
}
