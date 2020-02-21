package org.hkijena.acaq5.api.algorithm;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface AlgorithmOutputSlots {
    AlgorithmOutputSlot[] value();
}
