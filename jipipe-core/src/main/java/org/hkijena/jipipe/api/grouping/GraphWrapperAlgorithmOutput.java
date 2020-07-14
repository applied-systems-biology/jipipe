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

package org.hkijena.jipipe.api.grouping;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.algorithm.JIPipeNodeCategory;
import org.hkijena.jipipe.api.algorithm.JIPipeNodeInfo;
import org.hkijena.jipipe.api.compartments.algorithms.IOInterfaceAlgorithm;

@JIPipeDocumentation(name = "Group output", description = "Acts as output of a node group.")
@JIPipeOrganization(algorithmCategory = JIPipeNodeCategory.Internal)
public class GraphWrapperAlgorithmOutput extends IOInterfaceAlgorithm {
    public GraphWrapperAlgorithmOutput(JIPipeNodeInfo info) {
        super(info);
    }

    public GraphWrapperAlgorithmOutput(GraphWrapperAlgorithmOutput other) {
        super(other);
    }

    @Override
    public boolean renderOutputSlots() {
        return false;
    }
}
