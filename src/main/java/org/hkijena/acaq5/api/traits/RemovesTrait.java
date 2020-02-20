package org.hkijena.acaq5.api.traits;

import org.hkijena.acaq5.api.ACAQTrait;

import java.lang.annotation.*;

/**
 * Annotates an {@link org.hkijena.acaq5.api.ACAQAlgorithm} to remove given trait from all output slots
 * Attaching this annotation automatically runs removesTraits() in {@link org.hkijena.acaq5.api.ACAQTraitConfiguration}
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Repeatable(RemovesTraits.class)
public @interface RemovesTrait {
    /**
     * The removed trait of same or specialized type
     * @return
     */
    Class<? extends ACAQTrait> value();
}
