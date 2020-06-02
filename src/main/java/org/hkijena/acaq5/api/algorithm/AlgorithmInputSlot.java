package org.hkijena.acaq5.api.algorithm;

import org.hkijena.acaq5.api.data.ACAQData;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

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
     * @return data class
     */
    Class<? extends ACAQData> value();

    /**
     * An optional slot name. Cannot be empty if autoCreate is true.
     *
     * @return slot name
     */
    String slotName() default "";

    /**
     * If true, {@link ACAQAlgorithm} automatically configures its slots based on annotations (unless a custom {@link org.hkijena.acaq5.api.data.ACAQSlotConfiguration}
     * is provided.
     *
     * @return if auto-configuration is enabled
     */
    boolean autoCreate() default false;
}
