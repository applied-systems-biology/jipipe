package org.hkijena.acaq5.ui.extensionbuilder.traiteditor.api;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.eventbus.Subscribe;
import org.hkijena.acaq5.ACAQJsonExtension;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithm;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmGraph;
import org.hkijena.acaq5.api.data.ACAQData;
import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.api.data.ACAQMutableSlotConfiguration;
import org.hkijena.acaq5.api.data.ACAQSlotDefinition;
import org.hkijena.acaq5.api.events.AlgorithmGraphChangedEvent;
import org.hkijena.acaq5.api.events.ExtensionContentAddedEvent;
import org.hkijena.acaq5.api.events.ExtensionContentRemovedEvent;
import org.hkijena.acaq5.api.events.ParameterChangedEvent;
import org.hkijena.acaq5.api.traits.ACAQJsonTraitDeclaration;
import org.hkijena.acaq5.api.traits.ACAQTraitDeclaration;
import org.hkijena.acaq5.utils.StringUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ACAQTraitGraph extends ACAQAlgorithmGraph {
    boolean initialized = false;
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
            if (!StringUtils.isNullOrEmpty(declaration.getId())) {
                internalIds.add(declaration.getId());
            }
        }

        // First create inherited traits that are not part of the extension
        for (ACAQJsonTraitDeclaration declaration : extension.getTraitDeclarations()) {
            declaration.updatedInheritedDeclarations();
            for (ACAQTraitDeclaration inherited : declaration.getInherited()) {
                if (!nodeIdMap.containsKey(inherited.getId()) && !internalIds.contains(inherited.getId())) {
                    ACAQExistingTraitNode node = addExternalTrait(inherited);
                    nodeIdMap.put(inherited.getId(), node);
                }
            }
        }

        // Create all new nodes
        for (ACAQJsonTraitDeclaration declaration : extension.getTraitDeclarations()) {
            ACAQNewTraitNode node = addNewTraitNode(declaration);
            if (!StringUtils.isNullOrEmpty(declaration.getId()) && !nodeIdMap.containsKey(declaration.getId())) {
                nodeIdMap.put(declaration.getId(), node);
            }
        }

        // Add connections
        for (ACAQJsonTraitDeclaration declaration : extension.getTraitDeclarations()) {
            ACAQTraitNode node = traitNodes.get(declaration);
            ACAQMutableSlotConfiguration slotConfiguration = (ACAQMutableSlotConfiguration) node.getSlotConfiguration();
            int index = 1;
            for (String id : declaration.getInheritedIds()) {
                ACAQTraitNode sourceAlgorithm = nodeIdMap.getOrDefault(id, null);
                if (sourceAlgorithm == null)
                    continue;
                ACAQDataSlot source = sourceAlgorithm.getFirstOutputSlot();
                String targetSlotName = "Input " + (index++);
                Class<? extends ACAQData> slotClass = declaration.isDiscriminator() ? ACAQDiscriminatorNodeInheritanceData.class : ACAQTraitNodeInheritanceData.class;
                slotConfiguration.addSlot(targetSlotName, new ACAQSlotDefinition(slotClass,
                        ACAQDataSlot.SlotType.Input,
                        targetSlotName,
                        null));
                ACAQDataSlot target = node.getInputSlot(targetSlotName);
                connect(source, target);
            }
        }

        this.initialized = true;
    }

    @Override
    public void insertNode(String key, ACAQAlgorithm algorithm, String compartment) {
        super.insertNode(key, algorithm, compartment);
        if (initialized && algorithm instanceof ACAQNewTraitNode) {
            ACAQNewTraitNode traitNode = (ACAQNewTraitNode) algorithm;
            if (traitNode.getTraitDeclaration() == null) {
                ACAQJsonTraitDeclaration traitDeclaration = new ACAQJsonTraitDeclaration();
                traitNode.setTraitDeclaration(traitDeclaration);
                traitNodes.put(traitDeclaration, traitNode);
                extension.addTrait(traitDeclaration);
            }
        }
    }

    @Override
    public void removeNode(ACAQAlgorithm algorithm) {
        super.removeNode(algorithm);
        if (algorithm instanceof ACAQNewTraitNode) {
            // Workaround for registration bug
            algorithm.getEventBus().register(this);
            algorithm.getEventBus().unregister(this);
            ACAQJsonTraitDeclaration declaration = (ACAQJsonTraitDeclaration) ((ACAQNewTraitNode) algorithm).getTraitDeclaration();
            if (extension.getTraitDeclarations().contains(declaration)) {
                extension.removeAnnotation(declaration);
            }
        }
    }

    @Override
    public void connect(ACAQDataSlot source, ACAQDataSlot target, boolean userDisconnectable) {
        super.connect(source, target, userDisconnectable);
        updateInheritances();

        // Helper functions to find IDs
        if (target.getAlgorithm() instanceof ACAQNewTraitNode) {
            ACAQTraitNode sourceNode = (ACAQTraitNode) source.getAlgorithm();
            ACAQNewTraitNode traitNode = (ACAQNewTraitNode) target.getAlgorithm();
            if (StringUtils.isNullOrEmpty(traitNode.getTraitDeclaration().getId()) && traitNode.getInputSlots().size() == 1) {
                if (!StringUtils.isNullOrEmpty(sourceNode.getTraitDeclaration().getId())) {
                    ACAQJsonTraitDeclaration declaration = (ACAQJsonTraitDeclaration) traitNode.getTraitDeclaration();
                    declaration.setId(sourceNode.getTraitDeclaration().getId() + "-");
                }
            }
        }
    }

    @Override
    public boolean disconnect(ACAQDataSlot source, ACAQDataSlot target, boolean user) {
        if (super.disconnect(source, target, user)) {
            updateInheritances();
            return true;
        }
        return false;
    }

    public void updateInheritances() {
        if (!initialized)
            return;

        for (Map.Entry<ACAQTraitDeclaration, ACAQTraitNode> entry : traitNodes.entrySet()) {
            if (!containsNode(entry.getValue()))
                continue;
            if (entry.getKey() instanceof ACAQJsonTraitDeclaration) {
                ACAQJsonTraitDeclaration declaration = (ACAQJsonTraitDeclaration) entry.getKey();
                declaration.getInheritedIds().clear();
                for (ACAQDataSlot target : entry.getValue().getInputSlots()) {
                    ACAQDataSlot source = getSourceSlot(target);
                    if (source != null) {
                        ACAQTraitNode sourceNode = (ACAQTraitNode) source.getAlgorithm();
                        String id = sourceNode.getTraitDeclaration().getId();
                        if (!StringUtils.isNullOrEmpty(id)) {
                            declaration.getInheritedIds().add(id);
                        }
                    }
                }
            }
        }

    }

    private ACAQNewTraitNode addNewTraitNode(ACAQJsonTraitDeclaration declaration) {
        ACAQNewTraitNode node = ACAQAlgorithm.newInstance("acaq:new-trait-node");
        node.setTraitDeclaration(declaration);
        traitNodes.put(declaration, node);
        insertNode(node, COMPARTMENT_DEFAULT);
        node.getEventBus().register(this);
        return node;
    }

    @Subscribe
    public void onTraitAddedEvent(ExtensionContentAddedEvent event) {
        if (event.getContent() instanceof ACAQJsonTraitDeclaration) {
            ACAQJsonTraitDeclaration declaration = (ACAQJsonTraitDeclaration) event.getContent();
            if (!traitNodes.containsKey(declaration))
                addNewTraitNode(declaration);
        }
    }

    @Subscribe
    public void onTraitRemovedEvent(ExtensionContentRemovedEvent event) {
        if (event.getContent() instanceof ACAQJsonTraitDeclaration) {
            ACAQJsonTraitDeclaration declaration = (ACAQJsonTraitDeclaration) event.getContent();
            ACAQTraitNode node = traitNodes.getOrDefault(declaration, null);
            if (node != null && containsNode(node)) {
                removeNode(node);
            }
        }
    }

    @Subscribe
    public void onTraitIdChanged(ParameterChangedEvent event) {
        if ("id".equals(event.getKey())) {
            updateInheritances();
        } else if ("is-discriminator".equals(event.getKey())) {
            getEventBus().post(new AlgorithmGraphChangedEvent(this));
        }
    }

    public boolean containsTrait(ACAQTraitDeclaration declaration) {
        return traitNodes.containsKey(declaration);
    }

    public ACAQExistingTraitNode addExternalTrait(ACAQTraitDeclaration declaration) {
        ACAQExistingTraitNode node = ACAQAlgorithm.newInstance("acaq:existing-trait-node");
        node.setTraitDeclaration(declaration);
        insertNode(node, COMPARTMENT_DEFAULT);
        traitNodes.put(declaration, node);
        return node;
    }

    public ACAQTraitNode getNodeFor(ACAQTraitDeclaration declaration) {
        return traitNodes.getOrDefault(declaration, null);
    }
}
