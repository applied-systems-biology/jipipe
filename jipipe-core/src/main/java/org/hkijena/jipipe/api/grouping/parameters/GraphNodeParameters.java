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

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.hkijena.jipipe.api.JIPipeIssueReport;
import org.hkijena.jipipe.api.JIPipeValidatable;
import org.hkijena.jipipe.api.grouping.events.ParameterReferencesChangedEvent;
import org.hkijena.jipipe.api.nodes.JIPipeGraph;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Contains a list of {@link GraphNodeParameterReferenceGroup} and {@link GraphNodeParameterCollectionReference}
 * Stores references to parameters within an {@link JIPipeGraph}
 */
public class GraphNodeParameters implements JIPipeValidatable {
    private final EventBus eventBus = new EventBus();
    private JIPipeGraph graph;
    private List<GraphNodeParameterReferenceGroup> parameterReferenceGroups = new ArrayList<>();

    /**
     * Creates a new instance
     */
    public GraphNodeParameters() {
    }

    /**
     * Makes a copy
     *
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
     *
     * @return reference to the graph if available
     */
    public JIPipeGraph getGraph() {
        return graph;
    }

    public void setGraph(JIPipeGraph graph) {
        this.graph = graph;
    }

    /**
     * Event bus that triggers {@link org.hkijena.jipipe.api.grouping.events.ParameterReferencesChangedEvent}
     *
     * @return event bus
     */
    public EventBus getEventBus() {
        return eventBus;
    }

    /**
     * Adds a new empty group
     *
     * @return the group
     */
    public GraphNodeParameterReferenceGroup addNewGroup() {
        GraphNodeParameterReferenceGroup instance = new GraphNodeParameterReferenceGroup();
        instance.setName("New group");
        parameterReferenceGroups.add(instance);
        instance.getEventBus().register(this);
        eventBus.post(new ParameterReferencesChangedEvent());
        return instance;
    }

    /**
     * Adds multiple groups
     *
     * @param groups groups
     */
    public void addGroups(Collection<GraphNodeParameterReferenceGroup> groups) {
        for (GraphNodeParameterReferenceGroup group : groups) {
            parameterReferenceGroups.add(group);
            group.getEventBus().register(this);
        }
        eventBus.post(new ParameterReferencesChangedEvent());
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
     *
     * @param event the event
     */
    @Subscribe
    public void onReferencesChanged(ParameterReferencesChangedEvent event) {
        eventBus.post(event);
    }

    /**
     * Removes the group
     *
     * @param group the group
     */
    public void removeGroup(GraphNodeParameterReferenceGroup group) {
        parameterReferenceGroups.remove(group);
        eventBus.post(new ParameterReferencesChangedEvent());
    }

    @Override
    public void reportValidity(JIPipeIssueReport report) {
        if (graph != null) {
            JIPipeParameterTree tree = graph.getParameterTree(false);
            for (GraphNodeParameterReferenceGroup parameterReferenceGroup : parameterReferenceGroups) {
                JIPipeIssueReport group = report.resolve(parameterReferenceGroup.getName());
                for (GraphNodeParameterReference reference : parameterReferenceGroup.getContent()) {
                    if (reference.resolve(tree) == null) {
                        group.resolve(reference.getName(tree)).reportIsInvalid("Could not find parameter!",
                                "There is a an exported parameter referencing the internal ID '" + reference.getPath() + "'. " +
                                        "It could not be found.",
                                "Please check if you did not delete the node that contains the referenced parameter.",
                                this);
                    }
                }
            }
        }
    }
}
