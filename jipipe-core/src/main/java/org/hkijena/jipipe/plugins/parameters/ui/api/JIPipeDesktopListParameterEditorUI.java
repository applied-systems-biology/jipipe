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

package org.hkijena.jipipe.plugins.parameters.ui.api;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.parameters.JIPipeCustomTextDescriptionParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.desktop.api.JIPipeDesktopParameterEditorUI;
import org.hkijena.jipipe.plugins.parameters.api.collections.JIPipeListParameter;
import org.hkijena.jipipe.plugins.parameters.api.collections.JIPipeListParameterItemParameterAccess;

import javax.swing.*;

/**
 * Generic parameter editor for {@link JIPipeListParameter} - the thin subclass
 * of the shared list editor base: each item is rendered by its own parameter
 * type's editor UI.
 */
@SuppressWarnings("rawtypes")
public class JIPipeDesktopListParameterEditorUI extends JIPipeListParameterEditorUI {
    public JIPipeDesktopListParameterEditorUI(InitializationParameters parameters) {
        super(JIPipeListParameter.class, parameters);
    }

    @Override
    protected JComponent buildItemContent(int index, JIPipeListParameterItemParameterAccess<?> access) {
        JIPipeDesktopParameterEditorUI ui = JIPipe.getParameterTypes().createEditorInstance(access, getDesktopWorkbench(), new JIPipeParameterTree(access), null);
        return ui;
    }

    @Override
    public String itemDescription(int index) {
        return JIPipeCustomTextDescriptionParameter.getTextDescriptionOf(getParameter().get(index));
    }
}
