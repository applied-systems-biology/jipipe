package org.hkijena.acaq5;

public @interface ACAQDocumentation {
    String name() default "";
    String description() default "";
}
