package org.hkijena.acaq5.extensions.parameters.editors;

import org.hkijena.acaq5.api.parameters.ACAQParameterAccess;
import org.hkijena.acaq5.extensions.parameters.references.ACAQAlgorithmIconRef;
import org.hkijena.acaq5.ui.ACAQWorkbench;
import org.hkijena.acaq5.ui.components.ACAQIconPickerDialog;
import org.hkijena.acaq5.ui.parameters.ACAQParameterEditorUI;
import org.hkijena.acaq5.utils.ResourceUtils;
import org.hkijena.acaq5.utils.StringUtils;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

/**
 * Editor for {@link ACAQAlgorithmIconRef}
 */
public class ACAQAlgorithmIconRefParameterEditorUI extends ACAQParameterEditorUI {

    private static Set<String> availableAlgorithmIcons;
    private JButton currentlyDisplayed;

    /**
     * @param workbench       workbench
     * @param parameterAccess the parameter
     */
    public ACAQAlgorithmIconRefParameterEditorUI(ACAQWorkbench workbench, ACAQParameterAccess parameterAccess) {
        super(workbench, parameterAccess);
        initialize();
        reload();
    }

    public static Set<String> getAvailableAlgorithmIcons() {
        if (availableAlgorithmIcons == null) {
            availableAlgorithmIcons = new HashSet<>();
            Set<String> rawIcons = ResourceUtils.walkInternalResourceFolder("icons/algorithms");
            String basePath = ResourceUtils.getResourcePath("icons/algorithms/");
            for (String rawIcon : rawIcons) {
                if (rawIcon.endsWith(".png"))
                    availableAlgorithmIcons.add(rawIcon.substring(basePath.length()));
            }
        }
        return availableAlgorithmIcons;
    }

    private void initialize() {
        setLayout(new BorderLayout());

        currentlyDisplayed = new JButton();
        currentlyDisplayed.setHorizontalAlignment(SwingConstants.LEFT);
        currentlyDisplayed.addActionListener(e -> pickIcon());
        UIUtils.makeFlat(currentlyDisplayed);
        add(currentlyDisplayed, BorderLayout.CENTER);

        JButton selectButton = new JButton(UIUtils.getIconFromResources("edit.png"));
        UIUtils.makeFlat(selectButton);
        selectButton.setToolTipText("Select icon");
        selectButton.addActionListener(e -> pickIcon());
        add(selectButton, BorderLayout.EAST);
    }

    private void pickIcon() {
        String picked = ACAQIconPickerDialog.showDialog(this, ResourceUtils.getResourcePath("icons/algorithms"), getAvailableAlgorithmIcons());
        ACAQAlgorithmIconRef ref = getParameter(ACAQAlgorithmIconRef.class);
        ref.setIconName(picked);
        getParameterAccess().set(ref);
        reload();
    }

    @Override
    public boolean isUILabelEnabled() {
        return true;
    }

    @Override
    public void reload() {
        ACAQAlgorithmIconRef ref = getParameter(ACAQAlgorithmIconRef.class);
        if (!StringUtils.isNullOrEmpty(ref.getIconName())) {
            URL resource = ResourceUtils.getPluginResource("icons/algorithms/" + ref.getIconName());
            if (resource != null) {
                currentlyDisplayed.setText(ref.getIconName());
                currentlyDisplayed.setIcon(new ImageIcon(resource));
            } else {
                currentlyDisplayed.setText("<Invalid: " + ref.getIconName() + ">");
                currentlyDisplayed.setIcon(UIUtils.getIconFromResources("cog.png"));
            }
        } else {
            currentlyDisplayed.setText("<None selected>");
            currentlyDisplayed.setIcon(UIUtils.getIconFromResources("cog.png"));
        }
    }
}
