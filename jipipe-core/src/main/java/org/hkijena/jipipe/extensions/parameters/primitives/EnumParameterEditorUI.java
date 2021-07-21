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
import org.hkijena.jipipe.ui.components.PickEnumValueDialog;
import org.hkijena.jipipe.ui.parameters.JIPipeParameterEditorUI;
import org.hkijena.jipipe.utils.ReflectionUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Font;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;

/**
 * A parameter editor UI that works for all enumerations
 */
public class EnumParameterEditorUI extends JIPipeParameterEditorUI {

    private JComboBox<Object> comboBox;
    private JButton currentlyDisplayed;
    private boolean isComboBox = true;
    private EnumItemInfo enumItemInfo = new DefaultEnumItemInfo();

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

        if(isComboBox) {
            if (!Objects.equals(target, comboBox.getSelectedItem())) {
                comboBox.setSelectedItem(target);
            }
        }
        else {
            currentlyDisplayed.setIcon(enumItemInfo.getIcon(target));
            currentlyDisplayed.setToolTipText(enumItemInfo.getTooltip(target));
            currentlyDisplayed.setText(enumItemInfo.getLabel(target));
        }
    }

    private void initialize() {
        setLayout(new BorderLayout());

        // Combo box style
        Object[] values = getParameterAccess().getFieldClass().getEnumConstants();
        EnumParameterSettings settings = getParameterAccess().getAnnotationOfType(EnumParameterSettings.class);
        if (settings != null) {
            enumItemInfo = (EnumItemInfo) ReflectionUtils.newInstance(settings.itemInfo());
            isComboBox = !settings.searchable();
        }

        if(isComboBox) {
            Arrays.sort(values, Comparator.comparing(enumItemInfo::getLabel));
            comboBox = new JComboBox<>(values);
            comboBox.setSelectedItem(getParameterAccess().get(Object.class));
            comboBox.addActionListener(e -> {
                setParameter(comboBox.getSelectedItem(), false);
            });
            comboBox.setRenderer(new Renderer(enumItemInfo));
            add(comboBox, BorderLayout.CENTER);
        }
        else {
            currentlyDisplayed = new JButton();
            currentlyDisplayed.setHorizontalAlignment(SwingConstants.LEFT);
            currentlyDisplayed.addActionListener(e -> pickEnum());
            UIUtils.makeFlat(currentlyDisplayed);
            add(currentlyDisplayed, BorderLayout.CENTER);

            JButton selectButton = new JButton(UIUtils.getIconFromResources("actions/edit.png"));
            UIUtils.makeFlat(selectButton);
            selectButton.setToolTipText("Select value");
            selectButton.addActionListener(e -> pickEnum());
            add(selectButton, BorderLayout.EAST);
        }
    }

    private void pickEnum() {
        Object[] values = getParameterAccess().getFieldClass().getEnumConstants();
        Object target = getParameterAccess().get(Object.class);
        Object selected = PickEnumValueDialog.showDialog(getWorkbench().getWindow(), Arrays.asList(values), enumItemInfo, target, "Select value");
        if(selected != null) {
            setParameter(selected, true);
        }
    }

    /**
     * Renders items in enum parameters
     */
    public static class Renderer extends JLabel implements ListCellRenderer<Object> {

        private final EnumItemInfo info;

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
                setBackground(UIManager.getColor("List.selectionBackground"));
            } else {
                setBackground(UIManager.getColor("List.background"));
            }
            return this;
        }
    }
}
