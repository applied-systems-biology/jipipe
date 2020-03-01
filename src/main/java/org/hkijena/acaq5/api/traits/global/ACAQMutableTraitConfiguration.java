package org.hkijena.acaq5.api.traits.global;

import org.hkijena.acaq5.api.traits.ACAQTraitDeclaration;

public interface ACAQMutableTraitConfiguration extends ACAQTraitConfiguration {
    /**
     * Returns true if the user is not anymore able to modify trait modifications
     * @return
     */
    boolean isTraitModificationsSealed();

    /**
     * Returns true if the user is not anymore able to modify transfers
     * @return
     */
    boolean isTraitTransfersSealed();

    /**
     * Set how a trait is modified
     * @param slotName
     * @param operation
     */
    void setTraitModification(String slotName, ACAQTraitDeclaration traitDeclaration, ACAQTraitModificationOperation operation);

    /**
     * Adds a transfer task
     * @param task
     */
    void addTransfer(ACAQTraitTransferTask task);

    /**
     * Removes all transfer tasks equivalent to the provided one
     * @param task
     */
    void removeTransfer(ACAQTraitTransferTask task);
}
