package org.hkijena.jipipe.api.automation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.hkijena.jipipe.utils.json.JsonUtils;
import org.hkijena.jipipe.utils.json.schema.JsonSchemaValidator;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Validates operation parameters against simplified JSON schemas.
 * Supported keywords: type (object, string, integer, number, boolean, array), required, properties, enum,
 * and additionalProperties (false only). Unknown keywords are ignored.
 */
public class AutomationSchemaUtils {
    private AutomationSchemaUtils() {
    }

    /**
     * Parses a JSON literal into a node. Lets tests declare node literals (the operations
     * themselves derive their schemas from {@link AutomationSchemaSpec}).
     */
    public static JsonNode parseJson(String json) {
        try {
            return JsonUtils.getObjectMapper().readTree(json);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid JSON literal: " + e.getMessage(), e);
        }
    }

    /**
     * Validates arguments against a schema and returns the list of errors (empty when valid).
     * Null or missing arguments are treated as an empty object.
     * <p>
     * Fail-closed: a null or non-object schema (e.g. a malformed schema literal) produces a
     * single descriptive error instead of silently passing. The instance endpoint rejects
     * such operations before dispatch, so this is the second line of defense for direct
     * {@link AutomationOperationRegistry#execute} callers.
     */
    public static List<String> validate(JsonNode schema, JsonNode args) {
        return JsonSchemaValidator.validate(schema, args);
    }

    /**
     * Returns the effective arguments: a new node where absent optional parameters are filled
     * from the schema's top-level {@code default} keywords. The input node is never mutated.
     * <p>
     * Rules: an explicit value always wins over the default; required parameters are
     * unaffected (their absence fails validation first); only top-level properties are
     * considered (no recursion into nested objects or array items); a null default is treated
     * as no default. Null or non-object arguments are treated as an empty object; a null or
     * non-object schema yields the unchanged copy.
     */
    public static JsonNode applyDefaults(JsonNode schema, JsonNode args) {
        ObjectNode result = args != null && args.isObject()
                ? args.deepCopy()
                : JsonUtils.getObjectMapper().createObjectNode();
        if (schema == null || !schema.isObject()) {
            return result;
        }
        JsonNode properties = schema.get("properties");
        if (properties == null || !properties.isObject()) {
            return result;
        }
        Iterator<Map.Entry<String, JsonNode>> fields = properties.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            JsonNode defaultValue = entry.getValue().get("default");
            if (defaultValue != null && !defaultValue.isNull() && !result.has(entry.getKey())) {
                result.set(entry.getKey(), defaultValue.deepCopy());
            }
        }
        return result;
    }
}
