package org.hkijena.acaq5.api.algorithm;

import org.hkijena.acaq5.api.ACAQRunnerSubStatus;
import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.api.data.ACAQSlotConfiguration;
import org.hkijena.acaq5.api.data.traits.ACAQTraitConfiguration;
import org.hkijena.acaq5.api.exceptions.UserFriendlyRuntimeException;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

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
                throw new UserFriendlyRuntimeException("Input slots have a different row count!", "Unable to group input data together",
                        "Algorithm " + getName(), "The algorithm  has multiple input slots. ACAQ needs to group the input data together for the algorithm to work. " +
                                "But, the input slots have different row counts, so this cannot be safely done without leaving some data out.",
                        "Run the testbench on input algorithms and check why they create different amounts of data. Ensure that their output has the same row number, " +
                                "and can be grouped together by the annotation columns.");
        }
    }

    @Override
    public void run(ACAQRunnerSubStatus subProgress, Consumer<ACAQRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        checkInputSlots();
        ACAQDataSlot referenceSlot = getInputSlots().get(0);
        for (int row = 0; row < referenceSlot.getRowCount(); ++row) {
            if (isCancelled.get())
                return;
            algorithmProgress.accept(subProgress.resolve("Data row " + (row + 1) + " / " + referenceSlot.getRowCount()));
            ACAQDataInterface dataInterface = new ACAQDataInterface(this, getInputSlots(), row);
            runIteration(dataInterface, subProgress, algorithmProgress, isCancelled);
        }
    }

    /**
     * Runs code on one data row
     *
     * @param dataInterface     The data interface
     * @param subProgress       The current sub-progress this algorithm is scheduled in
     * @param algorithmProgress Consumer to publish a new sub-progress
     * @param isCancelled       Supplier that informs if the current task was canceled
     */
    protected abstract void runIteration(ACAQDataInterface dataInterface, ACAQRunnerSubStatus subProgress, Consumer<ACAQRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled);
}
