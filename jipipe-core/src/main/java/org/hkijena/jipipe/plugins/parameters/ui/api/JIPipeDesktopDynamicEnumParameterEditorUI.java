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
import org.hkijena.jipipe.plugins.parameters.api.enums.DynamicEnumParameterSettings;
import org.hkijena.jipipe.plugins.parameters.api.enums.JIPipeDynamicEnumParameter;
import org.hkijena.jipipe.plugins.parameters.api.enums.JIPipeEnumItemInfoRenderTarget;
import org.hkijena.jipipe.utils.ReflectionUtils;
import org.hkijena.jipipe.utils.ThemeUtils;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.function.Supplier;

/**
 * A parameter editor UI that works for all dynamic enumerations.
 * <p>
 * #1339: editable dynamic enums keep the editable combo (free-text input) in
 * all cases. Non-editable ones render a plain combo when the option set is
 * small (<= {@link EnumUiSelection#COMBO_MAX_VALUES}) and a
 * {@link JIPipeDesktopRichSelectionBox} (searchable popup) otherwise. Because
 * dynamic option sets can grow or shrink at runtime, the editor rebuilds its
 * control when the option count crosses the threshold (never dropping the
 * current value).
 */
public class JIPipeDesktopDynamicEnumParameterEditorUI extends JIPipeDesktopParameterEditorUI<JIPipeDynamicEnumParameter> {

    private JComboBox<Object> comboBox;
    private JIPipeDesktopRichSelectionBox selectionBox;
    /**
     * The mode the currently shown control was built for (null = not built).
     */
    private EnumUiMode currentMode;
    /**
     * Guards programmatic {@code setSelectedItem} from re-firing the action listener.
     */
    private boolean isReloading = false;

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
        Object[] values = allowedValues(parameter);

        EnumUiMode wanted = EnumUiSelection.mode(values.length, true, parameter.isEditable());
        if (currentMode != wanted || comboBox == null && selectionBox == null) {
            rebuild(wanted, parameter, values);
        } else if (comboBox != null) {
            if (!Objects.equals(parameter.getValue(), comboBox.getSelectedItem())) {
                isReloading = true;
                comboBox.setSelectedItem(parameter.getValue());
                isReloading = false;
            }
        } else if (selectionBox != null) {
            if (!selectionBox.getSelectedValues().contains(parameter.getValue())) {
                selectionBox.setSelectedValues(parameter.getValue() == null ? List.of() : List.of(parameter.getValue()));
            }
        }
    }

    private void initialize() {
        setLayout(new BorderLayout());
        // The control is created in reload() (which runs after super()).
    }

    /**
     * The allowed values of the parameter (from the parameter or the settings supplier).
     */
    private Object[] allowedValues(JIPipeDynamicEnumParameter<Object> parameter) {
        if (parameter.getAllowedValues() != null) {
            return parameter.getAllowedValues().toArray();
        }
        DynamicEnumParameterSettings dynamicEnumSettings = getParameterAccess().getAnnotationOfType(DynamicEnumParameterSettings.class);
        if (dynamicEnumSettings != null) {
            Supplier<List<Object>> supplier = (Supplier<List<Object>>) ReflectionUtils.newInstance(dynamicEnumSettings.supplier());
            return supplier.get().toArray();
        }
        System.err.println("In " + this + ": " + getParameterAccess().getFieldClass() + " not provided with a generator supplier!");
        return new Object[0];
    }

    private void rebuild(EnumUiMode mode, JIPipeDynamicEnumParameter<Object> parameter, Object[] values) {
        // Remove the old control (if any) so the mode flip is clean.
        removeAll();
        comboBox = null;
        selectionBox = null;
        currentMode = mode;

        Object[] sorted = values.clone();
        Arrays.sort(sorted, Comparator.comparing(v -> renderLabel(v, parameter)));

        if (mode == EnumUiMode.COMBO) {
            comboBox = new JComboBox<>(sorted);
            comboBox.setEditable(parameter.isEditable());
            isReloading = true;
            comboBox.setSelectedItem(parameter.getValue());
            isReloading = false;
            comboBox.addActionListener(e -> {
                if (!isReloading) {
                    parameter.setValue(comboBox.getSelectedItem());
                    setParameter(parameter, false);
                }
            });
            comboBox.setRenderer(new Renderer(parameter, JIPipeEnumItemInfoRenderTarget.ComboBox));
            add(comboBox, BorderLayout.CENTER);
        } else {
            selectionBox = new JIPipeDesktopRichSelectionBox("Select value", false,
                    () -> boxOptions(parameter, sorted),
                    selected -> {
                        if (!selected.isEmpty()) {
                            parameter.setValue(selected.get(0).getValue());
                            setParameter(parameter, true);
                        }
                    });
            add(selectionBox, BorderLayout.CENTER);
            selectionBox.setSelectedValues(parameter.getValue() == null ? List.of() : List.of(parameter.getValue()));
            selectionBox.refreshDisplay();
        }
        revalidate();
        repaint();
    }

    private List<JIPipeDesktopRichSelectionBox.Option<?>> boxOptions(JIPipeDynamicEnumParameter<Object> parameter, Object[] sorted) {
        List<JIPipeDesktopRichSelectionBox.Option<?>> result = new ArrayList<>();
        for (Object value : sorted) {
            result.add(new JIPipeDesktopRichSelectionBox.Option<>(value,
                    renderLabel(value, parameter),
                    renderIcon(value, parameter),
                    null,
                    renderTooltip(value, parameter)));
        }
        return result;
    }

    private String renderLabel(Object value, JIPipeDynamicEnumParameter<Object> parameter) {
        return parameter.renderLabel(value, JIPipeEnumItemInfoRenderTarget.ComboBox);
    }

    private Icon renderIcon(Object value, JIPipeDynamicEnumParameter<Object> parameter) {
        return parameter.renderIcon(value, JIPipeEnumItemInfoRenderTarget.List);
    }

    private String renderTooltip(Object value, JIPipeDynamicEnumParameter<Object> parameter) {
        return parameter.renderTooltip(value, JIPipeEnumItemInfoRenderTarget.List);
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
