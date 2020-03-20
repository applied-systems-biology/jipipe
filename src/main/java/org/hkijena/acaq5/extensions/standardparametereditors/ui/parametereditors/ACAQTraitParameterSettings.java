package org.hkijena.acaq5.extensions.standardparametereditors.ui.parametereditors;

import org.hkijena.acaq5.api.traits.ACAQTrait;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ACAQTraitParameterSettings {
    /**
     * Control which traits are available
     * Set to {@link org.hkijena.acaq5.api.traits.ACAQDiscriminator} to only allow discriminators to be selected
     *
     * @return
     */
    Class<? extends ACAQTrait> traitBaseClass() default ACAQTrait.class;
}
