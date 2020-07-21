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

package org.hkijena.jipipe.extensions.parameters.primitives;

import org.hkijena.jipipe.api.events.ParameterChangedEvent;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.parameters.JIPipeParameterEditorUI;
import org.hkijena.jipipe.ui.registries.JIPipeUIParameterTypeRegistry;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;

/**
 * A parameter editor UI for {@link StringOrDouble}
 */
public class StringOrDoubleParameterEditorUI extends JIPipeParameterEditorUI {

    private boolean isProcessing = false;
    private JToggleButton doubleToggle;
    private JToggleButton stringToggle;

    /**
     * @param workbench       workbench
     * @param parameterAccess the parameter
     */
    public StringOrDoubleParameterEditorUI(JIPipeWorkbench workbench, JIPipeParameterAccess parameterAccess) {
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

        StringOrDouble parameter = getParameter(StringOrDouble.class);
        parameter.getEventBus().register(this);
        JIPipeParameterTree traversedParameterCollection = new JIPipeParameterTree(parameter);

        ButtonGroup buttonGroup = new ButtonGroup();
        doubleToggle = addToggle(buttonGroup, UIUtils.getIconFromResources("actions/edit-select-number.png"), "Define a number");
        stringToggle = addToggle(buttonGroup, UIUtils.getIconFromResources("actions/edit-select-text.png"), "Define a string");
        add(Box.createHorizontalStrut(8));

        if (parameter.getMode() == StringOrDouble.Mode.Double) {
            doubleToggle.setSelected(true);
            add(JIPipeUIParameterTypeRegistry.getInstance().createEditorFor(getWorkbench(), traversedParameterCollection.getParameters().get("number")));
        } else if (parameter.getMode() == StringOrDouble.Mode.String) {
            stringToggle.setSelected(true);
            add(JIPipeUIParameterTypeRegistry.getInstance().createEditorFor(getWorkbench(), traversedParameterCollection.getParameters().get("string")));
        }

        revalidate();
        repaint();
        isProcessing = false;
    }

    @Override
    public void onParameterChanged(ParameterChangedEvent event) {
        if (event.getKey().equals("mode")) {
            reload();
            return;
        }
        super.onParameterChanged(event);
    }

    private void writeValueToParameter() {
        if (isProcessing)
            return;
        isProcessing = true;
        StringOrDouble parameter = getParameter(StringOrDouble.class);
        if (parameter == null) {
            parameter = new StringOrDouble();
        }
        if (doubleToggle.isSelected())
            parameter.setMode(StringOrDouble.Mode.Double);
        else if (stringToggle.isSelected())
            parameter.setMode(StringOrDouble.Mode.String);
        setParameter(parameter, true);
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
