/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.api.grouping;

import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.compartments.algorithms.IOInterfaceAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.graph.GraphWrappersFunction;

@SetJIPipeDocumentation(name = "Group output", description = "It makes the final result of a node group available to the rest of the workflow. It takes that result from the grouped steps and passes it on without changing it.")
@ConfigureJIPipeNode(function = GraphWrappersFunction.class)
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
