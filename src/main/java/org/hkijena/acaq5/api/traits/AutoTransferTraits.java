package org.hkijena.acaq5.api.traits;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotates an {@link org.hkijena.acaq5.api.ACAQAlgorithm} to transfer the union of all input slot traits
 * to all output slots.
 * Adding this annotation automatically executes transferAllToAll() in {@link org.hkijena.acaq5.api.ACAQTraitConfiguration}
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface AutoTransferTraits {
}
