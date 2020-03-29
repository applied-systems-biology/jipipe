package org.hkijena.acaq5.api.parameters;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableList;
import com.google.common.eventbus.EventBus;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.events.ParameterStructureChangedEvent;
import org.hkijena.acaq5.utils.JsonUtils;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

/**
 * Holds a user-definable set of parameters
 */
public class ACAQDynamicParameterHolder implements ACAQCustomParameterHolder {

    private EventBus eventBus = new EventBus();
    private BiMap<String, ACAQMutableParameterAccess> parameters = HashBiMap.create();
    private Set<Class<?>> allowedTypes = new HashSet<>();
    private String name;
    private String description;
    private boolean allowModification = true;

    public ACAQDynamicParameterHolder(Class<?>... allowedTypes) {
        this.allowedTypes.addAll(Arrays.asList(allowedTypes));
    }

    public ACAQDynamicParameterHolder(ACAQDynamicParameterHolder other) {
        this.allowedTypes.addAll(other.allowedTypes);
        for (Map.Entry<String, ACAQMutableParameterAccess> entry : other.parameters.entrySet()) {
            ACAQMutableParameterAccess parameterAccess = new ACAQMutableParameterAccess(entry.getValue());
            parameterAccess.setParameterHolder(this);
            parameters.put(entry.getKey(), parameterAccess);
        }
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

            String subAlgorithmName = null;
            String subAlgorithmDescription = null;

            ACAQDocumentation[] documentations = method.getAnnotationsByType(ACAQDocumentation.class);
            if (documentations.length > 0) {
                subAlgorithmName = documentations[0].name();
                subAlgorithmDescription = documentations[0].description();
            }

            if (subAlgorithms.length > 0) {
                try {
                    ACAQSubParameters subAlgorithmAnnotation = subAlgorithms[0];
                    Object subAlgorithm = method.invoke(parameterHolder);
                    if (subAlgorithm instanceof ACAQDynamicParameterHolder) {
                        ((ACAQDynamicParameterHolder) subAlgorithm).name = subAlgorithmName;
                        ((ACAQDynamicParameterHolder) subAlgorithm).description = subAlgorithmDescription;
                        result.put(subAlgorithmAnnotation.value(), (ACAQDynamicParameterHolder) subAlgorithm);
                    }
                } catch (IllegalAccessException | InvocationTargetException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return result;
    }

    @Override
    public Map<String, ACAQParameterAccess> getCustomParameters() {
        return Collections.unmodifiableMap(parameters);
    }

    @JsonGetter("parameters")
    private Map<String, ACAQMutableParameterAccess> getParameters() {
        return Collections.unmodifiableMap(parameters);
    }

    @JsonSetter("parameters")
    private void setParameters(Map<String, ACAQMutableParameterAccess> parameters) {
        this.parameters.putAll(parameters);
    }

    public ACAQMutableParameterAccess addParameter(String key, Class<?> fieldClass) {
        if (parameters.containsKey(key))
            throw new IllegalArgumentException("Parameter with key " + key + " already exists!");
        ACAQMutableParameterAccess parameterAccess = new ACAQMutableParameterAccess(this, key, fieldClass);
        parameterAccess.setName(key);
        parameters.put(key, parameterAccess);
        getEventBus().post(new ParameterStructureChangedEvent(this));
        return parameterAccess;
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

    public Set<Class<?>> getAllowedTypes() {
        return allowedTypes;
    }

    public void setAllowedTypes(Set<Class<?>> allowedTypes) {
        this.allowedTypes = allowedTypes;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public void fromJson(JsonNode node) {
        parameters.clear();
        JsonNode parametersNode = node.get("parameters");
        for (Map.Entry<String, JsonNode> entry : ImmutableList.copyOf(parametersNode.fields())) {
            try {
                ACAQMutableParameterAccess parameterAccess = JsonUtils.getObjectMapper().readerFor(ACAQMutableParameterAccess.class).readValue(entry.getValue());
                parameterAccess.setKey(entry.getKey());
                parameterAccess.setParameterHolder(this);
                parameters.put(entry.getKey(), parameterAccess);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public boolean isAllowModification() {
        return allowModification;
    }

    public void setAllowModification(boolean allowModification) {
        this.allowModification = allowModification;
    }

    @Override
    public EventBus getEventBus() {
        return eventBus;
    }
}
