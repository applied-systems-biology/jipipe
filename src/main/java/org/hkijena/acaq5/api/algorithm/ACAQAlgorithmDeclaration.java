package org.hkijena.acaq5.api.algorithm;

import org.hkijena.acaq5.api.traits.*;
import org.hkijena.acaq5.api.traits.global.ACAQTraitModificationTask;

import java.util.List;
import java.util.Set;

public interface ACAQAlgorithmDeclaration {

    /**
     * Generates an Id for this declaration
     *
     * @return
     */
    String getId();

    /**
     * The algorithm class that is generated
     *
     * @return
     */
    Class<? extends ACAQAlgorithm> getAlgorithmClass();

    /**
     * Creates a new algorithm instance
     *
     * @return
     */
    ACAQAlgorithm newInstance();

    /**
     * Copies an existing algorithm instance
     *
     * @param algorithm
     * @return
     */
    ACAQAlgorithm clone(ACAQAlgorithm algorithm);

    /**
     * Returns the algorithm name
     *
     * @return
     */
    String getName();

    /**
     * Returns the algorithm description
     *
     * @return
     */
    String getDescription();

    /**
     * Returns the algorithm category
     *
     * @return
     */
    ACAQAlgorithmCategory getCategory();

    /**
     * Returns the preferred traits
     *
     * @return
     */
    Set<ACAQTraitDeclaration> getPreferredTraits();

    /**
     * Returns the unwanted traits
     *
     * @return
     */
    Set<ACAQTraitDeclaration> getUnwantedTraits();

    /**
     * Returns all algorithm-global trait modification tasks
     * @return
     */
    List<ACAQTraitModificationTask> getTraitModificationTasks();

    /**
     * Returns input data
     *
     * @return
     */
    List<AlgorithmInputSlot> getInputSlots();

    /**
     * Returns output data
     *
     * @return
     */
    List<AlgorithmOutputSlot> getOutputSlots();
}
