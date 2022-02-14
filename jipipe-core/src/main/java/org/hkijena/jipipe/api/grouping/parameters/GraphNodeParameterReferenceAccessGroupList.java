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

package org.hkijena.jipipe.api.grouping.parameters;

import com.google.common.eventbus.EventBus;
import org.hkijena.jipipe.api.parameters.JIPipeCustomParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.utils.StringUtils;

import java.util.*;

/**
 * Contains {@link GraphNodeParameterReferenceAccess}
 */
public class GraphNodeParameterReferenceAccessGroupList implements JIPipeParameterCollection, JIPipeCustomParameterCollection {

    private final EventBus eventBus = new EventBus();
    private final Map<String, JIPipeParameterCollection> childCollections = new HashMap<>();

    /**
     * Creates a new instance
     *
     * @param parameters the parameters
     * @param tree       tree of the referenced graph
     * @param persistent if values are persistent
     */
    public GraphNodeParameterReferenceAccessGroupList(GraphNodeParameters parameters, JIPipeParameterTree tree, boolean persistent) {

        Set<JIPipeParameterAccess> existingParameters = new HashSet<>();
        for (GraphNodeParameterReferenceGroup group : parameters.getParameterReferenceGroups()) {
            GraphNodeParameterReferenceAccessGroup collection = new GraphNodeParameterReferenceAccessGroup(group);

            for (GraphNodeParameterReference reference : group.getContent()) {
                JIPipeParameterAccess access = reference.resolve(tree);
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
    public Map<String, JIPipeParameterCollection> getChildParameterCollections() {
        return childCollections;
    }

    @Override
    public Map<String, JIPipeParameterAccess> getParameters() {
        return Collections.emptyMap();
    }

    @Override
    public EventBus getEventBus() {
        return eventBus;
    }

}
