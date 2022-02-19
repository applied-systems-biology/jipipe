package org.hkijena.jipipe.api.data;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Added to {@link JIPipeData} classes to explain how the data type stores its data.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface JIPipeDataStorageDocumentation {
    /**
     * The storage documentation. Can contain HTML.
     *
     * @return the storage documentation.
     */
    String humanReadableDescription();

    /**
     * URL pointing to a JSON schema that describes how the data directory should be structured
     * @return URL to a JSON schema document
     */
    String jsonSchemaURL();
}
