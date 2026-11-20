package org.hkijena.jipipe.api.automation;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry of all automation operations (built-in and plugin-registered).
 */
public class AutomationOperationRegistry {
    private final Map<String, AutomationOperation> operations = new ConcurrentHashMap<>();

    public void register(String id, AutomationOperation operation) {
        operations.put(id, operation);
    }

    public AutomationOperation get(String id) {
        return operations.get(id);
    }

    public boolean has(String id) {
        return operations.containsKey(id);
    }

    public Set<String> getIds() {
        return Collections.unmodifiableSet(operations.keySet());
    }

    public Map<String, AutomationOperation> getAll() {
        return Collections.unmodifiableMap(operations);
    }

    /**
     * Validates the parameters against the operation's input schema, fills absent optional
     * parameters from the schema's top-level {@code default} keywords (unless the operation
     * opts out via {@link AutomationOperation#appliesSchemaDefaults()}), and executes the
     * operation with the effective arguments.
     * Throws an {@link IllegalArgumentException} listing the validation errors without invoking the operation.
     */
    public JsonNode execute(String id, AutomationContext ctx, JsonNode params) throws Exception {
        AutomationOperation operation = operations.get(id);
        if (operation == null) {
            throw new IllegalArgumentException("Unknown operation: " + id);
        }
        JsonNode schema = operation.getInputSchema();
        List<String> errors = AutomationSchemaUtils.validate(schema, params);
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException("Invalid parameters for operation '" + id + "': "
                    + String.join("; ", errors));
        }
        JsonNode effectiveParams = operation.appliesSchemaDefaults()
                ? AutomationSchemaUtils.applyDefaults(schema, params)
                : params;
        return operation.execute(ctx, effectiveParams);
    }
}
