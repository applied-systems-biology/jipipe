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

import net.imagej.updater.UpdateSite;
import org.hkijena.jipipe.JIPipeExtension;
import org.hkijena.jipipe.JIPipeImageJUpdateSiteDependency;
import org.hkijena.jipipe.ui.components.FormPanel;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.ui.RoundedLineBorder;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class InstallUpdateSitesConfirmationDialog extends JDialog {
    private final JIPipePluginManager pluginManager;
    private final JIPipeExtension extension;
    private Map<JIPipeImageJUpdateSiteDependency, Boolean> sitesToInstall = new HashMap<>();
    private boolean cancelled = true;

    public InstallUpdateSitesConfirmationDialog(Component parent, JIPipePluginManager pluginManager, JIPipeExtension extension, Set<String> filter) {
        super(SwingUtilities.getWindowAncestor(parent));
        this.pluginManager = pluginManager;
        this.extension = extension;
        for (JIPipeImageJUpdateSiteDependency dependency : extension.getImageJUpdateSiteDependencies()) {
            if(filter == null || filter.contains(dependency.getName())) {
                sitesToInstall.put(dependency, true);
            }
        }
        initialize();
    }

    private void initialize() {
        setIconImage(UIUtils.getIcon128FromResources("jipipe.png").getImage());
        setTitle("Activate " + extension.getMetadata().getName());


        getContentPane().setLayout(new BorderLayout());
        FormPanel formPanel = new FormPanel(null, FormPanel.WITH_SCROLLING);

        FormPanel.GroupHeaderPanel groupHeader = formPanel.addGroupHeader("ImageJ update site installation", UIUtils.getIconFromResources("apps/imagej.png"));
        if(extension instanceof UpdateSiteExtension) {
            groupHeader.setDescription("You requested the activation of the following ImageJ update site. Please note that an active internet connection is required to download the associated files.");
        }
        else {
            groupHeader.setDescription("The selected extension requires the installation of the following ImageJ update sites. You can choose to not install the dependencies, which can cause issues in the installed functionality. " +
                    "Please note that an active internet connection is required to download the associated files.");
        }

        for (Map.Entry<JIPipeImageJUpdateSiteDependency, Boolean> entry : sitesToInstall.entrySet()) {
            JIPipeImageJUpdateSiteDependency dependency = entry.getKey();
            String description = "No description available.";
            if(pluginManager.isUpdateSitesReady() && pluginManager.getUpdateSites() != null) {
                UpdateSite updateSite = pluginManager.getUpdateSites().getUpdateSite(dependency.getName(), true);
                if(updateSite != null) {
                    description = updateSite.getDescription();
                }
            }
            JPanel sitePanel = new JPanel(new GridBagLayout());
            sitePanel.setBorder(BorderFactory.createCompoundBorder(new RoundedLineBorder(UIManager.getColor("Button.borderColor"), 1, 2), BorderFactory.createEmptyBorder(8,8,8,8)));
            JLabel nameLabel = new JLabel(dependency.getName());
            nameLabel.setFont(new Font(Font.DIALOG, Font.PLAIN, 16));
            sitePanel.add(nameLabel, new GridBagConstraints(0,
                    0,
                    1,
                    1,
                    1,
                    0,
                    GridBagConstraints.NORTHWEST,
                    GridBagConstraints.HORIZONTAL,
                    new Insets(4,4,4,4),
                    0,
                    0));
            JTextPane descriptionLabel = UIUtils.makeBorderlessReadonlyTextPane(description, false);
            sitePanel.add(descriptionLabel, new GridBagConstraints(0,
                    1,
                    1,
                    1,
                    1,
                    0,
                    GridBagConstraints.NORTHWEST,
                    GridBagConstraints.HORIZONTAL,
                    new Insets(4,4,4,4),
                    0,
                    0));
            JCheckBox installCheckBox = new JCheckBox("Download & install", entry.getValue());
            installCheckBox.addActionListener(e -> {
                sitesToInstall.put(dependency, installCheckBox.isSelected());
            });
            sitePanel.add(installCheckBox, new GridBagConstraints(1,
                    1,
                    1,
                    1,
                    0,
                    0,
                    GridBagConstraints.NORTHWEST,
                    GridBagConstraints.HORIZONTAL,
                    new Insets(4,4,4,4),
                    0,
                    0));
            formPanel.addWideToForm(sitePanel, null);
        }

        formPanel.addVerticalGlue();
        getContentPane().add(formPanel, BorderLayout.CENTER);

        initializeButtonPanel();
    }

    private void initializeButtonPanel() {
        JPanel buttonPanel = new JPanel();
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(0, 8, 8, 8));
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));

        buttonPanel.add(Box.createHorizontalGlue());
        JButton cancelButton = new JButton("Cancel", UIUtils.getIconFromResources("actions/cancel.png"));
        cancelButton.addActionListener(e -> {
            cancelled = true;
            setVisible(false);
        });
        buttonPanel.add(cancelButton);

        JButton installButton = new JButton("Continue", UIUtils.getIconFromResources("actions/checkmark.png"));
        installButton.addActionListener(e -> {
            cancelled = false;
            setVisible(false);
        });
        buttonPanel.add(installButton);

        getContentPane().add(buttonPanel, BorderLayout.SOUTH);
    }

    public Map<JIPipeImageJUpdateSiteDependency, Boolean> getSitesToInstall() {
        return sitesToInstall;
    }

    public boolean isCancelled() {
        return cancelled;
    }
}
