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

package org.hkijena.jipipe.extensions.parameters.optional;

import com.google.common.eventbus.Subscribe;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.parameters.JIPipeParameterEditorUI;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.BorderLayout;

/**
 * Generic parameter for {@link OptionalParameter}
 */
public class OptionalParameterEditorUI extends JIPipeParameterEditorUI {
    /**
     * Creates new instance
     *
     * @param workbench       workbench
     * @param parameterAccess Parameter
     */
    public OptionalParameterEditorUI(JIPipeWorkbench workbench, JIPipeParameterAccess parameterAccess) {
        super(workbench, parameterAccess);
        initialize();
        reload();
    }

    private void initialize() {
        setLayout(new BorderLayout());
    }

    @Override
    public boolean isUILabelEnabled() {
        return true;
    }

    @Override
    public void reload() {
        OptionalParameter<?> parameter = getParameter(OptionalParameter.class);
        removeAll();

        // Create toggle button
        JToggleButton toggle = new JToggleButton("Enabled", UIUtils.getIconFromResources("emblems/check-square.png"));
        UIUtils.makeFlat(toggle);
        toggle.setToolTipText("If enabled, the parameter is not ignored.");
        toggle.setSelected(parameter.isEnabled());
        toggle.setIcon(toggle.isSelected() ? UIUtils.getIconFromResources("emblems/check-square.png") :
                UIUtils.getIconFromResources("emblems/empty-square.png"));
        toggle.addActionListener(e -> {
            parameter.setEnabled(toggle.isSelected());
            setParameter(parameter, true);
        });
        add(toggle, BorderLayout.WEST);

        OptionalParameterContentAccess<?> access = new OptionalParameterContentAccess(getParameterAccess(), parameter);
        JIPipeParameterEditorUI ui = JIPipe.getParameterTypes().createEditorFor(getWorkbench(), access);
        add(ui, BorderLayout.CENTER);

        // Listen for changes inside the parameter content
        access.getEventBus().register(new Object() {
            @Subscribe
            public void onContentChanged(JIPipeParameterCollection.ParameterChangedEvent event) {
                // We trigger the change event
                setParameter(parameter, false);
            }
        });

        revalidate();
        repaint();

    }
}
