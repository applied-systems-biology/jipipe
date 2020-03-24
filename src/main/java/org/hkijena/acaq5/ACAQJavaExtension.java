package org.hkijena.acaq5;

import org.scijava.plugin.SciJavaPlugin;

public interface ACAQJavaExtension extends SciJavaPlugin, ACAQDependency {

    /**
     * Returns the registry
     *
     * @return
     */
    ACAQDefaultRegistry getRegistry();

    /**
     * Sets the registry
     *
     * @param registry
     */
    void setRegistry(ACAQDefaultRegistry registry);

    /**
     * Registers custom modules into ACAQ5
     */
    void register();
}
