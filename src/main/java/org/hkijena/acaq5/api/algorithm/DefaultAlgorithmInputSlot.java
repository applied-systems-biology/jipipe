package org.hkijena.acaq5.api.algorithm;

import org.hkijena.acaq5.api.data.ACAQData;

import java.lang.annotation.Annotation;

/**
 * Default implementation of {@link AlgorithmInputSlot}
 */
public class DefaultAlgorithmInputSlot implements AlgorithmInputSlot {

    private Class<? extends ACAQData> mValue;
    private String mSlotName;
    private boolean mAutoCreate;

    /**
     * @param mValue the value
     * @param mSlotName the slot name
     * @param mAutoCreate if the slot should be automatically created
     */
    public DefaultAlgorithmInputSlot(Class<? extends ACAQData> mValue, String mSlotName, boolean mAutoCreate) {
        this.mValue = mValue;
        this.mSlotName = mSlotName;
        this.mAutoCreate = mAutoCreate;
    }

    @Override
    public Class<? extends ACAQData> value() {
        return mValue;
    }

    @Override
    public String slotName() {
        return mSlotName;
    }

    @Override
    public boolean autoCreate() {
        return mAutoCreate;
    }

    @Override
    public Class<? extends Annotation> annotationType() {
        return AlgorithmInputSlot.class;
    }
}
