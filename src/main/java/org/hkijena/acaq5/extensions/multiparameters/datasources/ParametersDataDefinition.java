package org.hkijena.acaq5.extensions.multiparameters.datasources;

import com.google.common.eventbus.Subscribe;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQRunnerSubStatus;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.*;
import org.hkijena.acaq5.api.events.ParameterStructureChangedEvent;
import org.hkijena.acaq5.api.parameters.*;
import org.hkijena.acaq5.extensions.multiparameters.datatypes.ParametersData;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Generates {@link org.hkijena.acaq5.extensions.multiparameters.datatypes.ParametersData} objects
 */
@ACAQDocumentation(name = "Define parameters", description = "Defines algorithm parameters that can be consumed by a multi-parameter algorithm")
@AlgorithmOutputSlot(value = ParametersData.class, slotName = "Parameters", autoCreate = true)
@AlgorithmMetadata(category = ACAQAlgorithmCategory.DataSource)
public class ParametersDataDefinition extends ACAQAlgorithm {

    private ACAQAlgorithmDeclarationRef algorithmDeclaration = new ACAQAlgorithmDeclarationRef();
    private ACAQDynamicParameterHolder algorithmParameters = new ACAQDynamicParameterHolder();

    /**
     * Creates a new instance
     * @param declaration the algorithm declaration
     */
    public ParametersDataDefinition(ACAQAlgorithmDeclaration declaration) {
        super(declaration);
        this.algorithmParameters.setAllowUserModification(false);
        this.algorithmParameters.getEventBus().register(this);
    }

    /**
     * Copies the algorithm
     * @param other the original
     */
    public ParametersDataDefinition(ACAQAlgorithm other) {
        super(other);
        this.algorithmParameters = new ACAQDynamicParameterHolder(algorithmParameters);
        this.algorithmParameters.getEventBus().register(this);
    }

    @Override
    public void run(ACAQRunnerSubStatus subProgress, Consumer<ACAQRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        ParametersData result = new ParametersData();
        result.setAlgorithmDeclaration(algorithmDeclaration.getDeclaration());
        for (Map.Entry<String, ACAQParameterAccess> entry : algorithmParameters.getCustomParameters().entrySet()) {
            result.getParameterData().put(entry.getKey(), entry.getValue().get());
        }
        getFirstOutputSlot().addData(result);
    }

    @Override
    public void reportValidity(ACAQValidityReport report) {
    }

    private void updateAlgorithmParameters() {
        if(algorithmDeclaration == null || algorithmDeclaration.getDeclaration() == null) {
            algorithmParameters.clear();
            return;
        }
        algorithmParameters.beginModificationBlock();
        ACAQAlgorithm instance = algorithmDeclaration.getDeclaration().newInstance();
        Map<String, ACAQParameterAccess> instanceParameters = ACAQParameterAccess.getParameters(instance);

        Set<String> toRemove = new HashSet<>();
        for (String key : algorithmParameters.getCustomParameters().keySet()) {
            if(!instanceParameters.containsKey(key)) {
                toRemove.add(key);
            }
        }
        for (String key : toRemove) {
            algorithmParameters.removeParameter(key);
        }
        for (Map.Entry<String, ACAQParameterAccess> entry : instanceParameters.entrySet()) {
            if(algorithmParameters.containsKey(entry.getKey())) {
                ACAQParameterAccess access = algorithmParameters.get(entry.getKey());
                if(access.getFieldClass() != entry.getValue().getFieldClass()) {
                    algorithmParameters.removeParameter(entry.getKey());
                }
            }
            if(!algorithmParameters.containsKey(entry.getKey())) {
                ACAQMutableParameterAccess parameterAccess = new ACAQMutableParameterAccess(entry.getValue());
                parameterAccess.setParameterHolder(entry.getValue().getParameterHolder());
                parameterAccess.setKey(entry.getKey());
                algorithmParameters.addParameter(parameterAccess);
            }
        }
        algorithmParameters.endModificationBlock();
    }

    @ACAQParameter("algorithm-type")
    @ACAQDocumentation(name = "Algorithm", description = "The algorithm the parameters are created for")
    public ACAQAlgorithmDeclarationRef getAlgorithmDeclaration() {
        return algorithmDeclaration;
    }

    @ACAQParameter("algorithm-type")
    public void setAlgorithmDeclaration(ACAQAlgorithmDeclarationRef algorithmDeclaration) {
        this.algorithmDeclaration = algorithmDeclaration;
        updateAlgorithmParameters();
    }

    @ACAQSubParameters("algorithm-parameters")
    @ACAQDocumentation(name = "Algorithm parameters", description = "Contains the parameters of the selected algorithm")
    public ACAQDynamicParameterHolder getAlgorithmParameters() {
        return algorithmParameters;
    }

    /**
     * Triggered when the parameter structure of algorithm parameters is changed
     *
     * @param event generated event
     */
    @Subscribe
    public void onParameterStructureChanged(ParameterStructureChangedEvent event) {
        getEventBus().post(event);
    }
}
