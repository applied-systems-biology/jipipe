package org.hkijena.acaq5.ui.grapheditor;

import org.hkijena.acaq5.api.ACAQAlgorithm;
import org.hkijena.acaq5.api.ACAQParameterAccess;

import javax.swing.*;

public abstract class ACAQParameterEditorUI extends JPanel {
    private ACAQAlgorithm algorithm;
    private ACAQParameterAccess parameterAccess;

    public ACAQParameterEditorUI(ACAQAlgorithm algorithm, ACAQParameterAccess parameterAccess) {
        this.algorithm = algorithm;
        this.parameterAccess = parameterAccess;
    }

    public ACAQAlgorithm getAlgorithm() {
        return algorithm;
    }

    public ACAQParameterAccess getParameterAccess() {
        return parameterAccess;
    }

    public abstract boolean isUILabelEnabled();
}
