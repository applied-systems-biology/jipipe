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

package org.hkijena.acaq5.api.parameters;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.hkijena.acaq5.api.ACAQDefaultDocumentation;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.events.ParameterChangedEvent;
import org.hkijena.acaq5.api.events.ParameterStructureChangedEvent;
import org.hkijena.acaq5.utils.StringUtils;
import org.scijava.Priority;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.Stack;
import java.util.stream.Collectors;

/**
 * An {@link ACAQParameterCollection} that contains all the parameters of one or multiple
 * {@link ACAQParameterCollection} instances in a traversed form.
 */
public class ACAQParameterTree implements ACAQParameterCollection, ACAQCustomParameterCollection {

    /**
     * No flags
     */
    public static final int NONE = 0;

    /**
     * Whether to ignore all reflection parameters
     */
    public static final int IGNORE_REFLECTION = 1;

    /**
     * Whether to force reflection
     */
    public static final int FORCE_REFLECTION = 2;

    /**
     * Whether to ignore all custom parameters
     */
    public static final int IGNORE_CUSTOM = 4;

    private EventBus eventBus = new EventBus();
    private Node root = new Node(null, null);
    private BiMap<ACAQParameterCollection, Node> nodeMap = HashBiMap.create();
    private BiMap<String, ACAQParameterAccess> parameters = HashBiMap.create();
    private PriorityQueue<ACAQParameterAccess> parametersByPriority = new PriorityQueue<>(ACAQParameterAccess::comparePriority);

    private boolean ignoreReflectionParameters = false;
    private boolean ignoreCustomParameters = false;
    private boolean forceReflection = false;

    public ACAQParameterTree() {
    }

    /**
     * Creates a new instance with a predefined root parameter
     *
     * @param rootParameter the root parameter
     */
    public ACAQParameterTree(ACAQParameterCollection rootParameter) {
        this.root = new Node(null, rootParameter);
        this.nodeMap.put(rootParameter, root);
        merge(rootParameter, root);
    }

    /**
     * Creates a new instance with a predefined root parameter
     *
     * @param rootParameter the root parameter
     * @param flags         additional flags
     */
    public ACAQParameterTree(ACAQParameterCollection rootParameter, int flags) {
        this.root = new Node(null, rootParameter);
        this.nodeMap.put(rootParameter, root);
        this.ignoreReflectionParameters = (flags & IGNORE_REFLECTION) == IGNORE_REFLECTION;
        this.ignoreCustomParameters = (flags & IGNORE_CUSTOM) == IGNORE_CUSTOM;
        this.forceReflection = (flags & FORCE_REFLECTION) == FORCE_REFLECTION;
        merge(rootParameter, root);
    }

    /**
     * Gets the parameters grouped by the source
     *
     * @return all parameters grouped by source
     */
    public Map<ACAQParameterCollection, List<ACAQParameterAccess>> getGroupedBySource() {
        Map<ACAQParameterCollection, List<ACAQParameterAccess>> result = parameters.values().stream().collect(Collectors.groupingBy(ACAQParameterAccess::getSource));
        for (ACAQParameterCollection registeredSource : nodeMap.keySet()) {
            if (!result.containsKey(registeredSource))
                result.put(registeredSource, new ArrayList<>());
        }
        return result;
    }

    /**
     * Gets the unique key of an parameter access
     *
     * @param parameterAccess the access
     * @return unique key
     */
    public String getUniqueKey(ACAQParameterAccess parameterAccess) {
        return parameters.inverse().get(parameterAccess);
    }

    /**
     * Adds a new {@link ACAQParameterCollection} into this collection.
     * Ignores sources that have already been added
     *
     * @param child  the added collection
     * @param key    the unique key within the parent node
     * @param parent the parent node. Can be null (then defaults to root)
     * @return the created node
     */
    public Node add(ACAQParameterCollection child, String key, Node parent) {
        if (nodeMap.containsKey(child))
            return nodeMap.get(child);
        if (parent == null)
            parent = root;

        // Add the new child into the tree
        Node childNode = new Node(parent, child);
        childNode.setKey(key);
        parent.addChild(key, childNode);
        nodeMap.put(child, childNode);

        merge(child, childNode);
        return childNode;
    }

    /**
     * Merges parameters into a given node
     *
     * @param source the parameters
     * @param target the target node
     */
    public void merge(ACAQParameterCollection source, Node target) {
        if (!forceReflection && source instanceof ACAQCustomParameterCollection) {
            if (ignoreCustomParameters)
                return;
            for (Map.Entry<String, ACAQParameterAccess> entry : ((ACAQCustomParameterCollection) source).getParameters().entrySet()) {
                addParameter(entry.getKey(), entry.getValue(), target);
            }
            for (Map.Entry<String, ACAQParameterCollection> entry : ((ACAQCustomParameterCollection) source).getChildParameterCollections().entrySet()) {
                String key = entry.getKey();
                ACAQParameterCollection child = entry.getValue();
                Node childNode = new Node(target, child);
                childNode.setKey(key);
                target.addChild(key, childNode);
                nodeMap.put(child, childNode);

                merge(child, childNode);
            }
        } else {
            if (ignoreReflectionParameters)
                return;
            addReflectionParameters(source, target);
        }
        source.getEventBus().register(this);
    }

    private void addParameter(String initialKey, ACAQParameterAccess parameterAccess, Node parent) {
        List<String> keys = parent.getPath();
        keys.add(initialKey);
        String key = String.join("/", keys);
        parameters.put(key, parameterAccess);
        parametersByPriority.add(parameterAccess);
        parent.getParameters().put(initialKey, parameterAccess);
    }

    private void addReflectionParameters(ACAQParameterCollection source, Node parent) {

        // Find getter and setter pairs
        Map<String, GetterSetterPair> getterSetterPairs = new HashMap<>();
        for (Method method : source.getClass().getMethods()) {
            ACAQParameter[] parameterAnnotations = method.getAnnotationsByType(ACAQParameter.class);
            if (parameterAnnotations.length == 0)
                continue;
            ACAQParameter parameterAnnotation = parameterAnnotations[0];

            String key = parameterAnnotation.value();
            GetterSetterPair pair = getterSetterPairs.getOrDefault(key, null);
            if (pair == null) {
                pair = new GetterSetterPair();
                getterSetterPairs.put(key, pair);
            }
            if (method.getParameters().length == 1) {
                // This is a setter
                pair.setter = method;
            } else {
                pair.getter = method;
            }
        }

        // Add parameters of this source. Sub-parameters are excluded
        for (Map.Entry<String, GetterSetterPair> entry : getterSetterPairs.entrySet()) {
            GetterSetterPair pair = entry.getValue();
            boolean isSubParameter = ACAQParameterCollection.class.isAssignableFrom(pair.getFieldClass()) && pair.setter == null;
            if (pair.getFieldClass() != null && !isSubParameter) {
                if (pair.getter == null || pair.setter == null)
                    throw new RuntimeException("Invalid parameter definition: Getter or setter could not be found for key '" + entry.getKey() + "' in " + source);

                ACAQReflectionParameterAccess parameterAccess = new ACAQReflectionParameterAccess();
                parameterAccess.setSource(source);
                parameterAccess.setKey(entry.getKey());
                parameterAccess.setGetter(pair.getter);
                parameterAccess.setSetter(pair.setter);
                parameterAccess.setShortKey(pair.getShortKey());
                parameterAccess.setUIOrder(pair.getUIOrder());
                parameterAccess.setDocumentation(pair.getDocumentation());
                parameterAccess.setVisibility(pair.getVisibility());
                parameterAccess.setPriority(pair.getPriority());

                addParameter(entry.getKey(), parameterAccess, parent);
            }
        }

        // Add sub-parameters
        for (Map.Entry<String, GetterSetterPair> entry : getterSetterPairs.entrySet()) {
            GetterSetterPair pair = entry.getValue();
            boolean isSubParameter = ACAQParameterCollection.class.isAssignableFrom(pair.getFieldClass()) && pair.setter == null;
            if (pair.getFieldClass() != null && isSubParameter) {
                try {
                    ACAQParameterCollection subParameters = (ACAQParameterCollection) pair.getter.invoke(source);
                    if (subParameters == null)
                        continue;

                    Node childNode = add(subParameters, entry.getKey(), parent);
                    if (pair.getDocumentation() != null) {
                        childNode.setName(pair.getDocumentation().name());
                        childNode.setDescription(pair.getDocumentation().description());
                    } else
                        childNode.setName(entry.getKey());

                    childNode.setUiOrder(entry.getValue().getUIOrder());
                    childNode.setVisibility(entry.getValue().getVisibility());
                } catch (IllegalAccessException | InvocationTargetException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * @return Modifiable map of parameters
     */
    @Override
    public Map<String, ACAQParameterAccess> getParameters() {
        return parameters;
    }

    /**
     * Triggered when a source informs that the parameter structure was changed.
     * Passes the event to listeners.
     *
     * @param event generated event
     */
    @Subscribe
    public void onParameterStructureChanged(ParameterStructureChangedEvent event) {
        eventBus.post(event);
    }

    /**
     * Triggered when a parameter value was changed.
     * Passes the event to listeners.
     *
     * @param event generated event
     */
    @Subscribe
    public void onParameterChangedEvent(ParameterChangedEvent event) {
        eventBus.post(event);
    }

    @Override
    public EventBus getEventBus() {
        return eventBus;
    }

    public boolean isIgnoreReflectionParameters() {
        return ignoreReflectionParameters;
    }

    public void setIgnoreReflectionParameters(boolean ignoreReflectionParameters) {
        this.ignoreReflectionParameters = ignoreReflectionParameters;
    }

    public boolean isIgnoreCustomParameters() {
        return ignoreCustomParameters;
    }

    public void setIgnoreCustomParameters(boolean ignoreCustomParameters) {
        this.ignoreCustomParameters = ignoreCustomParameters;
    }

    public PriorityQueue<ACAQParameterAccess> getParametersByPriority() {
        return parametersByPriority;
    }

    public boolean isForceReflection() {
        return forceReflection;
    }

    public void setForceReflection(boolean forceReflection) {
        this.forceReflection = forceReflection;
    }

    /**
     * Returns all child parameters of the entry
     * If the entry is an {@link ACAQParameterAccess}, it is returned. Otherwise all children of the {@link ACAQParameterCollection} are returned.
     *
     * @param entry the entry.
     * @return all parameters that are children of the entry or the entry itself
     */
    public List<ACAQParameterAccess> getAllChildParameters(Object entry) {
        if (entry instanceof ACAQParameterAccess) {
            return Collections.singletonList((ACAQParameterAccess) entry);
        } else if (entry instanceof ACAQParameterTree.Node) {
            List<ACAQParameterAccess> result = new ArrayList<>();
            Stack<Node> stack = new Stack<>();
            stack.push((Node) entry);
            while (!stack.isEmpty()) {
                Node top = stack.pop();
                result.addAll(top.getParameters().values());

                for (Node child : top.getChildren().values()) {
                    stack.push(child);
                }
            }
            return result;
        } else {
            return Collections.emptyList();
        }
    }

    public Node getRoot() {
        return root;
    }

    /**
     * Gets unique source key
     *
     * @param collection source
     * @return key
     */
    public String getSourceKey(ACAQParameterCollection collection) {
        return String.join("/", nodeMap.get(collection).getPath());
    }

    public Set<ACAQParameterCollection> getRegisteredSources() {
        return nodeMap.keySet();
    }

    /**
     * Gets source UI order
     *
     * @param source source
     * @return ui order
     */
    public int getUISourceOrder(ACAQParameterCollection source) {
        return nodeMap.get(source).getUiOrder();
    }

    /**
     * Gets source name
     *
     * @param source source
     * @return name
     */
    public String getSourceDocumentationName(ACAQParameterCollection source) {
        Node node = nodeMap.get(source);
        if (!StringUtils.isNullOrEmpty(node.getName())) {
            return node.getName();
        } else {
            return String.join("/", node.getPath());
        }
    }

    /**
     * Gets source visibility
     *
     * @param source source
     * @return visibility
     */
    public ACAQParameterVisibility getSourceVisibility(ACAQParameterCollection source) {
        return nodeMap.get(source).getVisibility();
    }

    /**
     * Gets source UI order
     *
     * @param source source
     * @return source UI order
     */
    public int getSourceUIOrder(ACAQParameterCollection source) {
        return nodeMap.get(source).getUiOrder();
    }

    /**
     * Gets source documentation
     *
     * @param source source
     * @return source documentation
     */
    public ACAQDocumentation getSourceDocumentation(ACAQParameterCollection source) {
        Node node = nodeMap.get(source);
        String name = getSourceDocumentationName(source);
        String description = node.getDescription();
        return new ACAQDefaultDocumentation(name, description);
    }

    /**
     * Sets source documentation
     *
     * @param source        source
     * @param documentation documentation
     */
    public void setSourceDocumentation(ACAQParameterCollection source, ACAQDefaultDocumentation documentation) {
        Node node = nodeMap.get(source);
        node.setName(documentation.name());
        node.setDescription(documentation.description());
    }

    /**
     * Returns the node of a collection
     *
     * @param collection the collection
     * @return node or null
     */
    public Node getSourceNode(ACAQParameterCollection collection) {
        return nodeMap.get(collection);
    }

    /**
     * Accesses the parameters of a collection
     *
     * @param collection the collection
     * @return traversed parameters
     */
    public static Map<String, ACAQParameterAccess> getParameters(ACAQParameterCollection collection) {
        return (new ACAQParameterTree(collection)).getParameters();
    }

    /**
     * Pair of getter and setter
     */
    private static class GetterSetterPair {
        public Method getter;
        public Method setter;

        public Class<?> getFieldClass() {
            return getter != null ? getter.getReturnType() : null;
        }

        public ACAQParameterVisibility getVisibility() {
            ACAQParameter getterAnnotation = getter.getAnnotation(ACAQParameter.class);
            if (setter == null)
                return getterAnnotation.visibility();
            ACAQParameter setterAnnotation = setter.getAnnotation(ACAQParameter.class);
            return getterAnnotation.visibility().intersectWith(setterAnnotation.visibility());
        }

        public double getPriority() {
            ACAQParameter getterAnnotation = getter.getAnnotation(ACAQParameter.class);
            if (setter == null)
                return getterAnnotation.priority();
            ACAQParameter setterAnnotation = setter.getAnnotation(ACAQParameter.class);
            return getterAnnotation.priority() != Priority.NORMAL ? getterAnnotation.priority() : setterAnnotation.priority();
        }

        public String getShortKey() {
            ACAQParameter getterAnnotation = getter.getAnnotation(ACAQParameter.class);
            if (!StringUtils.isNullOrEmpty(getterAnnotation.shortKey()))
                return getterAnnotation.shortKey();
            ACAQParameter setterAnnotation = setter.getAnnotation(ACAQParameter.class);
            return setterAnnotation.shortKey();
        }

        public int getUIOrder() {
            ACAQParameter getterAnnotation = getter.getAnnotation(ACAQParameter.class);
            if (setter == null)
                return getterAnnotation.uiOrder();
            ACAQParameter setterAnnotation = setter.getAnnotation(ACAQParameter.class);
            return getterAnnotation.uiOrder() != 0 ? getterAnnotation.uiOrder() : setterAnnotation.uiOrder();
        }

        public ACAQDocumentation getDocumentation() {
            ACAQDocumentation[] documentations = getter.getAnnotationsByType(ACAQDocumentation.class);
            if (documentations.length > 0)
                return documentations[0];
            if (setter == null)
                return null;
            documentations = setter.getAnnotationsByType(ACAQDocumentation.class);
            return documentations.length > 0 ? documentations[0] : null;
        }
    }

    /**
     * A node
     */
    public static class Node {
        private Node parent;
        private ACAQParameterCollection collection;
        private String key;
        private ACAQParameterVisibility visibility = ACAQParameterVisibility.Visible;
        private String name;
        private String description;
        private int order;
        private int uiOrder;
        private BiMap<String, ACAQParameterAccess> parameters = HashBiMap.create();
        private BiMap<String, Node> children = HashBiMap.create();

        /**
         * Creates a node
         *
         * @param parent     the parent
         * @param collection the collection
         */
        public Node(Node parent, ACAQParameterCollection collection) {
            this.parent = parent;
            this.collection = collection;
            if (collection instanceof ACAQNamedParameterCollection) {
                this.name = ((ACAQNamedParameterCollection) collection).getDefaultParameterCollectionName();
                this.description = ((ACAQNamedParameterCollection) collection).getDefaultParameterCollectionDescription();
            }
        }

        public List<String> getPath() {
            List<String> path = new ArrayList<>();
            constructPathFast(path);
            return path;
        }

        private void constructPathFast(List<String> path) {
            if (parent != null) {
                parent.constructPathFast(path);
                path.add(key);
            }
        }

        public ACAQParameterCollection getCollection() {
            return collection;
        }

        public void setCollection(ACAQParameterCollection collection) {
            this.collection = collection;
        }

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public ACAQParameterVisibility getVisibility() {
            return visibility;
        }

        public void setVisibility(ACAQParameterVisibility visibility) {
            this.visibility = visibility;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public int getOrder() {
            return order;
        }

        public void setOrder(int order) {
            this.order = order;
        }

        public BiMap<String, ACAQParameterAccess> getParameters() {
            return parameters;
        }

        public void setParameters(BiMap<String, ACAQParameterAccess> parameters) {
            this.parameters = parameters;
        }

        public BiMap<String, Node> getChildren() {
            return children;
        }

        public void setChildren(BiMap<String, Node> children) {
            this.children = children;
        }

        public void addChild(String key, Node child) {
            if (children.containsKey(key))
                throw new RuntimeException("Already contains child with key '" + key + "'!");
            children.put(key, child);
        }

        public Node getParent() {
            return parent;
        }

        public int getUiOrder() {
            return uiOrder;
        }

        public void setUiOrder(int uiOrder) {
            this.uiOrder = uiOrder;
        }
    }
}

