package org.hkijena.acaq5.api.data.traits;

import java.lang.annotation.*;

/**
 * Annotates an ACAQAlgorithm to be effective for a specified trait
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Repeatable(GoodForTraits.class)
public @interface GoodForTrait {
    String value();
}

