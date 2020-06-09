package org.hkijena.acaq5.api.data;

import org.apache.commons.lang3.reflect.ConstructorUtils;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQHidden;
import org.hkijena.acaq5.api.ACAQOrganization;
import org.hkijena.acaq5.api.exceptions.UserFriendlyRuntimeException;
import org.hkijena.acaq5.utils.StringUtils;

import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Base class for any ACAQ data wrapper class
 * Any custom data should have following constructors:
 * 1. A constructor that takes the wrapped data type
 * 2. A constructor that takes a path to a folder to load the data from
 */
public interface ACAQData {

    /**
     * Saves the data to a folder
     *
     * @param storageFilePath A folder that already exists
     * @param name            A name reference that can be used to generate filename(s)
     */
    void saveTo(Path storageFilePath, String name);

    /**
     * Duplicates the data
     *
     * @return a deep copy of the data
     */
    ACAQData duplicate();

    /**
     * Called when the data is flushed
     * Use this to help Java to clean up the memory.
     */
    default void flush() {};

    /**
     * Returns the name of a data type
     *
     * @param klass The data class
     * @return The name of the data class
     */
    static String getNameOf(Class<? extends ACAQData> klass) {
        ACAQDocumentation[] annotations = klass.getAnnotationsByType(ACAQDocumentation.class);
        if (annotations.length > 0) {
            return annotations[0].name();
        } else {
            return klass.getSimpleName();
        }
    }

    /**
     * Returns the description of a data type
     *
     * @param klass The data class
     * @return The data class description
     */
    static String getDescriptionOf(Class<? extends ACAQData> klass) {
        ACAQDocumentation[] annotations = klass.getAnnotationsByType(ACAQDocumentation.class);
        if (annotations.length > 0) {
            return annotations[0].description();
        } else {
            return null;
        }
    }

    /**
     * Returns the menu path of the data type
     *
     * @param klass The data class
     * @return The menu path of the data class
     */
    static String getMenuPathOf(Class<? extends ACAQData> klass) {
        ACAQOrganization[] annotations = klass.getAnnotationsByType(ACAQOrganization.class);
        if (annotations.length > 0) {
            return annotations[0].menuPath();
        } else {
            return "";
        }
    }

    /**
     * Returns true if this data should be be accessible by the user
     *
     * @param klass Data class
     * @return If the data should be hidden from user
     */
    static boolean isHidden(Class<? extends ACAQData> klass) {
        return klass.getAnnotationsByType(ACAQHidden.class).length > 0;
    }

    /**
     * Gets name sorted list of data classes
     *
     * @param classes The data classes
     * @return A name-sorted list of data classes
     */
    static List<Class<? extends ACAQData>> getSortedList(Collection<Class<? extends ACAQData>> classes) {
        return classes.stream().sorted(Comparator.comparing(ACAQData::getNameOf)).collect(Collectors.toList());
    }

    /**
     * Groups the data types by their menu path
     *
     * @param classes The data classes
     * @return Map from menu path to a set of data classes that have the menu path
     */
    static Map<String, Set<Class<? extends ACAQData>>> groupByMenuPath(Collection<Class<? extends ACAQData>> classes) {
        Map<String, Set<Class<? extends ACAQData>>> result = new HashMap<>();
        for (Class<? extends ACAQData> dataClass : classes) {
            String menuPath = StringUtils.getCleanedMenuPath(ACAQData.getMenuPathOf(dataClass));
            Set<Class<? extends ACAQData>> group = result.getOrDefault(menuPath, null);
            if (group == null) {
                group = new HashSet<>();
                result.put(menuPath, group);
            }
            group.add(dataClass);
        }

        return result;
    }

    /**
     * Instantiates a data class with the provided parameters
     * This method is helpful if output data is constructed based on slot types
     *
     * @param klass                 The data class
     * @param constructorParameters Constructor parameters
     * @param <T>                   Data class
     * @return Data instance
     */
    static <T extends ACAQData> T createInstance(Class<T> klass, Object... constructorParameters) {
        try {
            return ConstructorUtils.invokeConstructor(klass, constructorParameters);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
            throw new UserFriendlyRuntimeException(e, "Cannot create annotation instance!", "Undefined", "There is an error in the code that provides the annotation type.",
                    "Please contact the author of the plugin that provides the annotation type " + klass);
        }
    }
}
