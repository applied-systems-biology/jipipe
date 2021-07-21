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

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeHeavyData;
import org.hkijena.jipipe.api.JIPipeHidden;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.extensions.parameters.primitives.HTMLText;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.utils.DocumentationUtils;
import org.hkijena.jipipe.utils.StringUtils;

import java.awt.Component;
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
 * There must be a static function importFrom(Path) that imports the data from a row storage folder.
 * Additionally, there must be a annotation of type {@link JIPipeDataStorageDocumentation} that describes the structure of a valid row storage folder for humans.
 * The static importFrom(Path) method and the {@link JIPipeDataStorageDocumentation} annotation can be omitted for abstract data types or interfaces.
 * {@link JIPipeDataStorageDocumentation} can be inherited from parent classes.
 */
@JIPipeDocumentation(name = "Data", description = "Generic data")
public interface JIPipeData {

    /**
     * Saves the data to a folder
     *
     * @param storageFilePath A folder that already exists
     * @param name            A name reference that can be used to generate filename(s)
     * @param forceName       If enabled, the data is saved potentially destructively. Generated files must always contain the name parameter. This is used to collect results for humans or other algorithms.
     * @param progressInfo    the progress
     */
    void saveTo(Path storageFilePath, String name, boolean forceName, JIPipeProgressInfo progressInfo);

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
     * @param source      optional source of the data. Can be null or any kind of data type (e.g. {@link JIPipeDataSlot})
     */
    void display(String displayName, JIPipeWorkbench workbench, JIPipeDataSource source);

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
     * This method returns a detailed string description (with multi-line, etc.) of the data.
     * It can be used if toString() would yield too much information.
     * Defaults to toString()
     * @return detailed description string
     */
    default String toDetailedString() {
        return toString();
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
            return DocumentationUtils.getDocumentationDescription(annotations[0]);
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
     * Returns true if this data is considered as having a large size
     *
     * @param klass Data class
     * @return If this data is considered as having a large size
     */
    static boolean isHeavy(Class<? extends JIPipeData> klass) {
        return klass.getAnnotationsByType(JIPipeHeavyData.class).length > 0;
    }

    /**
     * Returns the storage documentation for the data type or null if none was provided.
     * Will go through parent classes to find a storage documentation
     *
     * @param klass the class
     * @return the storage documentation
     */
    static HTMLText getStorageDocumentation(Class<? extends JIPipeData> klass) {
        JIPipeDataStorageDocumentation annotation = klass.getAnnotation(JIPipeDataStorageDocumentation.class);
        if (annotation != null) {
            return new HTMLText(annotation.value());
        } else {
            if (klass == JIPipeData.class) {
                return null;
            } else {
                Class<?> superclass = klass.getSuperclass();
                if (superclass != null && JIPipeData.class.isAssignableFrom(superclass)) {
                    return getStorageDocumentation((Class<? extends JIPipeData>) superclass);
                } else {
                    return null;
                }
            }
        }
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

}
