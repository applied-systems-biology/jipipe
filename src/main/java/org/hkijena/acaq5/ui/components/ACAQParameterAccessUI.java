package org.hkijena.acaq5.ui.components;

import org.hkijena.acaq5.ACAQRegistryService;
import org.hkijena.acaq5.api.parameters.ACAQParameterAccess;
import org.hkijena.acaq5.api.parameters.ACAQParameterVisibility;
import org.hkijena.acaq5.ui.ACAQWorkbenchUI;
import org.hkijena.acaq5.ui.grapheditor.settings.ACAQParameterEditorUI;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ACAQParameterAccessUI extends FormPanel {
    private ACAQWorkbenchUI workbenchUI;
    private Object parameterHolder;

    public ACAQParameterAccessUI(ACAQWorkbenchUI workbenchUI, Object parameterHolder, MarkdownDocument documentation, boolean documentationBelow, boolean withDocumentation) {
        super(documentation, documentationBelow, withDocumentation);
        this.workbenchUI = workbenchUI;
        this.parameterHolder = parameterHolder;
        reloadForm();
    }

    public void reloadForm() {
        clear();
        Map<String, ACAQParameterAccess> parameters = ACAQParameterAccess.getParameters(getParameterHolder());
        boolean hasAddedParameters = false;

        Map<Object, List<String>> groupedByHolder = parameters.keySet().stream().collect(Collectors.groupingBy(key -> parameters.get(key).getParameterHolder()));

        // First display all parameters of the current holder
        if(groupedByHolder.containsKey(parameterHolder)) {
            for (String key : groupedByHolder.get(parameterHolder).stream().sorted().collect(Collectors.toList())) {
                ACAQParameterAccess parameterAccess = parameters.get(key);
                if (parameterAccess.getVisibility() == ACAQParameterVisibility.Hidden)
                    continue;

                ACAQParameterEditorUI ui = ACAQRegistryService.getInstance()
                        .getUIParametertypeRegistry().createEditorFor(workbenchUI, parameterAccess);
                if (ui.isUILabelEnabled())
                    addToForm(ui, new JLabel(parameterAccess.getName()), null);
                else
                    addToForm(ui, null);
                hasAddedParameters = true;
            }
        }

        // Display parameters of all other holders
        for(Object parameterHolder : groupedByHolder.keySet()) {
            if(parameterHolder == this.parameterHolder)
                continue;
            for (String key : groupedByHolder.get(parameterHolder).stream().sorted().collect(Collectors.toList())) {
                ACAQParameterAccess parameterAccess = parameters.get(key);
                if (parameterAccess.getVisibility() == ACAQParameterVisibility.Hidden)
                    continue;
                if (parameterAccess.getVisibility() == ACAQParameterVisibility.Visible)
                    continue;

                ACAQParameterEditorUI ui = ACAQRegistryService.getInstance()
                        .getUIParametertypeRegistry().createEditorFor(workbenchUI, parameterAccess);
                if (ui.isUILabelEnabled())
                    addToForm(ui, new JLabel(parameterAccess.getName()), null);
                else
                    addToForm(ui, null);
                hasAddedParameters = true;
            }
        }

        if (!hasAddedParameters) {
            addToForm(new JLabel("There are no parameters",
                            UIUtils.getIconFromResources("info.png"), JLabel.LEFT),
                    null);
        }
        addVerticalGlue();
    }

    public Object getParameterHolder() {
        return parameterHolder;
    }
}
