package org.hkijena.jipipe.api.environments;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface ExternalEnvironmentInfo {
    /**
     * Allows to tag the environment for the GUI.
     * @return the category
     */
    String category() default "";
}
