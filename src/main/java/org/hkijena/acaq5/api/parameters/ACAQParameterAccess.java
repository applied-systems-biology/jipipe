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
    Object getParameterHolder();

    /**
     * Finds all parameters of the provided object
     * This includes dynamic parameters
     *
     * @param parameterHolder
     * @return
     */
    static Map<String, ACAQParameterAccess> getParameters(Object parameterHolder) {
        Map<String, ACAQParameterAccess> result = new HashMap<>();
        if (parameterHolder instanceof ACAQDynamicParameterHolder) {
            for (Map.Entry<String, ACAQParameterAccess> entry : ((ACAQDynamicParameterHolder) parameterHolder).getDynamicParameters().entrySet()) {
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
