package org.hkijena.jipipe.api.automation;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry of all instrumentation operations (built-in and plugin-registered).
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
}
