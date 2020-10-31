package org.hkijena.jipipe.extensions.parameters.expressions;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Information about the parameters
 */
public class ParameterInfo {
    private final String name;
    private final String description;
    private final Set<Class<?>> types;

    public ParameterInfo(String name, String description, Class<?>... types) {
        this.name = name;
        this.description = description;
        this.types = new HashSet<>(Arrays.asList(types));
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Set<Class<?>> getTypes() {
        return types;
    }
}
