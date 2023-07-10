package org.hkijena.jipipe.extensions.imagejalgorithms.utils;

import org.hkijena.jipipe.api.parameters.JIPipeDynamicParameterCollection;

import java.lang.reflect.Method;
import java.util.List;

public class OMEAccessorTemplate {
    private final String name;
    private final String description;
    private final Method method;
    private final JIPipeDynamicParameterCollection parameterCollection;
    private final List<String> parameterIds;

    public OMEAccessorTemplate(String name, String description, Method method, JIPipeDynamicParameterCollection parameterCollection, List<String> parameterIds) {
        this.name = name;
        this.description = description;
        this.method = method;
        this.parameterCollection = parameterCollection;
        this.parameterIds = parameterIds;
    }

    public String getName() {
        return name;
    }

    public JIPipeDynamicParameterCollection getParameterCollection() {
        return parameterCollection;
    }

    public List<String> getParameterIds() {
        return parameterIds;
    }

    public String getDescription() {
        return description;
    }

    public Method getMethod() {
        return method;
    }
}
