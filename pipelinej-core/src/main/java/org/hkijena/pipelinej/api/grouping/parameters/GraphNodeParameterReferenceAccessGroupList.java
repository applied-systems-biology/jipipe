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

package org.hkijena.pipelinej.api.grouping.parameters;

import com.google.common.eventbus.EventBus;
import org.hkijena.pipelinej.api.parameters.ACAQCustomParameterCollection;
import org.hkijena.pipelinej.api.parameters.ACAQParameterAccess;
import org.hkijena.pipelinej.api.parameters.ACAQParameterCollection;
import org.hkijena.pipelinej.api.parameters.ACAQParameterTree;
import org.hkijena.pipelinej.utils.StringUtils;

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
