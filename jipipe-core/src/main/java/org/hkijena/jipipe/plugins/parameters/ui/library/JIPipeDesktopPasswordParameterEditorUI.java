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

package org.hkijena.jipipe.plugins.parameters.ui.library;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.desktop.api.JIPipeDesktopParameterEditorUI;
import org.hkijena.jipipe.desktop.commons.components.textfield.JIPipeDesktopFancyPasswordField;
import org.hkijena.jipipe.plugins.parameters.library.auth.JIPipePasswordParameter;

import javax.swing.*;
import java.awt.*;
import java.util.Objects;

/**
 * Parameter editor for {@link String}
 */
public class JIPipeDesktopPasswordParameterEditorUI extends JIPipeDesktopParameterEditorUI<JIPipePasswordParameter> {

    private final JIPipeDesktopFancyPasswordField passwordField = new JIPipeDesktopFancyPasswordField(new JLabel(JIPipe.RESOURCES.getIcon16("actions/pgp-keys.png")));

    public JIPipeDesktopPasswordParameterEditorUI(InitializationParameters parameters) {
        super(JIPipePasswordParameter.class, parameters);
        initialize();
        reload();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        add(passwordField, BorderLayout.CENTER);
        passwordField.addActionListener(e -> setParameter(new JIPipePasswordParameter(passwordField.getText()), false));
    }

    @Override
    public boolean isUILabelEnabled() {
        return true;
    }

    @Override
    public void reload() {
        JIPipePasswordParameter value = getParameterAccess().get(JIPipePasswordParameter.class);
        String stringValue = "";
        if (value != null) {
            stringValue = "" + value.getPassword();
        }
        if (!Objects.equals(stringValue, passwordField.getText()))
            passwordField.setText(stringValue);
    }
}
