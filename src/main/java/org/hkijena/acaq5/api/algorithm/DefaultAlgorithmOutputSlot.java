package org.hkijena.acaq5.api.algorithm;

import org.hkijena.acaq5.api.data.ACAQData;
import org.hkijena.acaq5.api.data.ACAQDataSlot;

import java.lang.annotation.Annotation;

public class DefaultAlgorithmOutputSlot implements AlgorithmOutputSlot {

    private Class<? extends ACAQData> mValue;
    private String mSlotName;
    private boolean mAutoCreate;

    public DefaultAlgorithmOutputSlot(Class<? extends ACAQData> mValue, String mSlotName, boolean mAutoCreate) {
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
        return AlgorithmOutputSlot.class;
    }
}
