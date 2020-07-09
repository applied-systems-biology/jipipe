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

package org.hkijena.pipelinej.extensions.tables.parameters.enums;

import org.hkijena.pipelinej.api.data.ACAQData;
import org.hkijena.pipelinej.api.data.ACAQDataDeclaration;
import org.hkijena.pipelinej.api.parameters.ACAQParameterAccess;
import org.hkijena.pipelinej.api.registries.ACAQDatatypeRegistry;
import org.hkijena.pipelinej.extensions.tables.ColumnContentType;
import org.hkijena.pipelinej.extensions.tables.TableColumn;
import org.hkijena.pipelinej.ui.ACAQWorkbench;
import org.hkijena.pipelinej.ui.components.ACAQDataDeclarationListCellRenderer;
import org.hkijena.pipelinej.ui.parameters.ACAQParameterEditorUI;
import org.hkijena.pipelinej.utils.UIUtils;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

/**
 * A parameter editor UI that works for all enumerations
 */
public class TableColumnGeneratorParameterEditorUI extends ACAQParameterEditorUI {

    private boolean isProcessing = false;
    private JComboBox<ACAQDataDeclaration> comboBox;
    private JToggleButton numericColumnToggle;
    private JToggleButton textColumnToggle;

    /**
     * @param workbench       workbench
     * @param parameterAccess the parameter
     */
    public TableColumnGeneratorParameterEditorUI(ACAQWorkbench workbench, ACAQParameterAccess parameterAccess) {
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
        comboBox.setRenderer(new ACAQDataDeclarationListCellRenderer());
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
            parameter.getGeneratorType().setDeclaration((ACAQDataDeclaration) comboBox.getSelectedItem());
        else
            parameter.getGeneratorType().setDeclaration(null);
        if (numericColumnToggle.isSelected())
            parameter.setGeneratedType(ColumnContentType.NumericColumn);
        else
            parameter.setGeneratedType(ColumnContentType.StringColumn);
        setParameter(parameter, false);
        isProcessing = false;
    }

    private ACAQDataDeclaration[] getAvailableGenerators() {
        List<Object> result = new ArrayList<>();
        result.add(null);
        for (Class<? extends ACAQData> klass : ACAQDatatypeRegistry.getInstance().getRegisteredDataTypes().values()) {
            if (TableColumn.isGeneratingTableColumn(klass)) {
                result.add(ACAQDataDeclaration.getInstance(klass));
            }
        }

        return result.toArray(new ACAQDataDeclaration[0]);
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
