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

package org.hkijena.jipipe.api.compartments.algorithms;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.algorithm.JIPipeNodeCategory;
import org.hkijena.jipipe.api.algorithm.JIPipeNodeInfo;
import org.hkijena.jipipe.api.algorithm.JIPipeGraphNode;

/**
 * A graph compartment output
 * Transfers data 1:1 from input to output
 */
@JIPipeDocumentation(name = "Compartment output", description = "Output of a compartment")
@JIPipeOrganization(algorithmCategory = JIPipeNodeCategory.Internal)
public class JIPipeCompartmentOutput extends IOInterfaceAlgorithm {

    /**
     * Creates a new instance.
     * Please do not use this constructor manually, but instead use {@link JIPipeGraphNode}'s newInstance() method
     *
     * @param info The algorithm info
     */
    public JIPipeCompartmentOutput(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Creates a copy of the other algorithm
     *
     * @param other The original
     */
    public JIPipeCompartmentOutput(JIPipeCompartmentOutput other) {
        super(other);
    }
}
