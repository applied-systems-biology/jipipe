package org.hkijena.acaq5.api.parameters;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Holds a user-definable set of parameters
 */
public class ACAQDynamicParameterHolder {

    private BiMap<String, ACAQMutableParameterAccess> parameters = HashBiMap.create();

    public ACAQDynamicParameterHolder() {

    }

    public Map<String, ACAQParameterAccess> getDynamicParameters() {
        return Collections.unmodifiableMap(parameters);
    }

    @JsonGetter("parameters")
    private Map<String, ACAQMutableParameterAccess> getParameters() {
        return parameters;
    }

    @JsonSetter("parameters")
    private void setParameters(Map<String, ACAQMutableParameterAccess> parameters) {
        this.parameters.putAll(parameters);
    }

    public void addParameter(String key, Class<?> fieldClass) {
        if (parameters.containsKey(key))
            throw new IllegalArgumentException("Parameter with key " + key + " already exists!");
        parameters.put(key, new ACAQMutableParameterAccess(this, key, fieldClass));
    }

    public void removeParameter(String key) {
        parameters.remove(key);
    }

    public ACAQMutableParameterAccess getParameter(String key) {
        return parameters.get(key);
    }

    public <T> T getValue(String key) {
        return getParameter(key).get();
    }

    public <T> boolean setValue(String key, T value) {
        return getParameter(key).set(value);
    }

    /**
     * Finds all dynamic parameter holders in the parameter holder
     * Does not find child parameter holders.
     *
     * @param parameterHolder
     * @return
     */
    public static Map<String, ACAQDynamicParameterHolder> findDynamicParameterHolders(Object parameterHolder) {
        Map<String, ACAQDynamicParameterHolder> result = new HashMap<>();
        for (Method method : parameterHolder.getClass().getMethods()) {
            ACAQSubParameters[] subAlgorithms = method.getAnnotationsByType(ACAQSubParameters.class);
            if (subAlgorithms.length > 0) {
                try {
                    ACAQSubParameters subAlgorithmAnnotation = subAlgorithms[0];
                    Object subAlgorithm = method.invoke(parameterHolder);
                    if (subAlgorithm instanceof ACAQDynamicParameterHolder)
                        result.put(subAlgorithmAnnotation.value(), (ACAQDynamicParameterHolder) subAlgorithm);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return result;
    }
}
