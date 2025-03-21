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

package org.hkijena.jipipe.plugins.parameters.api.enums;

import org.hkijena.jipipe.desktop.api.JIPipeDesktopParameterEditorUI;
import org.hkijena.jipipe.utils.ReflectionUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.function.Supplier;

/**
 * A parameter editor UI that works for all enumerations
 */
public class DynamicSetDesktopParameterEditorUI extends JIPipeDesktopParameterEditorUI {

    private Map<Object, JCheckBox> checkBoxMap = new HashMap<>();
    private JToggleButton collapseToggle;
    private JLabel collapseInfoLabel;

    public DynamicSetDesktopParameterEditorUI(InitializationParameters parameters) {
        super(parameters);
        initialize();
        reload();
    }

    @Override
    public boolean isUILabelEnabled() {
        return false;
    }

    @Override
    public boolean isUIImportantLabelEnabled() {
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
        for (JCheckBox checkBox : checkBoxMap.values()) {
            checkBox.setVisible(!parameter.isCollapsed());
        }
        collapseInfoLabel.setVisible(parameter.isCollapsed());
        collapseToggle.setSelected(parameter.isCollapsed());
        revalidate();
        repaint();
    }


    private void initialize() {
        setLayout(new BorderLayout());
        setBorder(UIUtils.createControlBorder());

        DynamicSetParameter<Object> parameter = getParameter(DynamicSetParameter.class);
        Object[] values;
        if (parameter.getAllowedValues() != null && !parameter.getAllowedValues().isEmpty()) {
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

        collapseInfoLabel = new JLabel("The available items are hidden. Click the 'Collapse' button to show it",
                UIUtils.getIconFromResources("actions/eye-slash.png"),
                JLabel.LEFT);
        collapseInfoLabel.setFont(new Font(Font.DIALOG, Font.PLAIN, 12));
        contentPanel.add(collapseInfoLabel);

        JToolBar toolBar = new JToolBar();
        toolBar.add(Box.createHorizontalStrut(4));

        JLabel nameLabel = new JLabel(getParameterAccess().getName());
        if (getParameterAccess().isImportant()) {
            nameLabel.setIcon(UIUtils.getIconFromResources("emblems/important.png"));
        }
        toolBar.add(nameLabel);
        toolBar.add(Box.createHorizontalGlue());

        collapseToggle = new JToggleButton("Collapse",
                UIUtils.getIconFromResources("actions/eye-slash.png"));
        toolBar.add(collapseToggle);
        collapseToggle.addActionListener(e -> saveCollapsedState());

        JButton selectAllButton = new JButton("Select all", UIUtils.getIconFromResources("actions/stock_select-all.png"));
        selectAllButton.addActionListener(e -> selectAll());
        toolBar.add(selectAllButton);

        JButton selectNoneButton = new JButton("Select none", UIUtils.getIconFromResources("actions/cancel.png"));
        selectNoneButton.addActionListener(e -> selectNone());
        toolBar.add(selectNoneButton);

        add(toolBar, BorderLayout.NORTH);
        add(contentPanel, BorderLayout.CENTER);
    }

    private void saveCollapsedState() {
        DynamicSetParameter<Object> parameter = getParameter(DynamicSetParameter.class);
        if (parameter.isCollapsed() != collapseToggle.isSelected()) {
            parameter.setCollapsed(collapseToggle.isSelected());
            setParameter(parameter, true);
        }
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
