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

package org.hkijena.jipipe.api;

import org.hkijena.jipipe.api.algorithm.JIPipeGraphNode;
import org.hkijena.jipipe.api.algorithm.JIPipeJavaNodeInfo;
import org.hkijena.jipipe.api.algorithm.JIPipeNodeCategory;
import org.hkijena.jipipe.api.algorithm.JIPipeNodeInfo;
import org.hkijena.jipipe.ui.extension.MenuExtension;
import org.hkijena.jipipe.ui.extension.MenuTarget;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Annotations used for organizing elements
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface JIPipeOrganization {
    /**
     * A submenu where the entry should be located.
     * This is valid for {@link JIPipeGraphNode} and {@link org.hkijena.jipipe.api.data.JIPipeData}
     * Menu entries are separated via newlines.
     *
     * @return The menu path
     */
    String menuPath() default "";

    /**
     * Assigns a category to an algorithm.
     * This is only valid for algorithm classes that use {@link JIPipeJavaNodeInfo} or any
     * other {@link JIPipeNodeInfo} that listens for algorithm categories.
     *
     * @return the algorithm category
     */
    JIPipeNodeCategory algorithmCategory() default JIPipeNodeCategory.Internal;

    /**
     * Only used if attached to a {@link MenuExtension}.
     * Determines in which main menu the item is placed
     *
     * @return menu target
     */
    MenuTarget menuExtensionTarget() default MenuTarget.None;
}
