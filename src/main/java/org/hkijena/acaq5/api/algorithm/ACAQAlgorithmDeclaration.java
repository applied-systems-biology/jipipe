package org.hkijena.acaq5.api.algorithm;

import com.fasterxml.jackson.databind.JsonNode;
import org.hkijena.acaq5.api.traits.ACAQTrait;
import org.hkijena.acaq5.api.traits.AddsTrait;
import org.hkijena.acaq5.api.traits.RemovesTrait;

import java.util.List;
import java.util.Set;

public interface ACAQAlgorithmDeclaration {

    /**
     * Generates an Id for this declaration
     * @return
     */
    String getId();

    /**
     * The algorithm class that is generated
     * @return
     */
    Class<? extends ACAQAlgorithm> getAlgorithmClass();

    /**
     * Creates a new algorithm instance
     * @return
     */
    ACAQAlgorithm newInstance();

    /**
     * Copies an existing algorithm instance
     * @param algorithm
     * @return
     */
    ACAQAlgorithm clone(ACAQAlgorithm algorithm);

    /**
     * Returns the algorithm name
     * @return
     */
    String getName();

    /**
     * Returns the algorithm description
     * @return
     */
    String getDescription();

    /**
     * Returns the algorithm category
     * @return
     */
    ACAQAlgorithmCategory getCategory();

    /**
     * Returns the preferred traits
     * @return
     */
    Set<Class<? extends ACAQTrait>> getPreferredTraits();

    /**
     * Returns the unwanted traits
     * @return
     */
    Set<Class<? extends ACAQTrait>> getUnwantedTraits();

    /**
     * Returns which traits are added
     * @return
     */
    List<AddsTrait> getAddedTraits();

    /**
     * Returns which traits are removed
     * @return
     */
    List<RemovesTrait> getRemovedTraits();

    /**
     * Returns input data
     * @return
     */
    List<AlgorithmInputSlot> getInputSlots();

    /**
     * Returns output data
     * @return
     */
    List<AlgorithmOutputSlot> getOutputSlots();
}
