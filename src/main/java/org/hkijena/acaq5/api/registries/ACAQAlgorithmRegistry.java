package org.hkijena.acaq5.api.registries;

import org.hkijena.acaq5.ACAQRegistryService;
import org.hkijena.acaq5.api.algorithm.*;
import org.hkijena.acaq5.api.data.ACAQData;
import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.api.data.ACAQDataSource;
import org.hkijena.acaq5.api.traits.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Manages known algorithms and their annotations
 */
public class ACAQAlgorithmRegistry {
    private Set<Class<? extends ACAQAlgorithm>> registeredAlgorithms = new HashSet<>();
    private Map<Class<? extends ACAQAlgorithm>, Set<Class<? extends ACAQTrait>>> preferredTraits = new HashMap<>();
    private Map<Class<? extends ACAQAlgorithm>, Set<Class<? extends ACAQTrait>>> unwantedTraits = new HashMap<>();
    private Map<Class<? extends ACAQAlgorithm>, List<AddsTrait>> addedTraits = new HashMap<>();
    private Map<Class<? extends ACAQAlgorithm>, List<RemovesTrait>> removedTraits = new HashMap<>();

    public ACAQAlgorithmRegistry() {

    }

    /**
     * Registers an algorithm class
     * Automatically registers {@link GoodForTrait} and {@link BadForTrait} annotations
     * @param klass
     */
    public void register(Class<? extends ACAQAlgorithm> klass) {
        registeredAlgorithms.add(klass);
        preferredTraits.put(klass, new HashSet<>());
        unwantedTraits.put(klass, new HashSet<>());
        addedTraits.put(klass, new ArrayList<>());
        removedTraits.put(klass, new ArrayList<>());
        for(GoodForTrait trait : klass.getAnnotationsByType(GoodForTrait.class)) {
            registerPreferredTraitFor(klass, trait.value());
        }
        for(BadForTrait trait : klass.getAnnotationsByType(BadForTrait.class)) {
            registerUnwantedTraitFor(klass, trait.value());
        }
        addedTraits.get(klass).addAll(Arrays.asList(klass.getAnnotationsByType(AddsTrait.class)));
        removedTraits.get(klass).addAll(Arrays.asList(klass.getAnnotationsByType(RemovesTrait.class)));
    }

    /**
     * Registers that the specified algorithm adds the specified trait to all of its outputs.
     * This is equivalent to attaching {@link AddsTrait} to the class, although autoAdd is always enabled
     * @param klass
     * @param trait
     */
    public void registerAlgorithmAddsTrait(Class<? extends ACAQAlgorithm> klass, Class<? extends ACAQTrait> trait) {
        addedTraits.get(klass).add(new DefaultAddsTrait(trait, true));
    }

    /**
     * Registers that the specified algorithm removes the specified trait from all of its outputs.
     * This is equivalent to attaching {@link RemovesTrait} to the class, although autoRemove is always enabled
     * @param klass
     * @param trait
     */
    public void registerAlgorithmRemovesTrait(Class<? extends ACAQAlgorithm> klass, Class<? extends ACAQTrait> trait) {
        removedTraits.get(klass).add(new DefaultRemovesTrait(trait, true));
    }

    /**
     * Registers that the specified algorithm is effective for the specified trait.
     * Equivalent to {@link GoodForTrait} annotation
     * @param klass
     * @param trait
     */
    public void registerPreferredTraitFor(Class<? extends ACAQAlgorithm> klass, Class<? extends ACAQTrait> trait) {
        preferredTraits.get(klass).add(trait);
    }

    /**
     * Registers that the specified algorithm is ineffective for the specified trait.
     * Equivalent to {@link BadForTrait} annotation
     * @param klass
     * @param trait
     */
    public void registerUnwantedTraitFor(Class<? extends ACAQAlgorithm> klass, Class<? extends ACAQTrait> trait) {
        unwantedTraits.get(klass).add(trait);
    }

    /**
     * Gets the set of all known algorithms
     * @return
     */
    public Set<Class<? extends ACAQAlgorithm>> getRegisteredAlgorithms() {
        return Collections.unmodifiableSet(registeredAlgorithms);
    }

    /**
     * Returns all data slot types that are inputs to the algorithm
     * @param klass
     * @return
     */
    public Set<Class<? extends ACAQDataSlot<?>>> getInputTypesOf(Class<? extends ACAQAlgorithm> klass) {
        return Arrays.stream(klass.getAnnotationsByType(AlgorithmInputSlot.class)).map(AlgorithmInputSlot::value).collect(Collectors.toSet());
    }

    /**
     * Returns all data slot types that are outputs of the algorithm
     * @param klass
     * @return
     */
    public Set<Class<? extends ACAQDataSlot<?>>> getOutputTypesOf(Class<? extends ACAQAlgorithm> klass) {
        return Arrays.stream(klass.getAnnotationsByType(AlgorithmOutputSlot.class)).map(AlgorithmOutputSlot::value).collect(Collectors.toSet());
    }

    /**
     * Returns all traits annotated to be well working with provided algorithm
     * @param klass
     * @return
     */
    public Set<Class<? extends ACAQTrait>> getPreferredTraitsOf(Class<? extends ACAQAlgorithm> klass) {
        return Collections.unmodifiableSet(preferredTraits.getOrDefault(klass, Collections.emptySet()));
    }

    /**
     * Returns all traits annotated to be badly working with provided algorithm
     * @param klass
     * @return
     */
    public Set<Class<? extends ACAQTrait>> getUnwantedTraitsOf(Class<? extends ACAQAlgorithm> klass) {
        return Collections.unmodifiableSet(unwantedTraits.getOrDefault(klass, Collections.emptySet()));
    }

    /**
     * Returns all traits added by the algorithm. Please note that this works on algorithm-level.
     * Algorithms can have different trait modifications per data slot.
     * @param klass
     * @return
     */
    public List<AddsTrait> getAddedTraitsOf(Class<? extends ACAQAlgorithm> klass) {
        return Collections.unmodifiableList(addedTraits.getOrDefault(klass, Collections.emptyList()));
    }

    /**
     * Returns all traits added by the algorithm. Please note that this works on algorithm-level.
     * Algorithms can have different trait modifications per data slot.
     * @param klass
     * @return
     */
    public List<RemovesTrait> getRemovedTraitsOf(Class<? extends ACAQAlgorithm> klass) {
        return Collections.unmodifiableList(removedTraits.getOrDefault(klass, Collections.emptyList()));
    }

    /**
     * Returns data source algorithms that can generate the specified data type
     * @param dataClass
     * @param <T>
     * @return
     */
    public <T extends ACAQData> Set<Class<? extends ACAQDataSource<T>>> getDataSourcesFor(Class<? extends T> dataClass) {
        Class<? extends ACAQDataSlot<?>> slotClass = ACAQRegistryService.getInstance().getDatatypeRegistry().getRegisteredSlotDataTypes().get(dataClass);
        Set<Class<? extends ACAQDataSource<T>>> result = new HashSet<>();
        for(Class<? extends ACAQAlgorithm> klass : registeredAlgorithms) {
            if(ACAQDataSource.class.isAssignableFrom(klass)) {
                if(Arrays.stream(ACAQAlgorithm.getOutputOf(klass)).anyMatch(slot -> slot.value().equals(slotClass))) {
                    result.add((Class<? extends ACAQDataSource<T>>) klass);
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
    public Set<Class<? extends ACAQAlgorithm>> getAlgorithmsOfCategory(ACAQAlgorithmCategory category) {
        Set<Class<? extends ACAQAlgorithm>> result = new HashSet<>();
        for(Class<? extends ACAQAlgorithm> klass : registeredAlgorithms) {
            AlgorithmMetadata[] metadatas = klass.getAnnotationsByType(AlgorithmMetadata.class);
            if(metadatas.length > 0) {
                if(Arrays.stream(metadatas).anyMatch(annot -> annot.category() == category))
                    result.add(klass);
            }
        }
        return result;
    }

    /**
     * Finds a known algorithm by its canonical class name
     * @param canonicalName
     * @return
     */
    public Class<? extends ACAQAlgorithm> findAlgorithmClass(String canonicalName) {
        return registeredAlgorithms.stream().filter(c -> c.getCanonicalName().equals(canonicalName)).findFirst().get();
    }

    /**
     * Creates a new algorithm instance by its canonical class name
     * @param canonicalName
     * @return
     */
    public ACAQAlgorithm createInstanceFromClassName(String canonicalName) {
        try {
            return findAlgorithmClass(canonicalName).newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
