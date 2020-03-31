package org.hkijena.acaq5.api.data.traits;

/**
 * Operations for trait modification
 */
public enum ACAQTraitModificationOperation {
    /**
     * Ignore the trait type
     */
    Ignore,
    /**
     * Add the trait type
     */
    Add,
    /**
     * Remove the specific trait type
     */
    RemoveThis,
    /**
     * Remove the specific trait type and dependants
     */
    RemoveCategory
}
