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

package org.hkijena.jipipe.plugins.parameters.library.jipipe;

import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.desktop.api.JIPipeDesktopParameterEditorUI;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopParameterFormPanel;
import org.hkijena.jipipe.utils.UIUtils;

import java.awt.*;

public class JIPipeParameterCollectionDesktopParameterEditorUI extends JIPipeDesktopParameterEditorUI {

    private JIPipeDesktopParameterFormPanel parameterPanel;
    private boolean isReloading;

    public JIPipeParameterCollectionDesktopParameterEditorUI(InitializationParameters parameters) {
       super(parameters);
        initialize();
        reload();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        setBorder(UIUtils.createControlBorder());
        parameterPanel = new JIPipeDesktopParameterFormPanel(getDesktopWorkbench(), null, null, JIPipeDesktopParameterFormPanel.NO_GROUP_HEADERS);
        add(parameterPanel, BorderLayout.CENTER);
    }

    @Override
    public boolean isUILabelEnabled() {
        return true;
    }

    @Override
    public void reload() {
        if (isReloading)
            return;
        try {
            JIPipeParameterCollection parameter = getParameter(JIPipeParameterCollection.class);
//            parameter.getEventBus().register(new Object() {
//                @Override
//                public void onParameterChanged(JIPipeParameterCollection.ParameterChangedEvent event) {
//                    // Workaround: causes UX issues
//                    //setParameter(parameter, false);
//                }
//            });
            parameterPanel.setDisplayedParameters(parameter);
        } finally {
            isReloading = false;
        }
    }
}
