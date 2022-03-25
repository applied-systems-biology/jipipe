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

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.reflect.ConstructorUtils;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.compat.*;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.compat.ImageJDataImporterUI;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.utils.ReflectionUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

/**
 * Registers all known adapters between ImageJ and JIPipe data types
 */
public class JIPipeImageJAdapterRegistry {
    private final BiMap<String, ImageJDataImporter> registeredImporters = HashBiMap.create();
    private final BiMap<String, ImageJDataExporter> registeredExporters = HashBiMap.create();
    private final Map<String, Class<? extends ImageJDataImporterUI>> registeredImporterUIs = new HashMap<>();
    private final Map<String, Class<? extends ImageJDataExporterUI>> registeredExporterUIs = new HashMap<>();
    private final Map<Class<? extends JIPipeData>, Set<ImageJDataImporter>> supportedImporters = new HashMap<>();
    private final Map<Class<? extends JIPipeData>, Set<ImageJDataImporter>> supportedConvertibleImporters = new HashMap<>();
    private final Map<Class<? extends JIPipeData>, Set<ImageJDataExporter>> supportedExporters = new HashMap<>();
    private final Map<Class<? extends JIPipeData>, Set<ImageJDataExporter>> supportedConvertibleExporters = new HashMap<>();
    private final Map<Class<? extends JIPipeData>, ImageJDataImporter> defaultImporters = new HashMap<>();
    private final Map<Class<? extends JIPipeData>, ImageJDataExporter> defaultExporters = new HashMap<>();

    /**
     * Registers an importer
     * @param id unique ID of the importer
     * @param importer the importer instance
     * @param uiClass UI class for the importer. can be null (falls back to {@link DefaultImageJDataImporterUI})
     */
    public void register(String id, ImageJDataImporter importer,  Class<? extends ImageJDataImporterUI> uiClass) {
        registeredImporters.put(id, importer);
        if(uiClass != null) {
            registeredImporterUIs.put(id, uiClass);
        }
    }

    /**
     * Registers an importer
     * @param id unique ID of the importer
     * @param exporter the exporter instance
     * @param uiClass UI class for the exporter. can be null (falls back to {@link DefaultImageJDataExporterUI})
     */
    public void register(String id, ImageJDataExporter exporter, Class<? extends ImageJDataExporterUI> uiClass) {
        registeredExporters.put(id, exporter);
        if(uiClass != null) {
            registeredExporterUIs.put(id, uiClass);
        }
    }

    public String getIdOf(ImageJDataImporter importer) {
        return registeredImporters.inverse().get(importer);
    }

    public String getIdOf(ImageJDataExporter exporter) {
        return registeredExporters.inverse().get(exporter);
    }

    /**
     * Returns all importer instances that
     * @param dataClass the data type to import
     * @param includeConvertible if the list should include importers that convert to the specified data type
     * @return the list of importers
     */
    public Set<ImageJDataImporter> getAvailableImporters(Class<? extends JIPipeData> dataClass, boolean includeConvertible) {
        Set<ImageJDataImporter> result;
        if(!includeConvertible) {
            result = supportedConvertibleImporters.getOrDefault(dataClass, null);
            if(result == null) {
                result = new HashSet<>();
                supportedConvertibleImporters.put(dataClass, result);
                for (ImageJDataImporter importer : registeredImporters.values()) {
                    if(dataClass.isAssignableFrom(importer.getImportedJIPipeDataType())) {
                        result.add(importer);
                    }
                }
            }
        }
        else {
            result = supportedImporters.getOrDefault(dataClass, null);
            if(result == null) {
                result = new HashSet<>();
                supportedImporters.put(dataClass, result);
                for (ImageJDataImporter importer : registeredImporters.values()) {
                    if(dataClass.isAssignableFrom(importer.getImportedJIPipeDataType())) {
                        result.add(importer);
                    }
                    else if(JIPipe.getDataTypes().isConvertible(importer.getImportedJIPipeDataType(), dataClass)) {
                        result.add(importer);
                    }
                }
            }
        }
        return result;
    }

    /**
     * Returns all exporter instances that
     * @param dataClass the data type to exporter
     * @param includeConvertible if the list should include importers that convert to the specified data type
     * @return the list of importers
     */
    public Set<ImageJDataExporter> getAvailableExporters(Class<? extends JIPipeData> dataClass, boolean includeConvertible) {
        Set<ImageJDataExporter> result;
        if(!includeConvertible) {
            result = supportedConvertibleExporters.getOrDefault(dataClass, null);
            if(result == null) {
                result = new HashSet<>();
                supportedConvertibleExporters.put(dataClass, result);
                for (ImageJDataExporter exporter : registeredExporters.values()) {
                    if(exporter.getExportedJIPipeDataType().isAssignableFrom(dataClass)) {
                        result.add(exporter);
                    }
                }
            }
        }
        else {
            result = supportedExporters.getOrDefault(dataClass, null);
            if(result == null) {
                result = new HashSet<>();
                supportedExporters.put(dataClass, result);
                for (ImageJDataExporter exporter : registeredExporters.values()) {
                    if(exporter.getExportedJIPipeDataType().isAssignableFrom(dataClass)) {
                        result.add(exporter);
                    }
                    else if(JIPipe.getDataTypes().isConvertible(dataClass, exporter.getExportedJIPipeDataType())) {
                        result.add(exporter);
                    }
                }
            }
        }
        return result;
    }

    /**
     * Generates a UI for the importer
     *
     *
     * @param workbench the workbench
     * @param importOperation importer
     * @return UI instance
     */
    public ImageJDataImporterUI createUIForImportOperation(JIPipeWorkbench workbench, ImageJDataImportOperation importOperation) {
        Class<? extends ImageJDataImporterUI> importerClass = registeredImporterUIs.get(importOperation.getImporterId());
        if(importerClass == null) {
            importerClass = DefaultImageJDataImporterUI.class;
        }
        try {
            return ConstructorUtils.invokeConstructor(importerClass, workbench, importOperation);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Generates a UI for the importer
     *
     *
     * @param workbench the workbench
     * @param exportOperation importer
     * @return UI instance
     */
    public ImageJDataExporterUI createUIForExportOperation(JIPipeWorkbench workbench, ImageJDataExportOperation exportOperation) {
        Class<? extends ImageJDataExporterUI> exporterClass = registeredExporterUIs.get(exportOperation.getExporterId());
        if(exporterClass == null) {
            exporterClass = DefaultImageJDataExporterUI.class;
        }
        try {
            return ConstructorUtils.invokeConstructor(exporterClass, workbench, exportOperation);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Gets the default importer for a data class
     * @param dataClass the data class
     * @return the importer
     */
    public ImageJDataImporter getDefaultImporterFor(Class<? extends JIPipeData> dataClass) {
        ImageJDataImporter importer = defaultImporters.get(dataClass);
        if(importer != null)
            return importer;
        Set<ImageJDataImporter> available = getAvailableImporters(dataClass, false);
        if(available.isEmpty()) {
            importer = getImporterById(DefaultImageJDataImporter.ID); // the default importer
        }
        else {
            importer = available.stream().min(Comparator.comparing(op -> {
                if (op.getImportedJIPipeDataType() == dataClass)
                    return 0;
                else if (dataClass.isAssignableFrom(op.getImportedJIPipeDataType()))
                    return ReflectionUtils.getClassDistance(dataClass, op.getImportedJIPipeDataType());
                else
                    return Integer.MAX_VALUE;
            })).get();
        }
        defaultImporters.put(dataClass, importer);
        return importer;
    }

    /**
     * Gets the default exporter for a data class
     * @param dataClass the data class
     * @return the exporter
     */
    public ImageJDataExporter getDefaultExporterFor(Class<? extends JIPipeData> dataClass) {
        ImageJDataExporter exporter = defaultExporters.get(dataClass);
        if(exporter != null)
            return exporter;
        Set<ImageJDataExporter> available = getAvailableExporters(dataClass, false);
        if(available.isEmpty()) {
            exporter = getExporterById(DefaultImageJDataExporter.ID); // the default importer
        }
        else {
            exporter = available.stream().min(Comparator.comparing(op -> {
                if (op.getExportedJIPipeDataType() == dataClass)
                    return 0;
                else if (op.getExportedJIPipeDataType().isAssignableFrom(dataClass))
                    return ReflectionUtils.getClassDistance(op.getExportedJIPipeDataType(), dataClass);
                else
                    return Integer.MAX_VALUE;
            })).get();
        }
        defaultExporters.put(dataClass, exporter);
        return exporter;
    }

    public ImageJDataImporter getImporterById(String id) {
        return registeredImporters.get(id);
    }

    public ImageJDataExporter getExporterById(String id) {
        return registeredExporters.get(id);
    }
}
