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

package org.hkijena.jipipe.plugins.parameters.library.colors;

import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.desktop.api.JIPipeDesktopParameterEditorUI;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.commons.components.icons.SolidJIPipeDesktopColorIcon;
import org.hkijena.jipipe.utils.ColorUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;

/**
 * Parameter editor for {@link String}
 */
public class ColorDesktopParameterEditorUI extends JIPipeDesktopParameterEditorUI {

    private final SolidJIPipeDesktopColorIcon icon = new SolidJIPipeDesktopColorIcon();
    private boolean isReloading = false;
    private JButton currentlyDisplayed;


    /**
     * @param workbench       workbench
     * @param parameterAccess the parameter
     */
    public ColorDesktopParameterEditorUI(JIPipeDesktopWorkbench workbench, JIPipeParameterTree parameterTree, JIPipeParameterAccess parameterAccess) {
        super(workbench, parameterTree, parameterAccess);
        initialize();
        reload();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        currentlyDisplayed = new JButton(icon);
        currentlyDisplayed.addActionListener(e -> pickColor());
        UIUtils.setStandardButtonBorder(currentlyDisplayed);
        add(currentlyDisplayed, BorderLayout.CENTER);

        JButton selectButton = new JButton(UIUtils.getIconFromResources("actions/edit.png"));
        UIUtils.setStandardButtonBorder(selectButton);
        selectButton.setToolTipText("Select color");
        selectButton.addActionListener(e -> pickColor());
        add(selectButton, BorderLayout.EAST);
    }

    private void pickColor() {
        Color value = getParameter(Color.class);
        value = JColorChooser.showDialog(this, "Select color", value);
        if (value != null) {
            setParameter(value, true);
        }
    }

    @Override
    public boolean isUILabelEnabled() {
        return true;
    }

    @Override
    public void reload() {
        if (isReloading)
            return;
        isReloading = true;
        Color value = getParameter(Color.class);
        if (value == null) {
            value = Color.WHITE;
        }
        icon.setFillColor(value);
        currentlyDisplayed.setText(ColorUtils.colorToHexString(value));
        isReloading = false;
    }
}
