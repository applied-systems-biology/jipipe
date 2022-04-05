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
import org.apache.commons.lang3.reflect.ConstructorUtils;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.compat.*;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.ui.JIPipeWorkbench;

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
    private final JIPipe jiPipe;

    public JIPipeImageJAdapterRegistry(JIPipe jiPipe) {

        this.jiPipe = jiPipe;
    }

    /**
     * Registers an importer
     *
     * @param id       unique ID of the importer
     * @param importer the importer instance
     * @param uiClass  UI class for the importer. can be null (falls back to {@link DefaultImageJDataImporterUI})
     */
    public void register(String id, ImageJDataImporter importer, Class<? extends ImageJDataImporterUI> uiClass) {
        registeredImporters.put(id, importer);
        if (uiClass != null) {
            registeredImporterUIs.put(id, uiClass);
        }
        getJIPipe().getProgressInfo().log("Registered ImageJ importer id=" + id + " object=" + importer + " ui=" + uiClass);
    }

    /**
     * Registers an importer
     *
     * @param id       unique ID of the importer
     * @param exporter the exporter instance
     * @param uiClass  UI class for the exporter. can be null (falls back to {@link DefaultImageJDataExporterUI})
     */
    public void register(String id, ImageJDataExporter exporter, Class<? extends ImageJDataExporterUI> uiClass) {
        registeredExporters.put(id, exporter);
        if (uiClass != null) {
            registeredExporterUIs.put(id, uiClass);
        }
        getJIPipe().getProgressInfo().log("Registered ImageJ exporter id=" + id + " object=" + exporter + " ui=" + uiClass);
    }

    public String getIdOf(ImageJDataImporter importer) {
        return registeredImporters.inverse().get(importer);
    }

    public String getIdOf(ImageJDataExporter exporter) {
        return registeredExporters.inverse().get(exporter);
    }

    /**
     * Returns all importer instances that
     *
     * @param dataClass          the data type to import
     * @param includeConvertible if the list should include importers that convert to the specified data type
     * @return the list of importers
     */
    public Set<ImageJDataImporter> getAvailableImporters(Class<? extends JIPipeData> dataClass, boolean includeConvertible) {
        Set<ImageJDataImporter> result;
        if (!includeConvertible) {
            result = supportedConvertibleImporters.getOrDefault(dataClass, null);
            if (result == null) {
                result = new HashSet<>();
                supportedConvertibleImporters.put(dataClass, result);
                for (ImageJDataImporter importer : registeredImporters.values()) {
                    if (dataClass.isAssignableFrom(importer.getImportedJIPipeDataType())) {
                        result.add(importer);
                    }
                }
            }
        } else {
            result = supportedImporters.getOrDefault(dataClass, null);
            if (result == null) {
                result = new HashSet<>();
                supportedImporters.put(dataClass, result);
                for (ImageJDataImporter importer : registeredImporters.values()) {
                    if (dataClass.isAssignableFrom(importer.getImportedJIPipeDataType())) {
                        result.add(importer);
                    } else if (JIPipe.getDataTypes().isConvertible(importer.getImportedJIPipeDataType(), dataClass)) {
                        result.add(importer);
                    }
                }
            }
        }
        return result;
    }

    /**
     * Returns all exporter instances that
     *
     * @param dataClass          the data type to exporter
     * @param includeConvertible if the list should include importers that convert to the specified data type
     * @return the list of importers
     */
    public Set<ImageJDataExporter> getAvailableExporters(Class<? extends JIPipeData> dataClass, boolean includeConvertible) {
        Set<ImageJDataExporter> result;
        if (!includeConvertible) {
            result = supportedConvertibleExporters.getOrDefault(dataClass, null);
            if (result == null) {
                result = new HashSet<>();
                supportedConvertibleExporters.put(dataClass, result);
                for (ImageJDataExporter exporter : registeredExporters.values()) {
                    if (exporter.getExportedJIPipeDataType().isAssignableFrom(dataClass)) {
                        result.add(exporter);
                    }
                }
            }
        } else {
            result = supportedExporters.getOrDefault(dataClass, null);
            if (result == null) {
                result = new HashSet<>();
                supportedExporters.put(dataClass, result);
                for (ImageJDataExporter exporter : registeredExporters.values()) {
                    if (exporter.getExportedJIPipeDataType().isAssignableFrom(dataClass)) {
                        result.add(exporter);
                    } else if (JIPipe.getDataTypes().isConvertible(dataClass, exporter.getExportedJIPipeDataType())) {
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
     * @param workbench       the workbench
     * @param importOperation importer
     * @return UI instance
     */
    public ImageJDataImporterUI createUIForImportOperation(JIPipeWorkbench workbench, ImageJDataImportOperation importOperation) {
        Class<? extends ImageJDataImporterUI> importerClass = registeredImporterUIs.get(importOperation.getImporterId());
        if (importerClass == null) {
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
     * @param workbench       the workbench
     * @param exportOperation importer
     * @return UI instance
     */
    public ImageJDataExporterUI createUIForExportOperation(JIPipeWorkbench workbench, ImageJDataExportOperation exportOperation) {
        Class<? extends ImageJDataExporterUI> exporterClass = registeredExporterUIs.get(exportOperation.getExporterId());
        if (exporterClass == null) {
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
     *
     * @param dataClass the data class
     * @return the importer
     */
    public ImageJDataImporter getDefaultImporterFor(Class<? extends JIPipeData> dataClass) {
        ImageJDataImporter importer = defaultImporters.get(dataClass);
        if (importer != null)
            return importer;
        Set<ImageJDataImporter> available = getAvailableImporters(dataClass, true);
        if (available.isEmpty()) {
            importer = getImporterById(DataTableImageJDataImporter.ID); // the default importer
        } else {
            JIPipeDatatypeRegistry datatypeRegistry = JIPipe.getDataTypes();
            importer = available.stream().min(Comparator.comparing(op -> {
                int conversionDistance = datatypeRegistry.getConversionDistance(op.getImportedJIPipeDataType(), dataClass);
                if (conversionDistance < 0)
                    conversionDistance = Integer.MAX_VALUE;
                return conversionDistance;
            })).get();
        }
        if (importer instanceof EmptyImageJDataImporter) {
            importer = getImporterById(DataTableImageJDataExporter.ID);
        }
        defaultImporters.put(dataClass, importer);
        return importer;
    }

    /**
     * Sets the default exporter for a data type
     *
     * @param dataClass the data type
     * @param id        the ID of the exporter
     */
    public void setDefaultExporterFor(Class<? extends JIPipeData> dataClass, String id) {
        defaultExporters.put(dataClass, getExporterById(id));
    }

    /**
     * Sets the default importer for a data type
     *
     * @param dataClass the data type
     * @param id        the ID of the exporter
     */
    public void setDefaultImporterFor(Class<? extends JIPipeData> dataClass, String id) {
        defaultImporters.put(dataClass, getImporterById(id));
    }

    /**
     * Gets the default exporter for a data class
     *
     * @param dataClass the data class
     * @return the exporter
     */
    public ImageJDataExporter getDefaultExporterFor(Class<? extends JIPipeData> dataClass) {
        ImageJDataExporter exporter = defaultExporters.get(dataClass);
        if (exporter != null)
            return exporter;
        Set<ImageJDataExporter> available = getAvailableExporters(dataClass, true);
        if (available.isEmpty()) {
            exporter = getExporterById(DataTableImageJDataExporter.ID); // the default importer
        } else {
            JIPipeDatatypeRegistry datatypeRegistry = JIPipe.getDataTypes();
            exporter = available.stream().min(Comparator.comparing(op -> {
                int conversionDistance = datatypeRegistry.getConversionDistance(dataClass, op.getExportedJIPipeDataType());
                if (conversionDistance < 0)
                    conversionDistance = Integer.MAX_VALUE;
                return conversionDistance;
            })).get();
        }
        if (exporter instanceof EmptyImageJDataExporter) {
            exporter = getExporterById(DataTableImageJDataExporter.ID);
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

    public JIPipe getJIPipe() {
        return jiPipe;
    }
}
