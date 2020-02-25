package org.hkijena.acaq5.api.traits;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotates a trait as category or an internal trait
 * It will not be selectable by the user, but can be used for organizing higher-level traits
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface HiddenTrait {
}
