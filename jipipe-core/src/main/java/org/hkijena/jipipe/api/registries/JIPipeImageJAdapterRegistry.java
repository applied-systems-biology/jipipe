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
import org.hkijena.jipipe.api.JIPipeDefaultDocumentation;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.compat.*;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.compat.ImageJDataImporterUI;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

/**
 * Registers all known adapters between ImageJ and JIPipe data types
 */
public class JIPipeImageJAdapterRegistry {
    private final BiMap<String, ImageJDataImporter> registeredImporters = HashBiMap.create();
    private final BiMap<String, ImageJDataExporter> registeredExporters = HashBiMap.create();
    private final Map<String, Class<? extends ImageJDataImporterUI>> registeredImporterUIs = new HashMap<>();
    private final Map<Class<? extends JIPipeData>, Set<ImageJDataImporter>> supportedImporters = new HashMap<>();
    private final Map<Class<? extends JIPipeData>, Set<ImageJDataImporter>> supportedConvertableImporters = new HashMap<>();
    private final Map<Class<? extends JIPipeData>, Set<ImageJDataExporter>> supportedExporters = new HashMap<>();
    private final Map<Class<? extends JIPipeData>, Set<ImageJDataExporter>> supportedConvertableExporters = new HashMap<>();
    private final Map<String, JIPipeDocumentation> importerDocumentations = new HashMap<>();
    private final Map<String, JIPipeDocumentation> exporterDocumentations = new HashMap<>();

    /**
     * Registers an importer
     * @param id unique ID of the importer
     * @param importer the importer instance
     * @param uiClass UI class for the importer. can be null (falls back to a default UI)
     */
    public void register(String id, ImageJDataImporter importer,  Class<? extends ImageJDataImporterUI> uiClass) {
        registeredImporters.put(id, importer);
        JIPipeDocumentation documentation = importer.getClass().getAnnotation(JIPipeDocumentation.class);
        if(documentation != null) {
            importerDocumentations.put(id, documentation);
        }
        if(uiClass != null) {
            registeredImporterUIs.put(id, uiClass);
        }
    }

    /**
     * Registers an importer
     * @param id unique ID of the importer
     * @param exporter the exporter instance
     */
    public void register(String id, ImageJDataExporter exporter) {
        JIPipeDocumentation documentation = exporter.getClass().getAnnotation(JIPipeDocumentation.class);
        if(documentation != null) {
            exporterDocumentations.put(id, documentation);
        }
        registeredExporters.put(id, exporter);
    }

    public JIPipeDocumentation getDocumentation(ImageJDataImporter importer) {
        String id = getIdOf(importer);
        JIPipeDocumentation documentation = importerDocumentations.getOrDefault(id, null);
        if(documentation == null) {
            documentation = new JIPipeDefaultDocumentation(importer.getClass().getName(), "");
            importerDocumentations.put(id, documentation);
        }
        return documentation;
    }

    public JIPipeDocumentation getDocumentation(ImageJDataExporter exporter) {
        String id = getIdOf(exporter);
        JIPipeDocumentation documentation = exporterDocumentations.getOrDefault(id, null);
        if(documentation == null) {
            documentation = new JIPipeDefaultDocumentation(exporter.getClass().getName(), "");
            exporterDocumentations.put(id, documentation);
        }
        return documentation;
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
     * @param includeConvertable if the list should include importers that convert to the specified data type
     * @return the list of importers
     */
    public Set<ImageJDataImporter> getAvailableImporters(Class<? extends JIPipeData> dataClass, boolean includeConvertable) {
        Set<ImageJDataImporter> result;
        if(includeConvertable) {
            result = supportedConvertableImporters.getOrDefault(dataClass, null);
            if(result == null) {
                result = new HashSet<>();
                supportedConvertableImporters.put(dataClass, result);
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
     * @param includeConvertable if the list should include importers that convert to the specified data type
     * @return the list of importers
     */
    public Set<ImageJDataExporter> getAvailableExporters(Class<? extends JIPipeData> dataClass, boolean includeConvertable) {
        Set<ImageJDataExporter> result;
        if(includeConvertable) {
            result = supportedConvertableExporters.getOrDefault(dataClass, null);
            if(result == null) {
                result = new HashSet<>();
                supportedConvertableExporters.put(dataClass, result);
                for (ImageJDataExporter exporter : registeredExporters.values()) {
                    if(dataClass.isAssignableFrom(exporter.getExportedJIPipeDataType())) {
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
                    if(dataClass.isAssignableFrom(exporter.getExportedJIPipeDataType())) {
                        result.add(exporter);
                    }
                    else if(JIPipe.getDataTypes().isConvertible(exporter.getExportedJIPipeDataType(), dataClass)) {
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
     * @param importOperation importer
     * @return UI instance
     */
    public ImageJDataImporterUI createUIForImportOperation(ImageJDataImportOperation importOperation) {
        Class<? extends ImageJDataImporterUI> importerClass = registeredImporterUIs.get(importOperation.getImporterId());
        if(importerClass == null) {
            importerClass = DefaultImageJDataImporterUI.class;
        }
        try {
            return ConstructorUtils.invokeConstructor(importerClass, importOperation);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
            throw new RuntimeException(e);
        }
    }

    public ImageJDataImporter getDefaultImporterFor(Class<? extends JIPipeData> dataClass) {
        Set<ImageJDataImporter> available = getAvailableImporters(dataClass, false);
        if(available.isEmpty()) {
            return getImporterById(DefaultImageJDataImporter.ID); // the default importer
        }
        return available.iterator().next();
    }

    public ImageJDataExporter getDefaultExporterFor(Class<? extends JIPipeData> dataClass) {
        Set<ImageJDataExporter> available = getAvailableExporters(dataClass, false);
        if(available.isEmpty()) {
            return getExporterById(DefaultImageJDataExporter.ID); // the default importer
        }
        return available.iterator().next();
    }

    public ImageJDataImporter getImporterById(String id) {
        return registeredImporters.get(id);
    }

    public ImageJDataExporter getExporterById(String id) {
        return registeredExporters.get(id);
    }
}
