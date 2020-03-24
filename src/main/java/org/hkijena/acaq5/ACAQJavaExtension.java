package org.hkijena.acaq5;

import org.scijava.plugin.SciJavaPlugin;

public interface ACAQJavaExtension extends SciJavaPlugin, ACAQDependency {
    /**
     * Registers custom modules into ACAQ5
     *
     * @param registryService
     */
    void register(ACAQDefaultRegistry registryService);
}
