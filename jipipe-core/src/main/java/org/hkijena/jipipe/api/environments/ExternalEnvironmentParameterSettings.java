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
}
