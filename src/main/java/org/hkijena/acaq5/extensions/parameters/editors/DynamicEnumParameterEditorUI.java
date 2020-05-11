package org.hkijena.acaq5.extensions.parameters.editors;

import org.hkijena.acaq5.api.parameters.ACAQParameterAccess;
import org.hkijena.acaq5.extensions.parameters.DynamicEnumParameter;
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
        DynamicEnumParameter parameter = getParameterAccess().get();
        comboBox.setSelectedItem(parameter.getValue());
        isReloading = false;
    }

    private void initialize() {
        setLayout(new BorderLayout());

        DynamicEnumParameter parameter = getParameterAccess().get();
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
        add(comboBox, BorderLayout.CENTER);
    }
}
