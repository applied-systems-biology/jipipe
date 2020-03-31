package org.hkijena.acaq5;

import org.scijava.plugin.SciJavaPlugin;

/**
 * A Java extension
 */
public interface ACAQJavaExtension extends SciJavaPlugin, ACAQDependency {

    /**
     * Returns the registry
     *
     * @return The registry
     */
    ACAQDefaultRegistry getRegistry();

    /**
     * Sets the registry
     *
     * @param registry The registry
     */
    void setRegistry(ACAQDefaultRegistry registry);

    /**
     * Registers custom modules into ACAQ5
     */
    void register();
}
