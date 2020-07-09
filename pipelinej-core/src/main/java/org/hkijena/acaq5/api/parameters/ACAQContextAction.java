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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An additional action (usually UI action) that is attached to the {@link org.hkijena.acaq5.ui.parameters.ParameterPanel}.
 * Annotate a method with this annotation to make it accessible to the UI.
 * Use {@link org.hkijena.acaq5.api.ACAQDocumentation} to add additional information
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ACAQContextAction {
    /**
     * The icon resource URL (optional)
     *
     * @return icon resource URL or empty
     */
    String iconURL() default "";
}
