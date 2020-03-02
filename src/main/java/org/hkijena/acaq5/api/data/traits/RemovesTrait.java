package org.hkijena.acaq5.api.data.traits;

import org.hkijena.acaq5.api.algorithm.ACAQAlgorithm;
import org.hkijena.acaq5.api.traits.ACAQTrait;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotates an {@link ACAQAlgorithm} to remove given trait from all output slots
 * Attaching this annotation automatically runs removesTraits() in {@link ACAQTraitConfiguration} if autoRemove is true
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Repeatable(RemovesTraits.class)
public @interface RemovesTrait {
    /**
     * The removed trait of same or specialized type
     *
     * @return
     */
    Class<? extends ACAQTrait> value();
}
