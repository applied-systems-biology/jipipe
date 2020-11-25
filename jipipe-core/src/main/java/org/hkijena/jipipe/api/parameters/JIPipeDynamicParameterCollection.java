/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.api.parameters;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableList;
import com.google.common.eventbus.EventBus;
import org.hkijena.jipipe.api.JIPipeValidatable;
import org.hkijena.jipipe.api.JIPipeValidityReport;
import org.hkijena.jipipe.api.events.ParameterStructureChangedEvent;
import org.hkijena.jipipe.api.exceptions.UserFriendlyRuntimeException;
import org.hkijena.jipipe.utils.JsonDeserializable;
import org.hkijena.jipipe.utils.JsonUtils;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Holds a user-definable set of parameters
 */
public class JIPipeDynamicParameterCollection implements JIPipeCustomParameterCollection, JIPipeValidatable, JsonDeserializable {

    private EventBus eventBus = new EventBus();
    private BiMap<String, JIPipeMutableParameterAccess> dynamicParameters = HashBiMap.create();
    private Set<Class<?>> allowedTypes = new HashSet<>();
    private Function<UserParameterDefinition, JIPipeMutableParameterAccess> instanceGenerator;
    private boolean allowUserModification = true;
    private boolean delayEvents = false;

    /**
     * Creates a new instance with user modification enabled
     *
     * @param allowedTypes The parameter types that can be added
     */
    public JIPipeDynamicParameterCollection(Class<?>... allowedTypes) {
        this.allowedTypes.addAll(Arrays.asList(allowedTypes));
    }

    /**
     * Creates a new instance
     *
     * @param allowUserModification let user modify this collection
     * @param allowedTypes          The parameter types that can be added by the user (ignored if user cannot add)
     */
    public JIPipeDynamicParameterCollection(boolean allowUserModification, Class<?>... allowedTypes) {
        this.allowUserModification = allowUserModification;
        this.allowedTypes.addAll(Arrays.asList(allowedTypes));
    }

    /**
     * Copies the parameters from the original
     *
     * @param other The original
     */
    public JIPipeDynamicParameterCollection(JIPipeDynamicParameterCollection other) {
        this.allowedTypes.addAll(other.allowedTypes);
        this.allowUserModification = other.allowUserModification;
        this.instanceGenerator = other.instanceGenerator;
        for (Map.Entry<String, JIPipeMutableParameterAccess> entry : other.dynamicParameters.entrySet()) {
            JIPipeMutableParameterAccess parameterAccess = new JIPipeMutableParameterAccess(entry.getValue());
            parameterAccess.setParameterHolder(this);
            dynamicParameters.put(entry.getKey(), parameterAccess);
        }
    }

    /**
     * Creates a new instance
     *
     * @param allowUserModification let user modify this collection
     * @param allowedTypes          The parameter types that can be added by the user (ignored if user cannot add)
     */
    public JIPipeDynamicParameterCollection(boolean allowUserModification, Set<JIPipeParameterTypeInfo> allowedTypes) {
        this.allowUserModification = allowUserModification;
        for (JIPipeParameterTypeInfo allowedType : allowedTypes) {
            this.allowedTypes.add(allowedType.getFieldClass());
        }
    }

    @Override
    public Map<String, JIPipeParameterAccess> getParameters() {
        return Collections.unmodifiableMap(dynamicParameters);
    }

    @JsonGetter("parameters")
    private Map<String, JIPipeMutableParameterAccess> getDynamicParameters() {
        return Collections.unmodifiableMap(dynamicParameters);
    }

    @JsonSetter("parameters")
    private void setDynamicParameters(Map<String, JIPipeMutableParameterAccess> dynamicParameters) {
        this.dynamicParameters.putAll(dynamicParameters);
    }

    /**
     * Adds a new parameter.
     * The parameterHolder attribute is changed
     *
     * @param parameterAccess the parameter
     * @return the parameter access or null if it was cancelled
     */
    public JIPipeMutableParameterAccess addParameter(JIPipeMutableParameterAccess parameterAccess) {
        if (dynamicParameters.containsKey(parameterAccess.getKey()))
            throw new IllegalArgumentException("Parameter with key " + parameterAccess.getKey() + " already exists!");
        parameterAccess.setParameterHolder(this);
        ParameterAddingEvent event = new ParameterAddingEvent(this, parameterAccess);
        eventBus.post(event);
        if (event.isCancel())
            return null;
        dynamicParameters.put(parameterAccess.getKey(), parameterAccess);
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
    public JIPipeMutableParameterAccess addParameter(String key, Class<?> fieldClass) {
        JIPipeMutableParameterAccess parameterAccess;
        if (instanceGenerator != null) {
            parameterAccess = instanceGenerator.apply(new UserParameterDefinition(this, key, fieldClass));
        } else {
            parameterAccess = new JIPipeMutableParameterAccess(this, key, fieldClass);
            parameterAccess.setName(key);
        }

        return addParameter(parameterAccess);
    }

    /**
     * Removes all parameters
     */
    public void clear() {
        dynamicParameters.clear();
        if (!delayEvents)
            getEventBus().post(new ParameterStructureChangedEvent(this));
    }

    /**
     * Removes a parameter
     *
     * @param key The parameter key
     */
    public void removeParameter(String key) {
        dynamicParameters.remove(key);
        if (!delayEvents)
            getEventBus().post(new ParameterStructureChangedEvent(this));
    }

    /**
     * Gets a parameter by its key
     *
     * @param key The parameter key
     * @return The parameter access
     */
    public JIPipeMutableParameterAccess getParameter(String key) {
        return dynamicParameters.get(key);
    }

    /**
     * Gets a parameter value
     *
     * @param <T>   The parameter type
     * @param key   The parameter key
     * @param klass The returned type
     * @return The parameter value
     */
    public <T> T getValue(String key, Class<T> klass) {
        return getParameter(key).get(klass);
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
     * Loads the holder from JSON
     *
     * @param node JSON data
     */
    @Override
    public void fromJson(JsonNode node) {
        dynamicParameters.clear();
        JsonNode parametersNode = node.get("parameters");
        for (Map.Entry<String, JsonNode> entry : ImmutableList.copyOf(parametersNode.fields())) {
            try {
                JIPipeMutableParameterAccess parameterAccess = JsonUtils.getObjectMapper().readerFor(JIPipeMutableParameterAccess.class).readValue(entry.getValue());
                parameterAccess.setKey(entry.getKey());
                parameterAccess.setParameterHolder(this);
                dynamicParameters.put(entry.getKey(), parameterAccess);
            } catch (IOException e) {
                throw new UserFriendlyRuntimeException(e, "Unable to read parameter from JSON!", "User-modifiable parameters", "There is essential information missing in the JSON data.",
                        "Please check if the JSON data is valid.");
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
        return dynamicParameters.containsKey(key);
    }

    /**
     * Returns the parameter with key
     *
     * @param key the parameter key
     * @return the parameter access
     */
    public JIPipeParameterAccess get(String key) {
        return dynamicParameters.get(key);
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
     * Copies the parameter configuration into the target
     *
     * @param target the target configuration
     */
    public void copyTo(JIPipeDynamicParameterCollection target) {
        target.setAllowUserModification(isAllowUserModification());
        target.setAllowedTypes(getAllowedTypes());
        target.beginModificationBlock();
        target.clear();
        for (Map.Entry<String, JIPipeMutableParameterAccess> entry : dynamicParameters.entrySet()) {
            target.addParameter(new JIPipeMutableParameterAccess(entry.getValue()));
        }
        target.endModificationBlock();
    }

    @Override
    public void reportValidity(JIPipeValidityReport report) {
        for (Map.Entry<String, JIPipeMutableParameterAccess> entry : dynamicParameters.entrySet()) {
            Object o = entry.getValue().get(Object.class);
            if (o instanceof JIPipeValidatable) {
                report.forCategory(entry.getKey()).report((JIPipeValidatable) o);
            }
        }
    }

    public Function<UserParameterDefinition, JIPipeMutableParameterAccess> getInstanceGenerator() {
        return instanceGenerator;
    }

    public void setInstanceGenerator(Function<UserParameterDefinition, JIPipeMutableParameterAccess> instanceGenerator) {
        this.instanceGenerator = instanceGenerator;
    }


    /**
     * Parameter definition by a user
     */
    public static class UserParameterDefinition {
        private JIPipeDynamicParameterCollection source;
        private String name;
        private Class<?> fieldClass;

        /**
         * @param source     parameter holder
         * @param name       parameter name
         * @param fieldClass parameter type
         */
        public UserParameterDefinition(JIPipeDynamicParameterCollection source, String name, Class<?> fieldClass) {
            this.source = source;
            this.name = name;
            this.fieldClass = fieldClass;
        }

        public JIPipeDynamicParameterCollection getSource() {
            return source;
        }

        public String getName() {
            return name;
        }

        public Class<?> getFieldClass() {
            return fieldClass;
        }
    }

    public static class ParameterAddingEvent {
        private final JIPipeDynamicParameterCollection parameterCollection;
        private final JIPipeMutableParameterAccess access;
        private boolean cancel;

        public ParameterAddingEvent(JIPipeDynamicParameterCollection parameterCollection, JIPipeMutableParameterAccess access) {
            this.parameterCollection = parameterCollection;
            this.access = access;
        }

        public JIPipeDynamicParameterCollection getParameterCollection() {
            return parameterCollection;
        }

        public JIPipeMutableParameterAccess getAccess() {
            return access;
        }

        public boolean isCancel() {
            return cancel;
        }

        public void setCancel(boolean cancel) {
            this.cancel = cancel;
        }
    }
}
