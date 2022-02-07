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

package org.hkijena.jipipe.extensions.parameters.library.primitives;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Settings for {@link String} parameters
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface StringParameterSettings {
    /**
     * If true, the editor allows to create multiple lines
     *
     * @return if the editor allows to create multiple lines
     */
    boolean multiline() default false;

    /**
     * If true, the text is rendered with monospaced font
     *
     * @return if the text is rendered with monospaced font
     */
    boolean monospace() default false;

    /**
     * @return Icon shown next to the text field (single line only)
     */
    String icon() default "";

    /**
     * @return Prompt shown on the text field
     */
    String prompt() default "";
}
