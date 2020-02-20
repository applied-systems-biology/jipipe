package org.hkijena.acaq5.api.traits;

import org.hkijena.acaq5.api.ACAQTrait;

import java.lang.annotation.*;

/**
 * Annotates an {@link org.hkijena.acaq5.api.ACAQAlgorithm} to add given trait to all output slots
 * Attaching this annotation automatically runs addsTraits() in {@link org.hkijena.acaq5.api.ACAQTraitConfiguration}
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Repeatable(AddsTraits.class)
public @interface AddsTrait {
    Class<? extends ACAQTrait> value();
}
