package org.hkijena.acaq5.api.events;

import org.hkijena.acaq5.ACAQDependency;
import org.hkijena.acaq5.ACAQRegistry;

/**
 * Triggered by {@link org.hkijena.acaq5.ACAQRegistry} when an extension is registered
 */
public class ExtensionRegisteredEvent {
    private ACAQRegistry registry;
    private ACAQDependency extension;

    public ExtensionRegisteredEvent(ACAQRegistry registry, ACAQDependency extension) {
        this.registry = registry;
        this.extension = extension;
    }

    public ACAQRegistry getRegistry() {
        return registry;
    }

    public ACAQDependency getExtension() {
        return extension;
    }
}
