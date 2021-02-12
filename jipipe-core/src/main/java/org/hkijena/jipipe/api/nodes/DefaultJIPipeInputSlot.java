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

import java.lang.annotation.Annotation;

/**
 * Default implementation of {@link JIPipeInputSlot}
 */
public class DefaultJIPipeInputSlot implements JIPipeInputSlot {

    private Class<? extends JIPipeData> value;
    private String slotName;
    private boolean autoCreate;
    private boolean optional;

    /**
     * @param value      the value
     * @param slotName   the slot name
     * @param autoCreate if the slot should be automatically created
     * @param optional if the slot is optional
     */
    public DefaultJIPipeInputSlot(Class<? extends JIPipeData> value, String slotName, boolean autoCreate, boolean optional) {
        this.value = value;
        this.slotName = slotName;
        this.autoCreate = autoCreate;
        this.optional = optional;
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
    public boolean autoCreate() {
        return autoCreate;
    }

    @Override
    public boolean optional() {
        return optional;
    }

    @Override
    public Class<? extends Annotation> annotationType() {
        return JIPipeInputSlot.class;
    }
}
