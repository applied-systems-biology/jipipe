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
import org.hkijena.jipipe.utils.JIPipeDefaultResourceManagerSupplier;
import org.hkijena.jipipe.utils.JIPipeResourceManager;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.function.Supplier;

/**
 * An additional action (usually UI action) that is attached to the {@link JIPipeDesktopParameterFormPanel}.
 * Annotate a method with this annotation to make it accessible to the UI.
 * Use {@link SetJIPipeDocumentation} to add additional information.
 * The method should take a {@link JIPipeWorkbench} instance as parameter.
 * <p>
 * JIPipe 1.74.0: Info - If you intend to create examples for your nodes, create a node template, export it into the plugin resources, and register the resource folder as example directory. We do not recommend anymore to utilize context actions as way to distribute examples.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RegisterJIPipeParameterCollectionContextAction {

    /**
     * The 16x16 icon name within JIPipe's the icon resource manager's database
     *
     * @return the icon
     */
    String icon() default "";

    /**
     * Points towards the resource manager that contains the icon
     *
     * @return supplier class with standard constructor that returns a {@link JIPipeResourceManager}
     */
    Class<? extends Supplier<JIPipeResourceManager>> iconResourceManager() default JIPipeDefaultResourceManagerSupplier.class;

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


    /**
     * If true, highlight with a green border
     *
     * @return if a green border should be shown
     */
    boolean highlighted() default false;
}
