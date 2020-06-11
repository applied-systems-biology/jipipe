package org.hkijena.acaq5.extensions.parameters.primitives;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Settings for {@link EnumParameterEditorUI}
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface EnumParameterSettings {
    /**
     * Provides information on how the enum item is rendered
     *
     * @return information on how the enum item is rendered
     */
    Class<? extends EnumItemInfo> itemInfo() default DefaultEnumItemInfo.class;
}
