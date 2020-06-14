package org.hkijena.acaq5.utils;

import org.hkijena.acaq5.api.parameters.ACAQDynamicParameterCollection;
import org.hkijena.acaq5.api.parameters.ACAQMutableParameterAccess;
import org.hkijena.acaq5.api.parameters.ACAQParameterTypeDeclaration;
import org.hkijena.acaq5.api.registries.ACAQParameterTypeRegistry;
import org.hkijena.acaq5.ui.ACAQWorkbench;
import org.hkijena.acaq5.ui.ACAQWorkbenchPanel;
import org.hkijena.acaq5.ui.parameters.ParameterPanel;

import java.awt.*;

/**
 * UI that shows all parameter types
 */
public class ParameterUITester extends ACAQWorkbenchPanel {

    private ACAQDynamicParameterCollection parameterCollection;

    /**
     * Creates a new instance
     *
     * @param workbench the workbench
     */
    public ParameterUITester(ACAQWorkbench workbench) {
        super(workbench);
        initializeParameters();
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        ParameterPanel panel = new ParameterPanel(getWorkbench(),
                parameterCollection,
                null,
                ParameterPanel.WITH_DOCUMENTATION | ParameterPanel.WITH_SCROLLING | ParameterPanel.WITH_SEARCH_BAR);
        add(panel, BorderLayout.CENTER);
    }

    private void initializeParameters() {
        parameterCollection = new ACAQDynamicParameterCollection(false);
        for (ACAQParameterTypeDeclaration declaration : ACAQParameterTypeRegistry.getInstance().getRegisteredParameters().values()) {
            ACAQMutableParameterAccess parameterAccess = parameterCollection.addParameter(declaration.getId(), declaration.getFieldClass());
            parameterAccess.setName(declaration.getName());
        }
    }

}
