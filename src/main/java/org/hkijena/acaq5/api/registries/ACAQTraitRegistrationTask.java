package org.hkijena.acaq5.api.registries;

import org.hkijena.acaq5.api.ACAQValidatable;

public interface ACAQTraitRegistrationTask extends ACAQValidatable {
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
