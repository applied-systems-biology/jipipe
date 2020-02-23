package org.hkijena.acaq5.ui.components;

import org.hkijena.acaq5.ACAQRegistryService;
import org.hkijena.acaq5.api.parameters.ACAQParameterAccess;
import org.hkijena.acaq5.ui.grapheditor.settings.ACAQParameterEditorUI;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;
import java.util.Map;
import java.util.stream.Collectors;

public class ACAQParameterAccessUI extends FormPanel {
    private Object parameterHolder;

    public ACAQParameterAccessUI(Object parameterHolder, String defaultHelpDocumentPath, boolean documentationBelow, boolean withDocumentation) {
        super(defaultHelpDocumentPath, documentationBelow, withDocumentation);
        this.parameterHolder = parameterHolder;
        initialize();
    }

    private void initialize() {
        Map<String, ACAQParameterAccess> parameters = ACAQParameterAccess.getParameters(getParameterHolder());
        if(!parameters.isEmpty()) {
            for(String key : parameters.keySet().stream().sorted().collect(Collectors.toList())) {
                ACAQParameterAccess parameterAccess = parameters.get(key);
                ACAQParameterEditorUI ui = ACAQRegistryService.getInstance()
                        .getUIParametertypeRegistry().createEditorFor(parameterAccess);

                if(ui.isUILabelEnabled())
                    addToForm(ui, new JLabel(parameterAccess.getName()), null);
                else
                    addToForm(ui, null);
            }
        }
        else {
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
