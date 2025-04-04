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

package org.hkijena.jipipe.plugins.plots.parameters;

import org.hkijena.jipipe.desktop.api.JIPipeDesktopParameterEditorUI;
import org.hkijena.jipipe.desktop.app.ploteditor.JIPipeDesktopPlotDataSeriesColumnListCellRenderer;
import org.hkijena.jipipe.plugins.parameters.api.enums.DynamicEnumParameter;
import org.hkijena.jipipe.plugins.parameters.api.enums.DynamicEnumParameterSettings;
import org.hkijena.jipipe.plugins.tables.datatypes.TableColumnData;

import javax.swing.*;
import java.awt.*;
import java.util.Objects;

/**
 * Works for {@link TableColumnData}.
 * Does not listen to {@link DynamicEnumParameterSettings}
 */
public class UIPlotDataSeriesColumnEnumDesktopParameterEditorUI extends JIPipeDesktopParameterEditorUI {

    private JComboBox<TableColumnData> comboBox;

    public UIPlotDataSeriesColumnEnumDesktopParameterEditorUI(InitializationParameters parameters) {
        super(parameters);
        initialize();
        reload();
    }

    @Override
    public boolean isUILabelEnabled() {
        return true;
    }

    @Override
    public void reload() {
        DynamicEnumParameter<TableColumnData> parameter = getParameter(DynamicEnumParameter.class);
        if (!Objects.equals(parameter.getValue(), comboBox.getSelectedItem()))
            comboBox.setSelectedItem(parameter.getValue());
    }

    private void initialize() {
        setLayout(new BorderLayout());

        DynamicEnumParameter<TableColumnData> parameter = getParameter(DynamicEnumParameter.class);
        TableColumnData[] values = parameter.getAllowedValues().toArray(new TableColumnData[0]);
        comboBox = new JComboBox<>(new DefaultComboBoxModel<>(values));
        comboBox.setRenderer(new JIPipeDesktopPlotDataSeriesColumnListCellRenderer());
        comboBox.setSelectedItem(parameter.getValue());
        comboBox.addActionListener(e -> {
            parameter.setValue((TableColumnData) comboBox.getSelectedItem());
            setParameter(parameter, false);
        });
        add(comboBox, BorderLayout.CENTER);
    }

}
