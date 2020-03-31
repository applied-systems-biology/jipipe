package org.hkijena.acaq5.api.data.traits;

import org.hkijena.acaq5.api.traits.ACAQTraitDeclaration;

/**
 * A mutable {@link ACAQTraitConfiguration}
 */
public interface ACAQMutableTraitConfiguration extends ACAQTraitConfiguration {
    /**
     * Returns true if the user is not anymore able to modify trait modifications
     *
     * @return if the user is not anymore able to modify trait modifications
     */
    boolean isTraitModificationsSealed();

    /**
     * Returns true if the user is not anymore able to modify transfers
     *
     * @return if the user is not anymore able to modify transfers
     */
    boolean isTraitTransfersSealed();

    /**
     * Set how a trait is modified
     *
     * @param slotName The slot name
     * @param traitDeclaration The trait declaration
     * @param operation The operation
     */
    void setTraitModification(String slotName, ACAQTraitDeclaration traitDeclaration, ACAQTraitModificationOperation operation);

    /**
     * Adds a transfer task
     *
     * @param task The task
     */
    void addTransfer(ACAQTraitTransferTask task);

    /**
     * Removes all transfer tasks equivalent to the provided one
     *
     * @param task The task
     */
    void removeTransfer(ACAQTraitTransferTask task);
}
