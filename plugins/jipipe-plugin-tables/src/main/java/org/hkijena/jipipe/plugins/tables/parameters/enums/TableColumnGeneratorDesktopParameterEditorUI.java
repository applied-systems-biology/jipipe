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

package org.hkijena.jipipe.plugins.tables.parameters.enums;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataInfo;
import org.hkijena.jipipe.desktop.api.JIPipeDesktopParameterEditorUI;
import org.hkijena.jipipe.desktop.commons.components.renderers.JIPipeDesktopDataInfoListCellRenderer;
import org.hkijena.jipipe.plugins.tables.ColumnContentType;
import org.hkijena.jipipe.plugins.tables.datatypes.TableColumnData;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

/**
 * A parameter editor UI that works for all enumerations
 */
public class TableColumnGeneratorDesktopParameterEditorUI extends JIPipeDesktopParameterEditorUI {

    private boolean isProcessing = false;
    private JComboBox<JIPipeDataInfo> comboBox;
    private JToggleButton numericColumnToggle;
    private JToggleButton textColumnToggle;

    public TableColumnGeneratorDesktopParameterEditorUI(InitializationParameters parameters) {
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
        isProcessing = true;
        TableColumnGeneratorParameter parameter = getParameter(TableColumnGeneratorParameter.class);
        comboBox.setSelectedItem(parameter.getGeneratorType().getInfo());
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
        comboBox.setRenderer(new JIPipeDesktopDataInfoListCellRenderer());
        comboBox.addActionListener(e -> writeValueToParameter());
        add(comboBox);

        ButtonGroup buttonGroup = new ButtonGroup();
        numericColumnToggle = addToggle(buttonGroup, UIUtils.getIconFromResources("actions/edit-select-number.png"), "Numeric column");
        textColumnToggle = addToggle(buttonGroup, UIUtils.getIconFromResources("actions/edit-select-text.png"), "Text column");
    }

    private void writeValueToParameter() {
        if (isProcessing)
            return;
        isProcessing = true;
        TableColumnGeneratorParameter parameter = getParameter(TableColumnGeneratorParameter.class);
        if (comboBox.getSelectedItem() != null)
            parameter.getGeneratorType().setInfo((JIPipeDataInfo) comboBox.getSelectedItem());
        else
            parameter.getGeneratorType().setInfo(null);
        if (numericColumnToggle.isSelected())
            parameter.setGeneratedType(ColumnContentType.NumericColumn);
        else
            parameter.setGeneratedType(ColumnContentType.StringColumn);
        setParameter(parameter, false);
        isProcessing = false;
    }

    private JIPipeDataInfo[] getAvailableGenerators() {
        List<Object> result = new ArrayList<>();
        result.add(null);
        for (Class<? extends JIPipeData> klass : JIPipe.getDataTypes().getRegisteredDataTypes().values()) {
            if (TableColumnData.isGeneratingTableColumn(klass)) {
                result.add(JIPipeDataInfo.getInstance(klass));
            }
        }

        return result.toArray(new JIPipeDataInfo[0]);
    }

    private JToggleButton addToggle(ButtonGroup group, Icon icon, String description) {
        JToggleButton toggleButton = new JToggleButton(icon);
        UIUtils.makeButtonFlat25x25(toggleButton);
        toggleButton.addActionListener(e -> writeValueToParameter());
        toggleButton.setToolTipText(description);
        group.add(toggleButton);
        add(toggleButton);
        return toggleButton;
    }
}
