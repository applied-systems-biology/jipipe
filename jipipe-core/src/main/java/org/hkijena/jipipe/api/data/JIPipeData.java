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

package org.hkijena.jipipe.api.data;

import org.apache.commons.lang3.reflect.ConstructorUtils;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeHidden;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.exceptions.UserFriendlyRuntimeException;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.utils.StringUtils;

import java.awt.Component;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Base class for any JIPipe data wrapper class
 * Any custom data should have following constructors:
 * 1. A constructor that takes the wrapped data type
 * 2. A constructor that takes a path to a folder to load the data from
 */
@JIPipeDocumentation(name = "Data", description = "Generic data")
public interface JIPipeData {

    /**
     * Saves the data to a folder
     *
     * @param storageFilePath A folder that already exists
     * @param name            A name reference that can be used to generate filename(s)
     * @param forceName       If enabled, the data is saved potentially destructively. Generated files must always contain the name parameter. This is used to collect results for humans or other algorithms.
     */
    void saveTo(Path storageFilePath, String name, boolean forceName);

    /**
     * Duplicates the data
     *
     * @return a deep copy of the data
     */
    JIPipeData duplicate();

    /**
     * This function should display the data in the GUI.
     * The UI can handle multiple of such operations via {@link JIPipeDataDisplayOperation} that can be registered separately.
     * This item will always be shown as "Default" in the list of operations.
     *
     * @param displayName a name that can be used
     * @param workbench   the workbench
     */
    void display(String displayName, JIPipeWorkbench workbench);

    /**
     * This function generates a preview component for usage within the GUI
     * Can return null
     *
     * @param width  the target width
     * @param height the target height
     * @return the component or null if none should be available
     */
    default Component preview(int width, int height) {
        return null;
    }

    /**
     * Called when the data is flushed
     * Use this to help Java to clean up the memory.
     */
    default void flush() {
    }

    /**
     * Returns the name of a data type
     *
     * @param klass The data class
     * @return The name of the data class
     */
    static String getNameOf(Class<? extends JIPipeData> klass) {
        JIPipeDocumentation[] annotations = klass.getAnnotationsByType(JIPipeDocumentation.class);
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
    static String getDescriptionOf(Class<? extends JIPipeData> klass) {
        JIPipeDocumentation[] annotations = klass.getAnnotationsByType(JIPipeDocumentation.class);
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
    static String getMenuPathOf(Class<? extends JIPipeData> klass) {
        JIPipeOrganization[] annotations = klass.getAnnotationsByType(JIPipeOrganization.class);
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
    static boolean isHidden(Class<? extends JIPipeData> klass) {
        return klass.getAnnotationsByType(JIPipeHidden.class).length > 0;
    }

    /**
     * Gets name sorted list of data classes
     *
     * @param classes The data classes
     * @return A name-sorted list of data classes
     */
    static List<Class<? extends JIPipeData>> getSortedList(Collection<Class<? extends JIPipeData>> classes) {
        return classes.stream().sorted(Comparator.comparing(JIPipeData::getNameOf)).collect(Collectors.toList());
    }

    /**
     * Groups the data types by their menu path
     *
     * @param classes The data classes
     * @return Map from menu path to a set of data classes that have the menu path
     */
    static Map<String, Set<Class<? extends JIPipeData>>> groupByMenuPath(Collection<Class<? extends JIPipeData>> classes) {
        Map<String, Set<Class<? extends JIPipeData>>> result = new HashMap<>();
        for (Class<? extends JIPipeData> dataClass : classes) {
            String menuPath = StringUtils.getCleanedMenuPath(JIPipeData.getMenuPathOf(dataClass));
            Set<Class<? extends JIPipeData>> group = result.getOrDefault(menuPath, null);
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
    static <T extends JIPipeData> T createInstance(Class<T> klass, Object... constructorParameters) {
        try {
            return ConstructorUtils.invokeConstructor(klass, constructorParameters);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
            throw new UserFriendlyRuntimeException(e, "Cannot create annotation instance!", "Undefined", "There is an error in the code that provides the annotation type.",
                    "Please contact the author of the plugin that provides the annotation type " + klass);
        }
    }
}
