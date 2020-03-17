package org.hkijena.acaq5.api.algorithm;

import org.hkijena.acaq5.api.data.ACAQData;

import java.lang.annotation.*;

/**
 * Annotates an {@link ACAQAlgorithm} with an input slot.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Repeatable(AlgorithmInputSlots.class)
public @interface AlgorithmInputSlot {
    /**
     * The data class
     *
     * @return
     */
    Class<? extends ACAQData> value();

    /**
     * An optional slot name. Cannot be empty if autoCreate is true.
     *
     * @return
     */
    String slotName() default "";

    /**
     * If true, {@link ACAQAlgorithm} automatically configures its slots based on annotations (unless a custom {@link org.hkijena.acaq5.api.data.ACAQSlotConfiguration}
     * is provided.
     *
     * @return
     */
    boolean autoCreate() default false;
}
