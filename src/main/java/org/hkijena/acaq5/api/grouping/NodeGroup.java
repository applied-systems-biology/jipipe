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
import org.hkijena.acaq5.api.grouping.parameters.NodeGroupContents;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.api.registries.ACAQAlgorithmRegistry;
import org.hkijena.acaq5.utils.StringUtils;

import java.util.Map;

/**
 * A sub-graph algorithm that can be defined by a user
 */
@ACAQDocumentation(name = "Group", description = "A sub-graph that contains its own pipeline.")
@ACAQOrganization(algorithmCategory = ACAQAlgorithmCategory.Miscellaneous)
public class NodeGroup extends GraphWrapperAlgorithm {

    private NodeGroupContents contents;

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
        contents.setParent(this);
    }

    /**
     * Creates a copy
     * @param other the original
     */
    public NodeGroup(GraphWrapperAlgorithm other) {
        super(other);
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
    }

}
