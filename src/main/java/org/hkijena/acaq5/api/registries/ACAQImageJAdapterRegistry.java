package org.hkijena.acaq5.api.registries;

import org.hkijena.acaq5.ACAQDefaultRegistry;
import org.hkijena.acaq5.api.compat.ImageJDatatypeAdapter;
import org.hkijena.acaq5.api.data.ACAQData;

import java.util.*;

/**
 * Registers all known adapters between ImageJ and ACAQ5 data types
 */
public class ACAQImageJAdapterRegistry {
    private List<ImageJDatatypeAdapter> registeredAdapters = new ArrayList<>();
    private Set<Class<? extends ACAQData>> registeredACAQDataTypes = new HashSet<>();
    private Set<Class<?>> registeredImageJDataTypes = new HashSet<>();

    public static ACAQImageJAdapterRegistry getInstance() {
        return ACAQDefaultRegistry.getInstance().getImageJDataAdapterRegistry();
    }

    /**
     * Registers an adapter
     *
     * @param adapter
     */
    public void register(ImageJDatatypeAdapter adapter) {
        registeredAdapters.add(adapter);
        registeredACAQDataTypes.add(adapter.getACAQDatatype());
        registeredImageJDataTypes.add(adapter.getImageJDatatype());
    }

    /**
     * Returns true if the ImageJ data type is supported
     *
     * @param klass
     * @return
     */
    public boolean supportsImageJData(Class<?> klass) {
        return registeredImageJDataTypes.contains(klass) || registeredImageJDataTypes.stream().anyMatch(k -> k.isAssignableFrom(klass));
    }

    /**
     * Returns true if the ACAQ data type is supported
     *
     * @param klass
     * @return
     */
    public boolean supportsACAQData(Class<? extends ACAQData> klass) {
        return registeredACAQDataTypes.contains(klass) || registeredACAQDataTypes.stream().anyMatch(k -> k.isAssignableFrom(klass));
    }

    /**
     * Returns a matching adapter for an ACAQ5 data type
     *
     * @param klass
     * @return
     */
    public ImageJDatatypeAdapter getAdapterForACAQData(Class<? extends ACAQData> klass) {
        if (registeredACAQDataTypes.contains(klass)) {
            for (ImageJDatatypeAdapter adapter : registeredAdapters) {
                if (adapter.getACAQDatatype() == klass)
                    return adapter;
            }
        } else {
            for (ImageJDatatypeAdapter adapter : registeredAdapters) {
                if (adapter.getACAQDatatype().isAssignableFrom(klass))
                    return adapter;
            }
        }
        return null;
    }

    /**
     * Returns a matching adapter for an ImageJ data type
     *
     * @param klass
     * @return
     */
    public ImageJDatatypeAdapter getAdapterForImageJData(Class<?> klass) {
        if (registeredACAQDataTypes.contains(klass)) {
            for (ImageJDatatypeAdapter adapter : registeredAdapters) {
                if (adapter.getImageJDatatype() == klass)
                    return adapter;
            }
        } else {
            for (ImageJDatatypeAdapter adapter : registeredAdapters) {
                if (adapter.getImageJDatatype().isAssignableFrom(klass))
                    return adapter;
            }
        }
        return null;
    }

    /**
     * Returns all supported ACAQ data types
     *
     * @return
     */
    public Set<Class<? extends ACAQData>> getSupportedACAQDataTypes() {
        return Collections.unmodifiableSet(registeredACAQDataTypes);
    }

    /**
     * Returns all supported ImageJ data types
     *
     * @return
     */
    public Set<Class<?>> getSupportedImageJDataTypes() {
        return Collections.unmodifiableSet(registeredImageJDataTypes);
    }

    /**
     * Gets a matching adapter for the provided data
     *
     * @param data
     * @return
     */
    public ImageJDatatypeAdapter getAdapterForACAQData(ACAQData data) {
        for (ImageJDatatypeAdapter adapter : registeredAdapters) {
            if (adapter.canConvertACAQToImageJ(data)) {
                return adapter;
            }
        }
        return null;
    }

    /**
     * Applies conversion from ACAQ5 to ImageJ data
     *
     * @param data
     * @return
     */
    public ImageJDatatypeAdapter getAdapterForImageJData(Object data) {
        for (ImageJDatatypeAdapter adapter : registeredAdapters) {
            if (adapter.canConvertImageJToACAQ(data)) {
                return adapter;
            }
        }
        return null;
    }

    public List<ImageJDatatypeAdapter> getRegisteredAdapters() {
        return registeredAdapters;
    }
}
