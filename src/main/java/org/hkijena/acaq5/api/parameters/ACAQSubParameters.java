package org.hkijena.acaq5.api.parameters;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotates the getter of an ACAQ sub-algorithm for inclusion into the set of parameters
 * Parameters are prepended with the specified name to make then differentiate from the already existing parameters
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ACAQSubParameters {
    /**
     * The name of the sub-algorithm
     *
     * @return name of the sub-algorithm
     */
    String value();
}
