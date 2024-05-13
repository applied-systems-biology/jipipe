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

package org.hkijena.jipipe.desktop.app.project;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.JIPipeDependency;
import org.hkijena.jipipe.JIPipeImageJUpdateSiteDependency;
import org.hkijena.jipipe.JIPipePlugin;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.app.extensions.JIPipeDesktopExtensionItemActionButton;
import org.hkijena.jipipe.desktop.app.extensions.JIPipeDesktopModernPluginManager;
import org.hkijena.jipipe.desktop.app.extensions.JIPipeDesktopUpdateSitePlugin;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopFormPanel;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopMessagePanel;
import org.hkijena.jipipe.desktop.commons.components.icons.JIPipeDesktopAnimatedIcon;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.VersionUtils;
import org.hkijena.jipipe.utils.ui.RoundedLineBorder;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Path;
import java.util.Set;

/**
 * Shown when potential issues are detected with the project
 */
public class JIPipeDesktopInvalidProjectDependenciesInfoDialog extends JDialog implements JIPipeDesktopModernPluginManager.UpdateSitesReadyEventListener {
    private final Set<JIPipeImageJUpdateSiteDependency> missingUpdateSites;
    private final JIPipeDesktopModernPluginManager pluginManager;
    private final Path fileName;
    private final Set<JIPipeDependency> dependencySet;
    private final JIPipeDesktopFormPanel formPanel = new JIPipeDesktopFormPanel(null, JIPipeDesktopFormPanel.WITH_SCROLLING);
    private final JIPipeDesktopMessagePanel messagePanel = new JIPipeDesktopMessagePanel();
    private boolean continueLoading = false;

    /**
     * @param workbench          the workbench
     * @param fileName           the project file or folder. Only for informational purposes
     * @param dependencySet      the unsatisfied dependencies
     * @param missingUpdateSites the missing update sites
     */
    public JIPipeDesktopInvalidProjectDependenciesInfoDialog(JIPipeDesktopWorkbench workbench, Path fileName, Set<JIPipeDependency> dependencySet, Set<JIPipeImageJUpdateSiteDependency> missingUpdateSites) {
        super(workbench.getWindow());
        this.fileName = fileName;
        this.dependencySet = dependencySet;
        this.missingUpdateSites = missingUpdateSites;

        pluginManager = new JIPipeDesktopModernPluginManager(workbench, this, messagePanel);
        initialize();

        pluginManager.getUpdateSitesReadyEventEmitter().subscribeWeak(this);
        pluginManager.initializeUpdateSites();
    }

    /**
     * Shows the dialog
     *
     * @param workbench          the parent
     * @param fileName           the project file or folder. Only for informational purposes
     * @param dependencySet      the unsatisfied dependencies
     * @param missingUpdateSites missing update sites
     * @return if loading should be continued anyway
     */
    public static boolean showDialog(JIPipeDesktopWorkbench workbench, Path fileName, Set<JIPipeDependency> dependencySet, Set<JIPipeImageJUpdateSiteDependency> missingUpdateSites) {
        JIPipeDesktopInvalidProjectDependenciesInfoDialog dialog = new JIPipeDesktopInvalidProjectDependenciesInfoDialog(workbench, fileName, dependencySet, missingUpdateSites);
        dialog.setModal(true);
        dialog.pack();
        dialog.setSize(1024, 768);
        dialog.setLocationRelativeTo(workbench.getWindow());
        dialog.setVisible(true);
        return dialog.continueLoading;
    }

    private void initialize() {
        setTitle("Potential issues detected");

        Insets insets = new Insets(4, 4, 4, 4);
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(messagePanel, BorderLayout.NORTH);

        formPanel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        formPanel.addWideToForm(UIUtils.createJLabel("Potential issues detected", UIUtils.getIcon32FromResources("dialog-warning.png"), 28));
        formPanel.addWideToForm(UIUtils.createBorderlessReadonlyTextPane("The project '" + fileName.toString() + "' might not be loadable due to invalid or missing dependencies. " +
                "If the required extensions are available, you can choose to activate them (requires a restart of ImageJ or JIPipe). " +
                "If you want, you can also ignore this message. " +
                "Please note that missing nodes or parameters will be deleted.", false));

        if (!dependencySet.isEmpty()) {
            formPanel.addWideToForm(Box.createVerticalStrut(32));
            formPanel.addWideToForm(UIUtils.createJLabel("JIPipe extensions", 22));

            for (JIPipeDependency dependency : dependencySet) {
                JPanel dependencyPanel = new JPanel(new GridBagLayout());
                dependencyPanel.setBorder(BorderFactory.createCompoundBorder(new RoundedLineBorder(UIManager.getColor("Button.borderColor"), 1, 2), BorderFactory.createEmptyBorder(8, 8, 8, 8)));
                dependencyPanel.add(UIUtils.createJLabel(dependency.getMetadata().getName(), UIUtils.getIcon32FromResources("module-json.png"), 16), new GridBagConstraints(0, 0, 1, 1, 1, 0, GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL, insets, 0, 0));
                JTextField idField = UIUtils.createReadonlyBorderlessTextField("ID: " + dependency.getDependencyId() + ", version: " + dependency.getDependencyVersion());
                idField.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
                dependencyPanel.add(idField, new GridBagConstraints(0, 1, 1, 1, 1, 0, GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL, insets, 0, 0));
                dependencyPanel.add(UIUtils.createBorderlessReadonlyTextPane(dependency.getMetadata().getDescription().getHtml(), false), new GridBagConstraints(0, 2, 1, 1, 1, 0, GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL, insets, 0, 0));

                // Try to find the extension
                JIPipePlugin extension = JIPipe.getInstance().getPluginRegistry().getKnownPluginById(dependency.getDependencyId());
                if (extension != null) {
                    if (VersionUtils.compareVersions(extension.getDependencyVersion(), dependency.getDependencyVersion()) == -1) {
                        dependencyPanel.add(UIUtils.createJLabel("Installed version too old (" + extension.getDependencyVersion() + " < " + dependency.getDependencyVersion() + ")",
                                UIUtils.getIconFromResources("emblems/emblem-rabbitvcs-conflicted.png")), new GridBagConstraints(1,
                                0,
                                1,
                                1,
                                0,
                                0,
                                GridBagConstraints.NORTHWEST,
                                GridBagConstraints.NONE,
                                insets,
                                0,
                                0));
                    } else {
                        JIPipeDesktopExtensionItemActionButton button = new JIPipeDesktopExtensionItemActionButton(pluginManager, extension);
                        button.setFont(new Font(Font.DIALOG, Font.PLAIN, 22));
                        dependencyPanel.add(button, new GridBagConstraints(1,
                                0,
                                1,
                                1,
                                0,
                                0,
                                GridBagConstraints.NORTHWEST,
                                GridBagConstraints.NONE,
                                insets,
                                0,
                                0));
                    }
                } else {
                    dependencyPanel.add(UIUtils.createJLabel("Extension not installed",
                            UIUtils.getIconFromResources("emblems/emblem-rabbitvcs-conflicted.png")), new GridBagConstraints(1,
                            0,
                            1,
                            1,
                            0,
                            0,
                            GridBagConstraints.NORTHWEST,
                            GridBagConstraints.NONE,
                            insets,
                            0,
                            0));
                }

                formPanel.addWideToForm(dependencyPanel);
            }
        }
        if (!missingUpdateSites.isEmpty()) {
            formPanel.addWideToForm(Box.createVerticalStrut(32));
            JIPipeDesktopAnimatedIcon hourglassAnimation = new JIPipeDesktopAnimatedIcon(this, UIUtils.getIconFromResources("actions/hourglass-half.png"),
                    UIUtils.getIconFromResources("emblems/hourglass-half.png"),
                    100, 0.05);
            hourglassAnimation.start();
            formPanel.addWideToForm(UIUtils.createJLabel("ImageJ update sites", 22));
            formPanel.addWideToForm(UIUtils.createJLabel("Please wait until the update sites are available ...", hourglassAnimation));
        }

        formPanel.addVerticalGlue();
        getContentPane().add(formPanel, BorderLayout.CENTER);

        initializeButtonPanel();
    }

    @Override
    public void onPluginManagerUpdateSitesReady(JIPipeDesktopModernPluginManager.UpdateSitesReadyEvent event) {
        if (!missingUpdateSites.isEmpty()) {
            formPanel.removeLastRow(); //Vertical glue
            formPanel.removeLastRow(); // "Please wait..."
            for (JIPipeImageJUpdateSiteDependency dependency : missingUpdateSites) {
                JPanel dependencyPanel = new JPanel(new GridBagLayout());
                dependencyPanel.setBorder(BorderFactory.createCompoundBorder(new RoundedLineBorder(UIManager.getColor("Button.borderColor"), 1, 2), BorderFactory.createEmptyBorder(8, 8, 8, 8)));
                dependencyPanel.add(UIUtils.createJLabel(dependency.getName(), UIUtils.getIcon32FromResources("module-imagej.png"), 16), new GridBagConstraints(0, 0, 1, 1, 1, 0, GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL, new Insets(4, 4, 4, 4), 0, 0));
                JTextField idField = UIUtils.createReadonlyBorderlessTextField("URL: " + dependency.getUrl());
                idField.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
                dependencyPanel.add(idField, new GridBagConstraints(0, 1, 1, 1, 1, 0, GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL, new Insets(4, 4, 4, 4), 0, 0));
                dependencyPanel.add(UIUtils.createBorderlessReadonlyTextPane(dependency.getDescription(), false), new GridBagConstraints(0, 2, 1, 1, 1, 0, GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL, new Insets(4, 4, 4, 4), 0, 0));

                // Try to find the extension
                JIPipeDesktopUpdateSitePlugin extension = new JIPipeDesktopUpdateSitePlugin(dependency);
                JIPipeDesktopExtensionItemActionButton button = new JIPipeDesktopExtensionItemActionButton(pluginManager, extension);
                button.setFont(new Font(Font.DIALOG, Font.PLAIN, 22));
                dependencyPanel.add(button, new GridBagConstraints(1, 0, 1, 1, 0, 0, GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(4, 4, 4, 4), 0, 0));

                formPanel.addWideToForm(dependencyPanel);
            }
            formPanel.addVerticalGlue();
        }
    }

    private void initializeButtonPanel() {
        JPanel buttonPanel = new JPanel();
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        buttonPanel.add(Box.createHorizontalGlue());

        JButton cancelButton = new JButton("Cancel", UIUtils.getIconFromResources("actions/cancel.png"));
        cancelButton.addActionListener(e -> {
            continueLoading = false;
            setVisible(false);
        });
        buttonPanel.add(cancelButton);

        JButton confirmButton = new JButton("Ignore and load project anyway", UIUtils.getIconFromResources("actions/document-open-folder.png"));
        confirmButton.addActionListener(e -> {
            continueLoading = true;
            setVisible(false);
        });
        buttonPanel.add(confirmButton);
        getContentPane().add(buttonPanel, BorderLayout.SOUTH);
    }

    public boolean isContinueLoading() {
        return continueLoading;
    }
}
