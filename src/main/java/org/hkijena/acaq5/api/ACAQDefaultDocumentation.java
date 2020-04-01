package org.hkijena.acaq5.api;

import java.lang.annotation.Annotation;

/**
 * Default implementation of {@link ACAQDocumentation}
 */
public class ACAQDefaultDocumentation implements ACAQDocumentation {
    private final String name;
    private final String description;

    /**
     * Creates a new instance
     *
     * @param name        The name
     * @param description The description
     */
    public ACAQDefaultDocumentation(String name, String description) {
        this.name = name;
        this.description = description;
    }

    @Override
    public String name() {
        return this.name;
    }

    @Override
    public String description() {
        return this.description;
    }

    @Override
    public Class<? extends Annotation> annotationType() {
        return ACAQDocumentation.class;
    }
}
