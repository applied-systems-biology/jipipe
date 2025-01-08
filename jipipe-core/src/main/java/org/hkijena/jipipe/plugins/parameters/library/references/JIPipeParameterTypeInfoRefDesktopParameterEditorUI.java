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

package org.hkijena.jipipe.plugins.parameters.library.references;

import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTypeInfo;
import org.hkijena.jipipe.desktop.api.JIPipeDesktopParameterEditorUI;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.commons.components.pickers.JIPipeDesktopParameterTypeInfoPicker;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;

/**
 * Parameter for {@link JIPipeParameterTypeInfoRef}
 */
public class JIPipeParameterTypeInfoRefDesktopParameterEditorUI extends JIPipeDesktopParameterEditorUI {
    private JButton currentlyDisplayed;

    /**
     * @param workbench       workbench
     * @param parameterAccess the parameter
     */
    public JIPipeParameterTypeInfoRefDesktopParameterEditorUI(InitializationParameters parameters) {
       super(parameters);
        initialize();
        reload();
    }

    private void initialize() {
        setLayout(new BorderLayout());

        currentlyDisplayed = new JButton();
        currentlyDisplayed.addActionListener(e -> pickNodeInfo());
        UIUtils.setStandardButtonBorder(currentlyDisplayed);
        add(currentlyDisplayed, BorderLayout.CENTER);

        JButton selectButton = new JButton(UIUtils.getIconFromResources("actions/edit.png"));
        UIUtils.setStandardButtonBorder(selectButton);
        selectButton.setToolTipText("Select parameter type");
        selectButton.addActionListener(e -> pickNodeInfo());
        add(selectButton, BorderLayout.EAST);
    }

    @Override
    public void reload() {
        JIPipeParameterTypeInfoRef infoRef = getParameter(JIPipeParameterTypeInfoRef.class);
        JIPipeParameterTypeInfo info = infoRef.getInfo();
        if (info != null) {
            currentlyDisplayed.setText(info.getName());
            currentlyDisplayed.setToolTipText(info.getDescription());
            currentlyDisplayed.setIcon(UIUtils.getIconFromResources("data-types/parameters.png"));
        } else {
            currentlyDisplayed.setText("None selected");
            currentlyDisplayed.setIcon(UIUtils.getIconFromResources("emblems/vcs-conflicting.png"));
        }
    }


    private void pickNodeInfo() {
        JIPipeParameterTypeInfoRef infoRef = getParameter(JIPipeParameterTypeInfoRef.class);
        JIPipeDesktopParameterTypeInfoPicker picker;
        if (infoRef.getUiAllowedParameterTypes() == null || infoRef.getUiAllowedParameterTypes().isEmpty()) {
            picker = new JIPipeDesktopParameterTypeInfoPicker(getDesktopWorkbench().getWindow());
        } else {
            picker = new JIPipeDesktopParameterTypeInfoPicker(getDesktopWorkbench().getWindow(), infoRef.getUiAllowedParameterTypes());
        }
        JIPipeParameterTypeInfo info = infoRef.getInfo();
        if (info != null) {
            picker.setSelectedItem(info);
        }
        JIPipeParameterTypeInfo selection = picker.showDialog();
        if (selection != null) {
            setParameter(new JIPipeParameterTypeInfoRef(selection), true);
        }
    }

    @Override
    public boolean isUILabelEnabled() {
        return true;
    }
}
