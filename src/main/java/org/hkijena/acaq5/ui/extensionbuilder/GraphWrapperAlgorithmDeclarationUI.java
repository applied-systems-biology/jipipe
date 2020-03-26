package org.hkijena.acaq5.ui.extensionbuilder;

import org.hkijena.acaq5.extensions.standardalgorithms.api.algorithms.macro.GraphWrapperAlgorithmDeclaration;
import org.hkijena.acaq5.ui.ACAQJsonExtensionUI;
import org.hkijena.acaq5.ui.ACAQJsonExtensionUIPanel;
import org.hkijena.acaq5.ui.components.ACAQParameterAccessUI;
import org.hkijena.acaq5.ui.components.FormPanel;

import java.awt.*;

public class GraphWrapperAlgorithmDeclarationUI extends ACAQJsonExtensionUIPanel {

    private GraphWrapperAlgorithmDeclaration declaration;

    public GraphWrapperAlgorithmDeclarationUI(ACAQJsonExtensionUI workbenchUI, GraphWrapperAlgorithmDeclaration declaration) {
        super(workbenchUI);
        this.declaration = declaration;

        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());

        ACAQParameterAccessUI parameterAccessUI = new ACAQParameterAccessUI(getWorkbenchUI(), declaration,
                null, false, false);
        add(parameterAccessUI, BorderLayout.CENTER);
    }
}
