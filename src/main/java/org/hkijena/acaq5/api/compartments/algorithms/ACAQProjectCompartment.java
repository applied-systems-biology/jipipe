package org.hkijena.acaq5.api.compartments.algorithms;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQProject;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithm;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmDeclaration;
import org.hkijena.acaq5.api.algorithm.AlgorithmInputSlot;
import org.hkijena.acaq5.api.algorithm.AlgorithmOutputSlot;
import org.hkijena.acaq5.api.compartments.datatypes.ACAQCompartmentOutputData;
import org.hkijena.acaq5.api.data.ACAQMutableSlotConfiguration;
import org.hkijena.acaq5.api.data.ACAQSlotConfiguration;
import org.hkijena.acaq5.api.parameters.ACAQParameter;

/**
 * A project compartment.
 * Its node functionality is structural.
 */
@ACAQDocumentation(name = "Graph compartment", description = "A compartment in the analysis graph")
@AlgorithmInputSlot(ACAQCompartmentOutputData.class)
@AlgorithmOutputSlot(ACAQCompartmentOutputData.class)
public class ACAQProjectCompartment extends ACAQAlgorithm {

    private ACAQProject project;
    private ACAQCompartmentOutput outputNode;

    /**
     * Creates new instance
     *
     * @param declaration Algorithm declaration
     */
    public ACAQProjectCompartment(ACAQAlgorithmDeclaration declaration) {
        super(declaration, createSlotConfiguration());
    }

    /**
     * Copies the compartment
     *
     * @param other Original compartment
     */
    public ACAQProjectCompartment(ACAQProjectCompartment other) {
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
    @ACAQParameter("name")
    public void setCustomName(String customName) {
        super.setCustomName(customName);
        if (outputNode != null) {
            outputNode.setCustomName(getName() + " output");
        }
    }

    @Override
    public void run() {

    }

    @Override
    public void reportValidity(ACAQValidityReport report) {

    }

    /**
     * @return The project
     */
    public ACAQProject getProject() {
        return project;
    }

    /**
     * Sets the project. Internal use only.
     *
     * @param project The project
     */
    public void setProject(ACAQProject project) {
        this.project = project;
    }

    /**
     * @return The compartment output
     */
    public ACAQCompartmentOutput getOutputNode() {
        return outputNode;
    }

    /**
     * Sets the compartment output. Internal use only.
     *
     * @param outputNode the compartment output
     */
    public void setOutputNode(ACAQCompartmentOutput outputNode) {
        this.outputNode = outputNode;
    }

    /**
     * @return Slot configuration for {@link ACAQProjectCompartment}
     */
    public static ACAQSlotConfiguration createSlotConfiguration() {
        return ACAQMutableSlotConfiguration.builder()
                .restrictInputTo(ACAQCompartmentOutputData.class)
                .restrictOutputTo(ACAQCompartmentOutputData.class)
                .addOutputSlot("Output", "", ACAQCompartmentOutputData.class)
                .sealOutput()
                .build();
    }
}
