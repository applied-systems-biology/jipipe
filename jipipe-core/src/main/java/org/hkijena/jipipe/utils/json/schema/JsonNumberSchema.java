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
 * A JSON Schema {@code number} node.
 * Carries exactly the number keywords: {@code default}.
 * <p>
 * The default keeps the spelling it was declared with: {@link #defaultsTo(int)} renders an
 * integral JSON number (2, not 2.0), {@link #defaultsTo(double)} renders a floating-point
 * one. JSON does not distinguish the two forms, but the exact rendering is observable on
 * the wire, so it follows the declaration.
 */
public final class JsonNumberSchema implements JsonSchema {

    private final String description;
    private final Number defaultValue;

    private JsonNumberSchema(String description, Number defaultValue) {
        this.description = description;
        this.defaultValue = defaultValue;
    }

    /**
     * Creates an empty number schema.
     *
     * @return the frozen number schema
     */
    public static JsonNumberSchema number() {
        return new JsonNumberSchema(null, null);
    }

    /**
     * Sets the description.
     *
     * @param description the description
     * @return a new frozen number schema with the description
     */
    public JsonNumberSchema description(String description) {
        return new JsonNumberSchema(description, defaultValue);
    }

    /**
     * Sets the default value.
     *
     * @param value the default value
     * @return a new frozen number schema with the default
     */
    public JsonNumberSchema defaultsTo(double value) {
        return new JsonNumberSchema(description, value);
    }

    /**
     * Sets an integral default value: it renders as a JSON integer (2, not 2.0).
     *
     * @param value the default value
     * @return a new frozen number schema with the default
     */
    public JsonNumberSchema defaultsTo(int value) {
        return new JsonNumberSchema(description, value);
    }

    @Override
    public JsonNode toJson() {
        ObjectNode node = JsonUtils.getObjectMapper().createObjectNode();
        node.put("type", "number");
        if (description != null) {
            node.put("description", description);
        }
        if (defaultValue instanceof Integer integerDefault) {
            node.put("default", integerDefault);
        } else if (defaultValue != null) {
            node.put("default", defaultValue.doubleValue());
        }
        return node;
    }
}
