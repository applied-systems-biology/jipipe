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

package org.hkijena.jipipe.plugins.parameters.api.enums;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Settings for {@link EnumDesktopParameterEditorUI}
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface EnumParameterSettings {
    /**
     * Provides information on how the enum item is rendered
     *
     * @return information on how the enum item is rendered
     */
    Class<? extends EnumItemInfo> itemInfo() default DefaultEnumItemInfo.class;

    /**
     * If enabled, the {@link EnumDesktopParameterEditorUI} will display a button with the enum value that opens a window for searching
     *
     * @return if the enum items should be searchable
     */
    boolean searchable() default false;
}
