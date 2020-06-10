package org.hkijena.acaq5.extensions.parameters.editors;

import org.hkijena.acaq5.api.traits.ACAQTrait;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Settings for {@link ACAQTrait} parameters
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ACAQTraitParameterSettings {
    /**
     * Control which traits are available
     * Set to {@link ACAQTrait} to only allow discriminators to be selected
     *
     * @return the trait base class
     */
    Class<? extends ACAQTrait> traitBaseClass() default ACAQTrait.class;

    /**
     * If true, users can pick hidden annotations
     *
     * @return if users can also pick hidden annotations
     */
    boolean showHidden() default false;
}
