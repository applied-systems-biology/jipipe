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

package org.hkijena.jipipe.plugins.parameters.api.pairs;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.desktop.api.JIPipeDesktopParameterEditorUI;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopFormPanel;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;

/**
 * Editor for {@link PairParameter}
 */
public class PairDesktopParameterEditorUI extends JIPipeDesktopParameterEditorUI {

    /**
     * Creates new instance
     *
     * @param workbench       workbench
     * @param parameterAccess Parameter
     */
    public PairDesktopParameterEditorUI(JIPipeDesktopWorkbench workbench, JIPipeParameterTree parameterTree, JIPipeParameterAccess parameterAccess) {
        super(workbench, parameterTree, parameterAccess);
        reload();
    }

    @Override
    public boolean isUILabelEnabled() {
        return true;
    }

    @Override
    public void reload() {
        removeAll();

        PairParameter<?, ?> renaming = getParameter(PairParameter.class);
        PairParameterKeyAccess<?, ?> keyAccess = new PairParameterKeyAccess<>(getParameterAccess(), renaming);
        PairParameterValueAccess<?, ?> valueAccess = new PairParameterValueAccess<>(getParameterAccess(), renaming);

        String keyLabel = "Key";
        String valueLabel = "Value";
        boolean singleRow = false;
        boolean singleRowChevron = false;

        if (getParameterAccess().getFieldClass().getAnnotation(PairParameterSettings.class) != null) {
            PairParameterSettings settings = getParameterAccess().getFieldClass().getAnnotation(PairParameterSettings.class);
            keyLabel = settings.keyLabel();
            valueLabel = settings.valueLabel();
            singleRow = settings.singleRow();
            singleRowChevron = settings.singleRowWithChevron();
        }
        if (getParameterAccess().getAnnotationOfType(PairParameterSettings.class) != null) {
            PairParameterSettings settings = getParameterAccess().getAnnotationOfType(PairParameterSettings.class);
            keyLabel = settings.keyLabel();
            valueLabel = settings.valueLabel();
            singleRow = settings.singleRow();
            singleRowChevron = settings.singleRowWithChevron();
        }

        JIPipeDesktopParameterEditorUI keyEditor = JIPipe.getParameterTypes().createEditorFor(getDesktopWorkbench(), getParameterTree(), keyAccess);
        JIPipeDesktopParameterEditorUI valueEditor = JIPipe.getParameterTypes().createEditorFor(getDesktopWorkbench(), getParameterTree(), valueAccess);

        if (singleRow) {
            setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
            add(new JLabel(keyLabel));
            if (!StringUtils.isNullOrEmpty(keyLabel))
                add(Box.createHorizontalStrut(4));
            add(keyEditor);
            if (singleRowChevron)
                add(new JLabel(UIUtils.getIconFromResources("actions/caret-right.png")));
            add(new JLabel(valueLabel));
            if (!StringUtils.isNullOrEmpty(valueLabel))
                add(Box.createHorizontalStrut(4));
            add(valueEditor);
        } else {
            setLayout(new BorderLayout());
            JIPipeDesktopFormPanel panel = new JIPipeDesktopFormPanel(null, JIPipeDesktopFormPanel.NONE);
            panel.setBorder(UIUtils.createControlBorder());
            panel.addToForm(keyEditor, new JLabel(keyLabel), null);
            panel.addToForm(valueEditor, new JLabel(valueLabel), null);
            add(panel, BorderLayout.CENTER);
        }

        revalidate();
        repaint();
    }
}
