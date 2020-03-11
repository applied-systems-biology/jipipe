package org.hkijena.acaq5.ui.grapheditor.settings;

import org.hkijena.acaq5.api.parameters.ACAQParameterAccess;
import org.hkijena.acaq5.ui.ACAQUIPanel;
import org.hkijena.acaq5.ui.ACAQWorkbenchUI;

public abstract class ACAQParameterEditorUI extends ACAQUIPanel {
    private ACAQParameterAccess parameterAccess;

    public ACAQParameterEditorUI(ACAQWorkbenchUI workbenchUI, ACAQParameterAccess parameterAccess) {
        super(workbenchUI);
        this.parameterAccess = parameterAccess;
    }

    public Object getAlgorithm() {
        return parameterAccess.getParameterHolder();
    }

    public ACAQParameterAccess getParameterAccess() {
        return parameterAccess;
    }

    public abstract boolean isUILabelEnabled();
}
