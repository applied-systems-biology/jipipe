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

package org.hkijena.acaq5.api.grouping;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmCategory;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmDeclaration;
import org.hkijena.acaq5.api.algorithm.ACAQGraph;
import org.hkijena.acaq5.api.algorithm.ACAQGraphNode;
import org.hkijena.acaq5.api.compartments.algorithms.ACAQCompartmentOutput;
import org.hkijena.acaq5.api.compartments.algorithms.IOInterfaceAlgorithm;
import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.api.data.ACAQMutableSlotConfiguration;
import org.hkijena.acaq5.api.grouping.parameters.GraphNodeParameterReferenceAccessGroupList;
import org.hkijena.acaq5.api.grouping.parameters.GraphNodeParameters;
import org.hkijena.acaq5.api.grouping.parameters.NodeGroupContents;
import org.hkijena.acaq5.api.parameters.ACAQCustomParameterCollection;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.api.parameters.ACAQParameterAccess;
import org.hkijena.acaq5.api.parameters.ACAQParameterCollection;
import org.hkijena.acaq5.api.parameters.ACAQParameterTree;
import org.hkijena.acaq5.api.registries.ACAQAlgorithmRegistry;
import org.hkijena.acaq5.utils.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * A sub-graph algorithm that can be defined by a user
 */
@ACAQDocumentation(name = "Group", description = "A sub-graph that contains its own pipeline.")
@ACAQOrganization(algorithmCategory = ACAQAlgorithmCategory.Miscellaneous)
public class NodeGroup extends GraphWrapperAlgorithm implements ACAQCustomParameterCollection {

    private NodeGroupContents contents;
    private GraphNodeParameters exportedParameters = new GraphNodeParameters();

    /**
     * Creates a new instance
     *
     * @param declaration the declaration
     */
    public NodeGroup(ACAQAlgorithmDeclaration declaration) {
        super(declaration, new ACAQGraph());
        initializeContents();
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public NodeGroup(NodeGroup other) {
        super(other);
        this.exportedParameters = new GraphNodeParameters(other.exportedParameters);
        initializeContents();
    }

    /**
     * Initializes from an existing graph
     *
     * @param graph           algorithms to be added
     * @param autoCreateSlots automatically create input and output slots
     */
    public NodeGroup(ACAQGraph graph, boolean autoCreateSlots) {
        super(ACAQAlgorithmRegistry.getInstance().getDeclarationById("node-group"), new ACAQGraph());

        // Remove all algorithms with no i/o
//        for (ACAQGraphNode node : ImmutableList.copyOf(graph.getAlgorithmNodes().values())) {
//            if (node.getInputSlots().isEmpty() && node.getOutputSlots().isEmpty()) {
//                graph.removeNode(node, false);
//            }
//        }

        // Clear locations
        for (ACAQGraphNode node : graph.getAlgorithmNodes().values()) {
            node.clearLocations();
        }

        // Replace all ACAQCompartmentOutput by IOInterfaceAlgorithm
        for (ACAQGraphNode node : ImmutableList.copyOf(graph.getAlgorithmNodes().values())) {
            if (node instanceof ACAQCompartmentOutput) {
                IOInterfaceAlgorithm.replaceCompartmentOutput((ACAQCompartmentOutput) node);
            }
        }

        // Second copy
        setWrappedGraph(graph);
        initializeContents();

        if (autoCreateSlots) {
            setPreventUpdateSlots(true);
            ACAQMutableSlotConfiguration inputSlotConfiguration = (ACAQMutableSlotConfiguration) getGroupInput().getSlotConfiguration();
            ACAQMutableSlotConfiguration outputSlotConfiguration = (ACAQMutableSlotConfiguration) getGroupOutput().getSlotConfiguration();
            BiMap<ACAQDataSlot, String> exportedInputSlotNames = HashBiMap.create();
            BiMap<ACAQDataSlot, String> exportedOutputSlotNames = HashBiMap.create();
            for (ACAQDataSlot slot : getWrappedGraph().getUnconnectedSlots()) {
                if (slot.isInput()) {
                    String uniqueName = StringUtils.makeUniqueString(slot.getName(), " ", exportedInputSlotNames::containsValue);
                    inputSlotConfiguration.addSlot(uniqueName, slot.getDefinition(), false);
                    exportedInputSlotNames.put(slot, uniqueName);
                } else if (slot.isOutput()) {
                    String uniqueName = StringUtils.makeUniqueString(slot.getName(), " ", exportedOutputSlotNames::containsValue);
                    outputSlotConfiguration.addSlot(uniqueName, slot.getDefinition(), false);
                    exportedOutputSlotNames.put(slot, uniqueName);
                }
            }
            setPreventUpdateSlots(false);
            updateGroupSlots();

            // Internal input slot -> Connect to group output
            for (Map.Entry<ACAQDataSlot, String> entry : exportedInputSlotNames.entrySet()) {
                ACAQDataSlot internalSlot = entry.getKey();
                String exportedName = entry.getValue();
                ACAQDataSlot source = getGroupInput().getOutputSlot(exportedName);
                getWrappedGraph().connect(source, internalSlot);
            }
            // Internal output slot -> Connect to group input
            for (Map.Entry<ACAQDataSlot, String> entry : exportedOutputSlotNames.entrySet()) {
                ACAQDataSlot internalSlot = entry.getKey();
                String exportedName = entry.getValue();
                ACAQDataSlot target = getGroupOutput().getInputSlot(exportedName);
                getWrappedGraph().connect(internalSlot, target);
            }
        }
    }

    private void initializeContents() {
        contents = new NodeGroupContents();
        contents.setWrappedGraph(getWrappedGraph());
        exportedParameters.setGraph(contents.getWrappedGraph());
        contents.setParent(this);
    }

    @ACAQDocumentation(name = "Wrapped graph", description = "The graph that is wrapped inside this node")
    @ACAQParameter("contents")
    public NodeGroupContents getContents() {
        return contents;
    }

    @ACAQParameter("contents")
    public void setContents(NodeGroupContents contents) {
        this.contents = contents;
        contents.setParent(this);
        setWrappedGraph(contents.getWrappedGraph());
        exportedParameters.setGraph(contents.getWrappedGraph());
    }

    @ACAQDocumentation(name = "Exported parameters", description = "Allows you to export parameters from the group into the group node")
    @ACAQParameter("exported-parameters")
    public GraphNodeParameters getExportedParameters() {
        return exportedParameters;
    }

    @ACAQParameter("exported-parameters")
    public void setExportedParameters(GraphNodeParameters exportedParameters) {
        this.exportedParameters = exportedParameters;
        this.exportedParameters.setGraph(getWrappedGraph());
    }

    @Override
    public void reportValidity(ACAQValidityReport report) {
        super.reportValidity(report);
        report.forCategory("Exported parameters").report(exportedParameters);

        // Only check if the graph creates a valid group output
        getWrappedGraph().reportValidity(report.forCategory("Wrapped graph"), getGroupOutput(), Sets.newHashSet(getGroupInput()));
    }

    @Override
    public Map<String, ACAQParameterAccess> getParameters() {
        ACAQParameterTree standardParameters = new ACAQParameterTree(this,
                ACAQParameterTree.IGNORE_CUSTOM | ACAQParameterTree.FORCE_REFLECTION);
        return standardParameters.getParameters();
    }

    @Override
    public Map<String, ACAQParameterCollection> getChildParameterCollections() {
        this.exportedParameters.setGraph(getWrappedGraph());
        Map<String, ACAQParameterCollection> result = new HashMap<>();
        result.put("exported", new GraphNodeParameterReferenceAccessGroupList(exportedParameters, getWrappedGraph().getParameterTree(), false));
        return result;
    }
}
