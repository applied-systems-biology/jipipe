package org.hkijena.acaq5.api.algorithm;

import org.hkijena.acaq5.api.ACAQRunnerSubStatus;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.data.ACAQSlotConfiguration;
import org.hkijena.acaq5.api.data.traits.ACAQTraitConfiguration;
import org.hkijena.acaq5.api.exceptions.UserFriendlyRuntimeException;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * An {@link ACAQAlgorithm} that iterates through each data row.
 * This is a simplified version of {@link ACAQIteratingAlgorithm} that assumes that there is only one or zero input slots.
 * An error is thrown if there are more than one input slots.
 */
public abstract class ACAQSimpleIteratingAlgorithm extends ACAQAlgorithm {

    /**
     * Creates a new instance
     *
     * @param declaration        Algorithm declaration
     * @param slotConfiguration  Slot configuration override
     * @param traitConfiguration Trait configuration override
     */
    public ACAQSimpleIteratingAlgorithm(ACAQAlgorithmDeclaration declaration, ACAQSlotConfiguration slotConfiguration, ACAQTraitConfiguration traitConfiguration) {
        super(declaration, slotConfiguration, traitConfiguration);
    }

    /**
     * Creates a new instance
     *
     * @param declaration       Algorithm declaration
     * @param slotConfiguration Slot configuration override
     */
    public ACAQSimpleIteratingAlgorithm(ACAQAlgorithmDeclaration declaration, ACAQSlotConfiguration slotConfiguration) {
        super(declaration, slotConfiguration, null);
    }

    /**
     * Creates a new instance
     *
     * @param declaration Algorithm declaration
     */
    public ACAQSimpleIteratingAlgorithm(ACAQAlgorithmDeclaration declaration) {
        super(declaration, null, null);
    }

    /**
     * Copies the algorithm
     *
     * @param other The original
     */
    public ACAQSimpleIteratingAlgorithm(ACAQSimpleIteratingAlgorithm other) {
        super(other);
    }

    @Override
    public void run(ACAQRunnerSubStatus subProgress, Consumer<ACAQRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        if (getInputSlots().size() > 1)
            throw new UserFriendlyRuntimeException("Too many input slots for ACAQSimpleIteratingAlgorithm!",
                    "Error in source code detected!",
                    "Algorithm '" + getName() + "'",
                    "The developer of this algorithm chose the wrong node type. The one that was selected only supports at most one input.",
                    "Please contact the plugin developers and tell them to let algorithm '" + getDeclaration().getId() + "' inherit from 'ACAQIteratingAlgorithm' instead.");

        if (getInputSlots().isEmpty()) {
            final int row = 0;
            ACAQRunnerSubStatus slotProgress = subProgress.resolve("Data row " + (row + 1) + " / " + 1);
            algorithmProgress.accept(slotProgress);
            ACAQDataInterface dataInterface = new ACAQDataInterface(this);
            runIteration(dataInterface, slotProgress, algorithmProgress, isCancelled);
        } else {
            for (int i = 0; i < getFirstInputSlot().getRowCount(); i++) {
                if (isCancelled.get())
                    return;
                ACAQRunnerSubStatus slotProgress = subProgress.resolve("Data row " + (i + 1) + " / " + getFirstInputSlot().getRowCount());
                algorithmProgress.accept(slotProgress);
                ACAQDataInterface dataInterface = new ACAQDataInterface(this);
                dataInterface.setData(getFirstInputSlot(), i);
                dataInterface.addGlobalAnnotations(getFirstInputSlot().getAnnotations(i), true);
                runIteration(dataInterface, slotProgress, algorithmProgress, isCancelled);
            }
        }
    }

    @Override
    public void reportValidity(ACAQValidityReport report) {
        if (getInputSlots().size() > 1) {
            report.forCategory("Internals").reportIsInvalid(
                    "Error in source code detected!",
                    "The developer of this algorithm chose the wrong node type. The one that was selected only supports at most one input.",
                    "Please contact the plugin developers and tell them to let algorithm '" + getDeclaration().getId() + "' inherit from 'ACAQIteratingAlgorithm' instead.",
                    this);
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
