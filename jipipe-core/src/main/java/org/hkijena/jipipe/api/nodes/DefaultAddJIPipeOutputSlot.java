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

import java.lang.annotation.Annotation;

/**
 * Default implementation of {@link AddJIPipeOutputSlot}
 */
public class DefaultAddJIPipeOutputSlot implements AddJIPipeOutputSlot {

    private final Class<? extends JIPipeData> value;
    private final String slotName;
    private final String description;
    private final String inheritedSlot;
    private final boolean autoCreate;
    private final JIPipeDataSlotRole role;

    /**
     * @param value         the data class
     * @param slotName      the slot name
     * @param description   the description
     * @param inheritedSlot An optional inherited slot.
     * @param autoCreate    Automatically create the slot if supported by the algorithm
     * @param role          the role
     */
    public DefaultAddJIPipeOutputSlot(Class<? extends JIPipeData> value, String slotName, String description, String inheritedSlot, boolean autoCreate, JIPipeDataSlotRole role) {
        this.value = value;
        this.slotName = slotName;
        this.description = description;
        this.inheritedSlot = inheritedSlot;
        this.autoCreate = autoCreate;
        this.role = role;
    }

    @Override
    public JIPipeDataSlotRole role() {
        return role;
    }

    @Override
    public Class<? extends JIPipeData> value() {
        return value;
    }

    @Override
    public String name() {
        return slotName;
    }

    @Override
    @Deprecated
    public String inheritedSlot() {
        return inheritedSlot;
    }

    @Override
    public boolean create() {
        return autoCreate;
    }

    @Override
    public String description() {
        return description;
    }

    @Override
    public Class<? extends Annotation> annotationType() {
        return AddJIPipeOutputSlot.class;
    }
}
