package org.hkijena.acaq5.ui.extensionbuilder.traiteditor.api;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.eventbus.Subscribe;
import org.hkijena.acaq5.ACAQJsonExtension;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithm;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmGraph;
import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.api.data.ACAQMutableSlotConfiguration;
import org.hkijena.acaq5.api.data.ACAQSlotDefinition;
import org.hkijena.acaq5.api.events.ExtensionContentAddedEvent;
import org.hkijena.acaq5.api.events.ExtensionContentRemovedEvent;
import org.hkijena.acaq5.api.traits.ACAQJsonTraitDeclaration;
import org.hkijena.acaq5.api.traits.ACAQTraitDeclaration;
import org.hkijena.acaq5.utils.StringUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ACAQTraitGraph extends ACAQAlgorithmGraph {
    private ACAQJsonExtension extension;
    private BiMap<ACAQTraitDeclaration, ACAQTraitNode> traitNodes = HashBiMap.create();

    public ACAQTraitGraph(ACAQJsonExtension extension) {
        this.extension = extension;
        initialize();
        extension.getEventBus().register(this);
    }

    private void initialize() {
        Map<String, ACAQTraitNode> nodeIdMap = new HashMap<>();

        Set<String> internalIds = new HashSet<>();
        for (ACAQJsonTraitDeclaration declaration : extension.getTraitDeclarations()) {
            if(!StringUtils.isNullOrEmpty(declaration.getId())) {
                internalIds.add(declaration.getId());
            }
        }

        // First create inherited traits that are not part of the extension
        for (ACAQJsonTraitDeclaration declaration : extension.getTraitDeclarations()) {
            declaration.updatedInheritedDeclarations();
            for (ACAQTraitDeclaration inherited : declaration.getInherited()) {
               if(!nodeIdMap.containsKey(inherited.getId()) && !internalIds.contains(inherited.getId())) {
                   ACAQExistingTraitNode node = ACAQAlgorithm.newInstance("acaq:existing-trait-node");
                   node.setTraitDeclaration(inherited);
                   insertNode(node, COMPARTMENT_DEFAULT);
                   nodeIdMap.put(inherited.getId(), node);
                   traitNodes.put(inherited, node);
               }
            }
        }

        // Create all new nodes
        for (ACAQJsonTraitDeclaration declaration : extension.getTraitDeclarations()) {
            ACAQNewTraitNode node = addNewTraitNode(declaration);
            if(!StringUtils.isNullOrEmpty(declaration.getId()) && !nodeIdMap.containsKey(declaration.getId())) {
                nodeIdMap.put(declaration.getId(), node);
            }
        }

        // Add connections
        for (ACAQJsonTraitDeclaration declaration : extension.getTraitDeclarations()) {
            ACAQTraitNode node = traitNodes.get(declaration);
            ACAQMutableSlotConfiguration slotConfiguration = (ACAQMutableSlotConfiguration)node.getSlotConfiguration();
            int index = 1;
            for (String id : declaration.getInheritedIds()) {
                ACAQTraitNode sourceAlgorithm = nodeIdMap.getOrDefault(id, null);
                if(sourceAlgorithm == null)
                    continue;
                ACAQDataSlot source = sourceAlgorithm.getFirstOutputSlot();
                String targetSlotName = "Output " + (index++);
                slotConfiguration.addSlot(targetSlotName, new ACAQSlotDefinition(ACAQTraitNodeInheritanceData.class,
                        ACAQDataSlot.SlotType.Output,
                        targetSlotName,
                        null));
                ACAQDataSlot target = node.getInputSlot(targetSlotName);
                connect(source, target);
            }
        }
    }

    private ACAQNewTraitNode addNewTraitNode(ACAQJsonTraitDeclaration declaration) {
        ACAQNewTraitNode node = ACAQAlgorithm.newInstance("acaq:new-trait-node");
        node.setTraitDeclaration(declaration);
        insertNode(node, COMPARTMENT_DEFAULT);
        traitNodes.put(declaration, node);
        return node;
    }

    @Subscribe
    public void onTraitAddedEvent(ExtensionContentAddedEvent event) {
        if(event.getContent() instanceof ACAQJsonTraitDeclaration) {
            ACAQJsonTraitDeclaration declaration = (ACAQJsonTraitDeclaration)event.getContent();
            addNewTraitNode(declaration);
        }
    }

    @Subscribe
    public void onTraitRemovedEvent(ExtensionContentRemovedEvent event) {
        if(event.getContent() instanceof ACAQJsonTraitDeclaration) {
            ACAQJsonTraitDeclaration declaration = (ACAQJsonTraitDeclaration)event.getContent();
            ACAQTraitNode node = traitNodes.getOrDefault(declaration, null);
            if(node != null) {
                removeNode(node);
            }
        }
    }
}
