package org.hkijena.acaq5.api.registries;

import org.hkijena.acaq5.ACAQRegistryService;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithm;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmCategory;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmDeclaration;
import org.hkijena.acaq5.api.algorithm.ACAQDefaultAlgorithmDeclaration;
import org.hkijena.acaq5.api.data.ACAQData;
import org.hkijena.acaq5.api.traits.*;
import org.hkijena.acaq5.api.traits.global.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Manages known algorithms and their annotations
 */
public class ACAQAlgorithmRegistry {
    private Map<String, ACAQAlgorithmDeclaration> registeredAlgorithms = new HashMap<>();

    public ACAQAlgorithmRegistry() {

    }

    /**
     * Registers an algorithm class
     * Automatically registers {@link GoodForTrait} and {@link BadForTrait} annotations
     *
     * @param klass
     */
    public void register(Class<? extends ACAQAlgorithm> klass) {
        register(new ACAQDefaultAlgorithmDeclaration(klass));
    }

    /**
     * Registers an algorithm declaration
     *
     * @param declaration
     */
    public void register(ACAQAlgorithmDeclaration declaration) {
        registeredAlgorithms.put(declaration.getId(), declaration);
    }

    /**
     * Returns an {@link ACAQDefaultAlgorithmDeclaration} instance for a class-based instance registration
     * Returns null if the algorithm class is not registered or defined by any other declaration method
     *
     * @param klass
     * @return
     */
    public ACAQDefaultAlgorithmDeclaration getDefaultDeclarationFor(Class<? extends ACAQAlgorithm> klass) {
        return (ACAQDefaultAlgorithmDeclaration) getDeclarationById(ACAQDefaultAlgorithmDeclaration.getDeclarationIdOf(klass));
    }

    /**
     * Registers that the specified algorithm adds the specified trait to all of its outputs.
     * This is equivalent to attaching {@link AddsTrait} to the class
     *
     * @param klass
     * @param trait
     */
    public void registerAlgorithmAddsTrait(Class<? extends ACAQAlgorithm> klass, Class<? extends ACAQTrait> trait) {
        getDefaultDeclarationFor(klass).getSlotTraitConfiguration().set(ACAQTraitRegistry.getInstance().getDefaultDeclarationFor(trait),
                ACAQTraitModificationOperation.Add);
    }

    /**
     * Registers that the specified algorithm removes the specified trait from all of its outputs.
     * This is equivalent to attaching {@link RemovesTrait} to the class
     *
     * @param klass
     * @param trait
     */
    public void registerAlgorithmRemovesTrait(Class<? extends ACAQAlgorithm> klass, Class<? extends ACAQTrait> trait) {
        getDefaultDeclarationFor(klass).getSlotTraitConfiguration().set(ACAQTraitRegistry.getInstance().getDefaultDeclarationFor(trait),
                ACAQTraitModificationOperation.RemoveCategory);
    }

    /**
     * Registers that the specified algorithm is effective for the specified trait.
     * Equivalent to {@link GoodForTrait} annotation
     *
     * @param klass
     * @param trait
     */
    public void registerPreferredTraitFor(Class<? extends ACAQAlgorithm> klass, Class<? extends ACAQTrait> trait) {
        getDefaultDeclarationFor(klass).getPreferredTraits().add(ACAQTraitRegistry.getInstance().getDefaultDeclarationFor(trait));
    }

    /**
     * Registers that the specified algorithm is ineffective for the specified trait.
     * Equivalent to {@link BadForTrait} annotation
     *
     * @param klass
     * @param trait
     */
    public void registerUnwantedTraitFor(Class<? extends ACAQAlgorithm> klass, Class<? extends ACAQTrait> trait) {
        getDefaultDeclarationFor(klass).getUnwantedTraits().add(ACAQTraitRegistry.getInstance().getDefaultDeclarationFor(trait));
    }

    /**
     * Gets the set of all known algorithms
     *
     * @return
     */
    public Map<String, ACAQAlgorithmDeclaration> getRegisteredAlgorithms() {
        return Collections.unmodifiableMap(registeredAlgorithms);
    }

    /**
     * Returns data source algorithms that can generate the specified data type
     *
     * @param <T>
     * @param dataClass
     * @return
     */
    public <T extends ACAQData> Set<ACAQAlgorithmDeclaration> getDataSourcesFor(Class<? extends T> dataClass) {
        Set<ACAQAlgorithmDeclaration> result = new HashSet<>();
        for (ACAQAlgorithmDeclaration declaration : registeredAlgorithms.values()) {
            if (declaration.getCategory() == ACAQAlgorithmCategory.DataSource) {
                if (declaration.getOutputSlots().stream().anyMatch(slot -> slot.value() == dataClass)) {
                    result.add(declaration);
                }
            }
        }
        return result;
    }

    /**
     * Gets all algorithms of specified category
     *
     * @param category
     * @return
     */
    public Set<ACAQAlgorithmDeclaration> getAlgorithmsOfCategory(ACAQAlgorithmCategory category) {
        return registeredAlgorithms.values().stream().filter(d -> d.getCategory() == category).collect(Collectors.toSet());
    }

    /**
     * Gets a matching declaration by Id
     *
     * @param id
     * @return
     */
    public ACAQAlgorithmDeclaration getDeclarationById(String id) {
        return registeredAlgorithms.get(id);
    }

    public static ACAQAlgorithmRegistry getInstance() {
        return ACAQRegistryService.getInstance().getAlgorithmRegistry();
    }

}
