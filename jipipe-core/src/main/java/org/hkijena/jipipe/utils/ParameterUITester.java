/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.utils;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.parameters.JIPipeDynamicParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeMutableParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTypeInfo;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbenchPanel;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopParameterPanel;

import java.awt.*;

/**
 * UI that shows all parameter types
 */
public class ParameterUITester extends JIPipeDesktopWorkbenchPanel {

    private JIPipeDynamicParameterCollection parameterCollection;

    /**
     * Creates a new instance
     *
     * @param workbench the workbench
     */
    public ParameterUITester(JIPipeDesktopWorkbench workbench) {
        super(workbench);
        initializeParameters();
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        JIPipeDesktopParameterPanel panel = new JIPipeDesktopParameterPanel(getDesktopWorkbench(),
                parameterCollection,
                null,
                JIPipeDesktopParameterPanel.WITH_DOCUMENTATION | JIPipeDesktopParameterPanel.WITH_SCROLLING | JIPipeDesktopParameterPanel.WITH_SEARCH_BAR);
        add(panel, BorderLayout.CENTER);
    }

    private void initializeParameters() {
        parameterCollection = new JIPipeDynamicParameterCollection(false);
        for (JIPipeParameterTypeInfo info : JIPipe.getParameterTypes().getRegisteredParameters().values()) {
            JIPipeMutableParameterAccess parameterAccess = parameterCollection.addParameter(info.getId(), info.getFieldClass());
            parameterAccess.setName(info.getName());
        }
    }

}
