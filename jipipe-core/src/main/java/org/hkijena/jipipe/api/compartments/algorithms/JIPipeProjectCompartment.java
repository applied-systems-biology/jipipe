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
import org.hkijena.jipipe.api.JIPipeProject;
import org.hkijena.jipipe.api.JIPipeRunnerSubStatus;
import org.hkijena.jipipe.api.JIPipeValidityReport;
import org.hkijena.jipipe.api.algorithm.JIPipeInputSlot;
import org.hkijena.jipipe.api.algorithm.JIPipeOutputSlot;
import org.hkijena.jipipe.api.algorithm.JIPipeAlgorithmDeclaration;
import org.hkijena.jipipe.api.algorithm.JIPipeGraphNode;
import org.hkijena.jipipe.api.compartments.datatypes.JIPipeCompartmentOutputData;
import org.hkijena.jipipe.api.data.JIPipeDefaultMutableSlotConfiguration;
import org.hkijena.jipipe.api.data.JIPipeSlotConfiguration;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * A project compartment.
 * Its node functionality is structural.
 */
@JIPipeDocumentation(name = "Graph compartment", description = "A compartment in the analysis graph")
@JIPipeInputSlot(JIPipeCompartmentOutputData.class)
@JIPipeOutputSlot(JIPipeCompartmentOutputData.class)
public class JIPipeProjectCompartment extends JIPipeGraphNode {

    private JIPipeProject project;
    private JIPipeCompartmentOutput outputNode;

    /**
     * Creates new instance
     *
     * @param declaration Algorithm declaration
     */
    public JIPipeProjectCompartment(JIPipeAlgorithmDeclaration declaration) {
        super(declaration, createSlotConfiguration());
    }

    /**
     * Copies the compartment
     *
     * @param other Original compartment
     */
    public JIPipeProjectCompartment(JIPipeProjectCompartment other) {
        super(other);
    }

    /**
     * @return The compartment ID
     */
    public String getProjectCompartmentId() {
        return getIdInGraph();
    }

    /**
     * @return If the compartment is initialized
     */
    public boolean isInitialized() {
        return project != null && outputNode != null;
    }

    @Override
    @JIPipeParameter("jipipe:node:name")
    public void setCustomName(String customName) {
        super.setCustomName(customName);
        if (outputNode != null) {
            outputNode.setCustomName(getName() + " output");
        }
    }

    @Override
    public void run(JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {

    }

    @Override
    public void reportValidity(JIPipeValidityReport report) {

    }

    /**
     * @return The project
     */
    public JIPipeProject getProject() {
        return project;
    }

    /**
     * Sets the project. Internal use only.
     *
     * @param project The project
     */
    public void setProject(JIPipeProject project) {
        this.project = project;
    }

    /**
     * @return The compartment output
     */
    public JIPipeCompartmentOutput getOutputNode() {
        return outputNode;
    }

    /**
     * Sets the compartment output. Internal use only.
     *
     * @param outputNode the compartment output
     */
    public void setOutputNode(JIPipeCompartmentOutput outputNode) {
        this.outputNode = outputNode;
    }

    /**
     * @return Slot configuration for {@link JIPipeProjectCompartment}
     */
    public static JIPipeSlotConfiguration createSlotConfiguration() {
        return JIPipeDefaultMutableSlotConfiguration.builder()
                .restrictInputTo(JIPipeCompartmentOutputData.class)
                .restrictOutputTo(JIPipeCompartmentOutputData.class)
                .addOutputSlot("Output", JIPipeCompartmentOutputData.class, "")
                .sealOutput()
                .build();
    }
}
