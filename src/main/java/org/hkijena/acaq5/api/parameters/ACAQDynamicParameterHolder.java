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
    private boolean allowUserModification = true;
    private boolean delayEvents = false;

    /**
     * @param allowedTypes The parameter types that can be added
     */
    public ACAQDynamicParameterHolder(Class<?>... allowedTypes) {
        this.allowedTypes.addAll(Arrays.asList(allowedTypes));
    }

    /**
     * Copies the parameters from the original
     *
     * @param other The original
     */
    public ACAQDynamicParameterHolder(ACAQDynamicParameterHolder other) {
        this.allowedTypes.addAll(other.allowedTypes);
        for (Map.Entry<String, ACAQMutableParameterAccess> entry : other.parameters.entrySet()) {
            ACAQMutableParameterAccess parameterAccess = new ACAQMutableParameterAccess(entry.getValue());
            parameterAccess.setParameterHolder(this);
            parameters.put(entry.getKey(), parameterAccess);
        }
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

    /**
     * Adds a new parameter.
     * The parameterHolder attribute is changed
     *
     * @param parameterAccess the parameter
     * @return the parameter access
     */
    public ACAQMutableParameterAccess addParameter(ACAQMutableParameterAccess parameterAccess) {
        if (parameters.containsKey(parameterAccess.getKey()))
            throw new IllegalArgumentException("Parameter with key " + parameterAccess.getKey() + " already exists!");
        parameterAccess.setParameterHolder(this);
        parameters.put(parameterAccess.getKey(), parameterAccess);
        if (!delayEvents)
            getEventBus().post(new ParameterStructureChangedEvent(this));
        return parameterAccess;
    }

    /**
     * Adds a new parameter
     *
     * @param key        A unique key
     * @param fieldClass The parameter class
     * @return The created parameter access
     */
    public ACAQMutableParameterAccess addParameter(String key, Class<?> fieldClass) {
        ACAQMutableParameterAccess parameterAccess = new ACAQMutableParameterAccess(this, key, fieldClass);
        parameterAccess.setName(key);
        return addParameter(parameterAccess);
    }

    /**
     * Removes all parameters
     */
    public void clear() {
        parameters.clear();
        if (!delayEvents)
            getEventBus().post(new ParameterStructureChangedEvent(this));
    }

    /**
     * Removes a parameter
     *
     * @param key The parameter key
     */
    public void removeParameter(String key) {
        parameters.remove(key);
        if (!delayEvents)
            getEventBus().post(new ParameterStructureChangedEvent(this));
    }

    /**
     * Gets a parameter by its key
     *
     * @param key The parameter key
     * @return The parameter access
     */
    public ACAQMutableParameterAccess getParameter(String key) {
        return parameters.get(key);
    }

    /**
     * Gets a parameter value
     *
     * @param key The parameter key
     * @param <T> The parameter type
     * @return The parameter value
     */
    public <T> T getValue(String key) {
        return getParameter(key).get();
    }

    /**
     * Sets a parameter value
     *
     * @param key   The parameter key
     * @param value The parameter value
     * @param <T>   The parameter type
     * @return True if setting the value was successful
     */
    public <T> boolean setValue(String key, T value) {
        return getParameter(key).set(value);
    }

    /**
     * @return Allowed parameter types
     */
    public Set<Class<?>> getAllowedTypes() {
        return allowedTypes;
    }

    /**
     * Sets allowed parameter types
     *
     * @param allowedTypes Parameter types
     */
    public void setAllowedTypes(Set<Class<?>> allowedTypes) {
        this.allowedTypes = allowedTypes;
    }

    /**
     * @return Parameter holder name
     */
    public String getName() {
        return name;
    }

    /**
     * @return Parameter holder description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Loads the holder from JSON
     *
     * @param node JSON data
     */
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

    /**
     * @return True if modification by users is allowed
     */
    public boolean isAllowUserModification() {
        return allowUserModification;
    }

    /**
     * Enabled/disables if modification by users is allowed
     *
     * @param allowUserModification True if modification is allowed
     */
    public void setAllowUserModification(boolean allowUserModification) {
        this.allowUserModification = allowUserModification;
    }

    @Override
    public EventBus getEventBus() {
        return eventBus;
    }

    /**
     * Returns true if there is a parameter with given key
     *
     * @param key the parameter key
     * @return if there is a parameter with given key
     */
    public boolean containsKey(String key) {
        return parameters.containsKey(key);
    }

    /**
     * Returns the parameter with key
     *
     * @param key the parameter key
     * @return the parameter access
     */
    public ACAQParameterAccess get(String key) {
        return parameters.get(key);
    }

    /**
     * Halts all {@link ParameterStructureChangedEvent} until endModificationBlock() is called.
     * Use this method for many changes at once
     */
    public void beginModificationBlock() {
        if (delayEvents)
            throw new UnsupportedOperationException("Modification block already started!");
        delayEvents = true;
    }

    /**
     * Ends a modification block started by beginModificationBlock().
     * Triggers a {@link ParameterStructureChangedEvent}
     */
    public void endModificationBlock() {
        if (!delayEvents)
            throw new UnsupportedOperationException("No modification block!");
        delayEvents = false;
        eventBus.post(new ParameterStructureChangedEvent(this));
    }

    /**
     * Finds all dynamic parameter holders in the parameter holder
     * Does not find child parameter holders.
     *
     * @param parameterHolder The parameter holder
     * @return Map from string to parameter holder
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
}
