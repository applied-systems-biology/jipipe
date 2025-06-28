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

package org.hkijena.jipipe.desktop.commons.theme;

import org.hkijena.jipipe.desktop.commons.theme.ui.*;
import org.hkijena.jipipe.utils.ThemeUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import javax.swing.plaf.metal.MetalLookAndFeel;

public enum JIPipeDesktopUITheme {
    Native("Native", false),
    Metal("Metal", false),
    Modern("Modern", true);

    private final String name;
    private final boolean isModern;

    JIPipeDesktopUITheme(String name, boolean isModern) {
        this.name = name;
        this.isModern = isModern;
    }

    public String getName() {
        return name;
    }

    public boolean isModern() {
        return isModern;
    }
    @Override
    public String toString() {
        return name;
    }
}
