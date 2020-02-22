package org.hkijena.acaq5.api.parameters;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotates a getter or setter function as parameter.
 * {@link ACAQParameterAccess} will look for this annotation to find parameters.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ACAQParameter {
    String value();
}
