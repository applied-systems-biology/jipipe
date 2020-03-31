package org.hkijena.acaq5.api.registries;

import org.hkijena.acaq5.api.ACAQValidatable;

/**
 * Task that registers {@link org.hkijena.acaq5.api.traits.ACAQTraitDeclaration}
 */
public interface ACAQTraitRegistrationTask extends ACAQValidatable {
    /**
     * Runs the registration
     */
    void register();

    /**
     * Returns true if the registration can be done
     *
     * @return true if the registration can be done
     */
    boolean canRegister();
}
