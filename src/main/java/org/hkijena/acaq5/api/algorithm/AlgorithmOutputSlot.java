package org.hkijena.acaq5.api.algorithm;

import org.hkijena.acaq5.api.data.ACAQData;

import java.lang.annotation.*;

/**
 * Annotates an {@link ACAQAlgorithm} with an output slot.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Repeatable(AlgorithmOutputSlots.class)
public @interface AlgorithmOutputSlot {
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
     * An optional inherited slot. Used if autoCreate is true
     * Either can be a valid input slot name to inherit the type of the input slot,
     * or can be '*' to inherit the type of the first slot
     *
     * @return
     */
    String inheritedSlot() default "";

    /**
     * If true, {@link ACAQAlgorithm} automatically configures its slots based on annotations (unless a custom {@link org.hkijena.acaq5.api.data.ACAQSlotConfiguration}
     * is provided.
     *
     * @return
     */
    boolean autoCreate() default false;
}
