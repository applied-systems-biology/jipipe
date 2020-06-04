package org.hkijena.acaq5.ui.extensionbuilder.traiteditor.api;

import org.hkijena.acaq5.api.ACAQRunnerSubStatus;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmDeclaration;
import org.hkijena.acaq5.api.algorithm.ACAQGraphNode;
import org.hkijena.acaq5.api.data.ACAQData;
import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.api.data.ACAQMutableSlotConfiguration;
import org.hkijena.acaq5.api.data.ACAQSlotConfiguration;
import org.hkijena.acaq5.api.events.AlgorithmSlotsChangedEvent;
import org.hkijena.acaq5.api.traits.ACAQTraitDeclaration;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Structural node that encapsulates an {@link ACAQTraitDeclaration}
 */
public class ACAQTraitNode extends ACAQGraphNode {

    private ACAQTraitDeclaration traitDeclaration;

    /**
     * @param declaration algorithm declaration
     */
    public ACAQTraitNode(ACAQAlgorithmDeclaration declaration) {
        super(declaration, createSlotConfiguration());
    }

    /**
     * Copies the node
     *
     * @param other original node
     */
    public ACAQTraitNode(ACAQTraitNode other) {
        super(other);
    }

    @Override
    public void run(ACAQRunnerSubStatus subProgress, Consumer<ACAQRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {

    }

    @Override
    public void reportValidity(ACAQValidityReport report) {

    }

    public ACAQTraitDeclaration getTraitDeclaration() {
        return traitDeclaration;
    }

    public void setTraitDeclaration(ACAQTraitDeclaration traitDeclaration) {
        this.traitDeclaration = traitDeclaration;
        setCustomName(traitDeclaration.getName());
        updateSlotTypes();
    }

    /**
     * Updates the slot data types depending on if the trait is a discriminator
     */
    public void updateSlotTypes() {
        ACAQMutableSlotConfiguration slotConfiguration = (ACAQMutableSlotConfiguration) getSlotConfiguration();
        slotConfiguration.getAllowedInputSlotTypes().clear();
        slotConfiguration.getAllowedOutputSlotTypes().clear();
        Class<? extends ACAQData> slotClass = getTraitDeclaration().isDiscriminator() ? ACAQDiscriminatorNodeInheritanceData.class : ACAQTraitNodeInheritanceData.class;
        slotConfiguration.getAllowedInputSlotTypes().add(slotClass);
        slotConfiguration.getAllowedOutputSlotTypes().add(slotClass);
        for (ACAQDataSlot value : getSlots().values()) {
            value.setAcceptedDataType(slotClass);
        }
        getEventBus().post(new AlgorithmSlotsChangedEvent(this));
    }

    /**
     * @return The slot configuration
     */
    public static ACAQSlotConfiguration createSlotConfiguration() {
        return ACAQMutableSlotConfiguration.builder()
                .restrictInputTo(ACAQTraitNodeInheritanceData.class, ACAQDiscriminatorNodeInheritanceData.class)
                .restrictOutputTo(ACAQTraitNodeInheritanceData.class, ACAQDiscriminatorNodeInheritanceData.class)
                .addOutputSlot("This", ACAQTraitNodeInheritanceData.class, "")
                .sealOutput()
                .build();
    }
}
