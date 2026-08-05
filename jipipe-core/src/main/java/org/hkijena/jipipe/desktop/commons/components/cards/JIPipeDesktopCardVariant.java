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

package org.hkijena.jipipe.desktop.commons.components.cards;

import org.hkijena.jipipe.desktop.commons.theme.JIPipeDesktopModernThemeStyle;

import java.awt.*;

public enum JIPipeDesktopCardVariant {
    Default,
    Primary,
    Secondary,
    Success,
    Warning,
    Danger,
    Info;

    public Color resolveColor(JIPipeDesktopModernThemeStyle style) {
        return switch (this) {
            case Default -> null;
            case Primary -> style.getPrimaryColor();
            case Secondary -> style.getSecondaryColor();
            case Success -> style.getSuccessColor();
            case Warning -> style.getWarningColor();
            case Danger -> style.getDangerColor();
            case Info -> style.getInfoColor();
        };
    }
}
