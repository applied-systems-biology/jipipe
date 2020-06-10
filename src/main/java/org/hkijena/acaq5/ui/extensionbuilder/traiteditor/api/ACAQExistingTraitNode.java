package org.hkijena.acaq5.ui.extensionbuilder.traiteditor.api;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmDeclaration;
import org.hkijena.acaq5.api.algorithm.AlgorithmInputSlot;
import org.hkijena.acaq5.api.algorithm.AlgorithmOutputSlot;
import org.hkijena.acaq5.api.data.ACAQMutableSlotConfiguration;
import org.hkijena.acaq5.api.parameters.ACAQCustomParameterCollection;
import org.hkijena.acaq5.api.parameters.ACAQParameterAccess;

import java.util.Collections;
import java.util.Map;

/**
 * Structural node that represents an existing {@link org.hkijena.acaq5.api.traits.ACAQTrait}
 */
@ACAQDocumentation(name = "Imported Annotation", description = "An annotation type that was imported from another extension")
@AlgorithmInputSlot(ACAQTraitNodeInheritanceData.class)
@AlgorithmOutputSlot(ACAQTraitNodeInheritanceData.class)
public class ACAQExistingTraitNode extends ACAQTraitNode implements ACAQCustomParameterCollection {

    /**
     * Creates a new instance
     *
     * @param declaration The algorithm declaration
     */
    public ACAQExistingTraitNode(ACAQAlgorithmDeclaration declaration) {
        super(declaration);
        ACAQMutableSlotConfiguration slotConfiguration = (ACAQMutableSlotConfiguration) getSlotConfiguration();
        slotConfiguration.setInputSealed(true);
    }

    /**
     * Copies the instance
     *
     * @param other The original
     */
    public ACAQExistingTraitNode(ACAQExistingTraitNode other) {
        super(other);
    }

    @Override
    public Map<String, ACAQParameterAccess> getParameters() {
        return Collections.emptyMap();
    }
}
