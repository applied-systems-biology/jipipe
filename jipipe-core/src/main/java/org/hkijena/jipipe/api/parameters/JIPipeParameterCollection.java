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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.hkijena.jipipe.api.JIPipeValidityReport;
import org.hkijena.jipipe.api.events.ParameterStructureChangedEvent;
import org.hkijena.jipipe.utils.JsonDeserializable;
import org.hkijena.jipipe.utils.JsonUtils;

import java.io.IOException;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Interfaced for a parameterized object
 */
public interface JIPipeParameterCollection {
    /**
     * Gets the event bus that posts events about the parameters
     *
     * @return The event bus triggering {@link org.hkijena.jipipe.api.events.ParameterChangedEvent} and {@link org.hkijena.jipipe.api.events.ParameterStructureChangedEvent}
     */
    EventBus getEventBus();

    /**
     * Deserializes parameters from JSON
     *
     * @param target the target object that contains the parameters
     * @param node   the JSON node
     * @param issues issues during deserialization
     */
    static void deserializeParametersFromJson(JIPipeParameterCollection target, JsonNode node, JIPipeValidityReport issues) {
        AtomicBoolean changedStructure = new AtomicBoolean();
        changedStructure.set(true);
        target.getEventBus().register(new Object() {
            @Subscribe
            public void onParametersChanged(ParameterStructureChangedEvent event) {
                changedStructure.set(true);
            }
        });

        JIPipeParameterTree parameterCollection = new JIPipeParameterTree(target);
        Stack<JIPipeParameterTree.Node> stack = new Stack<>();

        // Load all collection-type parameters
        Set<String> loadedParameters = new HashSet<>();
        while (changedStructure.get()) {
            changedStructure.set(false);

            parameterCollection = new JIPipeParameterTree(target);
            stack.clear();
            stack.push(parameterCollection.getRoot());

            outer:
            while (!stack.isEmpty()) {
                JIPipeParameterTree.Node top = stack.pop();

                if (top.getPersistence() == JIPipeParameterPersistence.Collection) {
                    for (Map.Entry<String, JIPipeParameterAccess> entry : top.getParameters().entrySet().stream()
                            .sorted(Comparator.comparing(kv -> -kv.getValue().getPriority())).collect(Collectors.toList())) {
                        JIPipeParameterAccess parameterAccess = entry.getValue();
                        if (parameterAccess.getPersistence() != JIPipeParameterPersistence.None) {
                            String key = parameterCollection.getUniqueKey(parameterAccess);
                            if (loadedParameters.contains(key))
                                continue;
                            loadedParameters.add(key);
                            JsonNode objectNode = node.path(key);
                            if (!objectNode.isMissingNode()) {
                                Object v;
                                try {
                                    v = JsonUtils.getObjectMapper().readerFor(parameterAccess.getFieldClass()).readValue(node.get(key));
                                    parameterAccess.set(v);
                                } catch (Exception | Error e) {
                                    issues.forCategory(key).reportIsInvalid("Could not load parameter '" + key + "'!",
                                            "The data might be not compatible with your operating system or from an older or newer JIPipe version.",
                                            "Please check the value of the parameter.",
                                            node.get(key));
                                }


                                // Stop loading here to prevent already traversed parameters from being not loaded
                                if (changedStructure.get())
                                    continue outer;
                            }
                        }
                    }
                    for (JIPipeParameterTree.Node child : top.getChildren().values()) {
                        stack.push(child);
                    }
                }
            }
        }

        // Load all object-type parameters
        stack.clear();
        stack.push(parameterCollection.getRoot());

        while (!stack.isEmpty()) {
            JIPipeParameterTree.Node top = stack.pop();

            if (top.getPersistence() == JIPipeParameterPersistence.Object) {
                JsonNode objectNode = node.path(String.join("/", top.getPath()));
                if (!objectNode.isMissingNode()) {
                    JIPipeParameterCollection collection = top.getCollection();
                    if (collection instanceof JsonDeserializable) {
                        ((JsonDeserializable) collection).fromJson(objectNode);
                    } else {
                        throw new RuntimeException("Cannot deserialize object-like persistence into non-Json-deserializable target!");
                    }
                }
            } else if (top.getPersistence() == JIPipeParameterPersistence.Collection) {
                for (JIPipeParameterTree.Node child : top.getChildren().values()) {
                    stack.push(child);
                }
            }
        }
    }

    /**
     * Serializes parameters to JSON using a generator
     *
     * @param target        the serialized parameter collection
     * @param jsonGenerator the JSON target
     */
    static void serializeParametersToJson(JIPipeParameterCollection target, JsonGenerator jsonGenerator) throws IOException {
        serializeParametersToJson(target, jsonGenerator, null);
    }

    /**
     * Serializes parameters to JSON using a generator
     *
     * @param target        the serialized parameter collection
     * @param jsonGenerator the JSON target
     * @param filter        filter to conditionally serialize entries
     */
    static void serializeParametersToJson(JIPipeParameterCollection target, JsonGenerator jsonGenerator, Predicate<Map.Entry<String, JIPipeParameterAccess>> filter) throws IOException {
        JIPipeParameterTree parameterCollection = new JIPipeParameterTree(target);
        Stack<JIPipeParameterTree.Node> stack = new Stack<>();
        stack.push(parameterCollection.getRoot());

        while (!stack.isEmpty()) {
            JIPipeParameterTree.Node top = stack.pop();

            if (top.getPersistence() == JIPipeParameterPersistence.Object) {
                jsonGenerator.writeObjectField(String.join("/", top.getPath()), top.getCollection());
            } else if (top.getPersistence() == JIPipeParameterPersistence.Collection) {
                for (Map.Entry<String, JIPipeParameterAccess> entry : top.getParameters().entrySet()) {
                    JIPipeParameterAccess parameterAccess = entry.getValue();
                    if (parameterAccess.getPersistence() != JIPipeParameterPersistence.None)
                        jsonGenerator.writeObjectField(parameterCollection.getUniqueKey(parameterAccess), parameterAccess.get(Object.class));
                }
                for (JIPipeParameterTree.Node node : top.getChildren().values()) {
                    stack.push(node);
                }
            }
        }
    }

    /**
     * Sets a parameter of a {@link JIPipeParameterCollection} and triggers the associated events
     *
     * @param collection the collection
     * @param key        the parameter key
     * @param value      the parameter value
     * @return if the parameter could be set
     */
    static boolean setParameter(JIPipeParameterCollection collection, String key, Object value) {
        JIPipeParameterTree tree = new JIPipeParameterTree(collection);
        return tree.getParameters().get(key).set(value);
    }

    /**
     * Gets a parameter from a {@link JIPipeParameterCollection}
     *
     * @param collection the collection
     * @param key        the parameter key
     * @param klass      the parameter class
     * @param <T>        the parameter class
     * @return the current value
     */
    static <T> T getParameter(JIPipeParameterCollection collection, String key, Class<T> klass) {
        JIPipeParameterTree tree = new JIPipeParameterTree(collection);
        return tree.getParameters().get(key).get(klass);
    }
}
