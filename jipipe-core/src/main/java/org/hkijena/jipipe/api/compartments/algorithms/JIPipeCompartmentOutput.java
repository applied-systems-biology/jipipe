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
import org.hkijena.jipipe.api.parameters.JIPipeParameter;

/**
 * A graph compartment output
 * Transfers data 1:1 from input to output
 */
@SetJIPipeDocumentation(name = "Compartment output", description = "Output of a compartment")
@ConfigureJIPipeNode()
public class JIPipeCompartmentOutput extends IOInterfaceAlgorithm {

    private String outputSlotName;

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
        this.outputSlotName = other.outputSlotName;
    }

    @JIPipeParameter(value = "jipipe:compartment:output-slot-name", hidden = true)
    public String getOutputSlotName() {
        return outputSlotName;
    }

    @JIPipeParameter("jipipe:compartment:output-slot-name")
    public void setOutputSlotName(String outputSlotName) {
        this.outputSlotName = outputSlotName;
    }
}
