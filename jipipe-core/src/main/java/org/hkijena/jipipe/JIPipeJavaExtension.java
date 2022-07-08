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

package org.hkijena.jipipe;

import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.utils.ResourceUtils;
import org.scijava.Context;
import org.scijava.plugin.SciJavaPlugin;

import javax.swing.*;
import java.net.URL;
import java.util.Collections;
import java.util.List;

/**
 * A Java extension
 */
public interface JIPipeJavaExtension extends SciJavaPlugin, JIPipeDependency {

    /**
     * URL pointing to the logo of the extension
     * Note: This is currently unused and only kept for backwards compatibility. Please instead use the thumbnail property of the metadata to provide thumbnails for the extension manager.
     * @return the logo URL
     */
    @Deprecated
    default URL getLogo() {
        return ResourceUtils.getPluginResource("logo-400.png");
    }

    /**
     * Returns the registry
     *
     * @return The registry
     */
    JIPipe getRegistry();

    /**
     * Sets the registry
     *
     * @param registry The registry
     */
    void setRegistry(JIPipe registry);

    /**
     * Registers custom modules into JIPipe
     *
     * @param jiPipe       the {@link JIPipe} instance that calls this function
     * @param context      the SciJava {@link Context}
     * @param progressInfo the progress info
     */
    void register(JIPipe jiPipe, Context context, JIPipeProgressInfo progressInfo);

    /**
     * Called after registration.
     */
    default void postprocess() {

    }

    /**
     * Returns icons that will be displayed in the splash screen.
     * They must have a size of 32x32.
     * There can only be at most 45 icons, so please do not over-do it
     *
     * @return the icons
     */
    default List<ImageIcon> getSplashIcons() {
        return Collections.emptyList();
    }

    /**
     * Returns whether this extension is a core extension and thus cannot be disabled by users.
     * Defaults to false.
     * Please be sure if you really want to mark an extension as core extension.
     * @return if the extension cannot be disabled
     */
    default boolean isCoreExtension() {
        return false;
    }
}
