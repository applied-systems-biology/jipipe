package org.hkijena.jipipe.extensions.parameters.library.images;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface ImageParameterSettings {
    /**
     * @return the maximum width of images. A negative value indicates no limit.
     */
    int maxWidth() default -1;
    /**
     * @return the maximum height of images. A negative value indicates no limit.
     */
    int maxHeight() default -1;
}
