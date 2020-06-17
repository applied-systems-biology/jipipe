package org.hkijena.acaq5.api.grouping;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmCategory;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmDeclaration;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmGraph;
import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.api.data.ACAQMutableSlotConfiguration;
import org.hkijena.acaq5.api.grouping.parameters.GraphNodeParameterReferenceAccessGroupList;
import org.hkijena.acaq5.api.grouping.parameters.GraphNodeParameters;
import org.hkijena.acaq5.api.grouping.parameters.NodeGroupContents;
import org.hkijena.acaq5.api.parameters.*;
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
     * @param declaration the declaration
     */
    public NodeGroup(ACAQAlgorithmDeclaration declaration) {
        super(declaration, new ACAQAlgorithmGraph());
        initializeContents();
    }

    private void initializeContents() {
        contents = new NodeGroupContents();
        contents.setWrappedGraph(getWrappedGraph());
        exportedParameters.setGraph(contents.getWrappedGraph());
        contents.setParent(this);
    }

    /**
     * Creates a copy
     * @param other the original
     */
    public NodeGroup(NodeGroup other) {
        super(other);
        this.exportedParameters = new GraphNodeParameters(other.exportedParameters);
        initializeContents();
    }

    /**
     * Initializes from an existing graph
     * @param graph algorithms to be added
     * @param autoCreateSlots automatically create input and output slots
     */
    public NodeGroup(ACAQAlgorithmGraph graph, boolean autoCreateSlots) {
        super(ACAQAlgorithmRegistry.getInstance().getDeclarationById("node-group"), new ACAQAlgorithmGraph());
        setWrappedGraph(graph);
        initializeContents();

        if(autoCreateSlots) {
            setPreventUpdateSlots(true);
            ACAQMutableSlotConfiguration inputSlotConfiguration = (ACAQMutableSlotConfiguration) getGroupInput().getSlotConfiguration();
            ACAQMutableSlotConfiguration outputSlotConfiguration = (ACAQMutableSlotConfiguration) getGroupOutput().getSlotConfiguration();
            BiMap<ACAQDataSlot, String> slotNames = HashBiMap.create();
            for (ACAQDataSlot slot : getWrappedGraph().getUnconnectedSlots()) {
                String uniqueName = StringUtils.makeUniqueString(slot.getName(), " ", slotNames::containsValue);
                if(slot.isInput()) {
                    inputSlotConfiguration.addSlot(uniqueName, slot.getDefinition(), false);
                }
                else if(slot.isOutput()) {
                    outputSlotConfiguration.addSlot(uniqueName, slot.getDefinition(), false);
                }
                slotNames.put(slot, uniqueName);
            }
            setPreventUpdateSlots(false);
            updateGroupSlots();

            for (Map.Entry<ACAQDataSlot, String> entry : slotNames.entrySet()) {
                ACAQDataSlot slot = entry.getKey();
                if(slot.isInput()) {
                    ACAQDataSlot source = getGroupInput().getOutputSlot("Output " + entry.getValue());
                    getWrappedGraph().connect(source, slot);
                }
                else if(slot.isOutput()) {
                    ACAQDataSlot target = getGroupOutput().getInputSlot(entry.getValue());
                    getWrappedGraph().connect(slot, target);
                }
            }
        }
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
    public Map<String, ACAQParameterAccess> getParameters() {
        ACAQParameterTree standardParameters = new ACAQParameterTree(this,
                ACAQParameterTree.IGNORE_CUSTOM | ACAQParameterTree.FORCE_REFLECTION);
        return standardParameters.getParameters();
    }

    @Override
    public Map<String, ACAQParameterCollection> getChildParameterCollections() {
        this.exportedParameters.setGraph(getWrappedGraph());
        Map<String, ACAQParameterCollection> result = new HashMap<>();
        result.put("exported", new GraphNodeParameterReferenceAccessGroupList(exportedParameters, getWrappedGraph().getParameterTree()));
        return result;
    }
}
