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

package org.hkijena.jipipe.utils;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.eventbus.Subscribe;
import org.hkijena.jipipe.api.JIPipeValidityReport;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameterPersistence;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class ParameterUtils {

    private ParameterUtils() {

    }

    /**
     * Deserializes parameters from JSON
     *
     * @param target the target object that contains the parameters
     * @param node   the JSON node
     * @param issues issues during deserialization
     */
    public static void deserializeParametersFromJson(JIPipeParameterCollection target, JsonNode node, JIPipeValidityReport issues) {
        AtomicBoolean changedStructure = new AtomicBoolean();
        changedStructure.set(true);
        target.getEventBus().register(new Object() {
            @Subscribe
            public void onParametersChanged(JIPipeParameterCollection.ParameterStructureChangedEvent event) {
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
                                            "In: node.get(key)\n\n" + e);
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
    public static void serializeParametersToJson(JIPipeParameterCollection target, JsonGenerator jsonGenerator) throws IOException {
        serializeParametersToJson(target, jsonGenerator, null);
    }

    /**
     * Serializes parameters to JSON using a generator
     *
     * @param target        the serialized parameter collection
     * @param jsonGenerator the JSON target
     * @param filter        filter to conditionally serialize entries. can be null
     */
    public static void serializeParametersToJson(JIPipeParameterCollection target, JsonGenerator jsonGenerator, Predicate<Map.Entry<String, JIPipeParameterAccess>> filter) throws IOException {
        JIPipeParameterTree parameterCollection = new JIPipeParameterTree(target);
        Stack<JIPipeParameterTree.Node> stack = new Stack<>();
        stack.push(parameterCollection.getRoot());

        while (!stack.isEmpty()) {
            JIPipeParameterTree.Node top = stack.pop();

            if (top.getPersistence() == JIPipeParameterPersistence.Object) {
                jsonGenerator.writeObjectField(String.join("/", top.getPath()), top.getCollection());
            } else if (top.getPersistence() == JIPipeParameterPersistence.Collection) {
                for (Map.Entry<String, JIPipeParameterAccess> entry : top.getParameters().entrySet()) {
                    if (filter != null && !filter.test(entry))
                        continue;
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
    public static boolean setParameter(JIPipeParameterCollection collection, String key, Object value) {
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
    public static <T> T getParameter(JIPipeParameterCollection collection, String key, Class<T> klass) {
        JIPipeParameterTree tree = new JIPipeParameterTree(collection);
        return tree.getParameters().get(key).get(klass);
    }

    /***
     * Returns true if the provided parameter is local (i.e. only shown if its not a sub-parameter) and therefore should be hidden
     * @param tree the parameter tree
     * @param access the tested parameter
     * @param keys list of local parameter keys. only parameters in the key set are tested. If empty, all keys are tested
     * @return if the parameter is hidden
     */
    public static boolean isHiddenLocalParameter(JIPipeParameterTree tree, JIPipeParameterAccess access, String... keys) {
        // Root is parent -> Always shown
        if (tree.getRoot().getCollection() == access.getSource())
            return false;
        if (keys.length == 0)
            return true;
        for (String key : keys) {
            if (key.equals(access.getKey())) {
                return true;
            }
        }
        return false;
    }


    /**
     * Returns true if the collection is not a direct child of the tree root
     *
     * @param tree       the parameter tree
     * @param collection the collection to be tested
     * @param keys       list of collection local keys. only collections with these keys are tested. If empty, all collections are tested.
     * @return if the collection is hidden
     */
    public static boolean isHiddenLocalParameterCollection(JIPipeParameterTree tree, JIPipeParameterCollection collection, String... keys) {
        JIPipeParameterTree.Node node = tree.getSourceNode(collection);
        if (keys.length > 0) {
            for (String key : keys) {
                if (Objects.equals(key, node.getKey())) {
                    return node.getParent() != tree.getRoot();
                }
            }
        } else {
            return node.getParent() != tree.getRoot();
        }
        return false;
    }
}
