package org.hkijena.acaq5.api;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Annotations used for organizing elements
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface ACAQOrganization {
    /**
     * A submenu where the entry should be located.
     * This is valid for {@link org.hkijena.acaq5.api.algorithm.ACAQAlgorithm} and {@link org.hkijena.acaq5.api.data.ACAQData}
     * Menu entries are separated via newlines.
     *
     * @return The menu path
     */
    String menuPath() default "";
}
