package org.hkijena.acaq5.extensions.parameters.predicates;

import org.hkijena.acaq5.api.events.ParameterChangedEvent;
import org.hkijena.acaq5.api.parameters.ACAQParameterAccess;
import org.hkijena.acaq5.api.parameters.ACAQTraversedParameterCollection;
import org.hkijena.acaq5.ui.ACAQWorkbench;
import org.hkijena.acaq5.ui.parameters.ACAQParameterEditorUI;
import org.hkijena.acaq5.ui.registries.ACAQUIParameterTypeRegistry;
import org.hkijena.acaq5.utils.UIUtils;
import org.scijava.Context;

import javax.swing.*;

/**
 * A parameter editor UI for {@link StringOrDoublePredicate}
 */
public class StringOrDoublePredicateParameterEditorUI extends ACAQParameterEditorUI {

    private boolean isProcessing = false;
    private JToggleButton doubleToggle;
    private JToggleButton stringToggle;

    /**
     * @param workbench        workbench
     * @param parameterAccess the parameter
     */
    public StringOrDoublePredicateParameterEditorUI(ACAQWorkbench workbench, ACAQParameterAccess parameterAccess) {
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
        ACAQTraversedParameterCollection traversedParameterCollection = new ACAQTraversedParameterCollection(parameter);

        ButtonGroup buttonGroup = new ButtonGroup();
        doubleToggle = addToggle(buttonGroup, UIUtils.getIconFromResources("number.png"), "Filter a number");
        stringToggle = addToggle(buttonGroup, UIUtils.getIconFromResources("text2.png"), "Filter a string");
        add(Box.createHorizontalStrut(8));

        if (parameter.getFilterMode() == StringOrDoublePredicate.FilterMode.Double) {
            doubleToggle.setSelected(true);
            add(ACAQUIParameterTypeRegistry.getInstance().createEditorFor(getWorkbench(), traversedParameterCollection.getParameters().get("number-filter")));
        } else if (parameter.getFilterMode() == StringOrDoublePredicate.FilterMode.String) {
            stringToggle.setSelected(true);
            add(ACAQUIParameterTypeRegistry.getInstance().createEditorFor(getWorkbench(), traversedParameterCollection.getParameters().get("string-filter")));
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
