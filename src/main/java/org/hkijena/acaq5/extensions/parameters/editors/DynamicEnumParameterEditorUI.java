package org.hkijena.acaq5.extensions.parameters.editors;

import org.hkijena.acaq5.api.parameters.ACAQParameterAccess;
import org.hkijena.acaq5.extensions.parameters.primitives.DynamicEnumParameter;
import org.hkijena.acaq5.ui.parameters.ACAQParameterEditorUI;
import org.hkijena.acaq5.utils.ReflectionUtils;
import org.scijava.Context;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.function.Supplier;

/**
 * A parameter editor UI that works for all enumerations
 */
public class DynamicEnumParameterEditorUI extends ACAQParameterEditorUI {

    private boolean skipNextReload = false;
    private boolean isReloading = false;
    private JComboBox<Object> comboBox;

    /**
     * @param context         SciJava context
     * @param parameterAccess the parameter
     */
    public DynamicEnumParameterEditorUI(Context context, ACAQParameterAccess parameterAccess) {
        super(context, parameterAccess);
        initialize();
        reload();
    }

    @Override
    public boolean isUILabelEnabled() {
        return true;
    }

    @Override
    public void reload() {
        if (skipNextReload) {
            skipNextReload = false;
            return;
        }
        isReloading = true;
        DynamicEnumParameter parameter = getParameterAccess().get(DynamicEnumParameter.class);
        comboBox.setSelectedItem(parameter.getValue());
        isReloading = false;
    }

    private void initialize() {
        setLayout(new BorderLayout());

        DynamicEnumParameter parameter = getParameterAccess().get(DynamicEnumParameter.class);
        Object[] values;
        if (parameter.getAllowedValues() != null) {
            values = parameter.getAllowedValues().toArray();
        } else {
            DynamicEnumParameterSettings settings = getParameterAccess().getAnnotationOfType(DynamicEnumParameterSettings.class);
            Supplier<List<Object>> supplier = (Supplier<List<Object>>) ReflectionUtils.newInstance(settings.supplier());
            values = supplier.get().toArray();
        }
        comboBox = new JComboBox<>(values);
        comboBox.setSelectedItem(parameter.getValue());
        comboBox.addActionListener(e -> {
            if (!isReloading) {
                skipNextReload = true;
                parameter.setValue(comboBox.getSelectedItem());
                if (!getParameterAccess().set(parameter)) {
                    skipNextReload = false;
                    reload();
                }
            }
        });
        comboBox.setRenderer(new Renderer(parameter));
        add(comboBox, BorderLayout.CENTER);
    }

    /**
     * Renders items in enum parameters
     */
    private static class Renderer extends JLabel implements ListCellRenderer<Object> {

        private final DynamicEnumParameter parameter;

        public Renderer(DynamicEnumParameter parameter) {
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
                setBackground(new Color(184, 207, 229));
            } else {
                setBackground(new Color(255, 255, 255));
            }
            return this;
        }
    }
}
