package org.hkijena.acaq5.extensions.standardparametereditors.ui.parametereditors;

import org.hkijena.acaq5.api.traits.ACAQTrait;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Settings for {@link org.hkijena.acaq5.api.traits.ACAQTraitDeclarationRef} parameters
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ACAQTraitDeclarationRefParameterSettings {
    /**
     * Control which traits are available
     * Set to {@link org.hkijena.acaq5.api.traits.ACAQDiscriminator} to only allow discriminators to be selected
     *
     * @return base class of traits that should be available
     */
    Class<? extends ACAQTrait> traitBaseClass() default ACAQTrait.class;
}
