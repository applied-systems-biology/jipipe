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
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.collect.*;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.JIPipeValidatable;
import org.hkijena.jipipe.api.events.AbstractJIPipeEvent;
import org.hkijena.jipipe.api.events.JIPipeEventEmitter;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntryCause;
import org.hkijena.jipipe.api.validation.JIPipeValidationRuntimeException;
import org.hkijena.jipipe.api.validation.causes.ParameterValidationReportEntryCause;
import org.hkijena.jipipe.utils.json.JsonDeserializable;
import org.hkijena.jipipe.utils.json.JsonUtils;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.*;
import java.util.function.Function;

/**
 * Holds a user-definable set of parameters
 */
@JsonDeserialize(using = JIPipeDynamicParameterCollection.Deserializer.class)
public class JIPipeDynamicParameterCollection implements JIPipeCustomParameterCollection, JIPipeValidatable, JsonDeserializable {
    private final BiMap<String, JIPipeMutableParameterAccess> dynamicParameters = HashBiMap.create();
    private Set<Class<?>> allowedTypes = new HashSet<>();
    private Function<UserParameterDefinition, JIPipeMutableParameterAccess> instanceGenerator;
    private boolean allowUserModification = false;
    private boolean delayEvents = false;

    private final ParameterChangedEventEmitter parameterChangedEventEmitter = new ParameterChangedEventEmitter();
    private final ParameterStructureChangedEventEmitter parameterStructureChangedEventEmitter = new ParameterStructureChangedEventEmitter();
    private final ParameterUIChangedEventEmitter parameterUIChangedEventEmitter = new ParameterUIChangedEventEmitter();

    private final BeforeAddParameterEventEmitter beforeAddParameterEventEmitter = new BeforeAddParameterEventEmitter();

    public JIPipeDynamicParameterCollection() {

    }

    /**
     * Creates a new instance.
     * Automatically adds all known registered parameter types
     *
     * @param allowUserModification let user modify this collection
     */
    public JIPipeDynamicParameterCollection(boolean allowUserModification) {
        this.allowUserModification = allowUserModification;
        if (allowUserModification && JIPipe.isInstantiated()) {
            for (JIPipeParameterTypeInfo info : JIPipe.getParameterTypes().getRegisteredParameters().values()) {
                this.allowedTypes.add(info.getFieldClass());
            }
        }
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
            parameterAccess.setSource(this);
            dynamicParameters.put(entry.getKey(), parameterAccess);
        }
    }

    /**
     * Creates a new instance
     *
     * @param allowUserModification let user modify this collection
     * @param allowedTypes          The parameter types that can be added by the user (ignored if user cannot add).
     */
    public JIPipeDynamicParameterCollection(boolean allowUserModification, Set<JIPipeParameterTypeInfo> allowedTypes) {
        this.allowUserModification = allowUserModification;
        for (JIPipeParameterTypeInfo allowedType : allowedTypes) {
            this.allowedTypes.add(allowedType.getFieldClass());
        }
    }

    /**
     * Creates a new instance
     *
     * @param allowUserModification let user modify this collection
     * @param allowedTypes          The parameter types that can be added by the user (ignored if user cannot add).
     */
    public JIPipeDynamicParameterCollection(boolean allowUserModification, Class<?>[] allowedTypes) {
        this.allowUserModification = allowUserModification;
        this.allowedTypes.addAll(Arrays.asList(allowedTypes));
    }

    public BeforeAddParameterEventEmitter getBeforeAddParameterEventEmitter() {
        return beforeAddParameterEventEmitter;
    }

    @Override
    public ParameterChangedEventEmitter getParameterChangedEventEmitter() {
        return parameterChangedEventEmitter;
    }

    @Override
    public ParameterStructureChangedEventEmitter getParameterStructureChangedEventEmitter() {
        return parameterStructureChangedEventEmitter;
    }

    @Override
    public ParameterUIChangedEventEmitter getParameterUIChangedEventEmitter() {
        return parameterUIChangedEventEmitter;
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
        if (parameterAccess.get(Object.class) == null) {
            // Set default
            parameterAccess.set(JIPipe.getParameterTypes().getInfoByFieldClass(parameterAccess.getFieldClass()).newInstance());
        }
        parameterAccess.setSource(this);
        BeforeAddParameterEvent event = new BeforeAddParameterEvent(this, parameterAccess);
        beforeAddParameterEventEmitter.emit(event);
        if (event.isCancelled())
            return null;
        dynamicParameters.put(parameterAccess.getKey(), parameterAccess);
        if (!delayEvents)
            emitParameterStructureChangedEvent();
        return parameterAccess;
    }

    public boolean supportsAllParameterTypes() {
        return getAllowedTypes() == null || getAllowedTypes().isEmpty() || getAllowedTypes().size() == JIPipe.getParameterTypes().getRegisteredParameters().size();
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
            emitParameterStructureChangedEvent();
    }

    /**
     * Removes a parameter
     *
     * @param key The parameter key
     */
    public void removeParameter(String key) {
        dynamicParameters.remove(key);
        if (!delayEvents)
            emitParameterStructureChangedEvent();
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
                parameterAccess.setSource(this);
                dynamicParameters.put(entry.getKey(), parameterAccess);
            } catch (IOException e) {
                throw new JIPipeValidationRuntimeException(e, "Unable to read parameter from JSON!",
                        "There is essential information missing in the JSON data.",
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
     * Halts all {@link JIPipeParameterCollection.ParameterStructureChangedEvent} until endModificationBlock() is called.
     * Use this method for many changes at once
     */
    public void beginModificationBlock() {
        if (delayEvents)
            throw new UnsupportedOperationException("Modification block already started!");
        delayEvents = true;
    }

    /**
     * Ends a modification block started by beginModificationBlock().
     * Triggers a {@link JIPipeParameterCollection.ParameterStructureChangedEvent}
     */
    public void endModificationBlock() {
        if (!delayEvents)
            throw new UnsupportedOperationException("No modification block!");
        delayEvents = false;
        parameterStructureChangedEventEmitter.emit(new ParameterStructureChangedEvent(this));
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
    public void reportValidity(JIPipeValidationReportEntryCause parentCause, JIPipeValidationReport report) {
        for (Map.Entry<String, JIPipeMutableParameterAccess> entry : dynamicParameters.entrySet()) {
            Object o = entry.getValue().get(Object.class);
            if (o instanceof JIPipeValidatable) {
                report.report(new ParameterValidationReportEntryCause(parentCause, this, entry.getValue().getName(), entry.getKey()),(JIPipeValidatable) o);
            }
        }
    }

    public Function<UserParameterDefinition, JIPipeMutableParameterAccess> getInstanceGenerator() {
        return instanceGenerator;
    }

    public void setInstanceGenerator(Function<UserParameterDefinition, JIPipeMutableParameterAccess> instanceGenerator) {
        this.instanceGenerator = instanceGenerator;
    }

    public JIPipeMutableParameterAccess addParameter(String key, Class<?> fieldClass, String name, String description, Annotation... annotations) {
        JIPipeMutableParameterAccess parameterAccess;
        if (instanceGenerator != null) {
            parameterAccess = instanceGenerator.apply(new UserParameterDefinition(this, key, fieldClass));
        } else {
            parameterAccess = new JIPipeMutableParameterAccess(this, key, fieldClass);
            parameterAccess.setName(key);
        }
        parameterAccess.setName(name);
        parameterAccess.setDescription(description);
        Multimap<Class<? extends Annotation>, Annotation> annotationMap = HashMultimap.create();
        for (Annotation annotation : annotations) {
            annotationMap.put(annotation.annotationType(), annotation);
        }
        parameterAccess.setAnnotationMap(annotationMap);

        return addParameter(parameterAccess);
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

    public static class BeforeAddParameterEvent extends AbstractJIPipeEvent {
        private final JIPipeDynamicParameterCollection parameterCollection;
        private final JIPipeMutableParameterAccess access;
        private boolean cancelled;

        public BeforeAddParameterEvent(JIPipeDynamicParameterCollection parameterCollection, JIPipeMutableParameterAccess access) {
            super(parameterCollection);
            this.parameterCollection = parameterCollection;
            this.access = access;
        }

        public JIPipeDynamicParameterCollection getParameterCollection() {
            return parameterCollection;
        }

        public JIPipeMutableParameterAccess getAccess() {
            return access;
        }

        public boolean isCancelled() {
            return cancelled;
        }

        public void setCancelled(boolean cancelled) {
            this.cancelled = cancelled;
        }
    }

    public interface BeforeAddParameterEventListener {
        void onDynamicParameterCollectionBeforeAddParameter(BeforeAddParameterEvent event);
    }

    public static class BeforeAddParameterEventEmitter extends JIPipeEventEmitter<BeforeAddParameterEvent, BeforeAddParameterEventListener> {

        @Override
        protected void call(BeforeAddParameterEventListener beforeAddParameterEventListener, BeforeAddParameterEvent event) {
            beforeAddParameterEventListener.onDynamicParameterCollectionBeforeAddParameter(event);
        }
    }

    public static class Deserializer extends JsonDeserializer<JIPipeDynamicParameterCollection> {
        @Override
        public JIPipeDynamicParameterCollection deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
            JIPipeDynamicParameterCollection result = new JIPipeDynamicParameterCollection();
            result.fromJson(p.readValueAsTree());
            return result;
        }
    }
}
