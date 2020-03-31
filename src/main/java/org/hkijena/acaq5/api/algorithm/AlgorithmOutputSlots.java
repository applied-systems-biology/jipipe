package org.hkijena.acaq5.api.algorithm;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Required to allow multiple {@link AlgorithmOutputSlot}
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface AlgorithmOutputSlots {
    /**
     * @return output slots
     */
    AlgorithmOutputSlot[] value();
}
