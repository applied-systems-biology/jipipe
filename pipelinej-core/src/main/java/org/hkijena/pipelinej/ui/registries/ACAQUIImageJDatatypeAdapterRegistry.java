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

package org.hkijena.pipelinej.ui.registries;

import org.apache.commons.lang3.reflect.ConstructorUtils;
import org.hkijena.pipelinej.ACAQDefaultRegistry;
import org.hkijena.pipelinej.api.compat.ImageJDatatypeImporter;
import org.hkijena.pipelinej.ui.compat.ImageJDatatypeImporterUI;

import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Registry for {@link org.hkijena.pipelinej.api.compat.ImageJDatatypeAdapter} UIs
 */
public class ACAQUIImageJDatatypeAdapterRegistry {
    private Map<Class<?>, Class<? extends ImageJDatatypeImporterUI>> registeredImporters = new HashMap<>();

    /**
     * @param imageJDataType imageJ data type
     * @param importerClass  importer UI class
     */
    public void registerImporterFor(Class<?> imageJDataType, Class<? extends ImageJDatatypeImporterUI> importerClass) {
        registeredImporters.put(imageJDataType, importerClass);
    }

    public Map<Class<?>, Class<? extends ImageJDatatypeImporterUI>> getRegisteredImporters() {
        return Collections.unmodifiableMap(registeredImporters);
    }

    /**
     * Gets the importer for the specified ImageJ data type
     *
     * @param imageJDataType imageJ data type
     * @return importer UI class
     */
    public Class<? extends ImageJDatatypeImporterUI> getImporterClassFor(Class<?> imageJDataType) {
        Class<? extends ImageJDatatypeImporterUI> importerClass = registeredImporters.getOrDefault(imageJDataType, null);
        if (importerClass != null) {
            return importerClass;
        } else {
            for (Map.Entry<Class<?>, Class<? extends ImageJDatatypeImporterUI>> entry : registeredImporters.entrySet()) {
                if (entry.getKey().isAssignableFrom(imageJDataType)) {
                    return entry.getValue();
                }
            }
            return null;
        }
    }

    /**
     * Generates a UI for the importer
     *
     * @param importer importer
     * @return UI instance
     */
    public ImageJDatatypeImporterUI getUIFor(ImageJDatatypeImporter importer) {
        Class<? extends ImageJDatatypeImporterUI> importerClass = getImporterClassFor(importer.getAdapter().getImageJDatatype());
        try {
            return ConstructorUtils.invokeConstructor(importerClass, importer);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
            throw new RuntimeException(e);
        }
    }

    public static ACAQUIImageJDatatypeAdapterRegistry getInstance() {
        return ACAQDefaultRegistry.getInstance().getUIImageJDatatypeAdapterRegistry();
    }
}
