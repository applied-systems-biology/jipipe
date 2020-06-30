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

package org.hkijena.acaq5.extensions.parameters.optional;

import org.hkijena.acaq5.api.parameters.ACAQParameterAccess;
import org.hkijena.acaq5.extensions.parameters.primitives.OptionalPathParameter;
import org.hkijena.acaq5.ui.ACAQWorkbench;
import org.hkijena.acaq5.ui.parameters.ACAQParameterEditorUI;
import org.hkijena.acaq5.ui.registries.ACAQUIParameterTypeRegistry;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;
import java.awt.BorderLayout;

/**
 * Generic parameter for {@link OptionalParameter}
 */
public class OptionalParameterEditorUI extends ACAQParameterEditorUI {
    /**
     * Creates new instance
     *
     * @param workbench       workbench
     * @param parameterAccess Parameter
     */
    public OptionalParameterEditorUI(ACAQWorkbench workbench, ACAQParameterAccess parameterAccess) {
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
        OptionalParameter<?> parameter = getParameter(OptionalPathParameter.class);
        removeAll();

        // Create toggle button
        JToggleButton toggle = new JToggleButton("Enabled", UIUtils.getIconFromResources("check-square.png"));
        UIUtils.makeFlat(toggle);
        toggle.setToolTipText("If enabled, the parameter is not ignored.");
        toggle.setSelected(parameter.isEnabled());
        toggle.setIcon(toggle.isSelected() ? UIUtils.getIconFromResources("check-square.png") :
                UIUtils.getIconFromResources("empty-square.png"));
        toggle.addActionListener(e -> {
            parameter.setEnabled(toggle.isSelected());
            reload();
        });
        add(toggle, BorderLayout.WEST);

        OptionalParameterContentAccess<?> access = new OptionalParameterContentAccess(getParameterAccess(), parameter);
        ACAQParameterEditorUI ui = ACAQUIParameterTypeRegistry.getInstance().createEditorFor(getWorkbench(), access);
        add(ui, BorderLayout.CENTER);

        revalidate();
        repaint();

    }
}
