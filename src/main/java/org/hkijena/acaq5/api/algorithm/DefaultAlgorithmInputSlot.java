package org.hkijena.acaq5.api.algorithm;

import org.hkijena.acaq5.api.data.ACAQData;

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
