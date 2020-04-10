package org.hkijena.acaq5.extensions.multiparameters.algorithms;

import com.google.common.eventbus.Subscribe;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQRunnerSubStatus;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithm;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmDeclaration;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmDeclarationRef;
import org.hkijena.acaq5.api.events.ParameterStructureChangedEvent;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.api.parameters.ACAQSubParameters;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * An algorithm that takes multi parameters
 */
public class MultiParameterAlgorithm extends ACAQAlgorithm {

    private ACAQAlgorithmDeclarationRef algorithmDeclaration = new ACAQAlgorithmDeclarationRef();
    private ACAQAlgorithm algorithmInstance;

    /**
     * Creates a new instance
     * @param declaration the algorithm declaration
     */
    public MultiParameterAlgorithm(ACAQAlgorithmDeclaration declaration) {
        super(declaration);
    }

    /**
     * Copies the algorithm
     * @param other the original
     */
    public MultiParameterAlgorithm(MultiParameterAlgorithm other) {
        super(other);
    }

    @Override
    public void run(ACAQRunnerSubStatus subProgress, Consumer<ACAQRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {

    }

    @Override
    public void reportValidity(ACAQValidityReport report) {

    }

    @ACAQParameter("algorithm-type")
    @ACAQDocumentation(name = "Algorithm", description = "The algorithm the parameters are created for")
    public ACAQAlgorithmDeclarationRef getAlgorithmDeclaration() {
        return algorithmDeclaration;
    }

    @ACAQParameter("algorithm-type")
    public void setAlgorithmDeclaration(ACAQAlgorithmDeclarationRef algorithmDeclaration) {
        this.algorithmDeclaration = algorithmDeclaration;
        updateAlgorithmInstance();
    }

    @ACAQSubParameters("algorithm-parameters")
    public ACAQAlgorithm getAlgorithmInstance() {
        return algorithmInstance;
    }

    private void updateAlgorithmInstance() {
        if(algorithmInstance != null) {
            if(algorithmDeclaration.getDeclaration() == algorithmInstance.getDeclaration())
                return;
            algorithmInstance.getEventBus().unregister(this);
        }
        algorithmInstance = algorithmDeclaration.getDeclaration().newInstance();
        algorithmInstance.getEventBus().register(this);
        getEventBus().post(new ParameterStructureChangedEvent(this));
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
