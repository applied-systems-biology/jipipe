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
import org.hkijena.jipipe.api.metadata.JIPipeOrganizationMetadata;
import org.hkijena.jipipe.desktop.api.JIPipeDesktopParameterEditorUI;
import org.hkijena.jipipe.desktop.commons.components.parameters.JIPipeDesktopParameterFormPanel;
import org.hkijena.jipipe.plugins.parameters.library.markup.MarkdownText;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.ThemeUtils;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.json.JsonUtils;

import javax.swing.*;
import java.awt.*;

public class JIPipeDesktopOrganizationMetadataParameterEditorUI extends JIPipeDesktopParameterEditorUI<JIPipeOrganizationMetadata> {

    private final JLabel nameLabel = new JLabel();
    private final JLabel websiteLabel = new JLabel();
    private final JLabel rorLabel = new JLabel();

    public JIPipeDesktopOrganizationMetadataParameterEditorUI(InitializationParameters parameters) {
        super(JIPipeOrganizationMetadata.class, parameters);
        initialize();
        reload();
    }

    private void initialize() {
        setLayout(new BorderLayout(4, 0));
        JLabel iconLabel = new JLabel(JIPipe.RESOURCES.getIcon16("actions/view-bank.png"));
        iconLabel.setFont(new Font(Font.DIALOG, Font.ITALIC, ThemeUtils.getCurrentStyle().getFontSizeNormal()));
        iconLabel.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 12));
        setOpaque(true);
        setBackground(UIManager.getColor("TextField.background"));
        setBorder(UIUtils.createControlBorder());
        add(iconLabel, BorderLayout.WEST);

        websiteLabel.setFont(new Font(Font.DIALOG, Font.PLAIN, ThemeUtils.getCurrentStyle().getFontSizeSmall()));
        rorLabel.setFont(new Font(Font.MONOSPACED, Font.PLAIN, ThemeUtils.getCurrentStyle().getFontSizeSmall()));
        add(UIUtils.boxVertical(nameLabel, websiteLabel, rorLabel), BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setOpaque(false);
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        add(buttonPanel, BorderLayout.EAST);

        JButton configureButton = new JButton("Configure ...", JIPipe.RESOURCES.getIcon16("actions/configure.png"));
        configureButton.setBackground(getBackground());
        configureButton.setOpaque(true);
        configureButton.setToolTipText("Edit/copy/paste organization");
        buttonPanel.add(configureButton);

        JPopupMenu configureMenu = UIUtils.addPopupMenuToButton(configureButton);
        configureMenu.add(UIUtils.createMenuItem("Edit", "Edits the organization", JIPipe.RESOURCES.getIcon16("actions/edit.png"), this::editOrganization));
        configureMenu.addSeparator();
        configureMenu.add(UIUtils.createMenuItem("Copy", "Copies the organization", JIPipe.RESOURCES.getIcon16("actions/edit-copy.png"), this::copyOrganization));
        configureMenu.add(UIUtils.createMenuItem("Paste", "Pastes the organization", JIPipe.RESOURCES.getIcon16("actions/edit-paste.png"), this::pasteOrganization));
    }

    private void pasteOrganization() {
        try {
            JIPipeOrganizationMetadata organizationMetadata = JsonUtils.readFromString(UIUtils.getStringFromClipboard(), JIPipeOrganizationMetadata.class);
            setParameter(organizationMetadata, true);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Error pasting organization", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void copyOrganization() {
        JIPipeOrganizationMetadata parameter = getParameter();
        UIUtils.copyToClipboard(JsonUtils.toPrettyJsonString(parameter));
    }

    private void editOrganization() {
        JIPipeOrganizationMetadata parameter = getParameter();
        JIPipeDesktopParameterFormPanel.showDialog(getDesktopWorkbench(),
                parameter,
                new MarkdownText("# Edit organization\n\nUse this editor to update organization metadata."),
                "Edit organization",
                JIPipeDesktopParameterFormPanel.WITH_DOCUMENTATION | JIPipeDesktopParameterFormPanel.DOCUMENTATION_NO_UI | JIPipeDesktopParameterFormPanel.WITH_SEARCH_BAR | JIPipeDesktopParameterFormPanel.WITH_SCROLLING);
        reload();
    }

    @Override
    public boolean isUILabelEnabled() {
        return true;
    }

    @Override
    public void reload() {
        JIPipeOrganizationMetadata organization = getParameter();
        if (!StringUtils.isNullOrEmpty(organization.getName())) {
            nameLabel.setText(organization.getName());
            nameLabel.setForeground(ThemeUtils.getCurrentStyle().getTextForeground());
        } else {
            nameLabel.setText("No name set. Please click Configure > Edit");
            nameLabel.setForeground(ThemeUtils.getCurrentStyle().getDangerColor());
        }

        if (!StringUtils.isNullOrEmpty(organization.getWebsite())) {
            websiteLabel.setText(organization.getWebsite());
            websiteLabel.setForeground(ThemeUtils.getCurrentStyle().getTextMuted());
        } else {
            websiteLabel.setText("No website set");
            websiteLabel.setForeground(ThemeUtils.getCurrentStyle().getDangerColor());
        }

        if (!StringUtils.isNullOrEmpty(organization.getRorUrl())) {
            rorLabel.setText(organization.getRorUrl());
            rorLabel.setForeground(ThemeUtils.getCurrentStyle().getTextMuted());
        } else {
            rorLabel.setText("No ROR set");
            rorLabel.setForeground(ThemeUtils.getCurrentStyle().getDangerColor());
        }
    }
}
