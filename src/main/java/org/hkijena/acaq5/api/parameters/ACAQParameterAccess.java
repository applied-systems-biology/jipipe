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
    String getSlotName();

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
    ACAQParameterCollection getParameterHolder();

    /**
     * A name for the parameter holder
     *
     * @return name for the parameter holder
     */
    String getHolderName();

    /**
     * Sets the name for the parameter holder
     *
     * @param name name for the parameter holder
     */
    void setHolderName(String name);

    /**
     * A description for the parameter holder
     *
     * @return description for the parameter holder
     */
    String getHolderDescription();

    /**
     * Sets the holder description
     *
     * @param description holder description
     */
    void setHolderDescription(String description);

    /**
     * Returns the priority for (de)serializing this parameter.
     * Please use the priority constants provided by {@link Priority}
     *
     * @return the priority
     */
    double getPriority();

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
