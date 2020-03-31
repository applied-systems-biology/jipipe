package org.hkijena.acaq5.api.algorithm;

import org.hkijena.acaq5.ACAQDependency;
import org.hkijena.acaq5.api.data.traits.ACAQDataSlotTraitConfiguration;
import org.hkijena.acaq5.api.traits.ACAQTraitDeclaration;

import java.util.List;
import java.util.Set;

/**
 * Describes an {@link ACAQAlgorithm}
 */
public interface ACAQAlgorithmDeclaration {

    /**
     * Generates an Id for this declaration
     *
     * @return The ID
     */
    String getId();

    /**
     * The algorithm class that is generated
     *
     * @return The algorithm class
     */
    Class<? extends ACAQAlgorithm> getAlgorithmClass();

    /**
     * Creates a new algorithm instance
     *
     * @return Algorithm instance
     */
    ACAQAlgorithm newInstance();

    /**
     * Copies an existing algorithm instance
     *
     * @param algorithm Original algorithm instance
     * @return Copied algorithm instance
     */
    ACAQAlgorithm clone(ACAQAlgorithm algorithm);

    /**
     * Returns the algorithm name
     *
     * @return The name
     */
    String getName();

    /**
     * Returns the algorithm description
     *
     * @return The description
     */
    String getDescription();

    /**
     * Gets the menu path of this algorithm
     *
     * @return String containing menu entries separated by new-lines
     */
    String getMenuPath();

    /**
     * Returns the algorithm category
     *
     * @return The category
     */
    ACAQAlgorithmCategory getCategory();

    /**
     * Returns the preferred traits
     *
     * @return Set of preferred traits
     */
    Set<ACAQTraitDeclaration> getPreferredTraits();

    /**
     * Returns the unwanted traits
     *
     * @return Set of unwanted traits
     */
    Set<ACAQTraitDeclaration> getUnwantedTraits();

    /**
     * Returns all algorithm-global trait modification tasks.
     * Instances can have a configuration that is different from this configuration.
     *
     * @return General trait configuration
     */
    ACAQDataSlotTraitConfiguration getSlotTraitConfiguration();

    /**
     * Returns general input data.
     * Instances can have a configuration that is different from this configuration.
     *
     * @return List of input slots
     */
    List<AlgorithmInputSlot> getInputSlots();

    /**
     * Returns general output data.
     * Instances can have a configuration that is different from this configuration.
     *
     * @return List of output slots
     */
    List<AlgorithmOutputSlot> getOutputSlots();

    /**
     * Returns all dependencies
     *
     * @return List of dependencies
     */
    Set<ACAQDependency> getDependencies();
}
