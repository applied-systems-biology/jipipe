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
 *
 */

package org.hkijena.jipipe.ui.extensions;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.JIPipeDependency;
import org.hkijena.jipipe.JIPipeJavaExtension;
import org.hkijena.jipipe.api.JIPipeAuthorMetadata;
import org.hkijena.jipipe.api.registries.JIPipeExtensionRegistry;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.JIPipeWorkbenchPanel;
import org.hkijena.jipipe.utils.UIUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ExtensionItemPanel extends JIPipeWorkbenchPanel {

    private final JIPipeDependency extension;
    private JButton actionButton;

    private ExtensionItemLogoPanel logoPanel;

    public ExtensionItemPanel(JIPipeWorkbench workbench, JIPipeDependency extension) {
        super(workbench);
        this.extension = extension;
        initialize();
        updateStatus();
    }

    private void initialize() {
        setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4),
                BorderFactory.createLineBorder(Color.DARK_GRAY, 1, true)));
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(350,350));
        setSize(350,350);

        logoPanel = new ExtensionItemLogoPanel(extension);
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
                new Insets(16,8,16,8),
                0,
                0));

        JTextPane descriptionLabel = UIUtils.makeBorderlessReadonlyTextPane(extension.getMetadata().getDescription().getHtml());
        descriptionLabel.setOpaque(false);
        logoPanel.add(descriptionLabel, new GridBagConstraints(0,
                1,
                1,
                1,
                1,
                1,
                GridBagConstraints.NORTHWEST,
                GridBagConstraints.HORIZONTAL,
                new Insets(4,8,4,8),
                0,
                0));

        if(!extension.getMetadata().getAuthors().isEmpty()) {
            JPanel authorPanel = createAuthorPanel(getExtension().getMetadata().getAuthors());
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
        if(!extension.getMetadata().getCitedAuthors().isEmpty()) {
            JPanel authorPanel = createAuthorPanel(getExtension().getMetadata().getCitedAuthors());
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

    private JPanel createAuthorPanel(JIPipeAuthorMetadata.List authors) {
        JPanel authorPanel = new JPanel();
        authorPanel.setLayout(new BoxLayout(authorPanel, BoxLayout.X_AXIS));
        authorPanel.setOpaque(false);

        if(authors.size() == 1) {
            JIPipeAuthorMetadata author = authors.get(0);
            JButton authorButton = new JButton(author.toString(), UIUtils.getIconFromResources("actions/im-user.png"));
            authorButton.setToolTipText("Click to show more information");
            authorButton.addActionListener(e -> {
                JIPipeAuthorMetadata.openAuthorInfoWindow(getWorkbench().getWindow(), authors, author);
            });
            authorButton.setOpaque(false);
            authorButton.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
            authorButton.setBackground(new Color(0, 0, 0, 0));
            authorPanel.add(authorButton);
        }
        else {
            List<JIPipeAuthorMetadata> firstAuthors = authors.stream().filter(JIPipeAuthorMetadata::isFirstAuthor).collect(Collectors.toList());
            if(firstAuthors.isEmpty()) {
                firstAuthors = Arrays.asList(authors.get(0));
            }
            String name = firstAuthors.stream().map(JIPipeAuthorMetadata::getLastName).collect(Collectors.joining(", "));
            if(firstAuthors.size() < authors.size()) {
                name += " et al.";
            }

            JIPipeAuthorMetadata author = firstAuthors.get(0);
            JButton authorButton = new JButton(name, UIUtils.getIconFromResources("actions/im-user.png"));
            authorButton.setToolTipText("Click to show more information");
            authorButton.addActionListener(e -> {
                JIPipeAuthorMetadata.openAuthorInfoWindow(getWorkbench().getWindow(), authors, author);
            });
            authorButton.setOpaque(false);
            authorButton.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
            authorButton.setBackground(new Color(0, 0, 0, 0));
            authorPanel.add(authorButton);
        }
        return authorPanel;
    }

    private void initializeButtonPanel() {
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(4,4,4,4));
        add(buttonPanel, BorderLayout.SOUTH);

        if(isCoreExtension()) {
            JLabel infoLabel = new JLabel("Core extension", UIUtils.getIconFromResources("emblems/emblem-important-blue.png"), JLabel.LEFT);
            infoLabel.setToolTipText("This is a mandatory core extension that cannot be disabled");
            buttonPanel.add(infoLabel);
        }

        buttonPanel.add(Box.createHorizontalGlue());
        actionButton = new JButton();
        actionButton.addActionListener(e -> executeAction());
        buttonPanel.add(actionButton);

        if(isCoreExtension())
            actionButton.setEnabled(false);
    }

    private void updateStatus() {
        updateActionButton();
        logoPanel.repaint();
    }

    private void updateActionButton() {
        if(extensionIsActivated()) {
            if(extensionIsScheduledToDeactivate()) {
                actionButton.setText("Undo deactivation");
                actionButton.setIcon(UIUtils.getIconFromResources("actions/undo.png"));
            }
            else {
                actionButton.setText("Deactivate");
                actionButton.setIcon(UIUtils.getIconFromResources("emblems/vcs-conflicting.png"));
            }
        }
        else {
            if(extensionIsScheduledToActivate()) {
                actionButton.setText("Undo activation");
                actionButton.setIcon(UIUtils.getIconFromResources("actions/undo.png"));
            }
            else {
                actionButton.setText("Activate");
                actionButton.setIcon(UIUtils.getIconFromResources("emblems/vcs-normal.png"));
            }
        }
    }

    private boolean extensionIsScheduledToDeactivate() {
        return getExtensionRegistry().getScheduledDeactivateExtensions().contains(extension.getDependencyId());
    }

    private JIPipeExtensionRegistry getExtensionRegistry() {
        return JIPipe.getInstance().getExtensionRegistry();
    }

    private boolean extensionIsScheduledToActivate() {
        return getExtensionRegistry().getScheduledActivateExtensions().contains(extension.getDependencyId());
    }

    private boolean extensionIsActivated() {
        if (isCoreExtension())
            return true;
        return getExtensionRegistry().getActivatedExtensions().contains(extension.getDependencyId());
    }

    private boolean isCoreExtension() {
        if(extension instanceof JIPipeJavaExtension) {
            return ((JIPipeJavaExtension) extension).isCoreExtension();
        }
        return false;
    }

    private void executeAction() {
        if(extensionIsActivated()) {
            if(extensionIsScheduledToDeactivate()) {
               getExtensionRegistry().clearSchedule(extension.getDependencyId());
            }
            else {
               getExtensionRegistry().scheduleDeactivateExtension(extension.getDependencyId());
            }
        }
        else {
            if(extensionIsScheduledToActivate()) {
                getExtensionRegistry().clearSchedule(extension.getDependencyId());
            }
            else {
                getExtensionRegistry().scheduleActivateExtension(extension.getDependencyId());
            }
        }
        updateStatus();
    }

    public JIPipeDependency getExtension() {
        return extension;
    }
}
