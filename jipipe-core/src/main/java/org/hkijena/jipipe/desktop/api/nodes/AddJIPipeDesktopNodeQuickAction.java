/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.desktop.api.nodes;

import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method inside a {@link JIPipeGraphNode} as a quick action for JIPipe Desktop.
 * The action will appear in the overview panel of the node
 * Should have the {@link org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphCanvasUI} as a parameter
 */
@Target(value = ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AddJIPipeDesktopNodeQuickAction {
    /**
     * The name of the quick action
     *
     * @return the name
     */
    String name();

    /**
     * The description of the quick action
     *
     * @return the description
     */
    String description();

    /**
     * The icon of the quick action (one of the JIPipe default icons)
     *
     * @return the icon
     */
    String icon();

    /**
     * The icon of the button (one of the JIPipe default icons)
     *
     * @return the icon
     */
    String buttonIcon();


    /**
     * The text of the button (one of the JIPipe default icons)
     *
     * @return the text
     */
    String buttonText();
}
