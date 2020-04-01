package org.hkijena.acaq5.api.algorithm;

import org.hkijena.acaq5.api.data.ACAQData;

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
