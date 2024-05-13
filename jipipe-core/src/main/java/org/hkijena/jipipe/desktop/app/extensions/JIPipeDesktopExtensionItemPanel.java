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

package org.hkijena.jipipe.desktop.app.extensions;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.JIPipeDependency;
import org.hkijena.jipipe.JIPipePlugin;
import org.hkijena.jipipe.api.JIPipeAuthorMetadata;
import org.hkijena.jipipe.api.registries.JIPipePluginRegistry;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbenchPanel;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class JIPipeDesktopExtensionItemPanel extends JIPipeDesktopWorkbenchPanel {

    private final JIPipeDesktopModernPluginManagerUI pluginManagerUI;
    private final JIPipePlugin extension;
    private JIPipeDesktopExtensionItemLogoPanel logoPanel;

    public JIPipeDesktopExtensionItemPanel(JIPipeDesktopModernPluginManagerUI pluginManagerUI, JIPipePlugin extension) {
        super(pluginManagerUI.getDesktopWorkbench());
        this.pluginManagerUI = pluginManagerUI;
        this.extension = extension;
        initialize();
    }

    public static JPanel createAuthorPanel(Component parent, JIPipeAuthorMetadata.List authors, Icon icon, String tooltip) {
        JPanel authorPanel = new JPanel();
        authorPanel.setLayout(new BoxLayout(authorPanel, BoxLayout.X_AXIS));
        authorPanel.setOpaque(false);

        if (authors.size() == 1) {
            JIPipeAuthorMetadata author = authors.get(0);
            JButton authorButton = new JButton(author.toString(), icon);
            authorButton.setToolTipText(tooltip);
            authorButton.addActionListener(e -> {
                JIPipeAuthorMetadata.openAuthorInfoWindow(parent, authors, author);
            });
            authorButton.setOpaque(false);
            authorButton.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
            authorButton.setBackground(new Color(0, 0, 0, 0));
            authorPanel.add(authorButton);
        } else {
            List<JIPipeAuthorMetadata> firstAuthors = authors.stream().filter(JIPipeAuthorMetadata::isFirstAuthor).collect(Collectors.toList());
            if (firstAuthors.isEmpty()) {
                firstAuthors = Arrays.asList(authors.get(0));
            }
            String name = firstAuthors.stream().map(JIPipeAuthorMetadata::getLastName).collect(Collectors.joining(", "));
            if (firstAuthors.size() < authors.size()) {
                name += " et al.";
            }

            JIPipeAuthorMetadata author = firstAuthors.get(0);
            JButton authorButton = new JButton(name, icon);
            authorButton.setToolTipText(tooltip);
            authorButton.addActionListener(e -> {
                JIPipeAuthorMetadata.openAuthorInfoWindow(parent, authors, author);
            });
            authorButton.setOpaque(false);
            authorButton.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
            authorButton.setBackground(new Color(0, 0, 0, 0));
            authorPanel.add(authorButton);
        }
        return authorPanel;
    }

    private JIPipePluginRegistry getExtensionRegistry() {
        return JIPipe.getInstance().getPluginRegistry();
    }

    private void initialize() {
        setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4),
                UIUtils.createControlBorder()));
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(350, 350));
        setSize(350, 350);

        logoPanel = new JIPipeDesktopExtensionItemLogoPanel(extension);
        logoPanel.setLayout(new GridBagLayout());
        add(logoPanel, BorderLayout.CENTER);

        JLabel nameLabel = new JLabel(extension.getMetadata().getName());
        nameLabel.setFont(new Font(Font.DIALOG, Font.PLAIN, 22));
        logoPanel.add(nameLabel, new GridBagConstraints(0,
                0,
                1,
                1,
                1,
                0,
                GridBagConstraints.NORTHWEST,
                GridBagConstraints.HORIZONTAL,
                new Insets(16, 8, 16, 8),
                0,
                0));

        JTextPane descriptionLabel = UIUtils.createBorderlessReadonlyTextPane(extension.getMetadata().getSummary().getHtml(), true);
        descriptionLabel.setOpaque(false);
        logoPanel.add(descriptionLabel, new GridBagConstraints(0,
                1,
                1,
                1,
                1,
                1,
                GridBagConstraints.NORTHWEST,
                GridBagConstraints.HORIZONTAL,
                new Insets(4, 8, 4, 8),
                0,
                0));

        if (!extension.getMetadata().getAuthors().isEmpty()) {
            JPanel authorPanel = createAuthorPanel(SwingUtilities.getWindowAncestor(this), getExtension().getMetadata().getAuthors(), UIUtils.getIconFromResources("actions/im-user.png"), "Plugin authors. Click to show more information.");
            logoPanel.add(authorPanel, new GridBagConstraints(0,
                    2,
                    1,
                    1,
                    1,
                    0,
                    GridBagConstraints.NORTHWEST,
                    GridBagConstraints.HORIZONTAL,
                    new Insets(4, 8, 4, 8),
                    0,
                    0));
        }
        if (!extension.getMetadata().getAcknowledgements().isEmpty()) {
            JPanel authorPanel = createAuthorPanel(SwingUtilities.getWindowAncestor(this), getExtension().getMetadata().getAcknowledgements(), UIUtils.getIconFromResources("actions/configure.png"), "Authors of the underlying functionality. Click to show more information.");
            logoPanel.add(authorPanel, new GridBagConstraints(0,
                    3,
                    1,
                    1,
                    1,
                    0,
                    GridBagConstraints.NORTHWEST,
                    GridBagConstraints.HORIZONTAL,
                    new Insets(4, 8, 4, 8),
                    0,
                    0));
        }

        initializeButtonPanel();
    }

    private void initializeButtonPanel() {
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        add(buttonPanel, BorderLayout.SOUTH);

        if (extension.isCorePlugin()) {
            JLabel infoLabel = new JLabel("Core extension", UIUtils.getIconFromResources("emblems/emblem-important-blue.png"), JLabel.LEFT);
            infoLabel.setToolTipText("This is a mandatory core extension that cannot be disabled");
            buttonPanel.add(infoLabel);
        } else if (extension instanceof JIPipeDesktopUpdateSitePlugin) {
            JLabel infoLabel = new JLabel("ImageJ plugin", UIUtils.getIconFromResources("apps/imagej.png"), JLabel.LEFT);
            infoLabel.setToolTipText("This is not an extension for JIPipe, but an ImageJ update site that might contain functionality for JIPipe.");
            buttonPanel.add(infoLabel);
        } else if (extension.isBeta()) {
            JLabel infoLabel = new JLabel("Beta", UIUtils.getIconFromResources("emblems/vcs-locally-modified-unstaged.png"), JLabel.LEFT);
            infoLabel.setToolTipText("This extension is currently in beta-testing and might receive extensive changes in future updates.");
            buttonPanel.add(infoLabel);
        }

        buttonPanel.add(Box.createHorizontalGlue());

        JButton infoButton = new JButton("Read more", UIUtils.getIconFromResources("actions/help.png"));
        infoButton.addActionListener(e -> openInformation());
        buttonPanel.add(infoButton);

        buttonPanel.add(new JIPipeDesktopExtensionItemActionButton(pluginManagerUI.getPluginManager(), extension));
    }

    private void openInformation() {
        pluginManagerUI.showExtensionDetails(extension);
    }

    public JIPipeDependency getExtension() {
        return extension;
    }
}
