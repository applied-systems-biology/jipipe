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
import org.hkijena.jipipe.desktop.api.JIPipeDesktopParameterEditorUI;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopPickDynamicEnumValueDialog;
import org.hkijena.jipipe.plugins.parameters.api.enums.DynamicEnumParameterSettings;
import org.hkijena.jipipe.plugins.parameters.api.enums.EnumParameterSettings;
import org.hkijena.jipipe.plugins.parameters.api.enums.JIPipeDynamicEnumParameter;
import org.hkijena.jipipe.plugins.parameters.api.enums.JIPipeEnumItemInfoRenderTarget;
import org.hkijena.jipipe.utils.ReflectionUtils;
import org.hkijena.jipipe.utils.ThemeUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * A parameter editor UI that works for all enumerations
 */
public class JIPipeDesktopDynamicEnumParameterEditorUI extends JIPipeDesktopParameterEditorUI<JIPipeDynamicEnumParameter> {

    private JComboBox<Object> comboBox;
    private JButton currentlyDisplayed;

    public JIPipeDesktopDynamicEnumParameterEditorUI(InitializationParameters parameters) {
        super(JIPipeDynamicEnumParameter.class, parameters);
        initialize();
        reload();
    }

    @Override
    public boolean isUILabelEnabled() {
        return true;
    }

    @Override
    public void reload() {
        JIPipeDynamicEnumParameter<Object> parameter = getParameter();
        if (!Objects.equals(parameter.getValue(), comboBox.getSelectedItem())) {
            comboBox.setSelectedItem(parameter.getValue());
        }
    }

    private void initialize() {
        setLayout(new BorderLayout());

        EnumParameterSettings enumSettings = getParameterAccess().getAnnotationOfType(EnumParameterSettings.class);

        JIPipeDynamicEnumParameter<Object> parameter = getParameter();
        Object[] values;
        if (parameter.getAllowedValues() != null) {
            values = parameter.getAllowedValues().toArray();
        } else {
            DynamicEnumParameterSettings dynamicEnumSettings = getParameterAccess().getAnnotationOfType(DynamicEnumParameterSettings.class);
            if (dynamicEnumSettings != null) {
                Supplier<List<Object>> supplier = (Supplier<List<Object>>) ReflectionUtils.newInstance(dynamicEnumSettings.supplier());
                values = supplier.get().toArray();
            } else {
                values = new Object[0];
                System.err.println("In " + this + ": " + getParameterAccess().getFieldClass() + " not provided with a generator supplier!");
            }
        }

        comboBox = new JComboBox<>(values);
        comboBox.setEditable(parameter.isEditable());
        comboBox.setSelectedItem(parameter.getValue());
        comboBox.addActionListener(e -> {
            parameter.setValue(comboBox.getSelectedItem());
            setParameter(parameter, false);
        });
        comboBox.setRenderer(new Renderer(parameter, JIPipeEnumItemInfoRenderTarget.ComboBox));
        add(comboBox, BorderLayout.CENTER);

        JButton selectButton = new JButton(JIPipe.RESOURCES.getIcon16("actions/edit.png"));
        UIUtils.setStandardButtonBorder(selectButton);
        selectButton.setToolTipText("Select value");
        selectButton.addActionListener(e -> pickEnum());
        add(selectButton, BorderLayout.EAST);
    }

    private void pickEnum() {
        JIPipeDynamicEnumParameter target = getParameterAccess().get(JIPipeDynamicEnumParameter.class);
        Object selected = JIPipeDesktopPickDynamicEnumValueDialog.showDialog(this, target, target.getValue(), "Select value");
        if (selected != null) {
            target.setValue(selected);
            setParameter(target, true);
        }
    }

    /**
     * Renders items in enum parameters
     */
    public static class Renderer<T> extends JLabel implements ListCellRenderer<T> {

        private final JIPipeDynamicEnumParameter<Object> parameter;
        private final JIPipeEnumItemInfoRenderTarget renderTarget;

        public Renderer(JIPipeDynamicEnumParameter<Object> parameter, JIPipeEnumItemInfoRenderTarget renderTarget) {
            this.parameter = parameter;
            this.renderTarget = renderTarget;
            setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
            setFont(new Font(Font.DIALOG, Font.PLAIN, ThemeUtils.getCurrentStyle().getFontSizeNormal()));
            setOpaque(true);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends T> list, T value, int index, boolean isSelected, boolean cellHasFocus) {
            setIcon(parameter.renderIcon(value, renderTarget));
            setText(parameter.renderLabel(value, renderTarget));
            setToolTipText(parameter.renderTooltip(value, renderTarget));
            if (isSelected || cellHasFocus) {
                setBackground(UIManager.getColor("List.selectionBackground"));
            } else {
                setBackground(UIManager.getColor("List.background"));
            }
            return this;
        }
    }
}
