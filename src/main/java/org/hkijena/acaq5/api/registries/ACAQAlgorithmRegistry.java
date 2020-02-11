package org.hkijena.acaq5.api.registries;

import org.hkijena.acaq5.api.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class ACAQAlgorithmRegistry {
    private Set<Class<? extends ACAQAlgorithm>> registeredAlgorithms = new HashSet<>();

    public ACAQAlgorithmRegistry() {

    }

    public void register(Class<? extends ACAQAlgorithm> klass) {
        registeredAlgorithms.add(klass);
    }

    public Set<Class<? extends ACAQAlgorithm>> getRegisteredAlgorithms() {
        return Collections.unmodifiableSet(registeredAlgorithms);
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
            ACAQGenerates[] annotations = klass.getAnnotationsByType(ACAQGenerates.class);
            if(annotations.length > 0) {
                if(Arrays.stream(annotations).anyMatch(annot -> annot.value() == dataClass))
                    result.add((Class<? extends ACAQDataSource<T>>) klass);
            }
        }
        return result;
    }

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
}
