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

package org.hkijena.jipipe.api.parameters;

import org.scijava.Priority;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Interface around accessing a parameter
 */
public interface JIPipeParameterAccess {

    /**
     * Compares the priority
     *
     * @param lhs access
     * @param rhs access
     * @return the order
     */
    static int comparePriority(JIPipeParameterAccess lhs, JIPipeParameterAccess rhs) {
        return -Double.compare(lhs.getPriority(), rhs.getPriority());
    }

    /**
     * Returns the unique ID of this parameter
     *
     * @return Unique parameter key
     */
    String getKey();

    /**
     * Returns the parameter name that is displayed to the user
     *
     * @return Parameter name
     */
    String getName();

    /**
     * Returns a description
     *
     * @return Parameter description
     */
    String getDescription();

    /**
     * Returns if the parameter should be hidden from the user
     *
     * @return Parameter visibility
     */
    boolean isHidden();

    /**
     * Returns true if the parameter should be marked in the UI as important
     *
     * @return if the parameter is important
     */
    boolean isImportant();

    /**
     * Returns true if the parameter is pinned to the top
     *
     * @return if the parameter is pinned to the top
     */
    default boolean isPinned() {
        return false;
    }

    /**
     * Gets an annotation for this parameter
     *
     * @param klass Annotation class
     * @param <T>   Annotation type
     * @return Annotation or null if not found
     */
    <T extends Annotation> T getAnnotationOfType(Class<T> klass);

    /**
     * Gets annotations for this parameter (including the field class)
     * Please note that there is no guarantee that repeatable annotations are resolved properly. This is an oversight of Java and cannot be resolved on our side.
     * We recommend to check both for the repeatable and container annotations.
     *
     * @param klass the annotation class
     * @param <T>   the annotation class
     * @return the list of annotations
     */
    default <T extends Annotation> List<T> getAnnotationsOfType(Class<T> klass) {
        T annotation = getAnnotationOfType(klass);
        if (annotation != null)
            return Collections.singletonList(annotation);
        else
            return Collections.emptyList();
    }

    /**
     * Gets all available annotations for this parameter
     *
     * @return the annotations
     */
    Collection<Annotation> getAnnotations();

    /**
     * Returns the parameter data type
     *
     * @return Parameter class
     */
    Class<?> getFieldClass();

    /**
     * Gets the parameter value
     *
     * @param <T>   Parameter data type
     * @param klass Parameter data type
     * @return Parameter value
     */
    <T> T get(Class<T> klass);

    /**
     * Sets the parameter value
     *
     * @param value Parameter value
     * @param <T>   Parameter data type
     * @return If setting the value was successful
     */
    <T> boolean set(T value);

    /**
     * Gets the object that holds the parameter
     *
     * @return the object that holds the parameter
     */
    JIPipeParameterCollection getSource();

    /**
     * Returns the priority for (de)serializing this parameter.
     * Please use the priority constants provided by {@link Priority}
     *
     * @return the priority
     */
    double getPriority();

    /**
     * Returns a short form of the ID used, for example to generate a parameter string.
     * Might return getKey() if none was provided
     *
     * @return a short form of the ID used, for example to generate a parameter string
     */
    String getShortKey();

    /**
     * Controls how the parameter is ordered within the user interface
     *
     * @return a low number indicates that this parameter is put first, while a high number indicates that this parameter is put last
     */
    int getUIOrder();

    /**
     * Controls the persistence of the parameter
     *
     * @return the persistence
     */
    default JIPipeParameterSerializationMode getPersistence() {
        return JIPipeParameterSerializationMode.Default;
    }
}
