package org.hkijena.acaq5.api.grouping.parameters;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.google.common.eventbus.EventBus;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmGraph;
import org.hkijena.acaq5.api.grouping.events.ParameterReferencesChangedEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Contains a list of {@link GraphNodeParameterReferenceGroup} and {@link GraphNodeParameterCollectionReference}
 * Stores references to parameters within an {@link org.hkijena.acaq5.api.algorithm.ACAQAlgorithmGraph}
 */
public class GraphNodeParameters {
    private final EventBus eventBus = new EventBus();
    private ACAQAlgorithmGraph graph;
    private List<GraphNodeParameterReferenceGroup> parameterReferenceGroups = new ArrayList<>();

    /**
     * Creates a new instance
     */
    public GraphNodeParameters() {
    }

    /**
     * Makes a copy
     * @param other the original
     */
    public GraphNodeParameters(GraphNodeParameters other) {
        for (GraphNodeParameterReferenceGroup group : other.parameterReferenceGroups) {
            this.parameterReferenceGroups.add(new GraphNodeParameterReferenceGroup(group));
        }
    }

    /**
     * Returns the reference to the graph. This will be accessed by the editor component to pick new parameters
     * and resolve them.
     * @return reference to the graph if available
     */
    public ACAQAlgorithmGraph getGraph() {
        return graph;
    }

    public void setGraph(ACAQAlgorithmGraph graph) {
        this.graph = graph;
    }

    /**
     * Event bus that triggers {@link org.hkijena.acaq5.api.grouping.events.ParameterReferencesChangedEvent}
     * @return event bus
     */
    public EventBus getEventBus() {
        return eventBus;
    }

    /**
     * Adds a new empty group
     * @return the group
     */
    public GraphNodeParameterReferenceGroup addGroup() {
        GraphNodeParameterReferenceGroup instance = new GraphNodeParameterReferenceGroup();
        instance.setName("New group");
        eventBus.post(new ParameterReferencesChangedEvent());
        return instance;
    }

    @JsonGetter("parameter-reference-groups")
    public List<GraphNodeParameterReferenceGroup> getParameterReferenceGroups() {
        return Collections.unmodifiableList(parameterReferenceGroups);
    }

    @JsonSetter("parameter-reference-groups")
    public void setParameterReferenceGroups(List<GraphNodeParameterReferenceGroup> parameterReferenceGroups) {
        this.parameterReferenceGroups = parameterReferenceGroups;
        eventBus.post(new ParameterReferencesChangedEvent());
    }
}
