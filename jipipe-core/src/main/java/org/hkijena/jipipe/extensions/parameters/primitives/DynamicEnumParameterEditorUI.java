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
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * A parameter editor UI that works for all enumerations
 */
public class DynamicEnumParameterEditorUI extends JIPipeParameterEditorUI {

    private JComboBox<Object> comboBox;

    /**
     * @param workbench       workbench
     * @param parameterAccess the parameter
     */
    public DynamicEnumParameterEditorUI(JIPipeWorkbench workbench, JIPipeParameterAccess parameterAccess) {
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
        DynamicEnumParameter<Object> parameter = getParameter(DynamicEnumParameter.class);
        if (!Objects.equals(parameter.getValue(), comboBox.getSelectedItem())) {
            comboBox.setSelectedItem(parameter.getValue());
        }
    }

    private void initialize() {
        setLayout(new BorderLayout());

        DynamicEnumParameter<Object> parameter = getParameter(DynamicEnumParameter.class);
        Object[] values;
        if (parameter.getAllowedValues() != null) {
            values = parameter.getAllowedValues().toArray();
        } else {
            DynamicEnumParameterSettings settings = getParameterAccess().getAnnotationOfType(DynamicEnumParameterSettings.class);
            if (settings != null) {
                Supplier<List<Object>> supplier = (Supplier<List<Object>>) ReflectionUtils.newInstance(settings.supplier());
                values = supplier.get().toArray();
            } else {
                values = new Object[0];
                System.err.println("In " + this + ": " + getParameterAccess().getFieldClass() + " not provided with a generator supplier!");
            }
        }
        comboBox = new JComboBox<>(values);
        comboBox.setSelectedItem(parameter.getValue());
        comboBox.addActionListener(e -> {
            parameter.setValue(comboBox.getSelectedItem());
            setParameter(parameter, false);
        });
        comboBox.setRenderer(new Renderer(parameter));
        add(comboBox, BorderLayout.CENTER);
    }

    /**
     * Renders items in enum parameters
     */
    private static class Renderer extends JLabel implements ListCellRenderer<Object> {

        private final DynamicEnumParameter<Object> parameter;

        public Renderer(DynamicEnumParameter<Object> parameter) {
            this.parameter = parameter;
            setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
            setFont(new Font(Font.DIALOG, Font.PLAIN, 12));
            setOpaque(true);
        }

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            setIcon(parameter.renderIcon(value));
            setText(parameter.renderLabel(value));
            setToolTipText(parameter.renderTooltip(value));
            if (isSelected || cellHasFocus) {
                setBackground(UIManager.getColor("List.selectionBackground"));
            } else {
                setBackground(UIManager.getColor("List.background"));
            }
            return this;
        }
    }
}
