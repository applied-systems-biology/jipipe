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

@ACAQDocumentation(name = "Graph compartment", description = "A compartment in the analysis graph")
@AlgorithmInputSlot(ACAQCompartmentOutputData.class)
@AlgorithmOutputSlot(ACAQCompartmentOutputData.class)
public class ACAQProjectCompartment extends ACAQAlgorithm {

    private ACAQProject project;
    private ACAQCompartmentOutput outputNode;

    public ACAQProjectCompartment(ACAQAlgorithmDeclaration declaration) {
        super(declaration, createSlotConfiguration());
    }

    public ACAQProjectCompartment(ACAQProjectCompartment other) {
        super(other);
    }

    public String getProjectCompartmentId() {
        return getIdInGraph();
    }

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

    public ACAQProject getProject() {
        return project;
    }

    public void setProject(ACAQProject project) {
        this.project = project;
    }

    public ACAQCompartmentOutput getOutputNode() {
        return outputNode;
    }

    public void setOutputNode(ACAQCompartmentOutput outputNode) {
        this.outputNode = outputNode;
    }

    public static ACAQSlotConfiguration createSlotConfiguration() {
        return ACAQMutableSlotConfiguration.builder()
                .restrictInputTo(ACAQCompartmentOutputData.class)
                .restrictOutputTo(ACAQCompartmentOutputData.class)
                .addOutputSlot("Output", "", ACAQCompartmentOutputData.class)
                .sealOutput()
                .build();
    }
}
