package org.hkijena.acaq5.api.traits;

import org.hkijena.acaq5.api.ACAQTrait;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotates an ACAQAlgorithm to be effective for a specified trait
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface GoodForTrait {
    Class<? extends ACAQTrait> value();
}
