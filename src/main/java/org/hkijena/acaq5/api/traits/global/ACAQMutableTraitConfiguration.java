package org.hkijena.acaq5.api.traits.global;

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
     * Adds a trait modification
     * @param slotName
     * @param task
     */
    void addTraitModification(String slotName, ACAQTraitModificationTask task);

    /**
     * Removes all trait modifications that are equivalent of the provided one
     * @param task
     */
    void removeTraitModification(String slotName, ACAQTraitModificationTask task);

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
