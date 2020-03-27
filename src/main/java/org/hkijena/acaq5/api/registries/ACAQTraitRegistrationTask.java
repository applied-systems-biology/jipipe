package org.hkijena.acaq5.api.registries;

public interface ACAQTraitRegistrationTask {
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
