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

package org.hkijena.jipipe.extensions.parameters.util;

import com.google.common.eventbus.Subscribe;
import org.hkijena.jipipe.JIPipeImageJUpdateSiteDependency;
import org.hkijena.jipipe.api.events.ParameterChangedEvent;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.parameters.JIPipeParameterEditorUI;
import org.hkijena.jipipe.ui.parameters.ParameterPanel;
import org.hkijena.jipipe.utils.ModernMetalTheme;

import javax.swing.*;
import java.awt.BorderLayout;

public class JIPipeParameterCollectionParameterEditorUI extends JIPipeParameterEditorUI {

    private ParameterPanel parameterPanel;

    /**
     * Creates new instance
     *
     * @param workbench       the workbech
     * @param parameterAccess Parameter
     */
    public JIPipeParameterCollectionParameterEditorUI(JIPipeWorkbench workbench, JIPipeParameterAccess parameterAccess) {
        super(workbench, parameterAccess);
        initialize();
        reload();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createLineBorder(ModernMetalTheme.MEDIUM_GRAY));
        parameterPanel = new ParameterPanel(getWorkbench(), null, null, ParameterPanel.NO_GROUP_HEADERS);
        add(parameterPanel, BorderLayout.CENTER);
    }

    @Override
    public boolean isUILabelEnabled() {
        return true;
    }

    @Override
    public void reload() {
        JIPipeParameterCollection parameter = getParameter(JIPipeParameterCollection.class);
        parameter.getEventBus().register(new Object() {
            @Subscribe
            public void onParameterChanged(ParameterChangedEvent event) {
                setParameter(parameter, false);
            }
        });
        parameterPanel.setDisplayedParameters(parameter);
    }
}
