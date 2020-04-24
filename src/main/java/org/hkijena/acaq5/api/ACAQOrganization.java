package org.hkijena.acaq5.api;

import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmCategory;

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

    /**
     * Assigns a category to an algorithm.
     * This is only valid for algorithm classes that use {@link org.hkijena.acaq5.api.algorithm.ACAQJavaAlgorithmDeclaration} or any
     * other {@link org.hkijena.acaq5.api.algorithm.ACAQAlgorithmDeclaration} that listens for algorithm categories.
     *
     * @return the algorithm category
     */
    ACAQAlgorithmCategory algorithmCategory() default ACAQAlgorithmCategory.Internal;
}
