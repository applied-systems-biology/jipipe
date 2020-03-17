package org.hkijena.acaq5.api.registries;

/**
 * A task for algorithm registration that can handle algorithm dependencies
 */
public interface ACAQAlgorithmRegistrationTask {
    void register();

    boolean canRegister();
}
