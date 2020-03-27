package org.hkijena.acaq5.ui.extensionbuilder.traiteditor.api;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.algorithm.*;
import org.hkijena.acaq5.api.parameters.ACAQCustomParameterHolder;
import org.hkijena.acaq5.api.parameters.ACAQParameterAccess;

import java.util.Collections;
import java.util.Map;

@ACAQDocumentation(name = "Imported Annotation", description = "An annotation type that was imported from another extension")
@AlgorithmInputSlot(ACAQTraitNodeInheritanceData.class)
@AlgorithmOutputSlot(ACAQTraitNodeInheritanceData.class)
@AlgorithmMetadata(category = ACAQAlgorithmCategory.Internal)
public class ACAQExistingTraitNode extends ACAQTraitNode implements ACAQCustomParameterHolder {

    public ACAQExistingTraitNode(ACAQAlgorithmDeclaration declaration) {
        super(declaration);
    }

    public ACAQExistingTraitNode(ACAQExistingTraitNode other) {
        super(other);
    }

    @Override
    public Map<String, ACAQParameterAccess> getCustomParameters() {
        return Collections.emptyMap();
    }
}
