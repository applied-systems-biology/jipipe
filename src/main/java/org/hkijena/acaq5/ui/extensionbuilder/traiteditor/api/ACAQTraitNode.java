package org.hkijena.acaq5.ui.extensionbuilder.traiteditor.api;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithm;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmDeclaration;
import org.hkijena.acaq5.api.algorithm.AlgorithmInputSlot;
import org.hkijena.acaq5.api.algorithm.AlgorithmOutputSlot;
import org.hkijena.acaq5.api.data.ACAQData;
import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.api.data.ACAQMutableSlotConfiguration;
import org.hkijena.acaq5.api.data.ACAQSlotConfiguration;
import org.hkijena.acaq5.api.events.AlgorithmSlotsChangedEvent;
import org.hkijena.acaq5.api.traits.ACAQTraitDeclaration;

public class ACAQTraitNode extends ACAQAlgorithm {

    private ACAQTraitDeclaration traitDeclaration;

    public ACAQTraitNode(ACAQAlgorithmDeclaration declaration) {
        super(declaration, createSlotConfiguration());
    }

    public ACAQTraitNode(ACAQTraitNode other) {
        super(other);
    }

    @Override
    public void run() {

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

    public void updateSlotTypes() {
        ACAQMutableSlotConfiguration slotConfiguration = (ACAQMutableSlotConfiguration)getSlotConfiguration();
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

    public static ACAQSlotConfiguration createSlotConfiguration() {
        return ACAQMutableSlotConfiguration.builder()
                .restrictInputTo(ACAQTraitNodeInheritanceData.class, ACAQDiscriminatorNodeInheritanceData.class)
                .restrictOutputTo(ACAQTraitNodeInheritanceData.class, ACAQDiscriminatorNodeInheritanceData.class)
                .addOutputSlot("This", "", ACAQTraitNodeInheritanceData.class)
                .sealOutput()
                .build();
    }
}
