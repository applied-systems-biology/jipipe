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

package org.hkijena.jipipe.extensions.parameters.library.references;

import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.components.JIPipeIconPickerDialog;
import org.hkijena.jipipe.ui.parameters.JIPipeParameterEditorUI;
import org.hkijena.jipipe.utils.ResourceUtils;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

/**
 * Editor for {@link IconRef}
 */
public class IconRefParameterEditorUI extends JIPipeParameterEditorUI {

    private static Set<String> availableAlgorithmIcons;
    private JButton currentlyDisplayed;

    /**
     * @param workbench       workbench
     * @param parameterAccess the parameter
     */
    public IconRefParameterEditorUI(JIPipeWorkbench workbench, JIPipeParameterAccess parameterAccess) {
        super(workbench, parameterAccess);
        initialize();
        reload();
    }

    public static Set<String> getAvailableIcons() {
        if (availableAlgorithmIcons == null) {
            availableAlgorithmIcons = new HashSet<>();
            Set<String> rawIcons = ResourceUtils.walkInternalResourceFolder("icons/");
            String basePath = ResourceUtils.getResourcePath("icons/");
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

        JButton selectButton = new JButton(UIUtils.getIconFromResources("actions/edit.png"));
        UIUtils.makeFlat(selectButton);
        selectButton.setToolTipText("Select icon");
        selectButton.addActionListener(e -> pickIcon());
        add(selectButton, BorderLayout.EAST);
    }

    private void pickIcon() {
        String picked = JIPipeIconPickerDialog.showDialog(this, ResourceUtils.getResourcePath("icons"), getAvailableIcons());
        IconRef ref = getParameter(IconRef.class);
        ref.setIconName(picked);
        setParameter(ref, true);
    }

    @Override
    public boolean isUILabelEnabled() {
        return true;
    }

    @Override
    public void reload() {
        IconRef ref = getParameter(IconRef.class);
        if (!StringUtils.isNullOrEmpty(ref.getIconName())) {
            URL resource = ResourceUtils.getPluginResource("icons/" + ref.getIconName());
            if (resource != null) {
                currentlyDisplayed.setText(ref.getIconName());
                currentlyDisplayed.setIcon(new ImageIcon(resource));
            } else {
                currentlyDisplayed.setText("<Invalid: " + ref.getIconName() + ">");
                currentlyDisplayed.setIcon(UIUtils.getIconFromResources("actions/configure.png"));
            }
        } else {
            currentlyDisplayed.setText("<None selected>");
            currentlyDisplayed.setIcon(UIUtils.getIconFromResources("actions/configure.png"));
        }
    }
}
