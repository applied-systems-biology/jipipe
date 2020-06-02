package org.hkijena.acaq5.api.traits;

import org.hkijena.acaq5.ACAQDependency;

import java.util.Set;

/**
 * Describes an {@link ACAQTrait}
 */
public interface ACAQTraitDeclaration {

    /**
     * Generates an Id for this declaration
     *
     * @return the ID
     */
    String getId();

    /**
     * Gets the class of the generated trait
     *
     * @return the trait class
     */
    Class<? extends ACAQTrait> getTraitClass();

    /**
     * Creates a new instance
     *
     * @return new instance
     */
    ACAQTrait newInstance();

    /**
     * Creates a new instance with a discriminator value.
     * If the trait is not discriminating, ths returns the same as newInstance()
     *
     * @param value discriminator value
     * @return new instance
     */
    ACAQTrait newInstance(String value);

    /**
     * Returns true if this trait is a discriminator
     *
     * @return if this trait is a discriminator
     */
    boolean isDiscriminator();

    /**
     * Returns true if the trait should be hidden from the user
     *
     * @return if the trait should be hidden from the user
     */
    boolean isHidden();

    /**
     * Returns the name
     *
     * @return the name
     */
    String getName();

    /**
     * Returns the description
     *
     * @return the description
     */
    String getDescription();

    /**
     * Returns all declarations that are parents of this declaration
     *
     * @return inherited trait types
     */
    Set<ACAQTraitDeclaration> getInherited();

    /**
     * Returns all dependencies
     *
     * @return dependencies
     */
    Set<ACAQDependency> getDependencies();
}
