package org.hkijena.acaq5.api.parameters;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.events.ParameterChangedEvent;
import org.hkijena.acaq5.api.events.ParameterStructureChangedEvent;
import org.scijava.Priority;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

/**
 * An {@link ACAQParameterCollection} that contains the parameters of one or multiple
 * {@link ACAQParameterCollection} instances in a traversed form.
 */
public class ACAQTraversedParameterCollection implements ACAQParameterCollection, ACAQCustomParameterCollection {

    private EventBus eventBus = new EventBus();
    private Set<ACAQParameterCollection> registeredSources = new HashSet<>();
    private Map<ACAQParameterCollection, String> sourceKeys = new HashMap<>();
    private Map<ACAQParameterCollection, ACAQDocumentation> sourceDocumentation = new HashMap<>();
    private BiMap<String, ACAQParameterAccess> parameters = HashBiMap.create();
    private PriorityQueue<ACAQParameterAccess> parametersByPriority = new PriorityQueue<>(ACAQParameterAccess::comparePriority);

    private boolean ignoreReflectionParameters = false;
    private boolean ignoreCustomParameters = false;
    private boolean forceReflection = false;

    /**
     * Creates a new instance
     *
     * @param sources Parameter collections to add. The list of parents is assumed to be empty for each entry.
     */
    public ACAQTraversedParameterCollection(ACAQParameterCollection... sources) {
        for (ACAQParameterCollection source : sources) {
            add(source, Collections.emptyList());
        }
    }

    /**
     * Sets the key of an {@link ACAQParameterCollection}
     * This is used to generate an unique key for sub-parameters
     *
     * @param source the collection
     * @param name   unique key within its parent
     */
    public void setSourceKey(ACAQParameterCollection source, String name) {
        sourceKeys.put(source, name);
    }

    /**
     * Gets the key of an {@link ACAQParameterCollection}
     *
     * @param source the collection
     * @return the key or null if none was set
     */
    public String getSourceKey(ACAQParameterCollection source) {
        return sourceKeys.getOrDefault(source, null);
    }

    /**
     * Gets the parameters grouped by the source
     *
     * @return
     */
    public Map<ACAQParameterCollection, List<ACAQParameterAccess>> getGroupedBySource() {
        Map<ACAQParameterCollection, List<ACAQParameterAccess>> result = parameters.values().stream().collect(Collectors.groupingBy(ACAQParameterAccess::getSource));
        for (ACAQParameterCollection registeredSource : registeredSources) {
            if (!result.containsKey(registeredSource))
                result.put(registeredSource, new ArrayList<>());
        }
        return result;
    }

    /**
     * Sets the documentation of an {@link ACAQParameterCollection}
     * This is queried by UI
     *
     * @param source        the collection
     * @param documentation the documentation
     */
    public void setSourceDocumentation(ACAQParameterCollection source, ACAQDocumentation documentation) {
        sourceDocumentation.put(source, documentation);
    }

    /**
     * Gets the documentation of an {@link ACAQParameterCollection}
     *
     * @param source the collection
     * @return the documentation or null if none was provided
     */
    public ACAQDocumentation getSourceDocumentation(ACAQParameterCollection source) {
        return sourceDocumentation.getOrDefault(source, null);
    }

    /**
     * Gets the documentation of an {@link ACAQParameterCollection}
     *
     * @param source the collection
     * @return the documentation or an empty string if none was provided
     */
    public String getSourceDocumentationName(ACAQParameterCollection source) {
        ACAQDocumentation documentation = sourceDocumentation.getOrDefault(source, null);
        if (documentation != null) {
            return "" + documentation.name();
        }
        return "";
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
     * @param source    the added collection
     * @param hierarchy hierarchy behind this parameter collection
     */
    public void add(ACAQParameterCollection source, List<ACAQParameterCollection> hierarchy) {
        if (registeredSources.contains(source))
            return;
        if (!forceReflection && source instanceof ACAQCustomParameterCollection) {
            if (ignoreCustomParameters)
                return;
            for (Map.Entry<String, ACAQParameterAccess> entry : ((ACAQCustomParameterCollection) source).getParameters().entrySet()) {
                addParameter(entry.getKey(), entry.getValue(), hierarchy);
            }
        } else {
            if (ignoreReflectionParameters)
                return;
            addReflectionParameters(source, hierarchy);
        }
        registeredSources.add(source);
        source.getEventBus().register(this);
    }

    private void addParameter(String initialKey, ACAQParameterAccess parameterAccess, List<ACAQParameterCollection> hierarchy) {
        List<String> keys = new ArrayList<>();
        for (ACAQParameterCollection collection : hierarchy) {
            keys.add(sourceKeys.getOrDefault(collection, ""));
        }
        keys.add(initialKey);
        String key = String.join("/", keys);
        parameters.put(key, parameterAccess);
        parametersByPriority.add(parameterAccess);
    }

    private void addReflectionParameters(ACAQParameterCollection source, List<ACAQParameterCollection> hierarchy) {

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
            if (pair.getFieldClass() != null && !ACAQParameterCollection.class.isAssignableFrom(pair.getFieldClass())) {
                if (pair.getter == null || pair.setter == null)
                    throw new RuntimeException("Invalid parameter definition: Getter or setter could not be found for key '" + entry.getKey() + "' in " + source);

                ACAQReflectionParameterAccess parameterAccess = new ACAQReflectionParameterAccess();
                parameterAccess.setSource(source);
                parameterAccess.setKey(entry.getKey());
                parameterAccess.setGetter(pair.getter);
                parameterAccess.setSetter(pair.setter);
                parameterAccess.setDocumentation(pair.getDocumentation());
                parameterAccess.setVisibility(pair.getVisibility());
                parameterAccess.setPriority(pair.getPriority());

                addParameter(entry.getKey(), parameterAccess, hierarchy);
            }
        }

        // Add sub-parameters
        for (Map.Entry<String, GetterSetterPair> entry : getterSetterPairs.entrySet()) {
            GetterSetterPair pair = entry.getValue();
            if (pair.getFieldClass() != null && ACAQParameterCollection.class.isAssignableFrom(pair.getFieldClass())) {
                try {
                    ACAQParameterCollection subParameters = (ACAQParameterCollection) pair.getter.invoke(source);
                    if (subParameters == null)
                        continue;
                    setSourceDocumentation(subParameters, pair.getDocumentation());
                    setSourceKey(subParameters, entry.getKey());
                    List<ACAQParameterCollection> subParameterHierarchy = new ArrayList<>(hierarchy);
                    subParameterHierarchy.add(subParameters);
                    add(subParameters, subParameterHierarchy);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public Map<String, ACAQParameterAccess> getParameters() {
        return Collections.unmodifiableMap(parameters);
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

    public Set<ACAQParameterCollection> getRegisteredSources() {
        return Collections.unmodifiableSet(registeredSources);
    }

    public boolean isForceReflection() {
        return forceReflection;
    }

    public void setForceReflection(boolean forceReflection) {
        this.forceReflection = forceReflection;
    }

    /**
     * Accesses the parameters of a collection
     *
     * @param collection the collection
     * @return traversed parameters
     */
    public static Map<String, ACAQParameterAccess> getParameters(ACAQParameterCollection collection) {
        return (new ACAQTraversedParameterCollection(collection)).getParameters();
    }

    /**
     * Accesses the parameters of a collection
     *
     * @param collection     the collection
     * @param withReflection if reflection parameters are included
     * @param withDynamic    if dynamic/custom parameters are included
     * @return traversed parameters
     */
    public static Map<String, ACAQParameterAccess> getParameters(ACAQParameterCollection collection, boolean withReflection, boolean withDynamic) {
        ACAQTraversedParameterCollection traversedParameterCollection = new ACAQTraversedParameterCollection();
        traversedParameterCollection.setIgnoreReflectionParameters(!withReflection);
        traversedParameterCollection.setIgnoreCustomParameters(!withDynamic);
        traversedParameterCollection.add(collection, Collections.emptyList());
        return traversedParameterCollection.parameters;
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
}
