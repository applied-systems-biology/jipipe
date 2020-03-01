package org.hkijena.acaq5.api.traits.global;

import org.hkijena.acaq5.api.algorithm.ACAQAlgorithm;
import org.hkijena.acaq5.api.traits.ACAQTrait;

import java.lang.annotation.*;

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

    /**
     * If true, {@link ACAQAlgorithm} will automatically remove the trait from all output
     *
     * @return
     */
    boolean autoRemove() default true;
}
