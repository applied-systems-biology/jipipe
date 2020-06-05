package org.hkijena.acaq5.extensions.parameters.editors;

import org.hkijena.acaq5.api.parameters.ACAQParameterAccess;
import org.hkijena.acaq5.ui.components.DocumentChangeListener;
import org.hkijena.acaq5.ui.parameters.ACAQParameterEditorUI;
import org.hkijena.acaq5.utils.UIUtils;
import org.jdesktop.swingx.JXColorSelectionButton;
import org.jdesktop.swingx.graphics.ColorUtilities;
import org.scijava.Context;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.text.JTextComponent;
import java.awt.*;

/**
 * Parameter editor for {@link String}
 */
public class ColorParameterEditorUI extends ACAQParameterEditorUI {

    private boolean isReloading = false;
    private JButton currentlyDisplayed;


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
        currentlyDisplayed = new JButton();
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
        Color value = getParameterAccess().get(Color.class);
        if(value == null) {
            value = Color.WHITE;
        }
        value = JColorChooser.showDialog(this, "Select color", value);
        if(value != null) {
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
        if(isReloading)
            return;
        isReloading = true;
        Color value = getParameterAccess().get(Color.class);
        if(value == null) {
            value = Color.WHITE;
        }
        currentlyDisplayed.setBackground(value);
        if(value.getAlpha() != 255) {
            currentlyDisplayed.setText("#" + Integer.toHexString(value.getRGB()) + Integer.toHexString(value.getAlpha()));
        }
        else {
            currentlyDisplayed.setText("#" + Integer.toHexString(value.getRGB()));
        }
        isReloading = false;
    }
}
