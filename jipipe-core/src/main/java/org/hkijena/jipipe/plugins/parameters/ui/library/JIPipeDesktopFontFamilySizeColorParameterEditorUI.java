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

package org.hkijena.jipipe.plugins.parameters.ui.library;

import org.hkijena.jipipe.plugins.parameters.library.primitives.JIPipeFontFamilySizeColorParameter;

/**
 * Thin editor for the {@link JIPipeFontFamilySizeColorParameter} (family +
 * size + color) — the adaptive base shows the Family, Size and Color rows
 * (no style section, #1339 Task 2).
 */
public class JIPipeDesktopFontFamilySizeColorParameterEditorUI extends JIPipeDesktopFontParameterEditorUI<JIPipeFontFamilySizeColorParameter> {

    public JIPipeDesktopFontFamilySizeColorParameterEditorUI(InitializationParameters parameters) {
        super(JIPipeFontFamilySizeColorParameter.class, parameters);
    }
}
