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

package org.hkijena.jipipe.plugins.parameters.library.auth;

import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopFancyPasswordField;
import org.hkijena.jipipe.desktop.api.JIPipeDesktopParameterEditorUI;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.util.Objects;

/**
 * Parameter editor for {@link String}
 */
public class PasswordDesktopParameterEditorUI extends JIPipeDesktopParameterEditorUI {

    private final JIPipeDesktopFancyPasswordField passwordField = new JIPipeDesktopFancyPasswordField(new JLabel(UIUtils.getIconFromResources("actions/pgp-keys.png")));

    /**
     * @param workbench       workbench
     * @param parameterAccess the parameter
     */
    public PasswordDesktopParameterEditorUI(JIPipeDesktopWorkbench workbench, JIPipeParameterTree parameterTree, JIPipeParameterAccess parameterAccess) {
        super(workbench, parameterTree, parameterAccess);
        initialize();
        reload();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        add(passwordField, BorderLayout.CENTER);
        passwordField.addActionListener(e -> setParameter(new PasswordParameter(passwordField.getText()), false));
    }

    @Override
    public boolean isUILabelEnabled() {
        return true;
    }

    @Override
    public void reload() {
        PasswordParameter value = getParameterAccess().get(PasswordParameter.class);
        String stringValue = "";
        if (value != null) {
            stringValue = "" + value.getPassword();
        }
        if (!Objects.equals(stringValue, passwordField.getText()))
            passwordField.setText(stringValue);
    }
}
