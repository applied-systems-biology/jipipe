package org.hkijena.acaq5.api.traits;

import java.lang.annotation.Annotation;

/**
 * Implementation of {@link RemovesTrait} for usage in {@link org.hkijena.acaq5.api.registries.ACAQAlgorithmRegistry}
 */
public class DefaultRemovesTrait implements RemovesTrait {

    private Class<? extends ACAQTrait> mValue;
    private boolean mAutoRemove;

    public DefaultRemovesTrait(Class<? extends ACAQTrait> mValue, boolean mAutoRemove) {
        this.mValue = mValue;
        this.mAutoRemove = mAutoRemove;
    }

    @Override
    public Class<? extends ACAQTrait> value() {
        return mValue;
    }

    @Override
    public boolean autoRemove() {
        return mAutoRemove;
    }

    @Override
    public Class<? extends Annotation> annotationType() {
        return RemovesTrait.class;
    }
}
