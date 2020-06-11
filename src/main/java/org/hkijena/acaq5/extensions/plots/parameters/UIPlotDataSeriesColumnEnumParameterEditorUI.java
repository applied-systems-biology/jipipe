package org.hkijena.acaq5.extensions.plots.parameters;

import org.hkijena.acaq5.api.parameters.ACAQParameterAccess;
import org.hkijena.acaq5.extensions.parameters.primitives.DynamicEnumParameter;
import org.hkijena.acaq5.extensions.parameters.editors.DynamicEnumParameterSettings;
import org.hkijena.acaq5.extensions.tables.datatypes.TableColumn;
import org.hkijena.acaq5.ui.parameters.ACAQParameterEditorUI;
import org.hkijena.acaq5.ui.plotbuilder.PlotDataSeriesColumnListCellRenderer;
import org.scijava.Context;

import javax.swing.*;
import java.awt.*;

/**
 * Works for {@link TableColumn}.
 * Does not listen to {@link DynamicEnumParameterSettings}
 */
public class UIPlotDataSeriesColumnEnumParameterEditorUI extends ACAQParameterEditorUI {

    private boolean skipNextReload = false;
    private boolean isReloading = false;
    private JComboBox<TableColumn> comboBox;

    /**
     * @param context         SciJava context
     * @param parameterAccess the parameter
     */
    public UIPlotDataSeriesColumnEnumParameterEditorUI(Context context, ACAQParameterAccess parameterAccess) {
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
        TableColumn[] values = parameter.getAllowedValues().toArray(new TableColumn[0]);
        comboBox = new JComboBox<>(new DefaultComboBoxModel<>(values));
        comboBox.setRenderer(new PlotDataSeriesColumnListCellRenderer());
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
