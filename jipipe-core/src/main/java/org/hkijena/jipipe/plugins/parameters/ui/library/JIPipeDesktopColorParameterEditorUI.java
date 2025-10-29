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
import org.hkijena.jipipe.desktop.commons.components.icons.SolidColorIcon;
import org.hkijena.jipipe.utils.ColorUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;

/**
 * Parameter editor for {@link String}
 */
public class JIPipeDesktopColorParameterEditorUI extends JIPipeDesktopParameterEditorUI<Color> {

    private final SolidColorIcon icon = new SolidColorIcon();
    private boolean isReloading = false;
    private JButton currentlyDisplayed;

    public JIPipeDesktopColorParameterEditorUI(InitializationParameters parameters) {
        super(Color.class, parameters);
        initialize();
        reload();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        currentlyDisplayed = new JButton(icon);
        currentlyDisplayed.addActionListener(e -> pickColor());
        UIUtils.setStandardButtonBorder(currentlyDisplayed);
        add(currentlyDisplayed, BorderLayout.CENTER);

        JButton selectButton = new JButton(JIPipe.RESOURCES.getIcon16("actions/edit.png"));
        UIUtils.setStandardButtonBorder(selectButton);
        selectButton.setToolTipText("Select color");
        selectButton.addActionListener(e -> pickColor());
        add(selectButton, BorderLayout.EAST);
    }

    private void pickColor() {
        Color value = getParameter();
        ColorParameterSettings settings = getParameterAccess().getAnnotationOfType(ColorParameterSettings.class);
        boolean withTransparency = false;
        if(settings != null) {
            withTransparency = settings.withTransparency();
        }
        value = UIUtils.selectColor(this, "Select color", value, withTransparency);
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
        Color value = getParameter();
        if (value == null) {
            value = Color.WHITE;
        }
        icon.setFillColor(value);
        currentlyDisplayed.setText(ColorUtils.colorToHexString(value));
        isReloading = false;
    }
}
