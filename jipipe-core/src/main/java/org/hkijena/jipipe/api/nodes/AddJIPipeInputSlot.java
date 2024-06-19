/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.api.nodes;

import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataSlotRole;

import java.lang.annotation.*;

/**
 * Annotates an {@link JIPipeGraphNode} with an input slot.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Repeatable(AddJIPipeInputSlots.class)
public @interface AddJIPipeInputSlot {
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
    String name() default "";

    /**
     * An optional description.
     *
     * @return slot description
     */
    String description() default "";

    /**
     * If true, {@link JIPipeGraphNode} automatically configures its slots based on annotations (unless a custom {@link org.hkijena.jipipe.api.data.JIPipeSlotConfiguration}
     * is provided.
     *
     * @return if autoconfiguration is enabled
     */
    boolean create() default false;

    /**
     * If true, the input is considered optional. The node will still run without a connection to the input (with an empty data table)
     *
     * @return if the input is optional
     */
    boolean optional() default false;

    /**
     * Assigns a role to the slot for internal usage within the node's code.
     * For example, this allows to distinguish data and parametric inputs from each other
     *
     * @return the role. Leave at Data if there is no special role.
     */
    JIPipeDataSlotRole role() default JIPipeDataSlotRole.Data;
}
