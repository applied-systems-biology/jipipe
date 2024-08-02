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

package org.hkijena.jipipe.api.parameters;

import org.hkijena.jipipe.api.JIPipeWorkbench;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopParameterFormPanel;
import org.hkijena.jipipe.utils.ResourceUtils;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An additional action (usually UI action) that is attached to the {@link JIPipeDesktopParameterFormPanel}.
 * Annotate a method with this annotation to make it accessible to the UI.
 * Use {@link SetJIPipeDocumentation} to add additional information.
 * The method should take a {@link JIPipeWorkbench} instance as parameter
 * <p>
 * JIPipe 1.74.0: Info - If you intend to create examples for your nodes, create a node template, export it into the plugin resources, and register the resource folder as example directory. We do not recommend anymore to utilize context actions as way to distribute examples.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface JIPipeContextAction {
    /**
     * The icon resource URL (optional)
     *
     * @return icon resource URL or empty
     */
    String iconURL() default "";

    /**
     * The icon resource URL (optional)
     *
     * @return icon resource URL or empty
     */
    String iconDarkURL() default "";

    /**
     * The class that loads the resource
     *
     * @return the resource class
     */
    Class<?> resourceClass() default ResourceUtils.class;

    /**
     * Determines if the context action is shown as button in the parameters
     *
     * @return if the action is shown in the parameters
     */
    boolean showInParameters() default true;

    /**
     * Determines if the context action is shown in the context menu
     *
     * @return if the action is shown in the context menu
     */
    boolean showInContextMenu() default true;


}
