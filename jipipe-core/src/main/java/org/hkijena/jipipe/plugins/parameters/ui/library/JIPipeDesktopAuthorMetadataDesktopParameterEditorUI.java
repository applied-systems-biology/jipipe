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
import org.hkijena.jipipe.api.metadata.JIPipeAuthorMetadata;
import org.hkijena.jipipe.desktop.api.JIPipeDesktopParameterEditorUI;
import org.hkijena.jipipe.desktop.commons.components.parameters.JIPipeDesktopParameterFormPanel;
import org.hkijena.jipipe.plugins.parameters.library.markup.MarkdownText;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.ThemeUtils;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.json.JsonUtils;

import javax.swing.*;
import java.awt.*;

public class JIPipeDesktopAuthorMetadataDesktopParameterEditorUI extends JIPipeDesktopParameterEditorUI<JIPipeAuthorMetadata> {

    private final JLabel nameLabel = new JLabel();
    private final JLabel affiliationLabel = new JLabel();
    private final JLabel orcidLabel = new JLabel();

    public JIPipeDesktopAuthorMetadataDesktopParameterEditorUI(InitializationParameters parameters) {
        super(JIPipeAuthorMetadata.class, parameters);
        initialize();
        reload();
    }

    private void initialize() {
        setLayout(new BorderLayout(4, 0));
        JLabel iconLabel = new JLabel(JIPipe.RESOURCES.getIcon16("actions/user.png"));
        iconLabel.setFont(new Font(Font.DIALOG, Font.ITALIC, ThemeUtils.getCurrentStyle().getFontSizeNormal()));
        iconLabel.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 12));
        setOpaque(true);
        setBackground(UIManager.getColor("TextField.background"));
        setBorder(UIUtils.createControlBorder());
        add(iconLabel, BorderLayout.WEST);

        affiliationLabel.setFont(new Font(Font.DIALOG, Font.PLAIN, ThemeUtils.getCurrentStyle().getFontSizeSmall()));
        orcidLabel.setFont(new Font(Font.MONOSPACED, Font.PLAIN, ThemeUtils.getCurrentStyle().getFontSizeSmall()));
        add(UIUtils.boxVertical(nameLabel, affiliationLabel, orcidLabel), BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setOpaque(false);
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        add(buttonPanel, BorderLayout.EAST);

        JButton configureButton = new JButton("Configure ...", JIPipe.RESOURCES.getIcon16("actions/configure.png"));
        configureButton.setBackground(getBackground());
        configureButton.setOpaque(true);
        configureButton.setToolTipText("Edit/copy/paste author");
        buttonPanel.add(configureButton);

        JPopupMenu configureMenu = UIUtils.addPopupMenuToButton(configureButton);
        configureMenu.add(UIUtils.createMenuItem("Edit", "Edits the author", JIPipe.RESOURCES.getIcon16("actions/edit.png"), this::editAuthor));
        configureMenu.addSeparator();
        configureMenu.add(UIUtils.createMenuItem("Copy", "Copies the author", JIPipe.RESOURCES.getIcon16("actions/edit-copy.png"), this::copyAuthor));
        configureMenu.add(UIUtils.createMenuItem("Paste", "Pastes the author", JIPipe.RESOURCES.getIcon16("actions/edit-paste.png"), this::pasteAuthor));
    }

    private void pasteAuthor() {
        try {
            JIPipeAuthorMetadata authorMetadata = JsonUtils.readFromString(UIUtils.getStringFromClipboard(), JIPipeAuthorMetadata.class);
            setParameter(authorMetadata, true);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Error pasting author", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void copyAuthor() {
        JIPipeAuthorMetadata parameter = getParameter();
        UIUtils.copyToClipboard(JsonUtils.toPrettyJsonString(parameter));
    }

    private void editAuthor() {
        JIPipeAuthorMetadata parameter = getParameter();
        JIPipeDesktopParameterFormPanel.showDialog(getDesktopWorkbench(),
                parameter,
                new MarkdownText("# Edit author\n\nUse this editor to update additional author properties."),
                "Edit author",
                JIPipeDesktopParameterFormPanel.WITH_DOCUMENTATION | JIPipeDesktopParameterFormPanel.DOCUMENTATION_NO_UI | JIPipeDesktopParameterFormPanel.WITH_SEARCH_BAR | JIPipeDesktopParameterFormPanel.WITH_SCROLLING);
        reload();
    }

    @Override
    public boolean isUILabelEnabled() {
        return true;
    }

    @Override
    public void reload() {
        JIPipeAuthorMetadata author = getParameter();
        if (!StringUtils.isNullOrEmpty(author.getFirstName()) || !StringUtils.isNullOrEmpty(author.getLastName())) {
            nameLabel.setText(author.getFirstName() + " " + author.getLastName());
            nameLabel.setForeground(ThemeUtils.getCurrentStyle().getTextForeground());
        } else {
            nameLabel.setText("No name set. Please click Configure > Edit");
            nameLabel.setForeground(ThemeUtils.getCurrentStyle().getDangerColor());
        }

        // TODO: Affiliations
        if (author.getAffiliations().size() == 1) {
            affiliationLabel.setText(author.getAffiliations().getFirst().getName());
        } else if (!author.getAffiliations().isEmpty()) {
            affiliationLabel.setText(author.getAffiliations().getFirst().getName() + " (+" + (author.getAffiliations().size() - 1) + ")");
        } else {
            affiliationLabel.setText("<No affiliations>");
        }

        if (!StringUtils.isNullOrEmpty(author.getOrcidUrl())) {
            orcidLabel.setText(author.getOrcidUrl());
            orcidLabel.setForeground(ThemeUtils.getCurrentStyle().getTextMuted());
        } else {
            orcidLabel.setText("No ORCID set");
            orcidLabel.setForeground(ThemeUtils.getCurrentStyle().getDangerColor());
        }
    }
}
