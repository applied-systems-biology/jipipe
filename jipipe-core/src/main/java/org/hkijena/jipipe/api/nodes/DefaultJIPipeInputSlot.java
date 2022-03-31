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
import org.hkijena.jipipe.api.data.JIPipeDataSlotRole;

import java.lang.annotation.Annotation;

/**
 * Default implementation of {@link JIPipeInputSlot}
 */
public class DefaultJIPipeInputSlot implements JIPipeInputSlot {

    private final Class<? extends JIPipeData> value;
    private final String slotName;
    private final String description;
    private final boolean autoCreate;
    private final boolean optional;
    private final JIPipeDataSlotRole role;

    /**
     * @param value       the value
     * @param slotName    the slot name
     * @param description the slot description
     * @param autoCreate  if the slot should be automatically created
     * @param optional    if the slot is optional
     * @param role the role
     */
    public DefaultJIPipeInputSlot(Class<? extends JIPipeData> value, String slotName, String description, boolean autoCreate, boolean optional, JIPipeDataSlotRole role) {
        this.value = value;
        this.slotName = slotName;
        this.description = description;
        this.autoCreate = autoCreate;
        this.optional = optional;
        this.role = role;
    }

    @Override
    public Class<? extends JIPipeData> value() {
        return value;
    }

    @Override
    public String slotName() {
        return slotName;
    }

    @Override
    public String description() {
        return description;
    }

    @Override
    public boolean autoCreate() {
        return autoCreate;
    }

    @Override
    public boolean optional() {
        return optional;
    }

    @Override
    public JIPipeDataSlotRole role() {
        return role;
    }

    @Override
    public Class<? extends Annotation> annotationType() {
        return JIPipeInputSlot.class;
    }
}
