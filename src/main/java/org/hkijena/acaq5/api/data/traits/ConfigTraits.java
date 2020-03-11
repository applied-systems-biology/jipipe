package org.hkijena.acaq5.api.data.traits;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Configures the behavior of a standard {@link ACAQDefaultMutableTraitConfiguration} that is generated by
 * {@link org.hkijena.acaq5.api.algorithm.ACAQAlgorithm}
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ConfigTraits {
    /**
     * If true, traits are automatically transferred the following way:
     * The union set of input data traits is transferred to all output data slots.
     *
     * @return
     */
    boolean autoTransfer() default true;

    /**
     * If true, users can add custom traits
     *
     * @return
     */
    boolean allowModify() default false;
}
