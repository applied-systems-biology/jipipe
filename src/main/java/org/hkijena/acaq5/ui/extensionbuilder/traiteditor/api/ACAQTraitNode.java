package org.hkijena.acaq5.ui.extensionbuilder.traiteditor.api;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithm;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmDeclaration;
import org.hkijena.acaq5.api.algorithm.AlgorithmInputSlot;
import org.hkijena.acaq5.api.algorithm.AlgorithmOutputSlot;
import org.hkijena.acaq5.api.data.ACAQMutableSlotConfiguration;
import org.hkijena.acaq5.api.data.ACAQSlotConfiguration;
import org.hkijena.acaq5.api.traits.ACAQTraitDeclaration;

@ACAQDocumentation(name = "Annotation", description = "An annotation")
@AlgorithmInputSlot(ACAQTraitNodeInheritanceData.class)
@AlgorithmOutputSlot(ACAQTraitNodeInheritanceData.class)
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
    }

    public static ACAQSlotConfiguration createSlotConfiguration() {
        return ACAQMutableSlotConfiguration.builder()
                .restrictInputTo(ACAQTraitNodeInheritanceData.class)
                .restrictOutputTo(ACAQTraitNodeInheritanceData.class)
                .addOutputSlot("This", "", ACAQTraitNodeInheritanceData.class)
                .sealOutput()
                .build();
    }
}
