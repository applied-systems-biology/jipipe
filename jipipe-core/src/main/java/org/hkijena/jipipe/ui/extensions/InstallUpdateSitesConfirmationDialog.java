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

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dialog;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

public class InstallUpdateSitesConfirmationDialog extends JDialog {
    private final JIPipeModernPluginManagerUI pluginManagerUI;
    private final JIPipeExtension extension;
    private Map<JIPipeImageJUpdateSiteDependency, Boolean> sitesToInstall = new HashMap<>();
    private boolean cancelled = false;

    public InstallUpdateSitesConfirmationDialog(Component parent, JIPipeModernPluginManagerUI pluginManagerUI, JIPipeExtension extension) {
        super(SwingUtilities.getWindowAncestor(parent));
        this.pluginManagerUI = pluginManagerUI;
        this.extension = extension;
        for (JIPipeImageJUpdateSiteDependency dependency : extension.getImageJUpdateSiteDependencies()) {
            sitesToInstall.put(dependency, true);
        }
        initialize();
    }

    private void initialize() {
        setIconImage(UIUtils.getIcon128FromResources("jipipe.png").getImage());
        setTitle("Activate " + extension.getMetadata().getName());


        getContentPane().setLayout(new BorderLayout());
        FormPanel formPanel = new FormPanel(null, FormPanel.WITH_SCROLLING);

        FormPanel.GroupHeaderPanel groupHeader = formPanel.addGroupHeader("ImageJ update site installation", UIUtils.getIconFromResources("apps/image.png"));
        if(extension instanceof UpdateSiteExtension) {
            groupHeader.setDescription("You requested the activation of the following ImageJ update site. Please note that an active internet connection is required to download the associated files.");
        }
        else {
            groupHeader.setDescription("The selected extension requires the installation of the following ImageJ update sites. You can choose to not install the dependencies, which can cause issues in the installed functionality. " +
                    "Please note that an active internet connection is required to download the associated files.");
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

        JButton installButton = new JButton("Activate", UIUtils.getIconFromResources("actions/checkmark.png"));
        installButton.addActionListener(e -> {
            cancelled = false;
            setVisible(false);
        });
        buttonPanel.add(installButton);

        getContentPane().add(buttonPanel, BorderLayout.SOUTH);
    }
}
