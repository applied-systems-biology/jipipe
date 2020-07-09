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

package org.hkijena.jipipe.extensions.parameters.primitives;

import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.parameters.JIPipeParameterEditorUI;
import org.hkijena.jipipe.utils.ReflectionUtils;

import javax.swing.*;
import java.awt.*;
import java.util.Objects;

/**
 * A parameter editor UI that works for all enumerations
 */
public class EnumParameterEditorUI extends JIPipeParameterEditorUI {

    private JComboBox<Object> comboBox;

    /**
     * @param workbench       workbench
     * @param parameterAccess the parameter
     */
    public EnumParameterEditorUI(JIPipeWorkbench workbench, JIPipeParameterAccess parameterAccess) {
        super(workbench, parameterAccess);
        initialize();
        reload();
    }

    @Override
    public boolean isUILabelEnabled() {
        return true;
    }

    @Override
    public void reload() {
        Object target = getParameterAccess().get(Object.class);
        if (!Objects.equals(target, comboBox.getSelectedItem())) {
            comboBox.setSelectedItem(target);
        }
    }

    private void initialize() {
        setLayout(new BorderLayout());
        Object[] values = getParameterAccess().getFieldClass().getEnumConstants();
        comboBox = new JComboBox<>(values);
        comboBox.setSelectedItem(getParameterAccess().get(Object.class));
        comboBox.addActionListener(e -> {
            setParameter(comboBox.getSelectedItem(), false);
        });
        EnumParameterSettings settings = getParameterAccess().getAnnotationOfType(EnumParameterSettings.class);
        EnumItemInfo info;
        if (settings != null) {
            info = (EnumItemInfo) ReflectionUtils.newInstance(settings.itemInfo());
        } else {
            info = new DefaultEnumItemInfo();
        }
        comboBox.setRenderer(new Renderer(info));

        add(comboBox, BorderLayout.CENTER);
    }

    /**
     * Renders items in enum parameters
     */
    private static class Renderer extends JLabel implements ListCellRenderer<Object> {

        private EnumItemInfo info;

        public Renderer(EnumItemInfo info) {
            this.info = info;
            setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
            setFont(new Font(Font.DIALOG, Font.PLAIN, 12));
            setOpaque(true);
        }

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            setIcon(info.getIcon(value));
            setText(info.getLabel(value));
            setToolTipText(info.getTooltip(value));
            if (isSelected || cellHasFocus) {
                setBackground(new Color(184, 207, 229));
            } else {
                setBackground(new Color(255, 255, 255));
            }
            return this;
        }
    }
}
