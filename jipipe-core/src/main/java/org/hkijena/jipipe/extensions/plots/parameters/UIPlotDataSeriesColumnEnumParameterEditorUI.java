/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.extensions.plots.parameters;

import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.extensions.parameters.primitives.DynamicEnumParameter;
import org.hkijena.jipipe.extensions.parameters.primitives.DynamicEnumParameterSettings;
import org.hkijena.jipipe.extensions.tables.datatypes.TableColumn;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.parameters.JIPipeParameterEditorUI;
import org.hkijena.jipipe.ui.plotbuilder.PlotDataSeriesColumnListCellRenderer;

import javax.swing.*;
import java.awt.*;

/**
 * Works for {@link TableColumn}.
 * Does not listen to {@link DynamicEnumParameterSettings}
 */
public class UIPlotDataSeriesColumnEnumParameterEditorUI extends JIPipeParameterEditorUI {

    private JComboBox<TableColumn> comboBox;

    /**
     * @param workbench       workbench
     * @param parameterAccess the parameter
     */
    public UIPlotDataSeriesColumnEnumParameterEditorUI(JIPipeWorkbench workbench, JIPipeParameterAccess parameterAccess) {
        super(workbench, parameterAccess);
        initialize();
        reload();
    }

    @Override
    public boolean isUILabelEnabled() {
        return true;
    }

    @Override
    public void reload() {
        DynamicEnumParameter parameter = getParameter(DynamicEnumParameter.class);
        comboBox.setSelectedItem(parameter.getValue());
    }

    private void initialize() {
        setLayout(new BorderLayout());

        DynamicEnumParameter parameter = getParameter(DynamicEnumParameter.class);
        TableColumn[] values = parameter.getAllowedValues().toArray(new TableColumn[0]);
        comboBox = new JComboBox<>(new DefaultComboBoxModel<>(values));
        comboBox.setRenderer(new PlotDataSeriesColumnListCellRenderer());
        comboBox.setSelectedItem(parameter.getValue());
        comboBox.addActionListener(e -> setParameter(parameter, false));
        add(comboBox, BorderLayout.CENTER);
    }

}
