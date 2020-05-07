package org.hkijena.acaq5.extensions.plots.parameters;

import org.hkijena.acaq5.api.parameters.ACAQParameterAccess;
import org.hkijena.acaq5.api.parameters.DynamicEnumParameter;
import org.hkijena.acaq5.extensions.parametereditors.editors.DynamicEnumParameterSettings;
import org.hkijena.acaq5.ui.parameters.ACAQParameterEditorUI;
import org.hkijena.acaq5.ui.plotbuilder.PlotDataSource;
import org.hkijena.acaq5.ui.plotbuilder.PlotDataSourceListCellRenderer;
import org.scijava.Context;

import javax.swing.*;
import java.awt.*;

/**
 * Works for {@link PlotDataSource}.
 * Does not listen to {@link DynamicEnumParameterSettings}
 */
public class UIPlotDataSourceEnumParameterEditorUI extends ACAQParameterEditorUI {

    private boolean skipNextReload = false;
    private boolean isReloading = false;
    private JComboBox<PlotDataSource> comboBox;

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
        PlotDataSource[] values = parameter.getAllowedValues().toArray(new PlotDataSource[0]);
        comboBox = new JComboBox<>(new DefaultComboBoxModel<>(values));
        comboBox.setRenderer(new PlotDataSourceListCellRenderer());
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
