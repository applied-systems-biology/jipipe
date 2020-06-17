package org.hkijena.acaq5.api.grouping;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmCategory;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmDeclaration;
import org.hkijena.acaq5.api.compartments.algorithms.IOInterfaceAlgorithm;

@ACAQDocumentation(name = "Group input", description = "Acts as input of a node group.")
@ACAQOrganization(algorithmCategory = ACAQAlgorithmCategory.Internal)
public class GraphWrapperAlgorithmInput extends IOInterfaceAlgorithm {

    public GraphWrapperAlgorithmInput(ACAQAlgorithmDeclaration declaration) {
        super(declaration);
    }

    public GraphWrapperAlgorithmInput(GraphWrapperAlgorithmInput other) {
        super(other);
    }

    @Override
    public boolean renderInputSlots() {
        return false;
    }
}
