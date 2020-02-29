package org.hkijena.acaq5.api;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Used within ACAQ to annotate types and methods with documentation
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface ACAQDocumentation {
    String name() default "";

    String description() default "";
}
