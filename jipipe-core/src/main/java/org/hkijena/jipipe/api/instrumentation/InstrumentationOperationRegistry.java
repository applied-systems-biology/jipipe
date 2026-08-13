package org.hkijena.jipipe.api.instrumentation;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry of all instrumentation operations (built-in and plugin-registered).
 */
public class InstrumentationOperationRegistry {
    private final Map<String, InstrumentationOperation> operations = new ConcurrentHashMap<>();

    public void register(String id, InstrumentationOperation operation) {
        operations.put(id, operation);
    }

    public InstrumentationOperation get(String id) {
        return operations.get(id);
    }

    public boolean has(String id) {
        return operations.containsKey(id);
    }

    public Set<String> getIds() {
        return Collections.unmodifiableSet(operations.keySet());
    }

    public Map<String, InstrumentationOperation> getAll() {
        return Collections.unmodifiableMap(operations);
    }
}
