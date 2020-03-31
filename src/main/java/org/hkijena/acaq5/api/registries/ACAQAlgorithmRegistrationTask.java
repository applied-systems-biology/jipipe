package org.hkijena.acaq5.api.registries;

import org.hkijena.acaq5.api.ACAQValidatable;

/**
 * A task for algorithm registration that can handle algorithm dependencies
 */
public interface ACAQAlgorithmRegistrationTask extends ACAQValidatable {
    /**
     * Runs the registration
     */
    void register();

    /**
     * Returns true if the registration can be done
     * This function should fail as fast as possible
     *
     * @return true if dependencies are met
     */
    boolean canRegister();
}
