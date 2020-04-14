package org.hkijena.acaq5.api.parameters;

import org.scijava.Priority;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotates a getter or setter function as parameter.
 * {@link ACAQParameterAccess} will look for this annotation to find parameters.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ACAQParameter {
    /**
     * The unique key of this parameter
     *
     * @return Parameter key
     */
    String value();

    /**
     * Sets if the parameter is visible to the user or only exported into JSON
     * Lower visibilities override higher visibilities.
     *
     * @return Parameter visibility
     */
    ACAQParameterVisibility visibility() default ACAQParameterVisibility.TransitiveVisible;

    /**
     * Sets the priority for (de)serializing this parameter.
     * Please use the priority constants provided by {@link Priority}
     *
     * @return the priority
     */
    double priority() default Priority.NORMAL;
}
