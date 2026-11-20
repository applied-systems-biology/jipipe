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
import org.hkijena.jipipe.utils.json.JsonUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Validates JSON values against simplified JSON schemas that are given as JSON nodes.
 * Supported keywords: type (object, string, integer, number, boolean, array), required, properties, enum,
 * and additionalProperties (false only). Unknown keywords are ignored.
 * <p>
 * The schema is read as a plain {@link JsonNode}: it may be rendered by the AST of this package
 * or arrive as raw JSON from an external source.
 */
public class JsonSchemaValidator {
    private JsonSchemaValidator() {
    }

    /**
     * Validates arguments against a schema and returns the list of errors (empty when valid).
     * Null or missing arguments are treated as an empty object.
     * <p>
     * Fail-closed: a null or non-object schema (e.g. a malformed schema literal) produces a
     * single descriptive error instead of silently passing.
     * <p>
     * The error messages are part of the stable contract with existing callers and must not change.
     */
    public static List<String> validate(JsonNode schema, JsonNode args) {
        List<String> errors = new ArrayList<>();
        if (schema == null || schema.isNull() || !schema.isObject()) {
            errors.add("operation schema is missing or malformed");
            return errors;
        }
        JsonNode argsNode = args == null || args.isNull() ? JsonUtils.getObjectMapper().createObjectNode() : args;
        validateNode(schema, argsNode, null, errors);
        return errors;
    }

    private static void validateNode(JsonNode schema, JsonNode value, String path, List<String> errors) {
        JsonNode typeNode = schema.get("type");
        if (typeNode != null && typeNode.isTextual() && !matchesType(typeNode.asText(), value)) {
            errors.add(at(path) + ": expected type '" + typeNode.asText() + "' but got " + describe(value));
            return;
        }
        JsonNode enumNode = schema.get("enum");
        if (enumNode != null && enumNode.isArray() && !containsNode(enumNode, value)) {
            errors.add(at(path) + ": must be one of " + enumNode + " but got " + describe(value));
        }
        if (value.isObject()) {
            validateObject(schema, value, path, errors);
        }
    }

    private static void validateObject(JsonNode schema, JsonNode value, String path, List<String> errors) {
        JsonNode properties = schema.get("properties");
        JsonNode required = schema.get("required");
        if (required != null && required.isArray()) {
            for (JsonNode requiredName : required) {
                String name = requiredName.asText();
                if (!value.has(name) || value.get(name).isNull()) {
                    errors.add(child(path, name) + " is required");
                }
            }
        }
        Set<String> allowed = new LinkedHashSet<>();
        if (properties != null && properties.isObject()) {
            properties.fieldNames().forEachRemaining(allowed::add);
            Iterator<Map.Entry<String, JsonNode>> fields = value.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                JsonNode propertySchema = properties.get(field.getKey());
                if (propertySchema != null) {
                    validateNode(propertySchema, field.getValue(), child(path, field.getKey()), errors);
                }
            }
        }
        JsonNode additionalProperties = schema.get("additionalProperties");
        if (additionalProperties != null && additionalProperties.isBoolean() && !additionalProperties.asBoolean()) {
            value.fieldNames().forEachRemaining(name -> {
                if (!allowed.contains(name)) {
                    errors.add(child(path, name) + " is not an allowed property");
                }
            });
        }
    }

    private static boolean matchesType(String type, JsonNode value) {
        return switch (type) {
            case "object" -> value.isObject();
            case "string" -> value.isTextual();
            case "integer" -> value.isIntegralNumber()
                    || (value.isFloatingPointNumber() && value.canConvertToExactIntegral());
            case "number" -> value.isNumber();
            case "boolean" -> value.isBoolean();
            case "array" -> value.isArray();
            default -> true;
        };
    }

    private static boolean containsNode(JsonNode array, JsonNode value) {
        for (JsonNode option : array) {
            if (option.equals(value)) {
                return true;
            }
        }
        return false;
    }

    private static String child(String path, String name) {
        return path == null ? name : path + "." + name;
    }

    private static String at(String path) {
        return path == null ? "parameters" : path;
    }

    private static String describe(JsonNode value) {
        if (value == null || value.isNull()) {
            return "null";
        }
        if (value.isTextual()) {
            return "'" + value.asText() + "'";
        }
        return value.getNodeType().name().toLowerCase().replace("_", " ");
    }
}
