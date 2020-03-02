package org.hkijena.acaq5.api.data.traits;

import org.hkijena.acaq5.api.traits.ACAQTrait;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotates an ACAQAlgorithm to be effective for a specified trait
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Repeatable(GoodForTraits.class)
public @interface GoodForTrait {
    Class<? extends ACAQTrait> value();
}

