package org.hkijena.acaq5.api.algorithm;

import org.hkijena.acaq5.api.data.ACAQDataSlot;

import java.lang.annotation.Annotation;

public class DefaultAlgorithmInputSlot implements AlgorithmInputSlot {

    private Class<? extends ACAQDataSlot<?>> mValue;
    private String mSlotName;
    private boolean mAutoCreate;

    public DefaultAlgorithmInputSlot(Class<? extends ACAQDataSlot<?>> mValue, String mSlotName, boolean mAutoCreate) {
        this.mValue = mValue;
        this.mSlotName = mSlotName;
        this.mAutoCreate = mAutoCreate;
    }

    @Override
    public Class<? extends ACAQDataSlot<?>> value() {
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
