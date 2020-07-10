/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.api.registries;

import org.hkijena.jipipe.JIPipeDefaultRegistry;
import org.hkijena.jipipe.api.compat.ImageJDatatypeAdapter;
import org.hkijena.jipipe.api.data.JIPipeData;

import java.util.*;

/**
 * Registers all known adapters between ImageJ and JIPipe data types
 */
public class JIPipeImageJAdapterRegistry {
    private List<ImageJDatatypeAdapter> registeredAdapters = new ArrayList<>();
    private Set<Class<? extends JIPipeData>> registeredJIPipeDataTypes = new HashSet<>();
    private Set<Class<?>> registeredImageJDataTypes = new HashSet<>();

    /**
     * Registers an adapter
     *
     * @param adapter Adapter instance
     */
    public void register(ImageJDatatypeAdapter adapter) {
        registeredAdapters.add(adapter);
        registeredJIPipeDataTypes.add(adapter.getJIPipeDatatype());
        registeredImageJDataTypes.add(adapter.getImageJDatatype());
    }

    /**
     * Returns true if the ImageJ data type is supported
     *
     * @param klass ImageJ data type
     * @return True if the type is supported
     */
    public boolean supportsImageJData(Class<?> klass) {
        return registeredImageJDataTypes.contains(klass) || registeredImageJDataTypes.stream().anyMatch(k -> k.isAssignableFrom(klass));
    }

    /**
     * Returns true if the JIPipe data type is supported
     *
     * @param klass JIPipe data class
     * @return True if the data class is supported
     */
    public boolean supportsJIPipeData(Class<? extends JIPipeData> klass) {
        return registeredJIPipeDataTypes.contains(klass) || registeredJIPipeDataTypes.stream().anyMatch(k -> k.isAssignableFrom(klass));
    }

    /**
     * Returns a matching adapter for a JIPipe data type
     *
     * @param klass JIPipe data class
     * @return An adapter that supports the data class
     */
    public ImageJDatatypeAdapter getAdapterForJIPipeData(Class<? extends JIPipeData> klass) {
        if (registeredJIPipeDataTypes.contains(klass)) {
            for (ImageJDatatypeAdapter adapter : registeredAdapters) {
                if (adapter.getJIPipeDatatype() == klass)
                    return adapter;
            }
        } else {
            for (ImageJDatatypeAdapter adapter : registeredAdapters) {
                if (adapter.getJIPipeDatatype().isAssignableFrom(klass))
                    return adapter;
            }
        }
        return null;
    }

    /**
     * Returns a matching adapter for an ImageJ data type
     *
     * @param klass ImageJ data type
     * @return An adapter that supports the ImageJ data type
     */
    public ImageJDatatypeAdapter getAdapterForImageJData(Class<?> klass) {
        if (registeredJIPipeDataTypes.contains(klass)) {
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
     * Returns all supported JIPipe data types
     *
     * @return The set of supported JIPipe data types
     */
    public Set<Class<? extends JIPipeData>> getSupportedJIPipeDataTypes() {
        return Collections.unmodifiableSet(registeredJIPipeDataTypes);
    }

    /**
     * Returns all supported ImageJ data types
     *
     * @return The set of supported ImageJ data types
     */
    public Set<Class<?>> getSupportedImageJDataTypes() {
        return Collections.unmodifiableSet(registeredImageJDataTypes);
    }

    /**
     * Gets a matching adapter for the provided data
     *
     * @param data JIPipe data instance
     * @return Adapter that supports the data
     */
    public ImageJDatatypeAdapter getAdapterForJIPipeData(JIPipeData data) {
        for (ImageJDatatypeAdapter adapter : registeredAdapters) {
            if (adapter.canConvertJIPipeToImageJ(data)) {
                return adapter;
            }
        }
        return null;
    }

    /**
     * Applies conversion from JIPipe to ImageJ data
     *
     * @param data ImageJ data
     * @return Adapter that supports the data
     */
    public ImageJDatatypeAdapter getAdapterForImageJData(Object data) {
        for (ImageJDatatypeAdapter adapter : registeredAdapters) {
            if (adapter.canConvertImageJToJIPipe(data)) {
                return adapter;
            }
        }
        return null;
    }

    /**
     * @return Registered adapters
     */
    public List<ImageJDatatypeAdapter> getRegisteredAdapters() {
        return registeredAdapters;
    }

    /**
     * @return Singleton instance
     */
    public static JIPipeImageJAdapterRegistry getInstance() {
        return JIPipeDefaultRegistry.getInstance().getImageJDataAdapterRegistry();
    }
}
