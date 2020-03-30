package org.hkijena.acaq5.api.compat;

import org.hkijena.acaq5.api.ACAQValidatable;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithm;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmDeclaration;
import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.api.registries.ACAQImageJAdapterRegistry;

/**
 * Settings class used for a single algorithm run
 */
public class SingleImageJAlgorithmRun implements ACAQValidatable {

    private ACAQAlgorithm algorithm;

    @Override
    public void reportValidity(ACAQValidityReport report) {
        if(algorithm == null) {
            report.reportIsInvalid("No algorithm was provided! This is an programming error. Please contact the plugin author.");
        }
    }

    public ACAQAlgorithm getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(ACAQAlgorithm algorithm) {
        this.algorithm = algorithm;
    }

    public void pushInput() {

    }

    public void pullOutput() {

    }

    /**
     * Returns true if an algorithm can be run in a single ImageJ algorithm run
     * @param declaration
     * @return
     */
    public static boolean isCompatible(ACAQAlgorithmDeclaration declaration) {
        switch (declaration.getCategory()) {
            case Internal:
            case Annotation:
                return false;
        }
        ACAQAlgorithm algorithm = declaration.newInstance();
        for (ACAQDataSlot inputSlot : algorithm.getInputSlots()) {
            if(!ACAQImageJAdapterRegistry.getInstance().supportsACAQData(inputSlot.getAcceptedDataType()))
                return false;
        }
        for (ACAQDataSlot outputSlot : algorithm.getOutputSlots()) {
            if(!ACAQImageJAdapterRegistry.getInstance().supportsACAQData(outputSlot.getAcceptedDataType()))
                return false;
        }

        return true;
    }
}
