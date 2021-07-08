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

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.hkijena.jipipe.api.JIPipeDefaultDocumentation;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.extensions.parameters.primitives.HTMLText;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;
import org.scijava.Priority;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * An {@link JIPipeParameterCollection} that contains all the parameters of one or multiple
 * {@link JIPipeParameterCollection} instances in a traversed form.
 */
public class JIPipeParameterTree implements JIPipeParameterCollection, JIPipeCustomParameterCollection {

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
    private BiMap<JIPipeParameterCollection, Node> nodeMap = HashBiMap.create();
    private BiMap<String, JIPipeParameterAccess> parameters = HashBiMap.create();
    private PriorityQueue<JIPipeParameterAccess> parametersByPriority = new PriorityQueue<>(JIPipeParameterAccess::comparePriority);

    private boolean ignoreReflectionParameters = false;
    private boolean ignoreCustomParameters = false;
    private boolean forceReflection = false;

    public JIPipeParameterTree() {
    }

    /**
     * Creates a new instance with a predefined root parameter
     *
     * @param rootParameter the root parameter
     */
    public JIPipeParameterTree(JIPipeParameterCollection rootParameter) {
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
    public JIPipeParameterTree(JIPipeParameterCollection rootParameter, int flags) {
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
    public Map<JIPipeParameterCollection, List<JIPipeParameterAccess>> getGroupedBySource() {
        Map<JIPipeParameterCollection, List<JIPipeParameterAccess>> result = parameters.values().stream().collect(Collectors.groupingBy(JIPipeParameterAccess::getSource));
        for (JIPipeParameterCollection registeredSource : nodeMap.keySet()) {
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
    public String getUniqueKey(JIPipeParameterAccess parameterAccess) {
        return parameters.inverse().get(parameterAccess);
    }

    /**
     * Adds a new {@link JIPipeParameterCollection} into this collection.
     * Ignores sources that have already been added
     *
     * @param child  the added collection
     * @param key    the unique key within the parent node
     * @param parent the parent node. Can be null (then defaults to root)
     * @return the created node
     */
    public Node add(JIPipeParameterCollection child, String key, Node parent) {
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
    public void merge(JIPipeParameterCollection source, Node target) {
        if (!forceReflection && source instanceof JIPipeCustomParameterCollection) {
            if (((JIPipeCustomParameterCollection) source).getIncludeReflectionParameters() && !ignoreReflectionParameters) {
                addReflectionParameters(source, target);
            }
            if (ignoreCustomParameters)
                return;
            for (Map.Entry<String, JIPipeParameterAccess> entry : ((JIPipeCustomParameterCollection) source).getParameters().entrySet()) {
                addParameter(entry.getKey(), entry.getValue(), target);
            }
            for (Map.Entry<String, JIPipeParameterCollection> entry : ((JIPipeCustomParameterCollection) source).getChildParameterCollections().entrySet()) {
                String key = entry.getKey();
                JIPipeParameterCollection child = entry.getValue();
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
        addContextActions(source, target);
        source.getEventBus().register(this);
    }

    private void addContextActions(JIPipeParameterCollection source, Node target) {
        for (Method method : source.getClass().getMethods()) {
            JIPipeContextAction actionAnnotation = method.getAnnotation(JIPipeContextAction.class);
            if (actionAnnotation == null)
                continue;
            if (!actionAnnotation.showInParameters())
                continue;
            JIPipeDocumentation documentationAnnotation = method.getAnnotation(JIPipeDocumentation.class);
            if (documentationAnnotation == null) {
                documentationAnnotation = new JIPipeDefaultDocumentation(method.getName(), "");
            }
            URL iconURL = null;
            if (UIUtils.DARK_THEME && !StringUtils.isNullOrEmpty(actionAnnotation.iconDarkURL())) {
                iconURL = actionAnnotation.resourceClass().getResource(actionAnnotation.iconDarkURL());
            } else {
                if (!StringUtils.isNullOrEmpty(actionAnnotation.iconURL())) {
                    iconURL = actionAnnotation.resourceClass().getResource(actionAnnotation.iconURL());
                }
            }
            target.actions.add(new ContextAction(source, method, iconURL, documentationAnnotation));
        }
    }

    private void addParameter(String initialKey, JIPipeParameterAccess parameterAccess, Node parent) {
        List<String> keys = parent.getPath();
        keys.add(initialKey);
        String key = String.join("/", keys);
        parameters.put(key, parameterAccess);
        parametersByPriority.add(parameterAccess);
        parent.getParameters().put(initialKey, parameterAccess);
    }

    private void addReflectionParameters(JIPipeParameterCollection source, Node parent) {

        // Find getter and setter pairs
        Map<String, GetterSetterPair> getterSetterPairs = new HashMap<>();
        for (Method method : source.getClass().getMethods()) {
            JIPipeParameter[] parameterAnnotations = method.getAnnotationsByType(JIPipeParameter.class);
            if (parameterAnnotations.length == 0)
                continue;
            JIPipeParameter parameterAnnotation = parameterAnnotations[0];

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
            boolean isSubParameter = JIPipeParameterCollection.class.isAssignableFrom(pair.getFieldClass()) && pair.setter == null;
            if (pair.getFieldClass() != null && !isSubParameter) {
                if (pair.getter == null || pair.setter == null)
                    throw new RuntimeException("Invalid parameter definition: Getter or setter could not be found for key '" + entry.getKey() + "' in " + source);

                JIPipeReflectionParameterAccess parameterAccess = new JIPipeReflectionParameterAccess();
                parameterAccess.setSource(source);
                parameterAccess.setKey(entry.getKey());
                parameterAccess.setGetter(pair.getter);
                parameterAccess.setSetter(pair.setter);
                parameterAccess.setShortKey(pair.getShortKey());
                parameterAccess.setUIOrder(pair.getUIOrder());
                parameterAccess.setDocumentation(pair.getDocumentation());
                parameterAccess.setHidden(pair.isHidden());
                parameterAccess.setPriority(pair.getPriority());
                parameterAccess.setPersistence(pair.getPersistence());
                parameterAccess.setImportant(pair.isImportant());

                addParameter(entry.getKey(), parameterAccess, parent);
            }
        }

        // Add sub-parameters
        for (Map.Entry<String, GetterSetterPair> entry : getterSetterPairs.entrySet()) {
            GetterSetterPair pair = entry.getValue();
            boolean isSubParameter = JIPipeParameterCollection.class.isAssignableFrom(pair.getFieldClass()) && pair.setter == null;
            if (pair.getFieldClass() != null && isSubParameter) {
                try {
                    JIPipeParameterCollection subParameters = (JIPipeParameterCollection) pair.getter.invoke(source);
                    if (subParameters == null)
                        continue;

                    Node childNode = add(subParameters, entry.getKey(), parent);
                    if (pair.getDocumentation() != null) {
                        childNode.setName(pair.getDocumentation().name());
                        childNode.setDescription(new HTMLText(pair.getDocumentation().description()));
                    } else
                        childNode.setName(entry.getKey());

                    childNode.setCollapsed(entry.getValue().isCollapsed());
                    childNode.setUiOrder(entry.getValue().getUIOrder());
                    childNode.setHidden(entry.getValue().isHidden());
                    childNode.setPersistence(entry.getValue().getPersistence());
                    childNode.setIconURL(entry.getValue().getIconURL());
                    childNode.setDarkIconURL(entry.getValue().getIconDarkURL());
                    childNode.setResourceClass(entry.getValue().getResourceClass());
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
    public Map<String, JIPipeParameterAccess> getParameters() {
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
        if (event.getVisitors().contains(this))
            return;
        event.getVisitors().add(this);
        eventBus.post(event);
    }

    /**
     * Triggered when a source informs that the parameter UI should be updated
     * Passes the event to listeners.
     *
     * @param event generated event
     */
    @Subscribe
    public void onParameterUIChanged(ParameterUIChangedEvent event) {
        if (event.getVisitors().contains(this))
            return;
        event.getVisitors().add(this);
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
        if (event.getVisitors().contains(this))
            return;
        event.getVisitors().add(this);
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

    public PriorityQueue<JIPipeParameterAccess> getParametersByPriority() {
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
     * If the entry is an {@link JIPipeParameterAccess}, it is returned. Otherwise all children of the {@link JIPipeParameterCollection} are returned.
     *
     * @param entry the entry.
     * @return all parameters that are children of the entry or the entry itself
     */
    public List<JIPipeParameterAccess> getAllChildParameters(Object entry) {
        if (entry instanceof JIPipeParameterAccess) {
            return Collections.singletonList((JIPipeParameterAccess) entry);
        } else if (entry instanceof JIPipeParameterTree.Node) {
            List<JIPipeParameterAccess> result = new ArrayList<>();
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
    public String getSourceKey(JIPipeParameterCollection collection) {
        return String.join("/", nodeMap.get(collection).getPath());
    }

    public Set<JIPipeParameterCollection> getRegisteredSources() {
        return nodeMap.keySet();
    }

    /**
     * Gets source UI order
     *
     * @param source source
     * @return ui order
     */
    public int getUISourceOrder(JIPipeParameterCollection source) {
        return nodeMap.get(source).getUiOrder();
    }

    /**
     * Gets source name
     *
     * @param source source
     * @return name
     */
    public String getSourceDocumentationName(JIPipeParameterCollection source) {
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
    public boolean isSourceHidden(JIPipeParameterCollection source) {
        return nodeMap.get(source).isHidden();
    }

    /**
     * Gets source UI order
     *
     * @param source source
     * @return source UI order
     */
    public int getSourceUIOrder(JIPipeParameterCollection source) {
        return nodeMap.get(source).getUiOrder();
    }

    /**
     * Gets information about if the source is collapsed
     *
     * @param source source
     * @return if the source is collapsed by default
     */
    public boolean getSourceCollapsed(JIPipeParameterCollection source) {
        return nodeMap.get(source).collapsed;
    }

    /**
     * Gets source documentation
     *
     * @param source source
     * @return source documentation
     */
    public JIPipeDocumentation getSourceDocumentation(JIPipeParameterCollection source) {
        Node node = nodeMap.get(source);
        String name = getSourceDocumentationName(source);
        HTMLText description = node.getDescription();
        if (description == null)
            description = new HTMLText();
        return new JIPipeDefaultDocumentation(name, description.getBody());
    }

    /**
     * Sets source documentation
     *
     * @param source        source
     * @param documentation documentation
     */
    public void setSourceDocumentation(JIPipeParameterCollection source, JIPipeDefaultDocumentation documentation) {
        Node node = nodeMap.get(source);
        node.setName(documentation.name());
        node.setDescription(new HTMLText(documentation.description()));
    }

    /**
     * Returns the node of a collection
     *
     * @param collection the collection
     * @return node or null
     */
    public Node getSourceNode(JIPipeParameterCollection collection) {
        return nodeMap.get(collection);
    }

    /**
     * Removes a parameter by key
     *
     * @param key the key
     */
    public void removeParameterByKey(String key) {
        JIPipeParameterAccess access = parameters.getOrDefault(key, null);
        if (access != null) {
            parameters.remove(key);
            for (Node node : nodeMap.values()) {
                node.getParameters().inverse().remove(access);
            }
        }
    }

    /**
     * Accesses the parameters of a collection
     *
     * @param collection the collection
     * @return traversed parameters
     */
    public static Map<String, JIPipeParameterAccess> getParameters(JIPipeParameterCollection collection) {
        return (new JIPipeParameterTree(collection)).getParameters();
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

        public boolean isImportant() {
            JIPipeParameter getterAnnotation = getter.getAnnotation(JIPipeParameter.class);
            if (setter == null)
                return getterAnnotation.important();
            JIPipeParameter setterAnnotation = setter.getAnnotation(JIPipeParameter.class);
            return getterAnnotation.important() || setterAnnotation.important();
        }

        public boolean isHidden() {
            JIPipeParameter getterAnnotation = getter.getAnnotation(JIPipeParameter.class);
            if (setter == null)
                return getterAnnotation.hidden();
            JIPipeParameter setterAnnotation = setter.getAnnotation(JIPipeParameter.class);
            return getterAnnotation.hidden() || setterAnnotation.hidden();
        }

        public JIPipeParameterPersistence getPersistence() {
            JIPipeParameter getterAnnotation = getter.getAnnotation(JIPipeParameter.class);
            if (setter == null)
                return getterAnnotation.persistence();
            JIPipeParameter setterAnnotation = setter.getAnnotation(JIPipeParameter.class);
            if (getterAnnotation.persistence() != JIPipeParameterPersistence.Collection)
                return setterAnnotation.persistence();
            else
                return getterAnnotation.persistence();
        }

        public double getPriority() {
            JIPipeParameter getterAnnotation = getter.getAnnotation(JIPipeParameter.class);
            if (setter == null)
                return getterAnnotation.priority();
            JIPipeParameter setterAnnotation = setter.getAnnotation(JIPipeParameter.class);
            return getterAnnotation.priority() != Priority.NORMAL ? getterAnnotation.priority() : setterAnnotation.priority();
        }

        public String getShortKey() {
            JIPipeParameter getterAnnotation = getter.getAnnotation(JIPipeParameter.class);
            if (!StringUtils.isNullOrEmpty(getterAnnotation.shortKey()))
                return getterAnnotation.shortKey();
            JIPipeParameter setterAnnotation = setter.getAnnotation(JIPipeParameter.class);
            return setterAnnotation.shortKey();
        }

        public int getUIOrder() {
            JIPipeParameter getterAnnotation = getter.getAnnotation(JIPipeParameter.class);
            if (setter == null)
                return getterAnnotation.uiOrder();
            JIPipeParameter setterAnnotation = setter.getAnnotation(JIPipeParameter.class);
            return getterAnnotation.uiOrder() != 0 ? getterAnnotation.uiOrder() : setterAnnotation.uiOrder();
        }

        public JIPipeDocumentation getDocumentation() {
            JIPipeDocumentation[] documentations = getter.getAnnotationsByType(JIPipeDocumentation.class);
            if (documentations.length > 0)
                return documentations[0];
            if (setter == null)
                return null;
            documentations = setter.getAnnotationsByType(JIPipeDocumentation.class);
            return documentations.length > 0 ? documentations[0] : null;
        }

        public boolean isCollapsed() {
            JIPipeParameter getterAnnotation = getter.getAnnotation(JIPipeParameter.class);
            return getterAnnotation.collapsed();
        }

        public String getIconURL() {
            JIPipeParameter getterAnnotation = getter.getAnnotation(JIPipeParameter.class);
            return getterAnnotation.iconURL();
        }

        public String getIconDarkURL() {
            JIPipeParameter getterAnnotation = getter.getAnnotation(JIPipeParameter.class);
            return getterAnnotation.iconDarkURL();
        }

        public Class<?> getResourceClass() {
            JIPipeParameter getterAnnotation = getter.getAnnotation(JIPipeParameter.class);
            return getterAnnotation.resourceClass();
        }
    }

    public static class ContextAction implements Consumer<JIPipeWorkbench> {
        private Object target;
        private Method function;
        private URL iconURL;
        private JIPipeDocumentation documentation;

        public ContextAction(Object target, Method function, URL iconURL, JIPipeDocumentation documentation) {
            this.target = target;
            this.function = function;
            this.iconURL = iconURL;
            this.documentation = documentation;
        }

        public Object getTarget() {
            return target;
        }

        public Method getFunction() {
            return function;
        }

        public JIPipeDocumentation getDocumentation() {
            return documentation;
        }

        public URL getIconURL() {
            return iconURL;
        }

        @Override
        public void accept(JIPipeWorkbench workbench) {
            try {
                function.invoke(target, workbench);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * A node
     */
    public static class Node {
        private Node parent;
        private JIPipeParameterCollection collection;
        private String key;
        private boolean hidden;
        private String name;
        private HTMLText description = new HTMLText();
        private int order;
        private int uiOrder;
        private BiMap<String, JIPipeParameterAccess> parameters = HashBiMap.create();
        private BiMap<String, Node> children = HashBiMap.create();
        private List<ContextAction> actions = new ArrayList<>();
        private Set<String> uiExcludedSubParameters = new HashSet<>();
        private JIPipeParameterPersistence persistence = JIPipeParameterPersistence.Collection;
        private boolean collapsed;
        private String iconURL;
        private String darkIconURL;
        private Class<?> resourceClass;

        /**
         * Creates a node
         *
         * @param parent     the parent
         * @param collection the collection
         */
        public Node(Node parent, JIPipeParameterCollection collection) {
            this.parent = parent;
            this.collection = collection;
            if (collection instanceof JIPipeNamedParameterCollection) {
                this.name = ((JIPipeNamedParameterCollection) collection).getDefaultParameterCollectionName();
                this.description = ((JIPipeNamedParameterCollection) collection).getDefaultParameterCollectionDescription();
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

        public JIPipeParameterCollection getCollection() {
            return collection;
        }

        public void setCollection(JIPipeParameterCollection collection) {
            this.collection = collection;
        }

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public boolean isHidden() {
            return hidden;
        }

        public void setHidden(boolean hidden) {
            this.hidden = hidden;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public HTMLText getDescription() {
            return description;
        }

        public void setDescription(HTMLText description) {
            this.description = description;
        }

        public int getOrder() {
            return order;
        }

        public void setOrder(int order) {
            this.order = order;
        }

        public BiMap<String, JIPipeParameterAccess> getParameters() {
            return parameters;
        }

        public void setParameters(BiMap<String, JIPipeParameterAccess> parameters) {
            this.parameters = parameters;
        }

        public BiMap<String, Node> getChildren() {
            return children;
        }

        public void setChildren(BiMap<String, Node> children) {
            this.children = children;
        }

        public boolean isCollapsed() {
            return collapsed;
        }

        public void setCollapsed(boolean collapsed) {
            this.collapsed = collapsed;
        }

        public String getIconURL() {
            return iconURL;
        }

        public void setIconURL(String iconURL) {
            this.iconURL = iconURL;
        }

        public String getDarkIconURL() {
            return darkIconURL;
        }

        public void setDarkIconURL(String darkIconURL) {
            this.darkIconURL = darkIconURL;
        }

        public Class<?> getResourceClass() {
            return resourceClass;
        }

        public void setResourceClass(Class<?> resourceClass) {
            this.resourceClass = resourceClass;
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

        public List<ContextAction> getActions() {
            return actions;
        }

        public void setActions(List<ContextAction> actions) {
            this.actions = actions;
        }

        public JIPipeParameterPersistence getPersistence() {
            return persistence;
        }

        public void setPersistence(JIPipeParameterPersistence persistence) {
            this.persistence = persistence;
        }
    }
}

