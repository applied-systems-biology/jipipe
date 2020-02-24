package org.hkijena.acaq5.ui.grapheditor.settings;

import org.hkijena.acaq5.api.parameters.ACAQParameterAccess;

import javax.swing.*;

public abstract class ACAQParameterEditorUI extends JPanel {
    private ACAQParameterAccess parameterAccess;

    public ACAQParameterEditorUI(ACAQParameterAccess parameterAccess) {
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
