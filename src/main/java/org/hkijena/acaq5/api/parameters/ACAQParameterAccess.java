package org.hkijena.acaq5.api.parameters;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;

public interface ACAQParameterAccess {

    /**
     * Returns the unique ID of this parameter
     *
     * @return
     */
    String getKey();

    /**
     * Returns the parameter name that is displayed to the user
     *
     * @return
     */
    String getName();

    /**
     * Returns a description
     *
     * @return
     */
    String getDescription();

    /**
     * Returns if the parameter should be visible to users or only stored to JSON
     *
     * @return
     */
    ACAQParameterVisibility getVisibility();

    /**
     * Finds an annotation for this parameter
     *
     * @param klass
     * @param <T>
     * @return
     */
    <T extends Annotation> T getAnnotationOfType(Class<T> klass);

    /**
     * Returns the parameter data type
     *
     * @return
     */
    Class<?> getFieldClass();

    /**
     * Gets the parameter value
     *
     * @param <T>
     * @return
     */
    <T> T get();

    /**
     * Sets the parameter value
     *
     * @param value
     * @param <T>
     * @return
     */
    <T> boolean set(T value);

    /**
     * Gets the object that holds the parameter
     *
     * @return
     */
    ACAQParameterHolder getParameterHolder();

    /**
     * A name for the parameter holder
     *
     * @return
     */
    String getHolderName();

    /**
     * Sets the name for the parameter holder
     * @param name
     */
    void setHolderName(String name);

    /**
     * A description for the parameter holder
     *
     * @return
     */
    String getHolderDescription();

    /**
     * Sets the holder description
     * @param description
     */
    void setHolderDescription(String description);

    /**
     * Finds all parameters of the provided object
     * This includes dynamic parameters
     *
     * @param parameterHolder
     * @return
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
