package org.hkijena.acaq5.api.events;

import org.hkijena.acaq5.api.traits.ACAQTraitDeclaration;

/**
 * Triggered when a trait is registered
 */
public class TraitRegisteredEvent {
    private ACAQTraitDeclaration traitDeclaration;

    /**
     * @param traitDeclaration the registered trait type
     */
    public TraitRegisteredEvent(ACAQTraitDeclaration traitDeclaration) {
        this.traitDeclaration = traitDeclaration;
    }

    public ACAQTraitDeclaration getTraitDeclaration() {
        return traitDeclaration;
    }
}
