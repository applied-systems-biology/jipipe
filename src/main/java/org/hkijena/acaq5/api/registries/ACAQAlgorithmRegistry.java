package org.hkijena.acaq5.api.registries;

import org.hkijena.acaq5.api.algorithm.ACAQAlgorithm;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmCategory;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmMetadata;
import org.hkijena.acaq5.api.data.ACAQData;
import org.hkijena.acaq5.api.data.ACAQDataSource;
import org.hkijena.acaq5.api.data.ACAQGeneratesData;
import org.hkijena.acaq5.api.traits.ACAQTrait;
import org.hkijena.acaq5.api.traits.BadForTrait;
import org.hkijena.acaq5.api.traits.GoodForTrait;

import java.util.*;

/**
 * Manages known algorithms and their annotations
 */
public class ACAQAlgorithmRegistry {
    private Set<Class<? extends ACAQAlgorithm>> registeredAlgorithms = new HashSet<>();
    private Map<Class<? extends ACAQAlgorithm>, Set<Class<? extends ACAQTrait>>> preferredTraits = new HashMap<>();
    private Map<Class<? extends ACAQAlgorithm>, Set<Class<? extends ACAQTrait>>> unwantedTraits = new HashMap<>();

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
        for(GoodForTrait trait : klass.getAnnotationsByType(GoodForTrait.class)) {
            registerPreferredTraitFor(klass, trait.value());
        }
        for(BadForTrait trait : klass.getAnnotationsByType(BadForTrait.class)) {
            registerUnwantedTraitFor(klass, trait.value());
        }
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
     * Returns all traits annotated to be well working with provided algorithm
     * @param klass
     * @return
     */
    public Set<Class<? extends ACAQTrait>> getPreferredTraitsOf(Class<? extends ACAQAlgorithm> klass) {
        return Collections.unmodifiableSet(preferredTraits.get(klass));
    }

    /**
     * Returns all traits annotated to be badly working with provided algorithm
     * @param klass
     * @return
     */
    public Set<Class<? extends ACAQTrait>> getUnwantedTraitsOf(Class<? extends ACAQAlgorithm> klass) {
        return Collections.unmodifiableSet(unwantedTraits.get(klass));
    }

    /**
     * Returns data source algorithms that can generate the specified data type
     * @param dataClass
     * @param <T>
     * @return
     */
    public <T extends ACAQData> Set<Class<? extends ACAQDataSource<T>>> getDataSourcesFor(Class<? extends T> dataClass) {
        Set<Class<? extends ACAQDataSource<T>>> result = new HashSet<>();
        for(Class<? extends ACAQAlgorithm> klass : registeredAlgorithms) {
            ACAQGeneratesData[] annotations = klass.getAnnotationsByType(ACAQGeneratesData.class);
            if(annotations.length > 0) {
                if(Arrays.stream(annotations).anyMatch(annot -> annot.value() == dataClass))
                    result.add((Class<? extends ACAQDataSource<T>>) klass);
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
            ACAQAlgorithmMetadata[] metadatas = klass.getAnnotationsByType(ACAQAlgorithmMetadata.class);
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
