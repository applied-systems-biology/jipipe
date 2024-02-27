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

package org.hkijena.jipipe.extensions.parameters.library.primitives;

import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.parameters.JIPipeParameterEditorUI;

import javax.swing.*;
import java.awt.*;

/**
 * Parameter editor for boolean data
 */
public class BooleanParameterEditorUI extends JIPipeParameterEditorUI {

    private JCheckBox checkBox;
    private JComboBox<Boolean> comboBox;
    private boolean isInComboBoxMode;
    private boolean skipNextReload = false;
    private boolean isReloading = false;

    /**
     * @param workbench       workbench
     * @param parameterAccess the parameter
     */
    public BooleanParameterEditorUI(JIPipeWorkbench workbench, JIPipeParameterTree parameterTree, JIPipeParameterAccess parameterAccess) {
        super(workbench, parameterTree, parameterAccess);
        initialize();
    }

    @Override
    public boolean isUILabelEnabled() {
        return isInComboBoxMode;
    }

    @Override
    public void reload() {
        if (skipNextReload) {
            skipNextReload = false;
            return;
        }
        isReloading = true;
        Object value = getParameterAccess().get(Object.class);
        boolean booleanValue = false;
        if (value != null)
            booleanValue = (boolean) value;

        if (!isInComboBoxMode) {
            checkBox.setSelected(booleanValue);
        } else {
            comboBox.setSelectedItem(booleanValue);
        }

        isReloading = false;
    }

    private void initialize() {
        setLayout(new BorderLayout());
        Object value = getParameterAccess().get(Object.class);
        boolean booleanValue = false;
        if (value != null)
            booleanValue = (boolean) value;

        comboBox = new JComboBox<>(new Boolean[]{true, false});
        comboBox.addActionListener(e -> {
            if (!isReloading) {
                skipNextReload = true;
                setParameter(comboBox.getSelectedItem(), false);
            }
        });

        checkBox = new JCheckBox(getParameterAccess().getName());
        checkBox.addActionListener(e -> {
            if (!isReloading) {
                skipNextReload = true;
                setParameter(checkBox.isSelected(), false);
            }
        });

        BooleanParameterSettings settings = getParameterAccess().getAnnotationOfType(BooleanParameterSettings.class);
        if (settings == null || !settings.comboBoxStyle()) {
            isInComboBoxMode = false;
            checkBox.setSelected(booleanValue);
            add(checkBox, BorderLayout.CENTER);
        } else {
            isInComboBoxMode = true;
            isReloading = true;
            comboBox.setSelectedItem(booleanValue);
            isReloading = false;
            add(comboBox, BorderLayout.CENTER);
        }

        updateComboBoxDisplay();
    }

    @Override
    public int getUIControlStyleType() {
        return CONTROL_STYLE_CHECKBOX;
    }

    private void updateComboBoxDisplay() {
        BooleanParameterSettings settings = getParameterAccess().getAnnotationOfType(BooleanParameterSettings.class);
        if (settings != null) {
            comboBox.setRenderer(new BooleanComboBoxItemRenderer(settings.trueLabel(), settings.falseLabel()));
        }
    }

    public static class BooleanComboBoxItemRenderer extends JLabel implements ListCellRenderer<Boolean> {

        private final String trueLabel;
        private final String falseLabel;

        public BooleanComboBoxItemRenderer(String trueLabel, String falseLabel) {
            setOpaque(true);
            setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
            this.trueLabel = trueLabel;
            this.falseLabel = falseLabel;
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends Boolean> list, Boolean value, int index, boolean isSelected, boolean cellHasFocus) {
            if (value)
                setText(trueLabel);
            else
                setText(falseLabel);
            if (isSelected) {
                setBackground(UIManager.getColor("List.selectionBackground"));
            } else {
                setBackground(UIManager.getColor("List.background"));
            }
            return this;
        }
    }
}
