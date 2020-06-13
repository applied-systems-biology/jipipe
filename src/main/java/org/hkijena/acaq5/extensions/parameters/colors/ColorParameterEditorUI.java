package org.hkijena.acaq5.extensions.parameters.colors;

import org.hkijena.acaq5.api.parameters.ACAQParameterAccess;
import org.hkijena.acaq5.ui.components.ColorIcon;
import org.hkijena.acaq5.ui.parameters.ACAQParameterEditorUI;
import org.hkijena.acaq5.utils.StringUtils;
import org.hkijena.acaq5.utils.UIUtils;
import org.scijava.Context;

import javax.swing.*;
import java.awt.*;

/**
 * Parameter editor for {@link String}
 */
public class ColorParameterEditorUI extends ACAQParameterEditorUI {

    private boolean isReloading = false;
    private JButton currentlyDisplayed;
    private ColorIcon icon = new ColorIcon();


    /**
     * @param context         SciJava context
     * @param parameterAccess the parameter
     */
    public ColorParameterEditorUI(Context context, ACAQParameterAccess parameterAccess) {
        super(context, parameterAccess);
        initialize();
        reload();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        currentlyDisplayed = new JButton(icon);
        currentlyDisplayed.addActionListener(e -> pickColor());
        UIUtils.makeFlat(currentlyDisplayed);
        add(currentlyDisplayed, BorderLayout.CENTER);

        JButton selectButton = new JButton(UIUtils.getIconFromResources("edit.png"));
        UIUtils.makeFlat(selectButton);
        selectButton.setToolTipText("Select color");
        selectButton.addActionListener(e -> pickColor());
        add(selectButton, BorderLayout.EAST);
    }

    private void pickColor() {
        Color value = getParameter(Color.class);
        value = JColorChooser.showDialog(this, "Select color", value);
        if (value != null) {
            getParameterAccess().set(value);
            reload();
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
        currentlyDisplayed.setText(StringUtils.colorToHexString(value));
        isReloading = false;
    }
}
