package org.hkijena.acaq5.ui.grapheditor;

import org.hkijena.acaq5.api.algorithm.ACAQAlgorithm;
import org.hkijena.acaq5.api.parameters.ACAQParameterAccess;

import javax.swing.*;

public abstract class ACAQParameterEditorUI extends JPanel {
    private ACAQParameterAccess parameterAccess;

    public ACAQParameterEditorUI(ACAQParameterAccess parameterAccess) {
        this.parameterAccess = parameterAccess;
    }

    public ACAQAlgorithm getAlgorithm() {
        return parameterAccess.getAlgorithm();
    }

    public ACAQParameterAccess getParameterAccess() {
        return parameterAccess;
    }

    public abstract boolean isUILabelEnabled();
}
