package org.hkijena.acaq5.api.traits;

import java.util.Set;

public interface ACAQTraitDeclaration {

    /**
     * Generates an Id for this declaration
     *
     * @return
     */
    String getId();

    /**
     * Gets the class of the generated trait
     *
     * @return
     */
    Class<? extends ACAQTrait> getTraitClass();

    /**
     * Creates a new instance
     *
     * @return
     */
    ACAQTrait newInstance();

    /**
     * Creates a new instance with a discriminator value.
     * If the trait is not discriminating, ths returns the same as newInstance()
     *
     * @param value
     * @return
     */
    ACAQTrait newInstance(String value);

    /**
     * Returns true if this trait is a discriminator
     *
     * @return
     */
    boolean isDiscriminator();

    /**
     * Returns true if the trait should be hidden from the user
     * @return
     */
    boolean isHidden();

    /**
     * Returns the algorithm name
     *
     * @return
     */
    String getName();

    /**
     * Returns the algorithm description
     *
     * @return
     */
    String getDescription();

    /**
     * Returns all declarations that are parents of this declaration
     *
     * @return
     */
    Set<ACAQTraitDeclaration> getInherited();
}
