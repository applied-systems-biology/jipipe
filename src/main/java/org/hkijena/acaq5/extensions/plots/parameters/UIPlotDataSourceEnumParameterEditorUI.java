package org.hkijena.acaq5.extensions.plots.parameters;

import org.hkijena.acaq5.api.parameters.ACAQParameterAccess;
import org.hkijena.acaq5.api.parameters.DynamicEnumParameter;
import org.hkijena.acaq5.extensions.standardparametereditors.editors.DynamicEnumParameterSettings;
import org.hkijena.acaq5.ui.parameters.ACAQParameterEditorUI;
import org.hkijena.acaq5.ui.plotbuilder.DoubleArrayPlotDataSource;
import org.hkijena.acaq5.ui.plotbuilder.StringArrayPlotDataSource;
import org.hkijena.acaq5.utils.ReflectionUtils;
import org.hkijena.acaq5.utils.UIUtils;
import org.scijava.Context;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.function.Supplier;

/**
 * A parameter editor UI that works for all enumerations
 */
public class UIPlotDataSourceEnumParameterEditorUI extends ACAQParameterEditorUI {

    private boolean skipNextReload = false;
    private boolean isReloading = false;
    private JComboBox<Object> comboBox;

    /**
     * @param context         SciJava context
     * @param parameterAccess the parameter
     */
    public UIPlotDataSourceEnumParameterEditorUI(Context context, ACAQParameterAccess parameterAccess) {
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
        comboBox = new JComboBox<>(new DefaultComboBoxModel<>(values));
        comboBox.setRenderer(new Renderer());
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

    /**
     * Renders entries
     */
    public static class Renderer extends JLabel implements ListCellRenderer<Object> {

        /**
         * Creates a new renderer
         */
        public Renderer() {
            setOpaque(true);
        }

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {

            if (value instanceof DoubleArrayPlotDataSource) {
                DoubleArrayPlotDataSource data = (DoubleArrayPlotDataSource) value;
                setText(data.getName() + " (" + data.getData().length + " rows)");
                setIcon(UIUtils.getIconFromResources("table.png"));
            } else if (value instanceof StringArrayPlotDataSource) {
                StringArrayPlotDataSource data = (StringArrayPlotDataSource) value;
                setText(data.getName() + " (" + data.getData().length + " rows)");
                setIcon(UIUtils.getIconFromResources("table.png"));
            } else {
                setText("<Null>");
                setIcon(null);
            }

            if (isSelected) {
                setBackground(new Color(184, 207, 229));
            } else {
                setBackground(new Color(255, 255, 255));
            }

            return this;
        }
    }
}
