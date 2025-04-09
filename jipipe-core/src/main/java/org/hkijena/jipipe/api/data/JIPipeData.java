/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.api.data;

import org.hkijena.jipipe.api.*;
import org.hkijena.jipipe.api.data.storage.JIPipeWriteDataStorage;
import org.hkijena.jipipe.api.data.thumbnails.JIPipeThumbnailData;
import org.hkijena.jipipe.plugins.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.utils.DocumentationUtils;
import org.hkijena.jipipe.utils.StringUtils;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Base class for any JIPipe data wrapper class
 * There must be a static function importData({@link org.hkijena.jipipe.api.data.storage.JIPipeReadDataStorage}, JIPipeProgressInfo) that imports the data from a row storage folder.
 * Additionally, there must be an annotation of type {@link JIPipeDataStorageDocumentation} that describes the structure of a valid row storage folder for humans.
 * The static importData(@link org.hkijena.jipipe.api.data.storage.JIPipeReadDataStorage}, JIPipeProgressInfo) method and the {@link JIPipeDataStorageDocumentation} annotation can be omitted for abstract data types or interfaces.
 * {@link JIPipeDataStorageDocumentation} can be inherited from parent classes.
 * <p>
 * Update: 1.74.0: The class is now closable, which is useful for handling external resources. {@link JIPipeDataTable} and {@link JIPipeDataItemStore} were adapted to handle the close() automatically.
 */
@SetJIPipeDocumentation(name = "Data", description = "Generic data. Can hold any supported JIPipe data.")
@JIPipeDataStorageDocumentation(humanReadableDescription = "Unknown storage schema (generic data)",
        jsonSchemaURL = "https://jipipe.org/schemas/datatypes/jipipe-empty-data.schema.json")
@LabelAsJIPipeCommonData
public interface JIPipeData extends Closeable, AutoCloseable {

    /**
     * Returns the name of a data type
     *
     * @param klass The data class
     * @return The name of the data class
     */
    static String getNameOf(Class<? extends JIPipeData> klass) {
        SetJIPipeDocumentation[] annotations = klass.getAnnotationsByType(SetJIPipeDocumentation.class);
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
        SetJIPipeDocumentation[] annotations = klass.getAnnotationsByType(SetJIPipeDocumentation.class);
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
        ConfigureJIPipeNode[] annotations = klass.getAnnotationsByType(ConfigureJIPipeNode.class);
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
        return klass.getAnnotationsByType(LabelAsJIPipeHidden.class).length > 0;
    }

    /**
     * Returns true if this data is considered as having a large size
     *
     * @param klass Data class
     * @return If this data is considered as having a large size
     */
    static boolean isHeavy(Class<? extends JIPipeData> klass) {
        return klass.getAnnotationsByType(LabelAsJIPipeHeavyData.class).length > 0;
    }

    /**
     * Returns true if this data is commonly used (only for UI)
     *
     * @param klass Data class
     * @return If this data is commonly used
     */
    static boolean isCommon(Class<? extends JIPipeData> klass) {
        return klass.getAnnotationsByType(LabelAsJIPipeCommonData.class).length > 0;
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
            return new HTMLText(annotation.humanReadableDescription());
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
     * Returns the URL pointing to a JSON schema that describes the storage of the data type.
     * Will return null if none was provided.
     * Will go through parent classes to find a storage documentation
     *
     * @param klass the class
     * @return the storage documentation
     */
    static String getStorageSchema(Class<? extends JIPipeData> klass) {
        JIPipeDataStorageDocumentation annotation = klass.getAnnotation(JIPipeDataStorageDocumentation.class);
        if (annotation != null) {
            return annotation.jsonSchemaURL();
        } else {
            if (klass == JIPipeData.class) {
                return null;
            } else {
                Class<?> superclass = klass.getSuperclass();
                if (superclass != null && JIPipeData.class.isAssignableFrom(superclass)) {
                    return getStorageSchema((Class<? extends JIPipeData>) superclass);
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

    /**
     * Saves the data to a folder
     *
     * @param storage      The storage where the data should be written
     * @param name         A name reference that can be used to generate filename(s)
     * @param forceName    If enabled, the data is saved potentially destructively. Generated files must always contain the name parameter. This is used to collect results for humans or other algorithms.
     * @param progressInfo the progress
     */
    void exportData(JIPipeWriteDataStorage storage, String name, boolean forceName, JIPipeProgressInfo progressInfo);

    /**
     * Duplicates the data
     *
     * @param progressInfo the progress of duplicating the data
     * @return a deep copy of the data
     */
    JIPipeData duplicate(JIPipeProgressInfo progressInfo);

    /**
     * This function generates a thumbnail for this data
     * Can return null
     *
     * @param width        the width
     * @param height       the height
     * @param progressInfo the progress info
     * @return the thumbnail or null
     */
    default JIPipeThumbnailData createThumbnail(int width, int height, JIPipeProgressInfo progressInfo) {
        return null;
    }

    /**
     * This method returns a detailed string description (with multi-line, etc.) of the data.
     * It can be used if toString() would yield too much information.
     * Defaults to toString()
     *
     * @return detailed description string
     */
    default String toDetailedString() {
        return toString();
    }

    @Override
    default void close() {

    }

    /**
     * Exports the thumbnails/previews of this data into the provided storage
     *
     * @param storage      the storage
     * @param target       the location of the data (as internal path within the data table) that is described by the thumbnails
     * @param sizes        the sizes to save (in pixels)
     * @param progressInfo the progress info
     */
    default void exportThumbnails(JIPipeWriteDataStorage storage, Path target, List<Dimension> sizes, JIPipeProgressInfo progressInfo) {
        JIPipeDataThumbnailsMetadata metadata = new JIPipeDataThumbnailsMetadata();
        metadata.setTarget(target);
        for (int i = 0; i < sizes.size(); i++) {
            Dimension size = sizes.get(i);
            progressInfo.resolveAndLog(size.width + "x" + size.height, i, sizes.size());
            JIPipeThumbnailData thumbnail = createThumbnail(size.width, size.height, progressInfo);
            if (thumbnail == null)
                continue;
            Component component = thumbnail.renderToComponent(size.width, size.height);
            if (component == null)
                continue;
            int trueWidth = Math.max(component.getWidth(), size.width);
            int trueHeight = Math.max(component.getHeight(), size.height);
            try {
                SwingUtilities.invokeAndWait(() -> {
                    component.setSize(trueWidth, trueHeight);
                    BufferedImage image = new BufferedImage(trueWidth, trueHeight, BufferedImage.TYPE_INT_ARGB);
                    Graphics2D g = (Graphics2D) image.getGraphics();
                    component.print(g);
                    try (OutputStream stream = storage.write(size.width + "x" + size.height + ".png")) {
                        ImageIO.write(image, "PNG", stream);
                        metadata.getThumbnails().add(new JIPipeDataThumbnailsMetadata.Thumbnail(size.width + "x" + size.height, size, Paths.get(size.width + "x" + size.height + ".png"), new ArrayList<>()));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
            } catch (InterruptedException | InvocationTargetException e) {
                e.printStackTrace();
            }
        }
        if (!metadata.getThumbnails().isEmpty()) {
            storage.writeJSON(Paths.get("thumbnails.json"), metadata);
        }
    }
}
