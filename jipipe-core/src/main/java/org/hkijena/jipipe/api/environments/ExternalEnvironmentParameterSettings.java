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

package org.hkijena.jipipe.api.environments;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Can be attached to control the UI of
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface ExternalEnvironmentParameterSettings {
    /**
     * @return if the edit button should be visible
     * @deprecated has no effect
     */
    @Deprecated
    boolean allowEditButton() default true;

    /**
     * @return if the install button should be visible
     * @deprecated has no effect
     */
    @Deprecated
    boolean allowInstallButton() default true;

    /**
     * @return if installation options are shown
     */
    boolean allowInstall() default true;

    /**
     * @return if managing (loading/saving) presets is allowed
     */
    boolean allowManagePreset() default true;

    /**
     * If non-empty, only installation items with the matching category are shown
     *
     * @return the category or empty (no category)
     */
    String showCategory() default "";

    /**
     * @return if this external environment parameter allows to be filled with artifact infos
     */
    boolean allowArtifact() default false;

    String[] artifactFilters() default {"*"};
}
