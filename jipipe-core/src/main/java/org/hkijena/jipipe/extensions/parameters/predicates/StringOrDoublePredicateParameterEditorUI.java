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

package org.hkijena.jipipe.extensions.parameters.predicates;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.events.ParameterChangedEvent;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.api.registries.JIPipeParameterTypeRegistry;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.parameters.JIPipeParameterEditorUI;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;

/**
 * A parameter editor UI for {@link StringOrDoublePredicate}
 */
public class StringOrDoublePredicateParameterEditorUI extends JIPipeParameterEditorUI {

    private boolean isProcessing = false;
    private JToggleButton doubleToggle;
    private JToggleButton stringToggle;

    /**
     * @param workbench       workbench
     * @param parameterAccess the parameter
     */
    public StringOrDoublePredicateParameterEditorUI(JIPipeWorkbench workbench, JIPipeParameterAccess parameterAccess) {
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

        StringOrDoublePredicate parameter = getParameter(StringOrDoublePredicate.class);
        parameter.getEventBus().register(this);
        JIPipeParameterTree traversedParameterCollection = new JIPipeParameterTree(parameter);

        ButtonGroup buttonGroup = new ButtonGroup();
        doubleToggle = addToggle(buttonGroup, UIUtils.getIconFromResources("actions/edit-select-number.png"), "Filter a number");
        stringToggle = addToggle(buttonGroup, UIUtils.getIconFromResources("actions/edit-select-text.png"), "Filter a string");
        add(Box.createHorizontalStrut(8));

        if (parameter.getFilterMode() == StringOrDoublePredicate.FilterMode.Double) {
            doubleToggle.setSelected(true);
            add(JIPipe.getParameterTypes().createEditorFor(getWorkbench(), traversedParameterCollection.getParameters().get("number-filter")));
        } else if (parameter.getFilterMode() == StringOrDoublePredicate.FilterMode.String) {
            stringToggle.setSelected(true);
            add(JIPipe.getParameterTypes().createEditorFor(getWorkbench(), traversedParameterCollection.getParameters().get("string-filter")));
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
        StringOrDoublePredicate parameter = getParameter(StringOrDoublePredicate.class);
        if (parameter == null) {
            parameter = new StringOrDoublePredicate();
        }
        if (doubleToggle.isSelected())
            parameter.setFilterMode(StringOrDoublePredicate.FilterMode.Double);
        else if (stringToggle.isSelected())
            parameter.setFilterMode(StringOrDoublePredicate.FilterMode.String);
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
