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

package org.hkijena.jipipe.extensions.tables.parameters;

import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.parameters.JIPipeParameterEditorUI;
import org.hkijena.jipipe.ui.registries.JIPipeUIParameterTypeRegistry;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;

/**
 * A parameter editor UI that works for all enumerations
 */
public class TableColumnSourceParameterEditorUI extends JIPipeParameterEditorUI {

    private boolean isProcessing = false;
    private JToggleButton pickColumnToggle;
    private JToggleButton generateColumnToggle;

    /**
     * @param workbench       workbench
     * @param parameterAccess the parameter
     */
    public TableColumnSourceParameterEditorUI(JIPipeWorkbench workbench, JIPipeParameterAccess parameterAccess) {
        super(workbench, parameterAccess);
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        reload();
    }

    @Override
    public boolean isUILabelEnabled() {
        return true;
    }

    @Override
    public void reload() {
        isProcessing = true;
        removeAll();

        TableColumnSourceParameter parameter = getParameter(TableColumnSourceParameter.class);
        JIPipeParameterTree traversedParameterCollection = new JIPipeParameterTree(parameter);

        ButtonGroup buttonGroup = new ButtonGroup();
        pickColumnToggle = addToggle(buttonGroup, UIUtils.getIconFromResources("select-column.png"), "Pick an existing column");
        generateColumnToggle = addToggle(buttonGroup, UIUtils.getIconFromResources("actions/configure.png"), "Generate data");
        add(Box.createHorizontalStrut(8));

        if (parameter.getMode() == TableColumnSourceParameter.Mode.PickColumn) {
            pickColumnToggle.setSelected(true);
            add(JIPipeUIParameterTypeRegistry.getInstance().createEditorFor(getWorkbench(), traversedParameterCollection.getParameters().get("column-source")));
        } else if (parameter.getMode() == TableColumnSourceParameter.Mode.GenerateColumn) {
            generateColumnToggle.setSelected(true);
            add(JIPipeUIParameterTypeRegistry.getInstance().createEditorFor(getWorkbench(), traversedParameterCollection.getParameters().get("generator-source")));
        }

        revalidate();
        repaint();
        isProcessing = false;
    }

    private void writeValueToParameter() {
        if (isProcessing)
            return;
        isProcessing = true;
        TableColumnSourceParameter parameter = getParameter(TableColumnSourceParameter.class);
        if (parameter == null) {
            parameter = new TableColumnSourceParameter();
        }
        if (pickColumnToggle.isSelected())
            parameter.setMode(TableColumnSourceParameter.Mode.PickColumn);
        else if (generateColumnToggle.isSelected())
            parameter.setMode(TableColumnSourceParameter.Mode.GenerateColumn);
        setParameter(parameter, false);
        isProcessing = false;
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
