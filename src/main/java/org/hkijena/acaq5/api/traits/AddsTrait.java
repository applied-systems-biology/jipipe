package org.hkijena.acaq5.api.traits;

import org.hkijena.acaq5.api.algorithm.ACAQAlgorithm;

import java.lang.annotation.*;

/**
 * Annotates an {@link ACAQAlgorithm} to add given trait to all output slots
 * Attaching this annotation automatically runs addsTraits() in {@link ACAQTraitConfiguration}
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Repeatable(AddsTraits.class)
public @interface AddsTrait {
    Class<? extends ACAQTrait> value();
}
