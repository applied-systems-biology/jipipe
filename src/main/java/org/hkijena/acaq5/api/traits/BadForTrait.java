package org.hkijena.acaq5.api.traits;

import java.lang.annotation.*;

/**
 * Annotates an ACAQAlgorithm to be ineffective against a specified trait
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Repeatable(BadForTraits.class)
public @interface BadForTrait {
    Class<? extends ACAQTrait> value();
}
