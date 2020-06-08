package org.hkijena.acaq5.extensions.parameters.editors;

import org.hkijena.acaq5.api.parameters.ACAQParameterAccess;
import org.hkijena.acaq5.ui.parameters.ACAQParameterEditorUI;
import org.hkijena.acaq5.utils.ReflectionUtils;
import org.scijava.Context;

import javax.swing.*;
import java.awt.*;

/**
 * A parameter editor UI that works for all enumerations
 */
public class EnumParameterEditorUI extends ACAQParameterEditorUI {

    private boolean skipNextReload = false;
    private boolean isReloading = false;
    private JComboBox<Object> comboBox;

    /**
     * @param context         SciJava context
     * @param parameterAccess the parameter
     */
    public EnumParameterEditorUI(Context context, ACAQParameterAccess parameterAccess) {
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
        comboBox.setSelectedItem(getParameterAccess().get(Object.class));
        isReloading = false;
    }

    private void initialize() {
        setLayout(new BorderLayout());
        Object[] values = getParameterAccess().getFieldClass().getEnumConstants();
        comboBox = new JComboBox<>(values);
        comboBox.setSelectedItem(getParameterAccess().get(Object.class));
        comboBox.addActionListener(e -> {
            if (!isReloading) {
                skipNextReload = true;
                if (!getParameterAccess().set(comboBox.getSelectedItem())) {
                    skipNextReload = false;
                    reload();
                }
            }
        });
        EnumParameterSettings settings = getParameterAccess().getAnnotationOfType(EnumParameterSettings.class);
        EnumItemInfo info;
        if(settings != null) {
            info = (EnumItemInfo) ReflectionUtils.newInstance(settings.itemInfo());
        }
        else {
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
        }

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            setIcon(info.getIcon(value));
            setText(info.getLabel(value));
            return this;
        }
    }
}
