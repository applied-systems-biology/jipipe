package org.hkijena.acaq5.api.algorithm;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * General {@link ACAQAlgorithm} metadata
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface AlgorithmMetadata {
    /**
     * The algorithm category
     * @return
     */
    ACAQAlgorithmCategory category();

    /**
     * Where the algorithm is visible
     * @return
     */
    ACAQAlgorithmVisibility visibility() default ACAQAlgorithmVisibility.All;
}
