package org.hkijena.acaq5.ui.extensionbuilder;

import org.hkijena.acaq5.api.traits.ACAQJsonTraitDeclaration;
import org.hkijena.acaq5.ui.ACAQJsonExtensionUI;
import org.hkijena.acaq5.ui.ACAQJsonExtensionUIPanel;
import org.hkijena.acaq5.ui.components.ACAQParameterAccessUI;

import java.awt.*;

public class ACAQJsonTraitDeclarationUI extends ACAQJsonExtensionUIPanel {

    private ACAQJsonTraitDeclaration declaration;

    public ACAQJsonTraitDeclarationUI(ACAQJsonExtensionUI workbenchUI, ACAQJsonTraitDeclaration declaration) {
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
