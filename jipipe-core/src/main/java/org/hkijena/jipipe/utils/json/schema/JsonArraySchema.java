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
 * A JSON Schema {@code array} node.
 * The item schema is optional; when absent, no {@code items} keyword is rendered.
 */
public final class JsonArraySchema implements JsonSchema {

    private final String description;
    private final JsonSchema items;

    private JsonArraySchema(String description, JsonSchema items) {
        this.description = description;
        this.items = items;
    }

    /**
     * Creates an array schema with the given item schema.
     *
     * @param items the schema of the array items (may be null for an untyped item schema)
     * @return the frozen array schema
     */
    public static JsonArraySchema array(JsonSchema items) {
        return new JsonArraySchema(null, items);
    }

    /**
     * Sets the description.
     *
     * @param description the description
     * @return a new frozen array schema with the description
     */
    public JsonArraySchema description(String description) {
        return new JsonArraySchema(description, items);
    }

    @Override
    public JsonNode toJson() {
        ObjectNode node = JsonUtils.getObjectMapper().createObjectNode();
        node.put("type", "array");
        if (description != null) {
            node.put("description", description);
        }
        if (items != null) {
            node.set("items", items.toJson());
        }
        return node;
    }
}
