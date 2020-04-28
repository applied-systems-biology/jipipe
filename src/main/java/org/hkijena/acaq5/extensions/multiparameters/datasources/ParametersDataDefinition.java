package org.hkijena.acaq5.extensions.multiparameters.datasources;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;
import org.hkijena.acaq5.api.ACAQRunnerSubStatus;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.*;
import org.hkijena.acaq5.api.events.ParameterStructureChangedEvent;
import org.hkijena.acaq5.api.parameters.*;
import org.hkijena.acaq5.extensions.multiparameters.datatypes.ParametersData;

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Generates {@link org.hkijena.acaq5.extensions.multiparameters.datatypes.ParametersData} objects
 */
@ACAQDocumentation(name = "Define parameter", description = "Defines an algorithm parameter that can be consumed by a multi-parameter algorithm")
@AlgorithmOutputSlot(value = ParametersData.class, slotName = "Parameters", autoCreate = true)
@ACAQOrganization(algorithmCategory = ACAQAlgorithmCategory.DataSource)
public class ParametersDataDefinition extends ACAQAlgorithm {

    private ACAQAlgorithm algorithmInstance;

    /**
     * Creates a new instance
     *
     * @param declaration the algorithm declaration
     */
    public ParametersDataDefinition(ACAQAlgorithmDeclaration declaration) {
        super(declaration);
    }

    /**
     * Copies the algorithm
     *
     * @param other the original
     */
    public ParametersDataDefinition(ParametersDataDefinition other) {
        super(other);
        this.algorithmInstance = other.algorithmInstance;
    }

    @Override
    public void run(ACAQRunnerSubStatus subProgress, Consumer<ACAQRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        ParametersData result = new ParametersData();
        for (Map.Entry<String, ACAQParameterAccess> entry : ACAQTraversedParameterCollection.getParameters(algorithmInstance).entrySet()) {
            if (entry.getValue().getVisibility().isVisibleIn(ACAQParameterVisibility.TransitiveVisible)) {
                result.getParameterData().put(entry.getKey(), entry.getValue().get());
            }
        }
        getFirstOutputSlot().addData(result);
    }

    @Override
    public void reportValidity(ACAQValidityReport report) {
        if (algorithmInstance == null) {
            report.reportIsInvalid("No algorithm selected!",
                    "Parameters are defined based on an algorithm.",
                    "Please select an algorithm.");
        } else {
            report.report(algorithmInstance);
        }
    }

    @ACAQParameter("algorithm-type")
    @ACAQDocumentation(name = "Algorithm", description = "The algorithm the parameters are created for")
    public ACAQAlgorithmDeclarationRef getAlgorithmDeclaration() {
        if (algorithmInstance != null) {
            return new ACAQAlgorithmDeclarationRef(algorithmInstance.getDeclaration());
        } else {
            return new ACAQAlgorithmDeclarationRef();
        }
    }

    @ACAQParameter("algorithm-type")
    public void setAlgorithmDeclaration(ACAQAlgorithmDeclarationRef algorithmDeclaration) {
        if (algorithmDeclaration == null || algorithmDeclaration.getDeclaration() == null) {
            algorithmInstance = null;
        } else {
            algorithmInstance = algorithmDeclaration.getDeclaration().newInstance();
        }
        getEventBus().post(new ParameterStructureChangedEvent(this));
    }

    @ACAQParameter("algorithm-parameters")
    @ACAQDocumentation(name = "Algorithm parameters", description = "Contains the parameters of the selected algorithm")
    public ACAQParameterCollection getAlgorithmParameters() {
        return algorithmInstance;
    }
}
