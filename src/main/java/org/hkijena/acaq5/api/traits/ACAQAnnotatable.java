package org.hkijena.acaq5.api.traits;

public interface ACAQAnnotatable {

    /**
     * Gets an annotation of given type
     *
     * @param klass
     * @param <T>
     * @return null if there is no instance
     */
    <T extends ACAQTrait> T getAnnotationOf(Class<T> klass);
}
