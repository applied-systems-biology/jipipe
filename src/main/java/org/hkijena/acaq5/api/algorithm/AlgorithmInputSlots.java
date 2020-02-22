package org.hkijena.acaq5.api.algorithm;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Required to allow multiple {@link AlgorithmInputSlot}
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface AlgorithmInputSlots {
    AlgorithmInputSlot[] value();
}
