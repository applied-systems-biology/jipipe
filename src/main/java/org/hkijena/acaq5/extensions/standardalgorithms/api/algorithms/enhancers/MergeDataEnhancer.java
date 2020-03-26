package org.hkijena.acaq5.extensions.standardalgorithms.api.algorithms.enhancers;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.*;
import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.api.data.ACAQMutableSlotConfiguration;
import org.hkijena.acaq5.api.data.traits.ACAQDefaultMutableTraitConfiguration;
import org.hkijena.acaq5.api.data.traits.ACAQTraitModificationOperation;
import org.hkijena.acaq5.api.events.ParameterChangedEvent;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.api.traits.ACAQDiscriminator;
import org.hkijena.acaq5.api.traits.ACAQTrait;

@ACAQDocumentation(name = "Merge slots", description = "Merges the input slot tables into one data slot")
@ACAQOrganization(menuPath = "Merge")
@AlgorithmMetadata(category = ACAQAlgorithmCategory.Enhancer)
public class MergeDataEnhancer extends ACAQAlgorithm {

    public MergeDataEnhancer(ACAQAlgorithmDeclaration declaration) {
        super(declaration, ACAQMutableSlotConfiguration.builder()
                            .restrictOutputSlotCount(1)
                            .build());
    }

    public MergeDataEnhancer(MergeDataEnhancer other) {
        super(other);
    }

    @Override
    public void run() {
        ACAQDataSlot outputSlot = getFirstOutputSlot();
        for (ACAQDataSlot inputSlot : getInputSlots()) {
            outputSlot.copyFrom(inputSlot);
        }
    }

    @Override
    public void reportValidity(ACAQValidityReport report) {
        if(getOutputSlots().isEmpty()) {
            report.reportIsInvalid("No output slot! Please add an output slot that is compatible to the input data.");
        }
        else {
            ACAQDataSlot outputSlot = getFirstOutputSlot();
            for (ACAQDataSlot inputSlot : getInputSlots()) {
                if(!outputSlot.getAcceptedDataType().isAssignableFrom(inputSlot.getAcceptedDataType())) {
                    report.forCategory("Slots").forCategory(inputSlot.getName())
                            .reportIsInvalid("Input slot is incompatible! Please add an output slot that is compatible to the input data.");
                }
            }
        }
    }
}
