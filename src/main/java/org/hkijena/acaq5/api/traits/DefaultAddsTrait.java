package org.hkijena.acaq5.api.traits;

import java.lang.annotation.Annotation;

/**
 * Implementation of {@link AddsTrait} for usage in {@link org.hkijena.acaq5.api.registries.ACAQAlgorithmRegistry}
 */
public class DefaultAddsTrait implements AddsTrait {

    private Class<? extends ACAQTrait> mValue;
    private boolean mAutoAdd;

    public DefaultAddsTrait(Class<? extends ACAQTrait> mValue, boolean mAutoAdd) {
        this.mValue = mValue;
        this.mAutoAdd = mAutoAdd;
    }

    @Override
    public Class<? extends ACAQTrait> value() {
        return mValue;
    }

    @Override
    public boolean autoAdd() {
        return mAutoAdd;
    }

    @Override
    public Class<? extends Annotation> annotationType() {
        return AddsTrait.class;
    }
}
