package org.hkijena.acaq5.api.grouping.parameters;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmGraph;
import org.hkijena.acaq5.api.grouping.events.ParameterReferencesChangedEvent;
import org.hkijena.acaq5.api.parameters.ACAQParameterAccess;
import org.hkijena.acaq5.api.parameters.ACAQParameterCollection;
import org.hkijena.acaq5.api.parameters.ACAQParameterTree;
import org.hkijena.acaq5.utils.StringUtils;

import java.util.*;

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
            GraphNodeParameterReferenceGroup copy = new GraphNodeParameterReferenceGroup(group);
            this.parameterReferenceGroups.add(copy);
            copy.getEventBus().register(this);
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
        parameterReferenceGroups.add(instance);
        instance.getEventBus().register(this);
        eventBus.post(new ParameterReferencesChangedEvent());
        return instance;
    }

    @JsonGetter("parameter-reference-groups")
    public List<GraphNodeParameterReferenceGroup> getParameterReferenceGroups() {
        return Collections.unmodifiableList(parameterReferenceGroups);
    }

    @JsonSetter("parameter-reference-groups")
    public void setParameterReferenceGroups(List<GraphNodeParameterReferenceGroup> parameterReferenceGroups) {
        for (GraphNodeParameterReferenceGroup group : this.parameterReferenceGroups) {
            group.getEventBus().unregister(this);
        }
        this.parameterReferenceGroups = parameterReferenceGroups;
        for (GraphNodeParameterReferenceGroup group : this.parameterReferenceGroups) {
            group.getEventBus().register(this);
        }
        eventBus.post(new ParameterReferencesChangedEvent());
    }

    /**
     * Triggered when some parameters were changed down the line
     * @param event the event
     */
    @Subscribe
    public void onReferencesChanged(ParameterReferencesChangedEvent event) {
        eventBus.post(event);
    }

    /**
     * Removes the group
     * @param group the group
     */
    public void removeGroup(GraphNodeParameterReferenceGroup group) {
        parameterReferenceGroups.remove(group);
        eventBus.post(new ParameterReferencesChangedEvent());
    }

    /**
     * Converts the references into actual parameter collections
     * @return referenced parameters
     * @param tree the tree of the referenced parameters
     */
    public List<ACAQParameterCollection> build(ACAQParameterTree tree) {
        List<ACAQParameterCollection> result = new ArrayList<>();

        return result;
    }

}
