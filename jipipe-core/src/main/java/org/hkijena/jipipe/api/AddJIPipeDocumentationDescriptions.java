package org.hkijena.jipipe.api;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Container for multiple {@link AddJIPipeDocumentationDescription}
 */
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface AddJIPipeDocumentationDescriptions {
    AddJIPipeDocumentationDescription[] value();
}
