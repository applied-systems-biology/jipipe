package org.hkijena.acaq5.api.grouping.parameters;

import com.google.common.eventbus.EventBus;
import org.hkijena.acaq5.api.parameters.ACAQCustomParameterCollection;
import org.hkijena.acaq5.api.parameters.ACAQNamedParameterCollection;
import org.hkijena.acaq5.api.parameters.ACAQParameterAccess;
import org.hkijena.acaq5.api.parameters.ACAQParameterCollection;

import java.util.HashMap;
import java.util.Map;

/**
 * Contains {@link GraphNodeParameterReferenceAccess}
 */
public class GraphNodeParameterReferenceAccessGroup implements ACAQParameterCollection, ACAQCustomParameterCollection, ACAQNamedParameterCollection {

    private final EventBus eventBus = new EventBus();
    private final GraphNodeParameterReferenceGroup group;
    private Map<String, ACAQParameterAccess> parameters = new HashMap<>();

    /**
     * Creates a new instance
     * @param group the group
     */
    public GraphNodeParameterReferenceAccessGroup(GraphNodeParameterReferenceGroup group) {
        this.group = group;
    }

    @Override
    public Map<String, ACAQParameterAccess> getParameters() {
        return parameters;
    }

    @Override
    public EventBus getEventBus() {
        return eventBus;
    }

    @Override
    public String getDefaultParameterCollectionName() {
        return group.getName();
    }

    @Override
    public String getDefaultParameterCollectionDescription() {
        return group.getDescription();
    }
}
