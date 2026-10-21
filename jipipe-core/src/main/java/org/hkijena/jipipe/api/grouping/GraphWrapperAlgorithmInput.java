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
import org.hkijena.jipipe.api.nodes.taxonomy.frameworks.CoreFramework;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.graph.GraphWrappersFunction;

@SetJIPipeDocumentation(name = "Group input", description = "Marks the entry point of a grouped set of analysis steps. It takes no specific data type and produces no separate result; it simply lets the data you connect to the group enter unchanged for the steps inside to use.")
@ConfigureJIPipeNode(function = GraphWrappersFunction.class)
public class GraphWrapperAlgorithmInput extends IOInterfaceAlgorithm {

    public GraphWrapperAlgorithmInput(JIPipeNodeInfo info) {
        super(info);
    }

    public GraphWrapperAlgorithmInput(GraphWrapperAlgorithmInput other) {
        super(other);
    }

    @Override
    public boolean renderInputSlots() {
        return false;
    }
}
