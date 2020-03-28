package org.hkijena.acaq5.api.registries;

import org.hkijena.acaq5.ACAQDefaultRegistry;
import org.hkijena.acaq5.api.compat.ImageJDatatypeAdapter;
import org.hkijena.acaq5.api.data.ACAQData;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
     * @param adapter
     */
    public void register(ImageJDatatypeAdapter adapter) {
        registeredAdapters.add(adapter);
        registeredACAQDataTypes.add(adapter.getACAQDatatype());
        registeredImageJDataTypes.add(adapter.getImageJDatatype());
    }

    /**
     * Returns true if the ImageJ data type is supported
     * @param klass
     * @return
     */
    public boolean supportsImageJData(Class<?> klass) {
        return registeredImageJDataTypes.contains(klass) || registeredImageJDataTypes.stream().anyMatch(k -> k.isAssignableFrom(klass));
    }

    /**
     * Returns true if the ACAQ data type is supported
     * @param klass
     * @return
     */
    public boolean supportsACAQData(Class<? extends ACAQData> klass) {
        return registeredACAQDataTypes.contains(klass) || registeredACAQDataTypes.stream().anyMatch(k -> k.isAssignableFrom(klass));
    }

    /**
     * Applies conversion from ImageJ to ACAQ5 data
     * @param data
     * @return
     */
    public ACAQData convertImageJToACAQ(Object data) {
        for (ImageJDatatypeAdapter adapter : registeredAdapters) {
            if(adapter.canConvertImageJToACAQ(data)) {
                return adapter.convertImageJToACAQ(data);
            }
        }
        return null;
    }

    /**
     * Applies conversion from ACAQ5 to ImageJ data
     * @param data
     * @param activate If true, the data should be put into foreground
     * @return
     */
    public Object convertACAQToImageJ(ACAQData data, boolean activate) {
        for (ImageJDatatypeAdapter adapter : registeredAdapters) {
            if(adapter.canConvertACAQToImageJ(data)) {
                return adapter.convertACAQToImageJ(data, activate);
            }
        }
        return null;
    }
}
