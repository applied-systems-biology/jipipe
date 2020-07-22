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

import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.InternalNodeTypeCategory;
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
     * Assigns a category to any {@link JIPipeNodeInfo} that listens to this annotation.
     * Determines in which menus the nodes are placed and how the user can interact with it.
     * Defaults to {@link InternalNodeTypeCategory}, which prevents users from creating such nodes and deleting them,
     * so change this categorization
     * @return the category
     */
    Class<? extends JIPipeNodeTypeCategory> nodeTypeCategory() default InternalNodeTypeCategory.class;

    /**
     * Only used if attached to a {@link MenuExtension}.
     * Determines in which main menu the item is placed
     *
     * @return menu target
     */
    MenuTarget menuExtensionTarget() default MenuTarget.None;
}
