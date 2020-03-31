package org.hkijena.acaq5.api.data.traits;

import java.lang.annotation.*;

/**
 * Annotates an ACAQAlgorithm to be ineffective against a specified trait
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Repeatable(BadForTraits.class)
public @interface BadForTrait {
    /**
     * @return trait id
     */
    String value();
}
