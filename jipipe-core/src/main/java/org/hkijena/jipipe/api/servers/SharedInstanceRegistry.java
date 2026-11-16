package org.hkijena.jipipe.api.servers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SharedInstanceRegistry<T extends SharedInstanceRegistration> {

    private final Map<String, T> instances = new ConcurrentHashMap<>();

    public void register(T registration) {
        instances.put(registration.getInstanceId(), registration);
    }

    public void unregister(String instanceId) {
        instances.remove(instanceId);
    }

    public int getInstanceCount() {
        return instances.size();
    }

    public Collection<T> getAll() {
        return instances.values();
    }

    public int removeDeadInstances() {
        int removed = 0;
        for (T reg : new ArrayList<>(instances.values())) {
            if (!SharedServerDiscoveryFile.isPidAlive(reg.getJipipePid())) {
                instances.remove(reg.getInstanceId());
                removed++;
            }
        }
        return removed;
    }
}
