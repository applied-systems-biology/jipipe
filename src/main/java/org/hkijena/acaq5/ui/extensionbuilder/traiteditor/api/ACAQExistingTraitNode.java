package org.hkijena.acaq5.ui.extensionbuilder.traiteditor.api;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithm;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmDeclaration;
import org.hkijena.acaq5.api.algorithm.AlgorithmInputSlot;
import org.hkijena.acaq5.api.algorithm.AlgorithmOutputSlot;
import org.hkijena.acaq5.api.data.ACAQMutableSlotConfiguration;
import org.hkijena.acaq5.api.data.ACAQSlotConfiguration;

@ACAQDocumentation(name = "Imported Annotation", description = "An annotation type that was imported from another extension")
@AlgorithmInputSlot(ACAQTraitNodeInheritanceData.class)
@AlgorithmOutputSlot(ACAQTraitNodeInheritanceData.class)
public class ACAQExistingTraitNode extends ACAQTraitNode {

    public ACAQExistingTraitNode(ACAQAlgorithmDeclaration declaration) {
        super(declaration);
    }

    public ACAQExistingTraitNode(ACAQExistingTraitNode other) {
        super(other);
    }
}
