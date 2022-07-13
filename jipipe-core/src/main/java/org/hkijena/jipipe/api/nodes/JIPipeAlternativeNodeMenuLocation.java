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
 *
 */

package org.hkijena.jipipe.api.nodes;

import org.hkijena.jipipe.api.nodes.categories.InternalNodeTypeCategory;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotates a node with an additional menu location
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Repeatable(JIPipeAlternativeNodeMenuLocations.class)
public @interface JIPipeAlternativeNodeMenuLocation {
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
     *
     * @return the category
     */
    Class<? extends JIPipeNodeTypeCategory> nodeTypeCategory() default InternalNodeTypeCategory.class;

    /**
     * An alternative name (optional) that will be given to the menu entry
     * @return the alternative name (if empty, the default name shall be used)
     */
    String alternativeName() default "";
}
