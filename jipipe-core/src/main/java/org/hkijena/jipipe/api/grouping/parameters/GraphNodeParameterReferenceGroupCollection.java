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
import org.hkijena.jipipe.api.validation.*;
import org.hkijena.jipipe.api.grouping.events.ParameterReferencesChangedEvent;
import org.hkijena.jipipe.api.grouping.events.ParameterReferencesChangedEventEmitter;
import org.hkijena.jipipe.api.grouping.events.ParameterReferencesChangedEventListener;
import org.hkijena.jipipe.api.nodes.JIPipeGraph;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.utils.json.JsonUtils;

import java.util.*;

/**
 * Contains a list of {@link GraphNodeParameterReferenceGroup} and {@link GraphNodeParameterCollectionReference}
 * Stores references to parameters within an {@link JIPipeGraph}
 */
public class GraphNodeParameterReferenceGroupCollection extends AbstractJIPipeParameterCollection implements JIPipeValidatable, ParameterReferencesChangedEventListener {

    private final ParameterReferencesChangedEventEmitter parameterReferencesChangedEventEmitter = new ParameterReferencesChangedEventEmitter();
    private JIPipeGraph graph;
    private List<GraphNodeParameterReferenceGroup> parameterReferenceGroups = new ArrayList<>();

    private Set<UUID> uiRestrictToCompartments;

    /**
     * Creates a new instance
     */
    public GraphNodeParameterReferenceGroupCollection() {
    }

    /**
     * Makes a copy
     *
     * @param other the original
     */
    public GraphNodeParameterReferenceGroupCollection(GraphNodeParameterReferenceGroupCollection other) {
        for (GraphNodeParameterReferenceGroup group : other.parameterReferenceGroups) {
            GraphNodeParameterReferenceGroup copy = new GraphNodeParameterReferenceGroup(group);
            this.parameterReferenceGroups.add(copy);
            copy.getParameterReferencesChangedEventEmitter().subscribeWeak(this);
        }
    }

    public ParameterReferencesChangedEventEmitter getParameterReferencesChangedEventEmitter() {
        return parameterReferencesChangedEventEmitter;
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
     * Adds a new empty group
     *
     * @return the group
     */
    public GraphNodeParameterReferenceGroup addNewGroup() {
        GraphNodeParameterReferenceGroup instance = new GraphNodeParameterReferenceGroup();
        instance.setName("New group");
        parameterReferenceGroups.add(instance);
//        instance.getEventBus().register(this);
        parameterReferencesChangedEventEmitter.emit(new ParameterReferencesChangedEvent(this));
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
//            group.getEventBus().register(this);
        }
        parameterReferencesChangedEventEmitter.emit(new ParameterReferencesChangedEvent(this));
    }

    @JsonGetter("parameter-reference-groups")
    public List<GraphNodeParameterReferenceGroup> getParameterReferenceGroups() {
        return Collections.unmodifiableList(parameterReferenceGroups);
    }

    @JsonSetter("parameter-reference-groups")
    public void setParameterReferenceGroups(List<GraphNodeParameterReferenceGroup> parameterReferenceGroups) {
        for (GraphNodeParameterReferenceGroup group : this.parameterReferenceGroups) {
            group.getParameterReferencesChangedEventEmitter().unsubscribe(this);
        }
        this.parameterReferenceGroups = parameterReferenceGroups;
        for (GraphNodeParameterReferenceGroup group : this.parameterReferenceGroups) {
            group.getParameterReferencesChangedEventEmitter().subscribeWeak(this);
        }
        parameterReferencesChangedEventEmitter.emit(new ParameterReferencesChangedEvent(this));
    }

    /**
     * Removes the group
     *
     * @param group the group
     */
    public void removeGroup(GraphNodeParameterReferenceGroup group) {
        parameterReferenceGroups.remove(group);
        parameterReferencesChangedEventEmitter.emit(new ParameterReferencesChangedEvent(this));
    }

    @Override
    public void reportValidity(JIPipeValidationReportEntryCause parentCause, JIPipeValidationReport report) {
        if (graph != null) {
            JIPipeParameterTree tree = graph.getParameterTree(false, null);
            for (GraphNodeParameterReferenceGroup parameterReferenceGroup : parameterReferenceGroups) {
                for (GraphNodeParameterReference reference : parameterReferenceGroup.getContent()) {
                    if (reference.resolve(tree) == null) {
                        report.add(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Error, parentCause,
                                "Could not find parameter!",
                                "There is a an exported parameter referencing the internal ID '" + reference.getPath() + "'. " +
                                        "It could not be found.",
                                "Please check if you did not delete the node that contains the referenced parameter.",
                                JsonUtils.toPrettyJsonString(this)));
                    }
                }
            }
        }
    }

    public Set<UUID> getUiRestrictToCompartments() {
        return uiRestrictToCompartments;
    }

    public void setUiRestrictToCompartments(Set<UUID> uiRestrictToCompartments) {
        this.uiRestrictToCompartments = uiRestrictToCompartments;
    }

    @Override
    public void onParameterReferencesChanged(ParameterReferencesChangedEvent event) {
        parameterReferencesChangedEventEmitter.emit(event);
    }
}
