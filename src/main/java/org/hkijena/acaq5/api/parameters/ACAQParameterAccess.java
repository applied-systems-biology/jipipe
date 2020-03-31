package org.hkijena.acaq5.api.parameters;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;

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
     * @param <T> Annotation type
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
     * @param <T> Parameter data type
     * @return If setting the value was successful
     */
    <T> boolean set(T value);

    /**
     * Gets the object that holds the parameter
     *
     * @return the object that holds the parameter
     */
    ACAQParameterHolder getParameterHolder();

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
     * Finds all parameters of the provided object
     * This includes dynamic parameters
     *
     * @param parameterHolder Parameterized object
     * @return All parameters
     */
    static Map<String, ACAQParameterAccess> getParameters(ACAQParameterHolder parameterHolder) {
        Map<String, ACAQParameterAccess> result = new HashMap<>();
        if (parameterHolder instanceof ACAQCustomParameterHolder) {
            for (Map.Entry<String, ACAQParameterAccess> entry : ((ACAQCustomParameterHolder) parameterHolder).getCustomParameters().entrySet()) {
                result.put(entry.getKey(), entry.getValue());
            }
        } else {
            for (Map.Entry<String, ACAQParameterAccess> entry : ACAQReflectionParameterAccess.getReflectionParameters(parameterHolder).entrySet()) {
                result.put(entry.getKey(), entry.getValue());
            }
        }

        return result;
    }
}
