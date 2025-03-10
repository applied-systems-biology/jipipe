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

package org.hkijena.jipipe.plugins.parameters.library.primitives;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.desktop.api.JIPipeDesktopParameterEditorUI;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;

/**
 * A parameter editor UI for {@link StringOrDouble}
 */
public class StringOrDoubleDesktopParameterEditorUI extends JIPipeDesktopParameterEditorUI {

    private boolean isProcessing = false;
    private JToggleButton doubleToggle;
    private JToggleButton stringToggle;

    public StringOrDoubleDesktopParameterEditorUI(InitializationParameters parameters) {
        super(parameters);
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
        parameter.getParameterChangedEventEmitter().subscribeWeak(this);
        JIPipeParameterTree traversedParameterCollection = new JIPipeParameterTree(parameter);

        ButtonGroup buttonGroup = new ButtonGroup();
        doubleToggle = addToggle(buttonGroup, UIUtils.getIconFromResources("actions/edit-select-number.png"), "Define a number");
        stringToggle = addToggle(buttonGroup, UIUtils.getIconFromResources("actions/edit-select-text.png"), "Define a string");
        add(Box.createHorizontalStrut(8));

        if (parameter.getMode() == StringOrDouble.Mode.Double) {
            doubleToggle.setSelected(true);
            add(JIPipe.getParameterTypes().createEditorInstance(traversedParameterCollection.getParameters().get("number"), getDesktopWorkbench(), traversedParameterCollection, null));
        } else if (parameter.getMode() == StringOrDouble.Mode.String) {
            stringToggle.setSelected(true);
            add(JIPipe.getParameterTypes().createEditorInstance(traversedParameterCollection.getParameters().get("string"), getDesktopWorkbench(), traversedParameterCollection, null));
        }

        revalidate();
        repaint();
        isProcessing = false;
    }

    @Override
    public void onParameterChanged(JIPipeParameterCollection.ParameterChangedEvent event) {
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
        UIUtils.makeButtonFlat25x25(toggleButton);
        toggleButton.addActionListener(e -> writeValueToParameter());
        toggleButton.setToolTipText(description);
        group.add(toggleButton);
        add(toggleButton);
        return toggleButton;
    }
}
