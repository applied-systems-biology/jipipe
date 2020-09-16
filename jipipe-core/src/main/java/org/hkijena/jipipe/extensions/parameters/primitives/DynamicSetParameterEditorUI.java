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
import org.hkijena.jipipe.utils.ModernMetalTheme;
import org.hkijena.jipipe.utils.ReflectionUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.BorderLayout;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * A parameter editor UI that works for all enumerations
 */
public class DynamicSetParameterEditorUI extends JIPipeParameterEditorUI {

    private Map<Object, JCheckBox> checkBoxMap = new HashMap<>();

    /**
     * @param workbench       workbench
     * @param parameterAccess the parameter
     */
    public DynamicSetParameterEditorUI(JIPipeWorkbench workbench, JIPipeParameterAccess parameterAccess) {
        super(workbench, parameterAccess);
        initialize();
        reload();
    }

    @Override
    public boolean isUILabelEnabled() {
        return false;
    }

    private Set<Object> getCurrentlySelected() {
        Set<Object> result = new HashSet<>();
        for (Map.Entry<Object, JCheckBox> entry : checkBoxMap.entrySet()) {
            if (entry.getValue().isSelected()) {
                result.add(entry.getKey());
            }
        }

        return result;
    }

    @Override
    public void reload() {
        DynamicSetParameter<Object> parameter = getParameter(DynamicSetParameter.class);
        Set<Object> currentlySelected = getCurrentlySelected();
        if (!currentlySelected.equals(parameter.getValues())) {
            for (Map.Entry<Object, JCheckBox> entry : checkBoxMap.entrySet()) {
                entry.getValue().setSelected(!parameter.getValues().contains(entry.getKey()));
            }
        }
    }


    private void initialize() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createLineBorder(ModernMetalTheme.MEDIUM_GRAY));

        DynamicSetParameter<Object> parameter = getParameter(DynamicSetParameter.class);
        Object[] values;
        if (parameter.getAllowedValues() != null) {
            values = parameter.getAllowedValues().toArray();
        } else {
            DynamicSetParameterSettings settings = getParameterAccess().getAnnotationOfType(DynamicSetParameterSettings.class);
            if (settings != null) {
                Supplier<List<Object>> supplier = (Supplier<List<Object>>) ReflectionUtils.newInstance(settings.supplier());
                values = supplier.get().toArray();
            } else {
                values = new Object[0];
                System.err.println("In " + this + ": " + getParameterAccess().getFieldClass() + " not provided with a generator supplier!");
            }
        }
        Arrays.sort(values, Comparator.comparing(parameter::renderLabel));
        checkBoxMap.clear();
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        for (Object value : values) {
            JCheckBox checkBox = new JCheckBox(parameter.renderLabel(value),
                    parameter.getValues().contains(value));
            checkBox.setToolTipText(parameter.renderTooltip(value));
            checkBox.addActionListener(e -> {
                if (checkBox.isSelected()) {
                    parameter.getValues().add(value);
                    setParameter(parameter, false);
                } else {
                    parameter.getValues().remove(value);
                    setParameter(parameter, false);
                }
            });
            checkBoxMap.put(value, checkBox);
            contentPanel.add(checkBox);
        }

        JToolBar toolBar = new JToolBar();
        toolBar.add(Box.createHorizontalStrut(4));
        toolBar.add(new JLabel(getParameterAccess().getName()));
        toolBar.add(Box.createHorizontalGlue());

        JButton selectAllButton = new JButton("Select all", UIUtils.getIconFromResources("actions/stock_select-all.png"));
        selectAllButton.addActionListener(e -> selectAll());
        toolBar.add(selectAllButton);

        JButton selectNoneButton = new JButton("Select none", UIUtils.getIconFromResources("actions/cancel.png"));
        selectNoneButton.addActionListener(e -> selectNone());
        toolBar.add(selectNoneButton);

        add(toolBar, BorderLayout.NORTH);
        add(contentPanel, BorderLayout.CENTER);
    }

    private void selectNone() {
        for (JCheckBox checkBox : checkBoxMap.values()) {
            checkBox.setSelected(false);
        }
        DynamicSetParameter<Object> parameter = getParameter(DynamicSetParameter.class);
        parameter.getValues().clear();
        setParameter(parameter, false);
    }

    private void selectAll() {
        for (JCheckBox checkBox : checkBoxMap.values()) {
            checkBox.setSelected(true);
        }
        DynamicSetParameter<Object> parameter = getParameter(DynamicSetParameter.class);
        parameter.getValues().addAll(checkBoxMap.keySet());
        setParameter(parameter, false);
    }
}
