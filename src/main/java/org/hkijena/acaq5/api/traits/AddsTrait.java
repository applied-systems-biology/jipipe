package org.hkijena.acaq5.api.traits;

import org.hkijena.acaq5.api.algorithm.ACAQAlgorithm;

import java.lang.annotation.*;

/**
 * Annotates an {@link ACAQAlgorithm} to add given trait to all output slots
 * Attaching this annotation automatically runs addsTraits() in {@link ACAQTraitConfiguration} if autoAdd is true
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Repeatable(AddsTraits.class)
public @interface AddsTrait {
    /**
     * Trait that will be added
     * @return
     */
    Class<? extends ACAQTrait> value();

    /**
     * If true, {@link ACAQAlgorithm} automatically adds the trait to all output slots
     * @return
     */
    boolean autoAdd() default true;
}
