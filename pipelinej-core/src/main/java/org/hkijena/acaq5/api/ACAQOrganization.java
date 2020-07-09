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

package org.hkijena.acaq5.api;

import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmCategory;
import org.hkijena.acaq5.api.algorithm.ACAQGraphNode;
import org.hkijena.acaq5.ui.extension.MenuExtension;
import org.hkijena.acaq5.ui.extension.MenuTarget;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Annotations used for organizing elements
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface ACAQOrganization {
    /**
     * A submenu where the entry should be located.
     * This is valid for {@link ACAQGraphNode} and {@link org.hkijena.acaq5.api.data.ACAQData}
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

    /**
     * Only used if attached to a {@link MenuExtension}.
     * Determines in which main menu the item is placed
     *
     * @return menu target
     */
    MenuTarget menuExtensionTarget() default MenuTarget.None;
}
