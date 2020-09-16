/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.api.nodes;

import org.hkijena.jipipe.api.data.JIPipeData;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotates an {@link JIPipeGraphNode} with an output slot.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Repeatable(JIPipeOutputSlots.class)
public @interface JIPipeOutputSlot {
    /**
     * The data class
     *
     * @return data class
     */
    Class<? extends JIPipeData> value();

    /**
     * An optional slot name. Cannot be empty if autoCreate is true.
     *
     * @return slot name
     */
    String slotName() default "";

    /**
     * An optional inherited slot. Used if autoCreate is true
     * Either can be a valid input slot name to inherit the type of the input slot,
     * or can be '*' to inherit the type of the first slot
     *
     * @return inherited slot
     */
    String inheritedSlot() default "";

    /**
     * If true, {@link JIPipeGraphNode} automatically configures its slots based on annotations (unless a custom {@link org.hkijena.jipipe.api.data.JIPipeSlotConfiguration}
     * is provided.
     *
     * @return if auto-configuration is enabled
     */
    boolean autoCreate() default false;
}
