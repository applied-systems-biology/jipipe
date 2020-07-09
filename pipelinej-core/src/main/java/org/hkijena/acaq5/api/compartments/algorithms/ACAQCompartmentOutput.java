/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.acaq5.api.compartments.algorithms;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmCategory;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmDeclaration;
import org.hkijena.acaq5.api.algorithm.ACAQGraphNode;

/**
 * A graph compartment output
 * Transfers data 1:1 from input to output
 */
@ACAQDocumentation(name = "Compartment output", description = "Output of a compartment")
@ACAQOrganization(algorithmCategory = ACAQAlgorithmCategory.Internal)
public class ACAQCompartmentOutput extends IOInterfaceAlgorithm {

    /**
     * Creates a new instance.
     * Please do not use this constructor manually, but instead use {@link ACAQGraphNode}'s newInstance() method
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
