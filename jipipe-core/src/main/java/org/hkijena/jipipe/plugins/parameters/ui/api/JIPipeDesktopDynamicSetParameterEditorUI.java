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

import org.hkijena.jipipe.desktop.api.JIPipeDesktopParameterEditorUI;
import org.hkijena.jipipe.desktop.commons.components.selection.JIPipeDesktopRichSelectionBox;
import org.hkijena.jipipe.plugins.parameters.api.options.DynamicSetParameterSettings;
import org.hkijena.jipipe.plugins.parameters.api.options.JIPipeDynamicSetParameter;
import org.hkijena.jipipe.utils.ReflectionUtils;

import javax.swing.*;
import java.awt.BorderLayout;
import java.util.*;
import java.util.function.Supplier;

/**
 * A parameter editor UI for dynamic sets: a multi-select
 * {@link JIPipeDesktopRichSelectionBox} whose button shows a count summary
 * ("Measurements (3)") and whose popup toggles items live (no OK, no
 * collapse) (#1339).
 */
public class JIPipeDesktopDynamicSetParameterEditorUI extends JIPipeDesktopParameterEditorUI<JIPipeDynamicSetParameter> {

    private JIPipeDesktopRichSelectionBox selectionBox;

    public JIPipeDesktopDynamicSetParameterEditorUI(InitializationParameters parameters) {
        super(JIPipeDynamicSetParameter.class, parameters);
        initialize();
    }

    @Override
    public void reload() {
        JIPipeDynamicSetParameter<Object> parameter = getParameter();
        if (!selectionBox.getSelectedValues().equals(parameter.getValues())) {
            selectionBox.setSelectedValues(parameter.getValues());
        }
    }

    private void initialize() {
        setLayout(new BorderLayout());

        JIPipeDynamicSetParameter<Object> parameter = getParameter();
        // The parameter name comes from the form panel's label column, so the
        // button must not repeat it — a neutral prompt + a count summary.
        selectionBox = new JIPipeDesktopRichSelectionBox("Select items", "Select items", true, this::options, selected -> {
            Set<Object> newValues = new LinkedHashSet<>();
            for (JIPipeDesktopRichSelectionBox.Option<?> option : selected) {
                newValues.add(option.getValue());
            }
            parameter.getValues().clear();
            parameter.getValues().addAll(newValues);
            setParameter(parameter, false);
        });
        selectionBox.setCountOnly(true);
        // "Select all" next to the popup's "Clear" (select none).
        selectionBox.setHasSelectAll(true);
        selectionBox.setSelectedValues(parameter.getValues());
        selectionBox.refreshDisplay();
        add(selectionBox, BorderLayout.CENTER);
    }

    private List<JIPipeDesktopRichSelectionBox.Option<?>> options() {
        JIPipeDynamicSetParameter<Object> parameter = getParameter();
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

        List<JIPipeDesktopRichSelectionBox.Option<?>> result = new ArrayList<>();
        for (Object value : values) {
            result.add(new JIPipeDesktopRichSelectionBox.Option<>(value,
                    parameter.renderLabel(value),
                    parameter.renderIcon(value),
                    null,
                    parameter.renderTooltip(value)));
        }
        return result;
    }
}
