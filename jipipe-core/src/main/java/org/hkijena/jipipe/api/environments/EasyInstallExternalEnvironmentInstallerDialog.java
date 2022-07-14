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

package org.hkijena.jipipe.api.environments;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.JIPipeExtension;
import org.hkijena.jipipe.ui.components.FormPanel;
import org.hkijena.jipipe.ui.extensions.ExtensionItemActionButton;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.ui.RoundedLineBorder;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;
import java.util.stream.Collectors;

public class EasyInstallExternalEnvironmentInstallerDialog extends JDialog {
    private final EasyInstallExternalEnvironmentInstaller installer;

    private final FormPanel formPanel = new FormPanel(null, FormPanel.WITH_SCROLLING);

    private EasyInstallExternalEnvironmentInstaller.AvailablePackage targetPackage;

    public EasyInstallExternalEnvironmentInstallerDialog(EasyInstallExternalEnvironmentInstaller installer) {
        super(installer.getWorkbench().getWindow());
        this.installer = installer;
        initialize();
    }
    private void initialize() {

        List<EasyInstallExternalEnvironmentInstaller.AvailablePackage> supportedPackages = installer.getAvailablePackages().stream().filter(EasyInstallExternalEnvironmentInstaller.AvailablePackage::isSupported).collect(Collectors.toList());
        List<EasyInstallExternalEnvironmentInstaller.AvailablePackage> unsupportedPackages = installer.getAvailablePackages().stream().filter(EasyInstallExternalEnvironmentInstaller.AvailablePackage::isUnsupported).collect(Collectors.toList());

        setTitle(installer.getTaskLabel());

        getContentPane().setLayout(new BorderLayout());

        formPanel.setBorder(BorderFactory.createEmptyBorder(16,16,16,16));

        formPanel.addWideToForm(UIUtils.createJLabel(installer.getDialogHeading(), UIUtils.getIcon32FromResources("install.png"), 28));
        formPanel.addWideToForm(UIUtils.makeBorderlessReadonlyTextPane(installer.getDialogDescription().getHtml(), false));

        formPanel.addWideToForm(Box.createVerticalStrut(16));
        formPanel.addWideToForm(UIUtils.createJLabel("Please be aware that some downloads can be as large as multiple gigabytes", UIUtils.getIconFromResources("emblems/emblem-important-blue.png")));
        formPanel.addWideToForm(UIUtils.createJLabel("We recommend to always review the download URL", UIUtils.getIconFromResources("emblems/emblem-important-blue.png")));

        formPanel.addWideToForm(Box.createVerticalStrut(32));
        formPanel.addWideToForm(UIUtils.createJLabel("Available packages", 22));

        if(supportedPackages.isEmpty()) {
            formPanel.addWideToForm(UIUtils.createJLabel("No supported packages found.", UIUtils.getIconFromResources("emblems/emblem-rabbitvcs-conflicted.png")));
        }
        else {
            for (EasyInstallExternalEnvironmentInstaller.AvailablePackage availablePackage : supportedPackages) {
                addPackagePanel(availablePackage);
            }
        }

        if(!unsupportedPackages.isEmpty()) {
            formPanel.addWideToForm(Box.createVerticalStrut(32));
            formPanel.addWideToForm(UIUtils.createJLabel("Unsupported packages", 22));
            for (EasyInstallExternalEnvironmentInstaller.AvailablePackage availablePackage : unsupportedPackages) {
                addPackagePanel(availablePackage);
            }
        }

        formPanel.addVerticalGlue();
        UIUtils.invokeScrollToTop(formPanel.getScrollPane());

        getContentPane().add(formPanel, BorderLayout.CENTER);

        initializeButtonPanel();
    }

    private void addPackagePanel(EasyInstallExternalEnvironmentInstaller.AvailablePackage availablePackage) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createCompoundBorder(new RoundedLineBorder(UIManager.getColor("Button.borderColor"), 1, 2), BorderFactory.createEmptyBorder(8,8,8,8)));
        panel.add(UIUtils.createJLabel(availablePackage.getName(), UIUtils.getIcon32FromResources("module-json.png"), 16), new GridBagConstraints(0,0,1,1,1,0,GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL,new Insets(4,4,4,4),0,0));
        JTextArea idField = UIUtils.makeReadonlyBorderlessTextArea(availablePackage.getUrl());
        panel.add(idField, new GridBagConstraints(0,1,1,1,1,0,GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL,new Insets(4,4,4,4),0,0));
        panel.add(UIUtils.makeBorderlessReadonlyTextPane(availablePackage.getDescription(), false), new GridBagConstraints(0,2,1,1,1,0,GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL,new Insets(4,4,4,4),0,0));

        JButton installButton = new JButton("Install now", UIUtils.getIconFromResources("emblems/vcs-normal.png"));
        installButton.setFont(new Font(Font.DIALOG, Font.PLAIN, 22));
        installButton.addActionListener(e -> confirmInstallation(availablePackage));
        panel.add(installButton, new GridBagConstraints(1,0,1,1,0,0,GridBagConstraints.NORTHWEST, GridBagConstraints.NONE,new Insets(4,4,4,4),0,0));

        formPanel.addWideToForm(panel);
    }

    private void confirmInstallation(EasyInstallExternalEnvironmentInstaller.AvailablePackage availablePackage) {
       this.targetPackage = availablePackage;
       setVisible(false);
    }


    private void initializeButtonPanel() {
        JPanel buttonPanel = new JPanel();
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(8,8,8,8));
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        buttonPanel.add(Box.createHorizontalGlue());

        JButton cancelButton = new JButton("Cancel", UIUtils.getIconFromResources("actions/cancel.png"));
        cancelButton.addActionListener(e -> {
            targetPackage = null;
            setVisible(false);
        });
        buttonPanel.add(cancelButton);

        getContentPane().add(buttonPanel, BorderLayout.SOUTH);
    }

}
