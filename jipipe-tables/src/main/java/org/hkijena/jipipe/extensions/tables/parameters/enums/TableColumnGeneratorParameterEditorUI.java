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

package org.hkijena.jipipe.extensions.tables.parameters.enums;

import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataDeclaration;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.registries.JIPipeDatatypeRegistry;
import org.hkijena.jipipe.extensions.tables.ColumnContentType;
import org.hkijena.jipipe.extensions.tables.datatypes.TableColumn;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.components.JIPipeDataDeclarationListCellRenderer;
import org.hkijena.jipipe.ui.parameters.JIPipeParameterEditorUI;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

/**
 * A parameter editor UI that works for all enumerations
 */
public class TableColumnGeneratorParameterEditorUI extends JIPipeParameterEditorUI {

    private boolean isProcessing = false;
    private JComboBox<JIPipeDataDeclaration> comboBox;
    private JToggleButton numericColumnToggle;
    private JToggleButton textColumnToggle;

    /**
     * @param workbench       workbench
     * @param parameterAccess the parameter
     */
    public TableColumnGeneratorParameterEditorUI(JIPipeWorkbench workbench, JIPipeParameterAccess parameterAccess) {
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
        isProcessing = true;
        TableColumnGeneratorParameter parameter = getParameter(TableColumnGeneratorParameter.class);
        comboBox.setSelectedItem(parameter.getGeneratorType().getDeclaration());
        if (parameter.getGeneratedType() == ColumnContentType.NumericColumn) {
            numericColumnToggle.setSelected(true);
        } else {
            textColumnToggle.setSelected(true);
        }
        isProcessing = false;
    }

    private void initialize() {
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        comboBox = new JComboBox<>(getAvailableGenerators());
        comboBox.setRenderer(new JIPipeDataDeclarationListCellRenderer());
        comboBox.addActionListener(e -> writeValueToParameter());
        add(comboBox);

        ButtonGroup buttonGroup = new ButtonGroup();
        numericColumnToggle = addToggle(buttonGroup, UIUtils.getIconFromResources("number.png"), "Numeric column");
        textColumnToggle = addToggle(buttonGroup, UIUtils.getIconFromResources("text.png"), "Text column");
    }

    private void writeValueToParameter() {
        if (isProcessing)
            return;
        isProcessing = true;
        TableColumnGeneratorParameter parameter = getParameter(TableColumnGeneratorParameter.class);
        if (comboBox.getSelectedItem() != null)
            parameter.getGeneratorType().setDeclaration((JIPipeDataDeclaration) comboBox.getSelectedItem());
        else
            parameter.getGeneratorType().setDeclaration(null);
        if (numericColumnToggle.isSelected())
            parameter.setGeneratedType(ColumnContentType.NumericColumn);
        else
            parameter.setGeneratedType(ColumnContentType.StringColumn);
        setParameter(parameter, false);
        isProcessing = false;
    }

    private JIPipeDataDeclaration[] getAvailableGenerators() {
        List<Object> result = new ArrayList<>();
        result.add(null);
        for (Class<? extends JIPipeData> klass : JIPipeDatatypeRegistry.getInstance().getRegisteredDataTypes().values()) {
            if (TableColumn.isGeneratingTableColumn(klass)) {
                result.add(JIPipeDataDeclaration.getInstance(klass));
            }
        }

        return result.toArray(new JIPipeDataDeclaration[0]);
    }

    private JToggleButton addToggle(ButtonGroup group, Icon icon, String description) {
        JToggleButton toggleButton = new JToggleButton(icon);
        UIUtils.makeFlat25x25(toggleButton);
        toggleButton.addActionListener(e -> writeValueToParameter());
        toggleButton.setToolTipText(description);
        group.add(toggleButton);
        add(toggleButton);
        return toggleButton;
    }
}
