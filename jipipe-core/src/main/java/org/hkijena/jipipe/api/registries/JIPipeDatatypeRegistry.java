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
import org.hkijena.jipipe.JIPipeDependency;
import org.hkijena.jipipe.JIPipeService;
import org.hkijena.jipipe.api.JIPipeHidden;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.*;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntry;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntryLevel;
import org.hkijena.jipipe.api.validation.JIPipeValidationRuntimeException;
import org.hkijena.jipipe.api.validation.contexts.CustomValidationReportContext;
import org.hkijena.jipipe.extensions.settings.GeneralDataSettings;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbench;
import org.hkijena.jipipe.ui.resultanalysis.JIPipeDefaultResultDataSlotPreview;
import org.hkijena.jipipe.ui.resultanalysis.JIPipeDefaultResultDataSlotRowUI;
import org.hkijena.jipipe.ui.resultanalysis.JIPipeResultDataSlotPreview;
import org.hkijena.jipipe.ui.resultanalysis.JIPipeResultDataSlotRowUI;
import org.hkijena.jipipe.utils.ReflectionUtils;
import org.hkijena.jipipe.utils.UIUtils;
import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.shortestpath.DijkstraShortestPath;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultWeightedEdge;

import javax.swing.*;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.*;
import java.util.function.Predicate;

/**
 * Contains known {@link JIPipeData} types, and associates them to their respective {@link JIPipeDataSlot}.
 */
public class JIPipeDatatypeRegistry {
    private final BiMap<String, Class<? extends JIPipeData>> registeredDataTypes = HashBiMap.create();
    private final Map<String, Map<String, JIPipeDataDisplayOperation>> registeredDisplayOperations = new HashMap<>();
    private final Map<String, Map<String, JIPipeDataImportOperation>> registeredImportOperations = new HashMap<>();
    private final Map<Class<? extends JIPipeData>, URL> iconsURLs = new HashMap<>();
    private final Map<Class<? extends JIPipeData>, ImageIcon> iconInstances = new HashMap<>();
    private final Map<Class<? extends JIPipeData>, Class<? extends JIPipeResultDataSlotRowUI>> resultUIs = new HashMap<>();
    private final Map<Class<? extends JIPipeData>, Class<? extends JIPipeResultDataSlotPreview>> resultTableCellUIs = new HashMap<>();
    private final Set<String> hiddenDataTypeIds = new HashSet<>();
    private final Map<String, JIPipeDependency> registeredDatatypeSources = new HashMap<>();
    private final Graph<JIPipeDataInfo, DataConverterEdge> conversionGraph = new DefaultDirectedGraph<>(DataConverterEdge.class);
    private final DijkstraShortestPath<JIPipeDataInfo, DataConverterEdge> shortestPath = new DijkstraShortestPath<>(conversionGraph);
    private final URL defaultIconURL;
    private final ImageIcon defaultIcon;
    private final JIPipe jiPipe;

    /**
     * Creates a new instance
     *
     * @param jiPipe the JIPipe instance
     */
    public JIPipeDatatypeRegistry(JIPipe jiPipe) {
        this.jiPipe = jiPipe;
        this.defaultIconURL = UIUtils.getIconURLFromResources("data-types/data-type.png");
        this.defaultIcon = UIUtils.getIconFromResources("data-types/data-type.png");
    }

    /**
     * Returns true if the input data type can be trivially converted into the output data type.
     * A trivial conversion is applied when the input data is the same as the output data type or inherits from it.
     *
     * @param inputDataType  the input data type
     * @param outputDataType the output data type
     * @return if the output data type can be assigned from the input data type without any explicit conversion rules
     */
    public static boolean isTriviallyConvertible(Class<? extends JIPipeData> inputDataType, Class<? extends JIPipeData> outputDataType) {
        return outputDataType.isAssignableFrom(inputDataType);
    }

    /**
     * Returns all {@link JIPipeData} classes that satisfy the provided interfaces
     *
     * @param interfaces the list of interfaces. if empty, all data types are returned
     * @return list of data types
     */
    public List<Class<? extends JIPipeData>> findDataTypesByInterfaces(Class<?>... interfaces) {
        List<Class<? extends JIPipeData>> result = new ArrayList<>();
        for (Class<? extends JIPipeData> value : this.registeredDataTypes.values()) {
            boolean success = true;
            for (Class<?> aClass : interfaces) {
                if (!aClass.isAssignableFrom(value)) {
                    success = false;
                    break;
                }
            }
            if (success) {
                result.add(value);
            }
        }
        return result;
    }

    /**
     * Returns all {@link JIPipeData} classes that satisfy the provided predicate
     *
     * @param predicate the predicate
     * @return list of data types
     */
    public List<Class<? extends JIPipeData>> findDataTypes(Predicate<Class<?>> predicate) {
        List<Class<? extends JIPipeData>> result = new ArrayList<>();
        for (Class<? extends JIPipeData> value : this.registeredDataTypes.values()) {
            if (predicate.test(value)) {
                result.add(value);
            }
        }
        return result;
    }

    /**
     * Registers a data converter that allows implicit conversion between data types
     *
     * @param converter the converter
     */
    public void registerConversion(JIPipeDataConverter converter) {
        if (!isRegistered(converter.getInputType())) {
            conversionGraph.addVertex(JIPipeDataInfo.getInstance(converter.getInputType()));
        }
        if (!isRegistered(converter.getOutputType())) {
            conversionGraph.addVertex(JIPipeDataInfo.getInstance(converter.getOutputType()));
        }
        conversionGraph.addEdge(JIPipeDataInfo.getInstance(converter.getInputType()),
                JIPipeDataInfo.getInstance(converter.getOutputType()));
        conversionGraph.getEdge(JIPipeDataInfo.getInstance(converter.getInputType()),
                JIPipeDataInfo.getInstance(converter.getOutputType())).setConverter(converter);
        getJIPipe().getProgressInfo().log("Registered data type conversion from" + converter.getInputType() + " to " + converter.getOutputType());
    }

    public boolean isRegistered(Class<? extends JIPipeData> klass) {
        return registeredDataTypes.containsValue(klass);
    }

    /**
     * Converts the input data to the output data type
     *
     * @param inputData      the input data
     * @param outputDataType the output data type
     * @param progressInfo   the progress info
     * @return the converted input data. Throws an exception if conversion is not possible
     */
    public <T extends JIPipeData> T convert(JIPipeData inputData, Class<T> outputDataType, JIPipeProgressInfo progressInfo) {
        if (isTriviallyConvertible(inputData.getClass(), outputDataType))
            return (T) inputData;
        else {
            GraphPath<JIPipeDataInfo, DataConverterEdge> path = shortestPath.getPath(JIPipeDataInfo.getInstance(inputData.getClass()), JIPipeDataInfo.getInstance(outputDataType));
            if (path == null) {
                throw new JIPipeValidationRuntimeException(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Error, new CustomValidationReportContext("Data type conversion"),
                        "Could not convert " + inputData.getClass() + " to " + outputDataType,
                        "Unable to convert data type!",
                        "An algorithm requested that the data of type '" + JIPipeData.getNameOf(inputData.getClass()) + "' should be converted to type '" + JIPipeData.getNameOf(outputDataType) + "'." +
                                " There no available conversion function.",
                        "Please check if the input data has the correct format by using the quick run. If you cannot resolve the issue, please contact the plugin or JIPipe authors."));
            }
            JIPipeData data = inputData;
            for (DataConverterEdge edge : path.getEdgeList()) {
                if (edge.getConverter() != null) {
                    data = edge.getConverter().convert(data, progressInfo);
                }
            }
            assert outputDataType.isAssignableFrom(data.getClass());
            return (T) data;
        }
    }

    /**
     * Returns the number of conversion steps.
     * Returns 0 if no conversion is needed.
     * Returns -1 if no conversion is possible.
     *
     * @param inputDataType  the input data type
     * @param outputDataType the output data type
     * @return number of conversion steps
     */
    public int getConversionDistance(Class<? extends JIPipeData> inputDataType, Class<? extends JIPipeData> outputDataType) {
        if (inputDataType == outputDataType) {
            return 0;
        } else if (isTriviallyConvertible(inputDataType, outputDataType)) {
            return ReflectionUtils.getClassDistance(outputDataType, inputDataType);
        } else {
            GraphPath<JIPipeDataInfo, DataConverterEdge> path = shortestPath.getPath(JIPipeDataInfo.getInstance(inputDataType), JIPipeDataInfo.getInstance(outputDataType));
            if (path == null)
                return -1;
            return path.getLength();
        }
    }

    /**
     * Returns true if the input data type can be converted into the output data type.
     * Returns true if both data types are the same or trivially convertible (inheritance)
     * Returns true if the input is {@link JIPipeData}
     *
     * @param inputDataType  the input data type
     * @param outputDataType the output data type
     * @return if the types are convertible
     */
    public boolean isConvertible(Class<? extends JIPipeData> inputDataType, Class<? extends JIPipeData> outputDataType) {
        if (inputDataType == JIPipeData.class)
            return true;
        if (isTriviallyConvertible(inputDataType, outputDataType)) {
            return true;
        } else {
            return shortestPath.getPath(JIPipeDataInfo.getInstance(inputDataType), JIPipeDataInfo.getInstance(outputDataType)) != null;
        }
    }

    /**
     * Registers a data type
     *
     * @param id     The datatype ID
     * @param klass  The data class
     * @param source The dependency that registers the data
     */
    public void register(String id, Class<? extends JIPipeData> klass, JIPipeDependency source) {

        if(registeredDataTypes.containsKey(id) && klass != registeredDataTypes.get(id)) {
            throw new IllegalArgumentException("CONFLICTING DATA TYPE ID REGISTRATION!!! Data type ID=" + id + " already exists and is assigned to " + registeredDataTypes.get(id) + ", but " + source + " tried to register the ID to " + klass);
        }

        registeredDataTypes.put(id, klass);
        registeredDatatypeSources.put(id, source);
        if (klass.getAnnotationsByType(JIPipeHidden.class).length > 0)
            hiddenDataTypeIds.add(id);

        JIPipeDataInfo info = JIPipeDataInfo.getInstance(klass);
        if (!conversionGraph.containsVertex(info))
            conversionGraph.addVertex(info);
        conversionGraph.addEdge(info, info);
        for (Class<? extends JIPipeData> otherClass : registeredDataTypes.values()) {
            JIPipeDataInfo otherInfo = JIPipeDataInfo.getInstance(otherClass);
            if (isTriviallyConvertible(info.getDataClass(), otherClass)) {
                conversionGraph.addEdge(info, otherInfo);
            }
            if (isTriviallyConvertible(otherClass, info.getDataClass())) {
                conversionGraph.addEdge(otherInfo, info);
            }
        }

        jiPipe.getDatatypeRegisteredEventEmitter().emit(new JIPipeService.DatatypeRegisteredEvent(jiPipe, id));
        getJIPipe().getProgressInfo().log("Registered data type id=" + id + " of class " + klass);
    }

    /**
     * Registers an operation that will be available to users in the results view (after runs).
     *
     * @param dataTypeId data type id. if empty, the operation applies to all data types.
     * @param operation  the operation
     */
    public void registerImportOperation(String dataTypeId, JIPipeDataImportOperation operation) {
        Map<String, JIPipeDataImportOperation> existing = registeredImportOperations.getOrDefault(dataTypeId, null);
        if (existing == null) {
            existing = new HashMap<>();
            registeredImportOperations.put(dataTypeId, existing);
        }
        if (existing.containsKey(operation.getId()))
            throw new RuntimeException("Import operation with ID '" + operation.getId() + "' already exists in data type '" + dataTypeId + "'");
        existing.put(operation.getId(), operation);
        getJIPipe().getProgressInfo().log("Registered data import operation id=" + operation.getId() + " for data type " + dataTypeId);
    }

    /**
     * Registers an operation that will be available to users in the cache view
     *
     * @param dataTypeId data type id. if empty, the operation applies to all data types.
     * @param operation  the operation
     */
    public void registerDisplayOperation(String dataTypeId, JIPipeDataDisplayOperation operation) {
        Map<String, JIPipeDataDisplayOperation> existing = registeredDisplayOperations.getOrDefault(dataTypeId, null);
        if (existing == null) {
            existing = new HashMap<>();
            registeredDisplayOperations.put(dataTypeId, existing);
        }
        if (existing.containsKey(operation.getId()))
            throw new RuntimeException("Display operation with ID '" + operation.getId() + "' already exists in data type '" + dataTypeId + "'");
        existing.put(operation.getId(), operation);
        getJIPipe().getProgressInfo().log("Registered data display operation id=" + operation.getId() + " for data type " + dataTypeId);
    }

    public Map<String, JIPipeDataDisplayOperation> getAllRegisteredDisplayOperations(String dataTypeId) {
        Map<String, JIPipeDataDisplayOperation> result = new HashMap<>(registeredDisplayOperations.getOrDefault(dataTypeId, Collections.emptyMap()));
        result.putAll(registeredDisplayOperations.getOrDefault("", Collections.emptyMap()));
        return result;
    }

    public Map<String, JIPipeDataImportOperation> getAllRegisteredImportOperations(String dataTypeId) {
        Map<String, JIPipeDataImportOperation> result = new HashMap<>(registeredImportOperations.getOrDefault(dataTypeId, Collections.emptyMap()));
        result.putAll(registeredImportOperations.getOrDefault("", Collections.emptyMap()));
        return result;
    }

    /**
     * Gets all import operations for a data type ID
     *
     * @param id the id
     * @return list of import operations
     */
    public List<JIPipeDataImportOperation> getSortedImportOperationsFor(String id) {
        List<JIPipeDataImportOperation> result = new ArrayList<>(registeredImportOperations.getOrDefault(id, Collections.emptyMap()).values());
        result.addAll(registeredImportOperations.getOrDefault("", Collections.emptyMap()).values());
        result.sort(Comparator.comparing(JIPipeDataImportOperation::getOrder));
        return result;
    }

    /**
     * Gets all import operations for a data type ID
     *
     * @param id the id
     * @return list of import operations
     */
    public List<JIPipeDataDisplayOperation> getSortedDisplayOperationsFor(String id) {
        List<JIPipeDataDisplayOperation> result = new ArrayList<>(registeredDisplayOperations.getOrDefault(id, Collections.emptyMap()).values());
        result.addAll(registeredDisplayOperations.getOrDefault("", Collections.emptyMap()).values());
        result.sort(Comparator.comparing(JIPipeDataDisplayOperation::getOrder));
        return result;
    }

    /**
     * Gets all registered data types
     *
     * @return Map from data type ID to data class
     */
    public Map<String, Class<? extends JIPipeData>> getRegisteredDataTypes() {
        return Collections.unmodifiableMap(registeredDataTypes);
    }

    /**
     * Gets all data types that are not hidden
     *
     * @return Map from data type ID to data class
     */
    public Map<String, Class<? extends JIPipeData>> getUnhiddenRegisteredDataTypes() {
        Map<String, Class<? extends JIPipeData>> result = new HashMap<>();
        for (Map.Entry<String, Class<? extends JIPipeData>> entry : registeredDataTypes.entrySet()) {
            if (!hiddenDataTypeIds.contains(entry.getKey())) {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        return result;
    }

    /**
     * Gets the ID of the data type
     *
     * @param dataClass The data class
     * @return The data type ID of the class
     */
    public String getIdOf(Class<? extends JIPipeData> dataClass) {
        return registeredDataTypes.inverse().get(dataClass);
    }

    /**
     * Returns true if there is a data type with given ID
     *
     * @param id The data type ID
     * @return True if the data type ID exists
     */
    public boolean hasDatatypeWithId(String id) {
        return registeredDataTypes.containsKey(id);
    }

    /**
     * Returns true if the data type with given ID is hidden
     *
     * @param id The data type ID
     * @return True if the data type is hidden
     */
    public boolean isHidden(String id) {
        return hiddenDataTypeIds.contains(id);
    }

    /**
     * Returns the data class with specified ID
     *
     * @param id The data type ID
     * @return The data class associated to the ID
     */
    public Class<? extends JIPipeData> getById(String id) {
        Class<? extends JIPipeData> klass = registeredDataTypes.getOrDefault(id, null);
        if (klass == null) {
            throw new JIPipeValidationRuntimeException(new NullPointerException("Could not find data type with id '" + id + "' in " +
                    String.join(", ", registeredDataTypes.keySet())),
                    "Unable to find an data type!",
                    "A project or extension requires an data type '" + id + "'. It could not be found.",
                    "Check if JIPipe is up-to-date and the newest version of all plugins are installed. If you know that a data type was assigned a new ID, " +
                            "search for '" + id + "' in the JSON file and replace it with the new identifier.");
        }
        return klass;
    }

    /**
     * Gets the registered data types, grouped by their menu paths
     *
     * @return Map from menu path to data types with this menu path
     */
    public Map<String, Set<Class<? extends JIPipeData>>> getDataTypesByMenuPaths() {
        return JIPipeData.groupByMenuPath(registeredDataTypes.values());
    }

    /**
     * Returns the source that registered that data type
     *
     * @param id Data type id
     * @return Dependency that registered the data type
     */
    public JIPipeDependency getSourceOf(String id) {
        return registeredDatatypeSources.getOrDefault(id, null);
    }

    /**
     * Returns the source that registered that data type
     *
     * @param dataClass The data class
     * @return Dependency that registered the data type
     */
    public JIPipeDependency getSourceOf(Class<? extends JIPipeData> dataClass) {
        return getSourceOf(getIdOf(dataClass));
    }

    /**
     * Returns true if the specified data type class is registered
     *
     * @param dataClass The data class
     * @return True if the data class is registered
     */
    public boolean hasDataType(Class<? extends JIPipeData> dataClass) {
        return registeredDataTypes.containsValue(dataClass);
    }

    /**
     * Returns all data infos added by the dependency
     *
     * @param dependency The dependency
     * @return Set of data infos registered by the dependency
     */
    public Set<JIPipeDataInfo> getDeclaredBy(JIPipeDependency dependency) {
        Set<JIPipeDataInfo> result = new HashSet<>();
        for (Map.Entry<String, Class<? extends JIPipeData>> entry : registeredDataTypes.entrySet()) {
            JIPipeDependency source = getSourceOf(entry.getKey());
            if (source == dependency)
                result.add(JIPipeDataInfo.getInstance(entry.getValue()));
        }
        return result;
    }

    /**
     * Registers a custom icon for a datatype
     *
     * @param klass        data class
     * @param resourcePath icon resource
     */
    public void registerIcon(Class<? extends JIPipeData> klass, URL resourcePath) {
        iconsURLs.put(klass, resourcePath);
        iconInstances.put(klass, new ImageIcon(resourcePath));
    }

    /**
     * Registers a custom UI for a result data slot
     *
     * @param klass   data class
     * @param uiClass slot ui
     */
    public void registerResultSlotUI(Class<? extends JIPipeData> klass, Class<? extends JIPipeResultDataSlotRowUI> uiClass) {
        resultUIs.put(klass, uiClass);
        getJIPipe().getProgressInfo().log("Registered result slot UI for data type " + klass + " UIClass=" + uiClass);
    }

    /**
     * Registers a custom renderer for the data displayed in the dataslot result table
     *
     * @param klass    data class
     * @param renderer cell renderer
     */
    public void registerResultTableCellUI(Class<? extends JIPipeData> klass, Class<? extends JIPipeResultDataSlotPreview> renderer) {
        resultTableCellUIs.put(klass, renderer);
        getJIPipe().getProgressInfo().log("Registered result table cell UI for data type " + klass + " RendererClass=" + renderer);
    }

    /**
     * Returns the icon for a datatype
     *
     * @param klass data class
     * @return icon instance
     */
    public ImageIcon getIconFor(Class<? extends JIPipeData> klass) {
        return iconInstances.getOrDefault(klass, defaultIcon);
    }

    /**
     * Generates a UI for a result data slot
     *
     * @param workbenchUI workbench UI
     * @param slot        data slot
     * @param row         table row
     * @return slot UI
     */
    public JIPipeResultDataSlotRowUI getUIForResultSlot(JIPipeProjectWorkbench workbenchUI, JIPipeDataSlot slot, JIPipeDataTableMetadataRow row) {
        Class<? extends JIPipeData> dataClass = getById(row.getTrueDataType());
        Class<? extends JIPipeResultDataSlotRowUI> uiClass = resultUIs.getOrDefault(dataClass, null);
        if (uiClass != null) {
            try {
                return ConstructorUtils.getMatchingAccessibleConstructor(uiClass, JIPipeProjectWorkbench.class, JIPipeDataSlot.class, JIPipeDataTableMetadataRow.class)
                        .newInstance(workbenchUI, slot, row);
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        } else {
            return new JIPipeDefaultResultDataSlotRowUI(workbenchUI, slot, row);
        }
    }

    /**
     * Returns a cell renderer for dataslot result table
     *
     * @param workbench      the workbench
     * @param table          the table that owns the renderer
     * @param slot           the slot
     * @param row            the data row
     * @param dataAnnotation the data annotation (optional)
     * @return cell renderer
     */
    public JIPipeResultDataSlotPreview getCellRendererFor(JIPipeProjectWorkbench workbench, JTable table, JIPipeDataSlot slot, JIPipeDataTableMetadataRow row, JIPipeExportedDataAnnotation dataAnnotation) {
        Class<? extends JIPipeData> dataClass = getById(row.getTrueDataType());
        if (GeneralDataSettings.getInstance().isGenerateResultPreviews()) {
            Class<? extends JIPipeResultDataSlotPreview> rendererClass = resultTableCellUIs.getOrDefault(dataClass, null);
            if (rendererClass != null) {
                try {
                    return rendererClass.getConstructor(JIPipeProjectWorkbench.class, JTable.class, JIPipeDataSlot.class, JIPipeDataTableMetadataRow.class, JIPipeExportedDataAnnotation.class)
                            .newInstance(workbench, table, slot, row, dataAnnotation);
                } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                         NoSuchMethodException e) {
                    throw new RuntimeException(e);
                }
            } else {
                return new JIPipeDefaultResultDataSlotPreview(workbench, table, slot, row, dataAnnotation);
            }
        } else {
            return new JIPipeDefaultResultDataSlotPreview(workbench, table, slot, row, dataAnnotation);
        }
    }

    /**
     * @param klass data class
     * @return icon resource
     */
    public URL getIconURLFor(Class<? extends JIPipeData> klass) {
        return iconsURLs.getOrDefault(klass, defaultIconURL);
    }

    /**
     * @param info data info
     * @return icon resource
     */
    public URL getIconURLFor(JIPipeDataInfo info) {
        return getIconURLFor(info.getDataClass());
    }

    public JIPipe getJIPipe() {
        return jiPipe;
    }

    /**
     * Converts all registered {@link JIPipeDataDisplayOperation} entries into {@link JIPipeDataImportOperation}
     */
    public void convertDisplayOperationsToImportOperations() {
        for (Map.Entry<String, Map<String, JIPipeDataDisplayOperation>> dataTypeEntry : registeredDisplayOperations.entrySet()) {
            String dataTypeId = dataTypeEntry.getKey();
            for (Map.Entry<String, JIPipeDataDisplayOperation> entry : dataTypeEntry.getValue().entrySet()) {
                JIPipeDataDisplayWrapperImportOperation operation = new JIPipeDataDisplayWrapperImportOperation(entry.getValue());
                registerImportOperation(dataTypeId, operation);
            }
        }
    }

    /**
     * Edge between {@link JIPipeDataInfo} instances that indicate a conversion
     */
    public static class DataConverterEdge extends DefaultWeightedEdge {
        private JIPipeDataConverter converter;

        /**
         * The converter if the data types cannot be trivially converted
         *
         * @return converter or null
         */
        public JIPipeDataConverter getConverter() {
            return converter;
        }

        public void setConverter(JIPipeDataConverter converter) {
            this.converter = converter;
        }
    }
}
