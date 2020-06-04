package org.hkijena.acaq5.api.algorithm;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.data.ACAQSlotConfiguration;
import org.hkijena.acaq5.api.data.traits.ACAQDefaultMutableTraitConfiguration;
import org.hkijena.acaq5.api.data.traits.ACAQTraitConfiguration;
import org.hkijena.acaq5.api.parameters.ACAQParameter;

/**
 * An {@link ACAQGraphNode} that contains a non-empty workload.
 * This class contains additional parameters to control the workload behavior.
 * Please prefer to use this class or its derivatives if you write your algorithms.
 */
public abstract class ACAQAlgorithm extends ACAQGraphNode {

    private boolean enabled = true;

    /**
     * Initializes this algorithm with a custom provided slot configuration and trait configuration
     *
     * @param declaration        Contains algorithm metadata
     * @param slotConfiguration  if null, generate the slot configuration
     * @param traitConfiguration if null, defaults to {@link ACAQDefaultMutableTraitConfiguration}
     */
    public ACAQAlgorithm(ACAQAlgorithmDeclaration declaration, ACAQSlotConfiguration slotConfiguration, ACAQTraitConfiguration traitConfiguration) {
        super(declaration, slotConfiguration, traitConfiguration);
    }

    /**
     * Initializes a new algorithm instance and sets a custom slot configuration
     *
     * @param declaration       The algorithm declaration
     * @param slotConfiguration The slot configuration
     */
    public ACAQAlgorithm(ACAQAlgorithmDeclaration declaration, ACAQSlotConfiguration slotConfiguration) {
        super(declaration, slotConfiguration);
    }

    /**
     * Initializes a new algorithm instance
     *
     * @param declaration The algorithm declaration
     */
    public ACAQAlgorithm(ACAQAlgorithmDeclaration declaration) {
        super(declaration);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public ACAQAlgorithm(ACAQAlgorithm other) {
        super(other);
        this.enabled = other.enabled;
    }

    @ACAQDocumentation(name = "Enabled", description = "If disabled, this algorithm will be skipped in a run. " +
            "Please note that this will also disable all algorithms dependent on this algorithm.")
    @ACAQParameter("acaq:algorithm:enabled")
    public boolean isEnabled() {
        return enabled;
    }

    @ACAQParameter("acaq:algorithm:enabled")
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
