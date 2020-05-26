package org.hkijena.acaq5.api.compartments.algorithms;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithm;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmCategory;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmDeclaration;

/**
 * A graph compartment output
 * Transfers data 1:1 from input to output
 */
@ACAQDocumentation(name = "Compartment output", description = "Output of a compartment")
@ACAQOrganization(algorithmCategory = ACAQAlgorithmCategory.Internal)
public class ACAQCompartmentOutput extends IOInterfaceAlgorithm {

    /**
     * Creates a new instance.
     * Please do not use this constructor manually, but instead use {@link ACAQAlgorithm}'s newInstance() method
     *
     * @param declaration The algorithm declaration
     */
    public ACAQCompartmentOutput(ACAQAlgorithmDeclaration declaration) {
        super(declaration);
    }

    /**
     * Creates a copy of the other algorithm
     *
     * @param other The original
     */
    public ACAQCompartmentOutput(ACAQCompartmentOutput other) {
        super(other);
    }
}
