package org.hkijena.jipipe.api.data.thumbnails;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a type to bypass the thumbnail generation queue and schedule thumbnails into the Swing threading system instead
 * Do not use for types that may need heavy processing for thumbnail generation
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface JIPipeFastThumbnail {
}
