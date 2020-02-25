package org.hkijena.acaq5.api.registries;

import com.fasterxml.jackson.databind.JsonNode;
import org.hkijena.acaq5.ACAQRegistryService;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithm;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmCategory;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmDeclaration;
import org.hkijena.acaq5.api.algorithm.ACAQDefaultAlgorithmDeclaration;
import org.hkijena.acaq5.api.data.ACAQData;
import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.api.data.ACAQDataSource;
import org.hkijena.acaq5.api.traits.*;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Manages known algorithms and their annotations
 */
public class ACAQAlgorithmRegistry {
    private Set<ACAQAlgorithmDeclaration> registeredAlgorithms = new HashSet<>();

    public ACAQAlgorithmRegistry() {

    }

    /**
     * Registers an algorithm class
     * Automatically registers {@link GoodForTrait} and {@link BadForTrait} annotations
     * @param klass
     */
    public void register(Class<? extends ACAQAlgorithm> klass) {
        registeredAlgorithms.add(new ACAQDefaultAlgorithmDeclaration(klass));
    }

    /**
     * Returns an {@link ACAQDefaultAlgorithmDeclaration} instance for a class-based instance registration
     * Returns null if the algorithm class is not registered or defined by any other declaration method
     * @param klass
     * @return
     */
    public ACAQDefaultAlgorithmDeclaration getDefaultDeclarationFor(Class<? extends ACAQAlgorithm> klass) {
        return (ACAQDefaultAlgorithmDeclaration) registeredAlgorithms.stream().filter(d -> d.getAlgorithmClass().equals(klass)).findFirst().orElse(null);
    }

    /**
     * Registers that the specified algorithm adds the specified trait to all of its outputs.
     * This is equivalent to attaching {@link AddsTrait} to the class, although autoAdd is always enabled
     * @param klass
     * @param trait
     */
    public void registerAlgorithmAddsTrait(Class<? extends ACAQAlgorithm> klass, Class<? extends ACAQTrait> trait) {
        getDefaultDeclarationFor(klass).getAddedTraits().add(new DefaultAddsTrait(trait, true));
    }

    /**
     * Registers that the specified algorithm removes the specified trait from all of its outputs.
     * This is equivalent to attaching {@link RemovesTrait} to the class, although autoRemove is always enabled
     * @param klass
     * @param trait
     */
    public void registerAlgorithmRemovesTrait(Class<? extends ACAQAlgorithm> klass, Class<? extends ACAQTrait> trait) {
        getDefaultDeclarationFor(klass).getRemovedTraits().add(new DefaultRemovesTrait(trait, true));
    }

    /**
     * Registers that the specified algorithm is effective for the specified trait.
     * Equivalent to {@link GoodForTrait} annotation
     * @param klass
     * @param trait
     */
    public void registerPreferredTraitFor(Class<? extends ACAQAlgorithm> klass, Class<? extends ACAQTrait> trait) {
        getDefaultDeclarationFor(klass).getPreferredTraits().add(trait);
    }

    /**
     * Registers that the specified algorithm is ineffective for the specified trait.
     * Equivalent to {@link BadForTrait} annotation
     * @param klass
     * @param trait
     */
    public void registerUnwantedTraitFor(Class<? extends ACAQAlgorithm> klass, Class<? extends ACAQTrait> trait) {
        getDefaultDeclarationFor(klass).getUnwantedTraits().add(trait);
    }

    /**
     * Gets the set of all known algorithms
     * @return
     */
    public Set<ACAQAlgorithmDeclaration> getRegisteredAlgorithms() {
        return Collections.unmodifiableSet(registeredAlgorithms);
    }

    /**
     * Returns data source algorithms that can generate the specified data type
     * @param <T>
     * @param dataClass
     * @return
     */
    public <T extends ACAQData> Set<ACAQAlgorithmDeclaration> getDataSourcesFor(Class<? extends T> dataClass) {
        Class<? extends ACAQDataSlot<?>> slotClass = ACAQRegistryService.getInstance().getDatatypeRegistry().getRegisteredSlotDataTypes().get(dataClass);
        Set<ACAQAlgorithmDeclaration> result = new HashSet<>();
        for(ACAQAlgorithmDeclaration declaration : registeredAlgorithms) {
            if(ACAQDataSource.class.isAssignableFrom(declaration.getAlgorithmClass())) {
                if(declaration.getOutputSlots().stream().anyMatch(slot -> slot.value().equals(slotClass))) {
                    result.add(declaration);
                }
            }
        }
        return result;
    }

    /**
     * Gets all algorithms of specified category
     * @param category
     * @return
     */
    public Set<ACAQAlgorithmDeclaration> getAlgorithmsOfCategory(ACAQAlgorithmCategory category) {
       return registeredAlgorithms.stream().filter(d -> d.getCategory() == category).collect(Collectors.toSet());
    }

    /**
     * Finds an {@link ACAQAlgorithmDeclaration} that matches the algorithm JSON node
     * @param value
     * @return
     */
    public ACAQAlgorithmDeclaration findMatchingDeclaration(JsonNode value) {
        return registeredAlgorithms.stream().filter(d -> d.matches(value)).findFirst().orElse(null);
    }
}
