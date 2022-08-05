package org.hkijena.jipipe.extensions.parameters.api.collections;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Settings for a {@link ListParameter} or derivative
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface ListParameterSettings {
    /**
     * If enabled, the contents are displayed in a scrollbar
     * @return if contents should be scrollable
     */
    boolean withScrollBar() default false;

    /**
     * If the scrollbar is enabled, the height of the list control
     * @return height of the list control if scrolling is enabled
     */
    int scrollableHeight() default 350;
}
