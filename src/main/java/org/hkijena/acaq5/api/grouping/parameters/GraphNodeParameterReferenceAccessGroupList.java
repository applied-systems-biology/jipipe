package org.hkijena.acaq5.api.grouping.parameters;

import com.google.common.eventbus.EventBus;
import org.hkijena.acaq5.api.parameters.ACAQCustomParameterCollection;
import org.hkijena.acaq5.api.parameters.ACAQParameterAccess;
import org.hkijena.acaq5.api.parameters.ACAQParameterCollection;
import org.hkijena.acaq5.api.parameters.ACAQParameterTree;
import org.hkijena.acaq5.utils.StringUtils;

import java.util.*;

/**
 * Contains {@link GraphNodeParameterReferenceAccess}
 */
public class GraphNodeParameterReferenceAccessGroupList implements ACAQParameterCollection, ACAQCustomParameterCollection {

    private final EventBus eventBus = new EventBus();
    private final Map<String, ACAQParameterCollection> childCollections = new HashMap<>();

    /**
     * Creates a new instance
     *
     * @param parameters the parameters
     * @param tree       tree of the referenced graph
     * @param persistent if values are persistent
     */
    public GraphNodeParameterReferenceAccessGroupList(GraphNodeParameters parameters, ACAQParameterTree tree, boolean persistent) {

        Set<ACAQParameterAccess> existingParameters = new HashSet<>();
        for (GraphNodeParameterReferenceGroup group : parameters.getParameterReferenceGroups()) {
            GraphNodeParameterReferenceAccessGroup collection = new GraphNodeParameterReferenceAccessGroup(group);

            for (GraphNodeParameterReference reference : group.getContent()) {
                ACAQParameterAccess access = reference.resolve(tree);
                if (access != null && !existingParameters.contains(access)) {
                    GraphNodeParameterReferenceAccess wrappedAccess = new GraphNodeParameterReferenceAccess(reference, tree, collection, persistent);
                    String key = StringUtils.makeUniqueString(access.getKey(), "-", collection.getParameters()::containsKey);
                    collection.getParameters().put(key, wrappedAccess);
                    existingParameters.add(access);
                }
            }

            String groupKey = StringUtils.makeUniqueString(("" + group.getName()).toLowerCase(), "-", childCollections::containsKey);
            childCollections.put(groupKey, collection);
        }
    }

    @Override
    public Map<String, ACAQParameterCollection> getChildParameterCollections() {
        return childCollections;
    }

    @Override
    public Map<String, ACAQParameterAccess> getParameters() {
        return Collections.emptyMap();
    }

    @Override
    public EventBus getEventBus() {
        return eventBus;
    }

}
