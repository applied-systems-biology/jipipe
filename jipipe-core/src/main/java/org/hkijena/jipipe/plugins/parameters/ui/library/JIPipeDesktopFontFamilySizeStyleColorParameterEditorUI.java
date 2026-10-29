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

import org.hkijena.jipipe.plugins.parameters.library.primitives.JIPipeFontFamilySizeStyleColorParameter;

/**
 * Thin editor for the full {@link JIPipeFontFamilySizeStyleColorParameter}
 * (family + size + style + color) - the adaptive base shows all four rows
 * (#1339 Task 2). The {@link #getPopoutContent()} seam is inherited from
 * {@link JIPipeDesktopFontParameterEditorUI}; the screenshot harness casts to
 * this class and renders that content directly.
 */
public class JIPipeDesktopFontFamilySizeStyleColorParameterEditorUI extends JIPipeDesktopFontParameterEditorUI<JIPipeFontFamilySizeStyleColorParameter> {

    public JIPipeDesktopFontFamilySizeStyleColorParameterEditorUI(InitializationParameters parameters) {
        super(JIPipeFontFamilySizeStyleColorParameter.class, parameters);
    }
}
