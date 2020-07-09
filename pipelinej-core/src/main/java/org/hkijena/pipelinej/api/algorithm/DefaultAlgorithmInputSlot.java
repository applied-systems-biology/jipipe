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
 * Default implementation of {@link AlgorithmInputSlot}
 */
public class DefaultAlgorithmInputSlot implements AlgorithmInputSlot {

    private Class<? extends ACAQData> value;
    private String slotName;
    private boolean autoCreate;

    /**
     * @param value      the value
     * @param slotName   the slot name
     * @param autoCreate if the slot should be automatically created
     */
    public DefaultAlgorithmInputSlot(Class<? extends ACAQData> value, String slotName, boolean autoCreate) {
        this.value = value;
        this.slotName = slotName;
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
    public boolean autoCreate() {
        return autoCreate;
    }

    @Override
    public Class<? extends Annotation> annotationType() {
        return AlgorithmInputSlot.class;
    }
}
