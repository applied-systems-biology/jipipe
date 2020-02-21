package org.hkijena.acaq5.api.algorithm;

import org.hkijena.acaq5.api.data.ACAQDataSlot;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotates an {@link ACAQAlgorithm} with an output slot.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface AlgorithmOutputSlot {
    /**
     * The slot class
     * @return
     */
    Class<? extends ACAQDataSlot<?>> value();

    /**
     * An optional slot name. Cannot be empty if autoCreate is true.
     * @return
     */
    String slotName() default "";

    /**
     * If true, {@link ACAQAlgorithm} automatically configures its slots based on annotations (unless a custom {@link org.hkijena.acaq5.api.data.ACAQSlotConfiguration}
     * is provided.
     * @return
     */
    boolean autoCreate() default false;
}
