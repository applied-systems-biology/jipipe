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

package org.hkijena.jipipe.extensions.parameters.roi;

import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.parameters.JIPipeParameterEditorUI;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.Dimension;

/**
 * Editor for {@link IntModificationParameter}
 */
public class IntModificationParameterEditorUI extends JIPipeParameterEditorUI {

    /**
     * @param workbench       workbench
     * @param parameterAccess the parameter
     */
    public IntModificationParameterEditorUI(JIPipeWorkbench workbench, JIPipeParameterAccess parameterAccess) {
        super(workbench, parameterAccess);
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        reload();
    }

    @Override
    public boolean isUILabelEnabled() {
        return true;
    }

    @Override
    public void reload() {
        removeAll();
        IntModificationParameter modification = getParameter(IntModificationParameter.class);
        if (modification.isUseExactValue()) {
            SpinnerNumberModel model = new SpinnerNumberModel(modification.getExactValue(), Integer.MIN_VALUE, Integer.MAX_VALUE, 1);
            JSpinner spinner = new JSpinner(model);
            spinner.addChangeListener(e -> {
                modification.setExactValue((int) model.getNumber());
                setParameter(modification, false);
            });
            spinner.setPreferredSize(new Dimension(100, spinner.getPreferredSize().height));
            add(spinner);
        } else {
            SpinnerNumberModel model = new SpinnerNumberModel(modification.getFactor(), 0.0, Double.MAX_VALUE, 0.1d);
            JSpinner spinner = new JSpinner(model);
            spinner.addChangeListener(e -> {
                modification.setFactor((double) model.getNumber());
                setParameter(modification, false);
            });
            spinner.setPreferredSize(new Dimension(100, spinner.getPreferredSize().height));
            add(spinner);
        }

        ButtonGroup group = new ButtonGroup();
        addModeSelection(modification,
                group,
                UIUtils.getIconFromResources("actions/equals.png"),
                true,
                "Set to exact value");
        addModeSelection(modification,
                group,
                UIUtils.getIconFromResources("actions/format-number-relative.png"),
                false,
                "Set to relative value (1 equals identity)");
        revalidate();
        repaint();
    }

    private void addModeSelection(IntModificationParameter modification, ButtonGroup group, Icon icon, boolean mode, String description) {
        JToggleButton toggleButton = new JToggleButton(icon);
        UIUtils.makeFlat25x25(toggleButton);
        toggleButton.addActionListener(e -> {
            if (toggleButton.isSelected())
                modification.setUseExactValue(mode);
            reload();
        });
        toggleButton.setToolTipText(description);
        toggleButton.setSelected(modification.isUseExactValue() == mode);
        group.add(toggleButton);
        add(toggleButton);
    }
}
