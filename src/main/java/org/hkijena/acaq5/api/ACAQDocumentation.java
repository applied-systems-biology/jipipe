package org.hkijena.acaq5.api;

public @interface ACAQDocumentation {
    String name() default "";
    String description() default "";
}
