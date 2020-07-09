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
     * If used as visibility for a sub-parameter collection, this property determined a minimum visibility the
     * sub-parameters must have to be shown in the UI.
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

    /**
     * A short key used for generating parameter strings.
     * Defaults to value() in {@link ACAQParameterAccess} implementations if not provided
     *
     * @return A short key used for generating parameter strings
     */
    String shortKey() default "";

    /**
     * Controls how the parameter is ordered within the user interface
     *
     * @return a low number indicates that this parameter is put first, while a high number indicates that this parameter is put last
     */
    int uiOrder() default 0;

    /**
     * Controls which sub-sub-parameters will not be shown in the UI
     *
     * @return array of sub-sub-parameter keys
     */
    String[] uiExcludeSubParameters() default {};
}
