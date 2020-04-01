package org.hkijena.acaq5.api.algorithm;

import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.api.data.ACAQSlotConfiguration;
import org.hkijena.acaq5.api.data.traits.ACAQTraitConfiguration;

import java.util.List;

/**
 * An {@link ACAQAlgorithm} that iterates through each data row
 * This algorithm type makes sure that input slots are always matched, otherwise errors are thrown
 */
public abstract class ACAQIteratingAlgorithm extends ACAQAlgorithm {

    /**
     * Creates a new instance
     *
     * @param declaration        Algorithm declaration
     * @param slotConfiguration  Slot configuration override
     * @param traitConfiguration Trait configuration override
     */
    public ACAQIteratingAlgorithm(ACAQAlgorithmDeclaration declaration, ACAQSlotConfiguration slotConfiguration, ACAQTraitConfiguration traitConfiguration) {
        super(declaration, slotConfiguration, traitConfiguration);
    }

    /**
     * Creates a new instance
     *
     * @param declaration       Algorithm declaration
     * @param slotConfiguration Slot configuration override
     */
    public ACAQIteratingAlgorithm(ACAQAlgorithmDeclaration declaration, ACAQSlotConfiguration slotConfiguration) {
        super(declaration, slotConfiguration, null);
    }

    /**
     * Creates a new instance
     *
     * @param declaration Algorithm declaration
     */
    public ACAQIteratingAlgorithm(ACAQAlgorithmDeclaration declaration) {
        super(declaration, null, null);
    }

    /**
     * Copies the algorithm
     *
     * @param other The original
     */
    public ACAQIteratingAlgorithm(ACAQIteratingAlgorithm other) {
        super(other);
    }

    private void checkInputSlots() {
        List<ACAQDataSlot> inputSlots = getInputSlots();
        int rows = inputSlots.get(0).getRowCount();
        for (int i = 1; i < inputSlots.size(); ++i) {
            if (rows != inputSlots.get(i).getRowCount())
                throw new RuntimeException("Input slots have a different row count!");
        }
    }

    @Override
    public void run() {
        checkInputSlots();
        ACAQDataSlot referenceSlot = getInputSlots().get(0);
        for (int row = 0; row < referenceSlot.getRowCount(); ++row) {
            ACAQDataInterface dataInterface = new ACAQDataInterface(this, referenceSlot, row);
            runIteration(dataInterface);
        }
    }

    /**
     * Runs code on one data row
     *
     * @param dataInterface The data interface
     */
    protected abstract void runIteration(ACAQDataInterface dataInterface);
}
