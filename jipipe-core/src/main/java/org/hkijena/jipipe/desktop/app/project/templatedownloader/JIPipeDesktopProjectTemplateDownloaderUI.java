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

package org.hkijena.jipipe.desktop.app.project.templatedownloader;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopFormPanel;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.ui.RoundedLineBorder;

import javax.swing.*;
import java.awt.*;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class JIPipeDesktopProjectTemplateDownloaderUI extends JDialog {
    private final JIPipeDesktopProjectTemplateDownloaderRun installer;

    private final JIPipeDesktopFormPanel formPanel = new JIPipeDesktopFormPanel(null, JIPipeDesktopFormPanel.WITH_SCROLLING);

    private final Set<JIPipeDesktopProjectTemplateDownloaderPackage> targetPackages = new HashSet<>();

    public JIPipeDesktopProjectTemplateDownloaderUI(JIPipeDesktopProjectTemplateDownloaderRun installer) {
        super(installer.getDesktopWorkbench().getWindow());
        this.installer = installer;
        initialize();
    }

    private void initialize() {

        setTitle("Download project templates");

        getContentPane().setLayout(new BorderLayout());

        formPanel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        formPanel.addWideToForm(UIUtils.createJLabel("Download project templates", UIUtils.getIcon32FromResources("install.png"), 28));

        formPanel.addWideToForm(Box.createVerticalStrut(16));
        formPanel.addWideToForm(UIUtils.createJLabel("Please select which templates should be downloaded", UIUtils.getIconFromResources("emblems/emblem-important-blue.png")));
        formPanel.addWideToForm(UIUtils.createJLabel("We recommend to always review the download URL", UIUtils.getIconFromResources("emblems/emblem-important-blue.png")));

        formPanel.addWideToForm(Box.createVerticalStrut(32));
        formPanel.addWideToForm(UIUtils.createJLabel("Available templates", 22));

        List<JIPipeDesktopProjectTemplateDownloaderPackage> newPackages = installer.getAvailablePackages().stream().filter(p -> !JIPipe.getInstance().getProjectTemplateRegistry().getRegisteredTemplates().containsKey(p.getId())).collect(Collectors.toList());
        List<JIPipeDesktopProjectTemplateDownloaderPackage> existingPackages = installer.getAvailablePackages().stream().filter(p -> JIPipe.getInstance().getProjectTemplateRegistry().getRegisteredTemplates().containsKey(p.getId())).collect(Collectors.toList());

        if (newPackages.isEmpty()) {
            formPanel.addWideToForm(UIUtils.createJLabel("No additional templates found.", UIUtils.getIconFromResources("emblems/emblem-important-blue.png")));
        } else {
            for (JIPipeDesktopProjectTemplateDownloaderPackage availablePackage : newPackages) {
                addPackagePanel(availablePackage);
            }
        }

        if (!existingPackages.isEmpty()) {
            formPanel.addWideToForm(Box.createVerticalStrut(32));
            formPanel.addWideToForm(UIUtils.createJLabel("Already downloaded", 22));
            for (JIPipeDesktopProjectTemplateDownloaderPackage availablePackage : existingPackages) {
                addPackagePanel(availablePackage);
            }
        }

        formPanel.addVerticalGlue();
        UIUtils.invokeScrollToTop(formPanel.getScrollPane());

        getContentPane().add(formPanel, BorderLayout.CENTER);

        initializeButtonPanel();
    }

    private void addPackagePanel(JIPipeDesktopProjectTemplateDownloaderPackage availablePackage) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createCompoundBorder(new RoundedLineBorder(UIManager.getColor("Button.borderColor"), 1, 2), BorderFactory.createEmptyBorder(8, 8, 8, 8)));
        panel.add(UIUtils.createJLabel(availablePackage.getName(), UIUtils.getIcon32FromResources("jipipe-file.png"), 16), new GridBagConstraints(0, 0, 1, 1, 1, 0, GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL, new Insets(4, 4, 4, 4), 0, 0));

        panel.add(UIUtils.createBorderlessReadonlyTextPane(availablePackage.getDescription(), false), new GridBagConstraints(0, 1, 1, 1, 1, 0, GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL, new Insets(4, 4, 4, 4), 0, 0));
        panel.add(UIUtils.createBorderlessReadonlyTextPane("<html><a href=\"" + availablePackage.getWebsite() + "\">" + availablePackage.getWebsite() + "</a></html>", false),
                new GridBagConstraints(0, 2, 1, 1, 1, 0, GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL, new Insets(4, 4, 4, 4), 0, 0));
        panel.add(UIUtils.createBorderlessReadonlyTextPane(availablePackage.getSizeInfo(), false), new GridBagConstraints(0, 3, 1, 1, 1, 0, GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL, new Insets(4, 4, 4, 4), 0, 0));

        JTextArea idField = UIUtils.createReadonlyBorderlessTextArea(availablePackage.getUrl());
        idField.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        panel.add(idField, new GridBagConstraints(0, 4, 1, 1, 1, 0, GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL, new Insets(4, 4, 4, 4), 0, 0));


        JCheckBox installToggle = new JCheckBox("Download this template");
        installToggle.setFont(new Font(Font.DIALOG, Font.PLAIN, 22));
        installToggle.addActionListener(e -> {
            if (installToggle.isSelected()) {
                targetPackages.add(availablePackage);
            } else {
                targetPackages.remove(availablePackage);
            }
        });
        panel.add(installToggle, new GridBagConstraints(1, 0, 1, 1, 0, 0, GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(4, 4, 4, 4), 0, 0));

        formPanel.addWideToForm(panel);
    }

    private void initializeButtonPanel() {
        JPanel buttonPanel = new JPanel();
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        buttonPanel.add(Box.createHorizontalGlue());

        JButton cancelButton = new JButton("Cancel", UIUtils.getIconFromResources("actions/cancel.png"));
        cancelButton.addActionListener(e -> {
            targetPackages.clear();
            setVisible(false);
        });
        buttonPanel.add(cancelButton);

        JButton downloadButton = new JButton("Download selected templates", UIUtils.getIconFromResources("actions/download.png"));
        downloadButton.addActionListener(e -> {
            setVisible(false);
        });
        buttonPanel.add(downloadButton);

        getContentPane().add(buttonPanel, BorderLayout.SOUTH);
    }

    public Set<JIPipeDesktopProjectTemplateDownloaderPackage> getTargetPackages() {
        return targetPackages;
    }
}
