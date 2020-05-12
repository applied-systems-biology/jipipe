package org.hkijena.acaq5.api.parameters;

import org.scijava.Priority;

import java.lang.annotation.Annotation;

/**
 * Interface around accessing a parameter
 */
public interface ACAQParameterAccess {

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
     * Returns if the parameter should be visible to users or only stored to JSON
     *
     * @return Parameter visibility
     */
    ACAQParameterVisibility getVisibility();

    /**
     * Finds an annotation for this parameter
     *
     * @param klass Annotation class
     * @param <T>   Annotation type
     * @return Annotation or null if not found
     */
    <T extends Annotation> T getAnnotationOfType(Class<T> klass);

    /**
     * Returns the parameter data type
     *
     * @return Parameter class
     */
    Class<?> getFieldClass();

    /**
     * Gets the parameter value
     *
     * @param <T> Parameter data type
     * @return Parameter value
     */
    <T> T get();

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
    ACAQParameterCollection getSource();

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
     * Compares the priority
     *
     * @param lhs access
     * @param rhs access
     * @return the order
     */
    static int comparePriority(ACAQParameterAccess lhs, ACAQParameterAccess rhs) {
        return -Double.compare(lhs.getPriority(), rhs.getPriority());
    }
}
