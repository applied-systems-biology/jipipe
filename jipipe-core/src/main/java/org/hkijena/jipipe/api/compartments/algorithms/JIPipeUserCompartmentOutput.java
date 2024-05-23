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

package org.hkijena.jipipe.api.compartments.algorithms;

import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.categories.MiscellaneousNodeTypeCategory;

/**
 * A graph compartment output that can be created by the user
 * Transfers data 1:1 from input to output
 */
@SetJIPipeDocumentation(name = "Compartment output", description = "Passes all inputs to all compartments connected to the current compartment. " +
        "Has the same functionality as the standard compartment output that is present in all compartments. " +
        "Acts as additional output node. " +
        "Please note that the compartments view does currently not recognize additional outputs.")
@ConfigureJIPipeNode(nodeTypeCategory = MiscellaneousNodeTypeCategory.class)
public class JIPipeUserCompartmentOutput extends JIPipeCompartmentOutput {

    /**
     * Creates a new instance.
     * Please do not use this constructor manually, but instead use {@link JIPipeGraphNode}'s newInstance() method
     *
     * @param info The algorithm info
     */
    public JIPipeUserCompartmentOutput(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Creates a copy of the other algorithm
     *
     * @param other The original
     */
    public JIPipeUserCompartmentOutput(JIPipeUserCompartmentOutput other) {
        super(other);
    }
}
