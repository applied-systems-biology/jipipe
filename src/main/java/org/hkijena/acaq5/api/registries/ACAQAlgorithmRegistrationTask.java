package org.hkijena.acaq5.api.registries;

/**
 * A task for algorithm registration that can handle algorithm dependencies
 */
public interface ACAQAlgorithmRegistrationTask {
    /**
     * Runs the registration
     */
    void register();

    /**
     * Returns true if the registration can be done
     *
     * @return
     */
    boolean canRegister();
}
