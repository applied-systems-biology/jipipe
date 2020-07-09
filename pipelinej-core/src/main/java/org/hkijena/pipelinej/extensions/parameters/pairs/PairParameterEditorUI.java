/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.pipelinej.extensions.parameters.pairs;

import org.hkijena.pipelinej.api.parameters.ACAQParameterAccess;
import org.hkijena.pipelinej.ui.ACAQWorkbench;
import org.hkijena.pipelinej.ui.components.FormPanel;
import org.hkijena.pipelinej.ui.parameters.ACAQParameterEditorUI;
import org.hkijena.pipelinej.ui.registries.ACAQUIParameterTypeRegistry;
import org.hkijena.pipelinej.utils.StringUtils;
import org.hkijena.pipelinej.utils.UIUtils;

import javax.swing.*;
import java.awt.*;

/**
 * Editor for {@link Pair}
 */
public class PairParameterEditorUI extends ACAQParameterEditorUI {

    /**
     * Creates new instance
     *
     * @param workbench       workbench
     * @param parameterAccess Parameter
     */
    public PairParameterEditorUI(ACAQWorkbench workbench, ACAQParameterAccess parameterAccess) {
        super(workbench, parameterAccess);
        reload();
    }

    @Override
    public boolean isUILabelEnabled() {
        return true;
    }

    @Override
    public void reload() {
        removeAll();

        Pair<?, ?> renaming = getParameter(Pair.class);
        PairParameterKeyAccess<?, ?> keyAccess = new PairParameterKeyAccess<>(getParameterAccess(), renaming);
        PairParameterValueAccess<?, ?> valueAccess = new PairParameterValueAccess<>(getParameterAccess(), renaming);

        String keyLabel = "";
        String valueLabel = "";
        boolean singleRow = true;
        boolean singleRowChevron = true;

        if (getParameterAccess().getAnnotationOfType(PairParameterSettings.class) != null) {
            PairParameterSettings settings = getParameterAccess().getAnnotationOfType(PairParameterSettings.class);
            keyLabel = settings.keyLabel();
            valueLabel = settings.valueLabel();
            singleRow = settings.singleRow();
            singleRowChevron = settings.singleRowWithChevron();
        }

        ACAQParameterEditorUI keyEditor = ACAQUIParameterTypeRegistry.getInstance().createEditorFor(getWorkbench(), keyAccess);
        ACAQParameterEditorUI valueEditor = ACAQUIParameterTypeRegistry.getInstance().createEditorFor(getWorkbench(), valueAccess);

        if (singleRow) {
            setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
            add(new JLabel(keyLabel));
            if (!StringUtils.isNullOrEmpty(keyLabel))
                add(Box.createHorizontalStrut(4));
            add(keyEditor);
            if (singleRowChevron)
                add(new JLabel(UIUtils.getIconFromResources("chevron-right.png")));
            add(new JLabel(valueLabel));
            if (!StringUtils.isNullOrEmpty(valueLabel))
                add(Box.createHorizontalStrut(4));
            add(valueEditor);
        } else {
            setLayout(new BorderLayout());
            FormPanel panel = new FormPanel(null, FormPanel.NONE);
            panel.setBorder(BorderFactory.createEtchedBorder());
            panel.addToForm(keyEditor, new JLabel(keyLabel), null);
            panel.addToForm(valueEditor, new JLabel(valueLabel), null);
            add(panel, BorderLayout.CENTER);
        }

        revalidate();
        repaint();
    }
}
