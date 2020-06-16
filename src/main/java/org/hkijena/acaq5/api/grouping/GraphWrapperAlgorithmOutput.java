package org.hkijena.acaq5.api.grouping;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmCategory;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmDeclaration;
import org.hkijena.acaq5.api.compartments.algorithms.IOInterfaceAlgorithm;

@ACAQDocumentation(name = "Group output", description = "Acts as output of a node group.")
@ACAQOrganization(algorithmCategory = ACAQAlgorithmCategory.Internal)
public class GraphWrapperAlgorithmOutput extends IOInterfaceAlgorithm {
    public GraphWrapperAlgorithmOutput(ACAQAlgorithmDeclaration declaration) {
        super(declaration);
    }

    public GraphWrapperAlgorithmOutput(GraphWrapperAlgorithmOutput other) {
        super(other);
    }

    @Override
    public boolean renderOutputSlots() {
        return false;
    }
}
