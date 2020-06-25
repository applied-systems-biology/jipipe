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
