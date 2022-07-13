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
 */

package org.hkijena.jipipe.ui.project;

import com.google.common.eventbus.Subscribe;
import net.imagej.updater.UpdateSite;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.JIPipeDependency;
import org.hkijena.jipipe.JIPipeExtension;
import org.hkijena.jipipe.JIPipeImageJUpdateSiteDependency;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.components.FormPanel;
import org.hkijena.jipipe.ui.components.MessagePanel;
import org.hkijena.jipipe.ui.components.icons.AnimatedIcon;
import org.hkijena.jipipe.ui.components.markdown.MarkdownDocument;
import org.hkijena.jipipe.ui.components.markdown.MarkdownReader;
import org.hkijena.jipipe.ui.extensions.ExtensionItemActionButton;
import org.hkijena.jipipe.ui.extensions.JIPipePluginManager;
import org.hkijena.jipipe.ui.extensions.UpdateSiteExtension;
import org.hkijena.jipipe.ui.ijupdater.JIPipeImageJPluginManager;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.ui.RoundedLineBorder;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Shown when unsatisfied dependencies are found
 */
public class UnsatisfiedDependenciesDialog extends JDialog {
    private final Set<JIPipeImageJUpdateSiteDependency> missingUpdateSites;
    private final JIPipePluginManager pluginManager;
    private final Path fileName;
    private final Set<JIPipeDependency> dependencySet;
    private boolean continueLoading = false;
    private final FormPanel formPanel = new FormPanel(null, FormPanel.WITH_SCROLLING);

    private final MessagePanel messagePanel = new MessagePanel();

    /**
     * @param workbench          the workbench
     * @param fileName           the project file or folder. Only for informational purposes
     * @param dependencySet      the unsatisfied dependencies
     * @param missingUpdateSites the missing update sites
     */
    public UnsatisfiedDependenciesDialog(JIPipeWorkbench workbench, Path fileName, Set<JIPipeDependency> dependencySet, Set<JIPipeImageJUpdateSiteDependency> missingUpdateSites) {
        super(workbench.getWindow());
        this.fileName = fileName;
        this.dependencySet = dependencySet;
        this.missingUpdateSites = missingUpdateSites;

        initialize();

        pluginManager = new JIPipePluginManager(messagePanel);
        pluginManager.getEventBus().register(this);
        pluginManager.initializeUpdateSites();
    }

    /**
     * Shows the dialog
     *
     * @param workbench          the parent
     * @param fileName           the project file or folder. Only for informational purposes
     * @param dependencySet      the unsatisfied dependencies
     * @param missingUpdateSites missing update sites
     * @return if loading should be continued anyways
     */
    public static boolean showDialog(JIPipeWorkbench workbench, Path fileName, Set<JIPipeDependency> dependencySet, Set<JIPipeImageJUpdateSiteDependency> missingUpdateSites) {
        UnsatisfiedDependenciesDialog dialog = new UnsatisfiedDependenciesDialog(workbench, fileName, dependencySet, missingUpdateSites);
        dialog.setModal(true);
        dialog.pack();
        dialog.setSize(1024, 768);
        dialog.setLocationRelativeTo(workbench.getWindow());
        dialog.setVisible(true);
        return dialog.continueLoading;
    }

    private void initialize() {
        setTitle("Missing dependencies detected");

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(messagePanel, BorderLayout.NORTH);

        formPanel.setBorder(BorderFactory.createEmptyBorder(16,16,16,16));

        formPanel.addWideToForm(UIUtils.createJLabel("Missing dependencies detected", UIUtils.getIcon32FromResources("dialog-warning.png"), 28));
        formPanel.addWideToForm(UIUtils.makeBorderlessReadonlyTextPane("The project '" + fileName.toString() + "' might not be loadable due to missing dependencies. You can choose to activate the dependencies (requires a restart of ImageJ or JIPipe) or ignore this message.", false));

        if(!dependencySet.isEmpty()) {
            formPanel.addWideToForm(Box.createVerticalStrut(32));
            formPanel.addWideToForm(UIUtils.createJLabel("JIPipe extensions", 22));

            for (JIPipeDependency dependency : dependencySet) {
                JPanel dependencyPanel = new JPanel(new GridBagLayout());
                dependencyPanel.setBorder(BorderFactory.createCompoundBorder(new RoundedLineBorder(UIManager.getColor("Button.borderColor"), 1, 2), BorderFactory.createEmptyBorder(8,8,8,8)));
                dependencyPanel.add(UIUtils.createJLabel(dependency.getMetadata().getName(), UIUtils.getIcon32FromResources("module-json.png"), 16), new GridBagConstraints(0,0,1,1,1,0,GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL,new Insets(4,4,4,4),0,0));
                JTextField idField = UIUtils.makeReadonlyBorderlessTextField("ID: " + dependency.getDependencyId() + ", version: " + dependency.getDependencyVersion());
                idField.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
                dependencyPanel.add(idField, new GridBagConstraints(0,1,1,1,1,0,GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL,new Insets(4,4,4,4),0,0));
                dependencyPanel.add(UIUtils.makeBorderlessReadonlyTextPane(dependency.getMetadata().getDescription().getHtml(), false), new GridBagConstraints(0,2,1,1,1,0,GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL,new Insets(4,4,4,4),0,0));

                // Try to find the extension
                JIPipeExtension extension = JIPipe.getInstance().getExtensionRegistry().getKnownExtensionById(dependency.getDependencyId());
                if(extension != null) {

                    ExtensionItemActionButton button = new ExtensionItemActionButton(pluginManager, extension);
                    button.setFont(new Font(Font.DIALOG, Font.PLAIN, 22));
                    dependencyPanel.add(button, new GridBagConstraints(1,0,1,1,0,0,GridBagConstraints.NORTHWEST, GridBagConstraints.NONE,new Insets(4,4,4,4),0,0));
                }
                else {
                    dependencyPanel.add(UIUtils.createJLabel("Extension not installed", UIUtils.getIcon32FromResources("emblems/emblem-rabbitvcs-conflicted.png")), new GridBagConstraints(1,0,1,1,0,0,GridBagConstraints.NORTHWEST, GridBagConstraints.NONE,new Insets(4,4,4,4),0,0));
                }

                formPanel.addWideToForm(dependencyPanel);
            }
        }
        if(!missingUpdateSites.isEmpty()) {
            formPanel.addWideToForm(Box.createVerticalStrut(32));
            AnimatedIcon hourglassAnimation = new AnimatedIcon(this, UIUtils.getIconFromResources("actions/hourglass-half.png"),
                    UIUtils.getIconFromResources("emblems/hourglass-half.png"),
                    100, 0.05);
            formPanel.addWideToForm(UIUtils.createJLabel("ImageJ update sites", 22));
            formPanel.addWideToForm(UIUtils.createJLabel("Please wait until the update sites are available ...", hourglassAnimation));
        }

        formPanel.addVerticalGlue();
        getContentPane().add(formPanel, BorderLayout.CENTER);

        initializeButtonPanel();
    }

    @Subscribe
    public void onUpdateSitesReady(JIPipePluginManager.UpdateSitesReadyEvent event) {
        if(!missingUpdateSites.isEmpty()) {
            formPanel.removeLastRow(); //Vertical glue
            formPanel.removeLastRow(); // "Please wait..."
            for (JIPipeImageJUpdateSiteDependency dependency : missingUpdateSites) {
                JPanel dependencyPanel = new JPanel(new GridBagLayout());
                dependencyPanel.setBorder(BorderFactory.createCompoundBorder(new RoundedLineBorder(UIManager.getColor("Button.borderColor"), 1, 2), BorderFactory.createEmptyBorder(8,8,8,8)));
                dependencyPanel.add(UIUtils.createJLabel(dependency.getName(), UIUtils.getIcon32FromResources("module-imagej.png"), 16), new GridBagConstraints(0,0,1,1,1,0,GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL,new Insets(4,4,4,4),0,0));
                JTextField idField = UIUtils.makeReadonlyBorderlessTextField("URL: " + dependency.getUrl());
                idField.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
                dependencyPanel.add(idField, new GridBagConstraints(0,1,1,1,1,0,GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL,new Insets(4,4,4,4),0,0));
                dependencyPanel.add(UIUtils.makeBorderlessReadonlyTextPane(dependency.getDescription(), false), new GridBagConstraints(0,2,1,1,1,0,GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL,new Insets(4,4,4,4),0,0));

                // Try to find the extension
                UpdateSiteExtension extension = new UpdateSiteExtension(dependency);
                ExtensionItemActionButton button = new ExtensionItemActionButton(pluginManager, extension);
                button.setFont(new Font(Font.DIALOG, Font.PLAIN, 22));
                dependencyPanel.add(button, new GridBagConstraints(1,0,1,1,0,0,GridBagConstraints.NORTHWEST, GridBagConstraints.NONE,new Insets(4,4,4,4),0,0));

                formPanel.addWideToForm(dependencyPanel);
            }
            formPanel.addVerticalGlue();
        }
    }

    private void initializeButtonPanel() {
        JPanel buttonPanel = new JPanel();
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(8,8,8,8));
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        buttonPanel.add(Box.createHorizontalGlue());

        JButton cancelButton = new JButton("Cancel", UIUtils.getIconFromResources("actions/cancel.png"));
        cancelButton.addActionListener(e -> {
            continueLoading = false;
            setVisible(false);
        });
        buttonPanel.add(cancelButton);

        JButton confirmButton = new JButton("Ignore and load project anyways", UIUtils.getIconFromResources("actions/document-open-folder.png"));
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
