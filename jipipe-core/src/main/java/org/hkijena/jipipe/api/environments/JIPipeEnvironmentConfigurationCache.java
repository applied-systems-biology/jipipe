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

package org.hkijena.jipipe.api.environments;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.StampedLock;

/**
 * A thread-safe cache where {@link JIPipeEnvironmentConfigurator} can store fully configured environments for re-use.
 */
public class JIPipeEnvironmentConfigurationCache implements Map<JIPipeEnvironment, JIPipeEnvironment> {
    private final StampedLock lock = new StampedLock();
    private final HashMap<JIPipeEnvironment, JIPipeEnvironment> cache = new HashMap<>();

    @Override
    public int size() {
        long stamp = lock.readLock();
        try {
            return cache.size();
        } finally {
            lock.unlock(stamp);
        }
    }

    @Override
    public boolean isEmpty() {
        long stamp = lock.readLock();
        try {
            return cache.isEmpty();
        } finally {
            lock.unlock(stamp);
        }
    }

    @Override
    public boolean containsKey(Object key) {
        long stamp = lock.readLock();
        try {
            return cache.containsKey(key);
        } finally {
            lock.unlock(stamp);
        }
    }

    @Override
    public boolean containsValue(Object value) {
        long stamp = lock.readLock();
        try {
            return cache.containsValue(value);
        } finally {
            lock.unlock(stamp);
        }
    }

    @Override
    public JIPipeEnvironment get(Object key) {
        long stamp = lock.readLock();
        try {
            return cache.get(key);
        } finally {
            lock.unlock(stamp);
        }
    }

    @Override
    public @Nullable JIPipeEnvironment put(JIPipeEnvironment key, JIPipeEnvironment value) {
        if (!key.getClass().isAssignableFrom(value.getClass())) {
            throw new IllegalArgumentException(String.format("%s cannot be a resolved environment of %s", value.getClass(), key.getClass()));
        }
        long stamp = lock.writeLock();
        try {
            return cache.put(key, value);
        } finally {
            lock.unlock(stamp);
        }
    }

    @Override
    public JIPipeEnvironment remove(Object key) {
        long stamp = lock.writeLock();
        try {
            return cache.remove(key);
        } finally {
            lock.unlock(stamp);
        }
    }

    @Override
    public void putAll(@NotNull Map<? extends JIPipeEnvironment, ? extends JIPipeEnvironment> m) {
        for (Entry<? extends JIPipeEnvironment, ? extends JIPipeEnvironment> entry : m.entrySet()) {
            if (!entry.getKey().getClass().isAssignableFrom(entry.getValue().getClass())) {
                throw new IllegalArgumentException(String.format("%s cannot be a resolved environment of %s", entry.getValue().getClass(), entry.getKey().getClass()));
            }
        }
        long stamp = lock.writeLock();
        try {
            cache.putAll(m);
        } finally {
            lock.unlock(stamp);
        }
    }

    @Override
    public void clear() {
        long stamp = lock.writeLock();
        try {
            cache.clear();
        } finally {
            lock.unlock(stamp);
        }
    }

    @Override
    public @NotNull Set<JIPipeEnvironment> keySet() {
        long stamp = lock.readLock();
        try {
            return cache.keySet();
        } finally {
            lock.unlock(stamp);
        }
    }

    @Override
    public @NotNull Collection<JIPipeEnvironment> values() {
        long stamp = lock.readLock();
        try {
            return cache.values();
        } finally {
            lock.unlock(stamp);
        }
    }

    @Override
    public @NotNull Set<Entry<JIPipeEnvironment, JIPipeEnvironment>> entrySet() {
        long stamp = lock.readLock();
        try {
            return cache.entrySet();
        } finally {
            lock.unlock(stamp);
        }
    }
}
