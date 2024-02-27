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

package org.hkijena.jipipe.api;

import org.hkijena.jipipe.JIPipe;

import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Allows to separate the description part of a {@link SetJIPipeDocumentation} into multiple items.
 */
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(AddJIPipeDocumentationDescriptions.class)
@Inherited
public @interface AddJIPipeDocumentationDescription {
    /**
     * @return The description
     */
    String description() default "";

    /**
     * A resource URL that points to a markdown/html file within a JAR resource.
     * Use descriptionResourceClass to change this class if needed.
     * If not empty, this overrides the description setting.
     *
     * @return the description resource URL
     */
    String descriptionResourceURL() default "";

    /**
     * The class where descriptionResourceURL loads the description from.
     *
     * @return resource class
     */
    Class<?> descriptionResourceClass() default JIPipe.class;
}
