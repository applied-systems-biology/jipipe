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

package org.hkijena.pipelinej.api.algorithm;

import org.hkijena.pipelinej.api.data.ACAQData;

import java.lang.annotation.Annotation;

/**
 * Default implementation of {@link AlgorithmOutputSlot}
 */
public class DefaultAlgorithmOutputSlot implements AlgorithmOutputSlot {

    private Class<? extends ACAQData> value;
    private String slotName;
    private String inheritedSlot;
    private boolean autoCreate;

    /**
     * @param value         the data class
     * @param slotName      the slot name
     * @param inheritedSlot An optional inherited slot.
     * @param autoCreate    Automatically create the slot if supported by the algorithm
     */
    public DefaultAlgorithmOutputSlot(Class<? extends ACAQData> value, String slotName, String inheritedSlot, boolean autoCreate) {
        this.value = value;
        this.slotName = slotName;
        this.inheritedSlot = inheritedSlot;
        this.autoCreate = autoCreate;
    }

    @Override
    public Class<? extends ACAQData> value() {
        return value;
    }

    @Override
    public String slotName() {
        return slotName;
    }

    @Override
    public String inheritedSlot() {
        return inheritedSlot;
    }

    @Override
    public boolean autoCreate() {
        return autoCreate;
    }

    @Override
    public Class<? extends Annotation> annotationType() {
        return AlgorithmOutputSlot.class;
    }
}
