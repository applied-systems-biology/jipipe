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

import org.hkijena.jipipe.utils.JIPipeDefaultResourceManagerSupplier;
import org.hkijena.jipipe.utils.JIPipeResourceManager;
import org.scijava.Priority;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.function.Supplier;

/**
 * Annotates a getter or setter function as parameter.
 * {@link JIPipeParameterAccess} will look for this annotation to find parameters.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface JIPipeParameter {
    /**
     * The unique key of this parameter
     *
     * @return Parameter key
     */
    String value();

    /**
     * Sets the priority for (de)serializing this parameter.
     * Please use the priority constants provided by {@link Priority}
     *
     * @return the priority
     */
    double priority() default Priority.NORMAL;

    /**
     * A short key used for generating parameter strings.
     * Defaults to value() in {@link JIPipeParameterAccess} implementations if not provided
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
     * Controls how the parameter is serialized/deserialized
     *
     * @return the serialization behavior
     */
    JIPipeParameterSerializationMode persistence() default JIPipeParameterSerializationMode.Default;

    /**
     * Determines if a sub-parameter is collapsed.
     *
     * @return if a sub-parameter is collapsed
     */
    boolean collapsed() default false;

    /**
     * Allows to hide the parameter by default (Can be overridden within the parameter collection=
     * )
     *
     * @return if the parameter or sub-parameter is hidden by default
     */
    boolean hidden() default false;

    /**
     * The 16x16 icon name within JIPipe's the icon resource manager's database
     *
     * @return the icon
     */
    String icon() default "";

    /**
     * Points towards the resource manager that contains the icon
     *
     * @return supplier class with standard constructor that returns a {@link JIPipeResourceManager}
     */
    Class<? extends Supplier<JIPipeResourceManager>> iconResourceManager() default JIPipeDefaultResourceManagerSupplier.class;

    /**
     * Marks the parameter as important, which will add an icon to the parameter UI
     *
     * @return if the parameter is important
     */
    boolean important() default false;

    /**
     * Marks the parameter as pinned. It will be sorted to the top in the parameter panel and visually separated.
     * Intended mostly for JIPipe-internal node parameters that should be separated from other parameters
     *
     * @return if the parameter is pinned
     */
    boolean pinned() default false;

    /**
     * Marks the parameter as affecting the functionality of an algorithm.
     * If false, the parameter will be ignored while testing whether two nodes are functionally equal.
     *
     * @return if the parameter affects the functionality of the node
     */
    boolean functional() default true;
}
