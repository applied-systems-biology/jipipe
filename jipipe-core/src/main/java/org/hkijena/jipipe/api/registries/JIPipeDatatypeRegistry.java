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
import com.google.common.eventbus.EventBus;
import org.hkijena.jipipe.JIPipeDefaultRegistry;
import org.hkijena.jipipe.JIPipeDependency;
import org.hkijena.jipipe.api.JIPipeHidden;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataConverter;
import org.hkijena.jipipe.api.data.JIPipeDataInfo;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.events.DatatypeRegisteredEvent;
import org.hkijena.jipipe.api.exceptions.UserFriendlyRuntimeException;

import java.util.*;

/**
 * Contains known {@link JIPipeData} types, and associates them to their respective {@link JIPipeDataSlot}.
 */
public class JIPipeDatatypeRegistry {
    private BiMap<String, Class<? extends JIPipeData>> registeredDataTypes = HashBiMap.create();
    private Set<String> hiddenDataTypeIds = new HashSet<>();
    private Map<String, JIPipeDependency> registeredDatatypeSources = new HashMap<>();
    private Map<Class<? extends JIPipeData>, Map<Class<? extends JIPipeData>, JIPipeDataConverter>> registeredConverters = new HashMap<>();
    private EventBus eventBus = new EventBus();

    /**
     * Creates a new instance
     */
    public JIPipeDatatypeRegistry() {

    }

    /**
     * Registers a data converter that allows implicit conversion between data types
     *
     * @param converter the converter
     */
    public void registerConversion(JIPipeDataConverter converter) {
        Map<Class<? extends JIPipeData>, JIPipeDataConverter> targetMap = registeredConverters.getOrDefault(converter.getInputType(), null);
        if (targetMap == null) {
            targetMap = new HashMap<>();
            registeredConverters.put(converter.getInputType(), targetMap);
        }
        targetMap.put(converter.getOutputType(), converter);
    }

    /**
     * Converts the input data to the output data type
     *
     * @param inputData      the input data
     * @param outputDataType the output data type
     * @return the converted input data. Throws an exception if conversion is not possible
     */
    public JIPipeData convert(JIPipeData inputData, Class<? extends JIPipeData> outputDataType) {
        if (isTriviallyConvertible(inputData.getClass(), outputDataType))
            return inputData;
        else {
            Map<Class<? extends JIPipeData>, JIPipeDataConverter> targetMap = registeredConverters.getOrDefault(inputData.getClass(), null);
            if (targetMap != null) {
                JIPipeDataConverter converter = targetMap.getOrDefault(outputDataType, null);
                if (converter != null) {
                    return converter.convert(inputData.duplicate());
                }
            }

            // Find a converter fuzzy
            for (Map.Entry<Class<? extends JIPipeData>, Map<Class<? extends JIPipeData>, JIPipeDataConverter>> entry : registeredConverters.entrySet()) {
                if (entry.getKey().isAssignableFrom(inputData.getClass())) {
                    targetMap = entry.getValue();
                    JIPipeDataConverter converter = targetMap.getOrDefault(outputDataType, null);
                    if (converter != null) {
                        return converter.convert(inputData.duplicate());
                    }
                }
            }
        }
        throw new UserFriendlyRuntimeException("Could not convert " + inputData.getClass() + " to " + outputDataType,
                "Unable to convert data type!",
                "JIPipe plugin manager",
                "An algorithm requested that the data of type '" + JIPipeData.getNameOf(inputData.getClass()) + "' should be converted to type '" + JIPipeData.getNameOf(outputDataType) + "'." +
                        " There no available conversion function.",
                "Please check if the input data has the correct format by using the quick run. If you cannot resolve the issue, please contact the plugin or JIPipe authors.");
    }

    /**
     * Returns true if the input data type can be converted into the output data type.
     * Returns true if both data types are the same or trivially convertible (inheritance)
     *
     * @param inputDataType  the input data type
     * @param outputDataType the output data type
     * @return if the types are convertible
     */
    public boolean isConvertible(Class<? extends JIPipeData> inputDataType, Class<? extends JIPipeData> outputDataType) {
        if (isTriviallyConvertible(inputDataType, outputDataType)) {
            return true;
        } else {
            Map<Class<? extends JIPipeData>, JIPipeDataConverter> targetMap = registeredConverters.getOrDefault(inputDataType, null);
            if (targetMap != null) {
                if (targetMap.containsKey(outputDataType))
                    return true;
            }

            // Search fuzzy
            for (Map.Entry<Class<? extends JIPipeData>, Map<Class<? extends JIPipeData>, JIPipeDataConverter>> entry : registeredConverters.entrySet()) {
                if (entry.getKey().isAssignableFrom(inputDataType)) {
                    targetMap = entry.getValue();
                    if (targetMap.containsKey(outputDataType))
                        return true;
                }
            }
            return false;
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
        registeredDataTypes.put(id, klass);
        registeredDatatypeSources.put(id, source);
        if (klass.getAnnotationsByType(JIPipeHidden.class).length > 0)
            hiddenDataTypeIds.add(id);
        eventBus.post(new DatatypeRegisteredEvent(id));
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
     * Gets the event bus that post registration events
     *
     * @return The event bus
     */
    public EventBus getEventBus() {
        return eventBus;
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
            throw new UserFriendlyRuntimeException(new NullPointerException("Could not find data type with id '" + id + "' in " +
                    String.join(", ", registeredDataTypes.keySet())),
                    "Unable to find an data type!",
                    "JIPipe plugin manager",
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
     * @return Singleton instance
     */
    public static JIPipeDatatypeRegistry getInstance() {
        return JIPipeDefaultRegistry.getInstance().getDatatypeRegistry();
    }
}
