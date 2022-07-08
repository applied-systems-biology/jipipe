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
import org.hkijena.jipipe.api.registries.JIPipeExtensionRegistry;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.JIPipeWorkbenchPanel;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;

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
        logoPanel.setLayout(new BorderLayout());
        add(logoPanel, BorderLayout.CENTER);

        JLabel label = new JLabel(extension.getMetadata().getName());
        logoPanel.add(label, BorderLayout.CENTER);

        initializeButtonPanel();
    }

    private void initializeButtonPanel() {
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(4,4,4,4));
        add(buttonPanel, BorderLayout.SOUTH);

        if(isCoreExtension()) {
            JLabel infoLabel = new JLabel("Core extension", UIUtils.getIconFromResources("emblems/emblem-important-blue.png"), JLabel.LEFT);
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
