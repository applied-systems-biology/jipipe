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

package org.hkijena.jipipe.api.grouping;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import com.google.common.eventbus.Subscribe;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeValidityReport;
import org.hkijena.jipipe.api.compartments.algorithms.IOInterfaceAlgorithm;
import org.hkijena.jipipe.api.compartments.algorithms.JIPipeCompartmentOutput;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeMutableSlotConfiguration;
import org.hkijena.jipipe.api.events.ParameterStructureChangedEvent;
import org.hkijena.jipipe.api.grouping.events.ParameterReferencesChangedEvent;
import org.hkijena.jipipe.api.grouping.parameters.GraphNodeParameterReferenceAccessGroupList;
import org.hkijena.jipipe.api.grouping.parameters.GraphNodeParameters;
import org.hkijena.jipipe.api.grouping.parameters.NodeGroupContents;
import org.hkijena.jipipe.api.nodes.JIPipeGraph;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.categories.MiscellaneousNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeCustomParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.utils.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * A sub-graph algorithm that can be defined by a user
 */
@JIPipeDocumentation(name = "Group", description = "A sub-graph that contains its own pipeline.")
@JIPipeOrganization(nodeTypeCategory = MiscellaneousNodeTypeCategory.class)
public class NodeGroup extends GraphWrapperAlgorithm implements JIPipeCustomParameterCollection {

    private NodeGroupContents contents;
    private GraphNodeParameters exportedParameters = new GraphNodeParameters();

    /**
     * Creates a new instance
     *
     * @param info the info
     */
    public NodeGroup(JIPipeNodeInfo info) {
        super(info, new JIPipeGraph());
        initializeContents();
        this.exportedParameters.getEventBus().register(this);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public NodeGroup(NodeGroup other) {
        super(other);
        this.exportedParameters = new GraphNodeParameters(other.exportedParameters);
        this.exportedParameters.getEventBus().register(this);
        initializeContents();
    }

    /**
     * Initializes from an existing graph
     *
     * @param graph           algorithms to be added
     * @param autoCreateSlots automatically create input and output slots
     */
    public NodeGroup(JIPipeGraph graph, boolean autoCreateSlots) {
        super(JIPipe.getNodes().getInfoById("node-group"), new JIPipeGraph());

        // Remove all algorithms with no i/o
//        for (JIPipeGraphNode node : ImmutableList.copyOf(graph.getAlgorithmNodes().values())) {
//            if (node.getInputSlots().isEmpty() && node.getOutputSlots().isEmpty()) {
//                graph.removeNode(node, false);
//            }
//        }

        // Clear locations
        for (JIPipeGraphNode node : graph.getNodes().values()) {
            node.clearLocations();
        }

        // Replace all JIPipeCompartmentOutput by IOInterfaceAlgorithm
        for (JIPipeGraphNode node : ImmutableList.copyOf(graph.getNodes().values())) {
            if (node instanceof JIPipeCompartmentOutput) {
                IOInterfaceAlgorithm.replaceCompartmentOutput((JIPipeCompartmentOutput) node);
            }
        }

        // Second copy
        setWrappedGraph(graph);
        initializeContents();

        if (autoCreateSlots) {
            setPreventUpdateSlots(true);
            JIPipeMutableSlotConfiguration inputSlotConfiguration = (JIPipeMutableSlotConfiguration) getGroupInput().getSlotConfiguration();
            JIPipeMutableSlotConfiguration outputSlotConfiguration = (JIPipeMutableSlotConfiguration) getGroupOutput().getSlotConfiguration();
            BiMap<JIPipeDataSlot, String> exportedInputSlotNames = HashBiMap.create();
            BiMap<JIPipeDataSlot, String> exportedOutputSlotNames = HashBiMap.create();
            for (JIPipeDataSlot slot : getWrappedGraph().getUnconnectedSlots()) {
                if (slot.isInput()) {
                    String uniqueName = StringUtils.makeUniqueString(slot.getName(), " ", exportedInputSlotNames::containsValue);
                    inputSlotConfiguration.addSlot(uniqueName, slot.getInfo(), false);
                    exportedInputSlotNames.put(slot, uniqueName);
                } else if (slot.isOutput()) {
                    String uniqueName = StringUtils.makeUniqueString(slot.getName(), " ", exportedOutputSlotNames::containsValue);
                    outputSlotConfiguration.addSlot(uniqueName, slot.getInfo(), false);
                    exportedOutputSlotNames.put(slot, uniqueName);
                }
            }
            setPreventUpdateSlots(false);
            updateGroupSlots();

            // Internal input slot -> Connect to group output
            for (Map.Entry<JIPipeDataSlot, String> entry : exportedInputSlotNames.entrySet()) {
                JIPipeDataSlot internalSlot = entry.getKey();
                String exportedName = entry.getValue();
                JIPipeDataSlot source = getGroupInput().getOutputSlot(exportedName);
                getWrappedGraph().connect(source, internalSlot);
            }
            // Internal output slot -> Connect to group input
            for (Map.Entry<JIPipeDataSlot, String> entry : exportedOutputSlotNames.entrySet()) {
                JIPipeDataSlot internalSlot = entry.getKey();
                String exportedName = entry.getValue();
                JIPipeDataSlot target = getGroupOutput().getInputSlot(exportedName);
                getWrappedGraph().connect(internalSlot, target);
            }
        }

        this.exportedParameters.getEventBus().register(this);
    }

    private void initializeContents() {
        contents = new NodeGroupContents();
        contents.setWrappedGraph(getWrappedGraph());
        exportedParameters.setGraph(contents.getWrappedGraph());
        contents.setParent(this);
    }

    @JIPipeDocumentation(name = "Wrapped graph", description = "The graph that is wrapped inside this node")
    @JIPipeParameter("contents")
    public NodeGroupContents getContents() {
        return contents;
    }

    @JIPipeParameter("contents")
    public void setContents(NodeGroupContents contents) {
        this.contents = contents;
        contents.setParent(this);
        setWrappedGraph(contents.getWrappedGraph());
        exportedParameters.setGraph(contents.getWrappedGraph());
    }

    @JIPipeDocumentation(name = "Exported parameters", description = "Allows you to export parameters from the group into the group node")
    @JIPipeParameter("exported-parameters")
    public GraphNodeParameters getExportedParameters() {
        return exportedParameters;
    }

    @JIPipeParameter("exported-parameters")
    public void setExportedParameters(GraphNodeParameters exportedParameters) {
        this.exportedParameters = exportedParameters;
        this.exportedParameters.setGraph(getWrappedGraph());
        this.exportedParameters.getEventBus().register(this);
    }

    @Override
    public void reportValidity(JIPipeValidityReport report) {
        super.reportValidity(report);
        report.forCategory("Exported parameters").report(exportedParameters);

        // Only check if the graph creates a valid group output
        getWrappedGraph().reportValidity(report.forCategory("Wrapped graph"), getGroupOutput(), Sets.newHashSet(getGroupInput()));
    }

    @Override
    public Map<String, JIPipeParameterAccess> getParameters() {
        JIPipeParameterTree standardParameters = new JIPipeParameterTree(this,
                JIPipeParameterTree.IGNORE_CUSTOM | JIPipeParameterTree.FORCE_REFLECTION);
        return standardParameters.getParameters();
    }

    @Override
    public Map<String, JIPipeParameterCollection> getChildParameterCollections() {
        this.exportedParameters.setGraph(getWrappedGraph());
        Map<String, JIPipeParameterCollection> result = new HashMap<>();
        result.put("exported", new GraphNodeParameterReferenceAccessGroupList(exportedParameters, getWrappedGraph().getParameterTree(), false));
        return result;
    }

    @Subscribe
    public void onParameterReferencesChanged(ParameterReferencesChangedEvent event) {
        getEventBus().post(new ParameterStructureChangedEvent(this));
    }
}
