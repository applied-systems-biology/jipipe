/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

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
