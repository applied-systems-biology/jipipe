package org.hkijena.acaq5.extensions.tables.parameters;

import org.hkijena.acaq5.api.parameters.ACAQParameterAccess;
import org.hkijena.acaq5.api.parameters.ACAQParameterTree;
import org.hkijena.acaq5.ui.ACAQWorkbench;
import org.hkijena.acaq5.ui.parameters.ACAQParameterEditorUI;
import org.hkijena.acaq5.ui.registries.ACAQUIParameterTypeRegistry;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;

/**
 * A parameter editor UI that works for all enumerations
 */
public class TableColumnSourceParameterEditorUI extends ACAQParameterEditorUI {

    private boolean isProcessing = false;
    private JToggleButton pickColumnToggle;
    private JToggleButton generateColumnToggle;

    /**
     * @param workbench       workbench
     * @param parameterAccess the parameter
     */
    public TableColumnSourceParameterEditorUI(ACAQWorkbench workbench, ACAQParameterAccess parameterAccess) {
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
        ACAQParameterTree traversedParameterCollection = new ACAQParameterTree(parameter);

        ButtonGroup buttonGroup = new ButtonGroup();
        pickColumnToggle = addToggle(buttonGroup, UIUtils.getIconFromResources("select-column.png"), "Pick an existing column");
        generateColumnToggle = addToggle(buttonGroup, UIUtils.getIconFromResources("cog.png"), "Generate data");
        add(Box.createHorizontalStrut(8));

        if (parameter.getMode() == TableColumnSourceParameter.Mode.PickColumn) {
            pickColumnToggle.setSelected(true);
            add(ACAQUIParameterTypeRegistry.getInstance().createEditorFor(getWorkbench(), traversedParameterCollection.getParameters().get("column-source")));
        } else if (parameter.getMode() == TableColumnSourceParameter.Mode.GenerateColumn) {
            generateColumnToggle.setSelected(true);
            add(ACAQUIParameterTypeRegistry.getInstance().createEditorFor(getWorkbench(), traversedParameterCollection.getParameters().get("generator-source")));
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
        getParameterAccess().set(parameter);
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
