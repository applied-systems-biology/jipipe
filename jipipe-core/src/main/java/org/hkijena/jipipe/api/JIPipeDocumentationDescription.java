package org.hkijena.jipipe.api;

import org.hkijena.jipipe.JIPipe;

import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Allows to separate the description part of a {@link JIPipeDocumentation} into multiple items.
 */
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(JIPipeDocumentationDescriptions.class)
@Inherited
public @interface JIPipeDocumentationDescription {
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
