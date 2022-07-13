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

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.JIPipeDependency;
import org.hkijena.jipipe.JIPipeExtension;
import org.hkijena.jipipe.JIPipeImageJUpdateSiteDependency;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.components.FormPanel;
import org.hkijena.jipipe.ui.components.MessagePanel;
import org.hkijena.jipipe.ui.components.markdown.MarkdownDocument;
import org.hkijena.jipipe.ui.components.markdown.MarkdownReader;
import org.hkijena.jipipe.ui.extensions.ExtensionItemActionButton;
import org.hkijena.jipipe.ui.extensions.JIPipePluginManager;
import org.hkijena.jipipe.ui.ijupdater.JIPipeImageJPluginManager;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.ui.RoundedLineBorder;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Shown when unsatisfied dependencies are found
 */
public class UnsatisfiedDependenciesDialog extends JDialog {
    private final JIPipeWorkbench workbench;
    private final Set<JIPipeImageJUpdateSiteDependency> missingUpdateSites;
    private Path fileName;
    private Set<JIPipeDependency> dependencySet;
    private boolean continueLoading = false;

    /**
     * @param workbench          the workbench
     * @param fileName           the project file or folder. Only for informational purposes
     * @param dependencySet      the unsatisfied dependencies
     * @param missingUpdateSites the missing update sites
     */
    public UnsatisfiedDependenciesDialog(JIPipeWorkbench workbench, Path fileName, Set<JIPipeDependency> dependencySet, Set<JIPipeImageJUpdateSiteDependency> missingUpdateSites) {
        super(workbench.getWindow());
        this.workbench = workbench;
        this.fileName = fileName;
        this.dependencySet = dependencySet;
        this.missingUpdateSites = missingUpdateSites;

        initialize();
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

        MessagePanel messagePanel = new MessagePanel();
        getContentPane().add(messagePanel, BorderLayout.NORTH);
        JIPipePluginManager pluginManager = null;

        FormPanel content = new FormPanel(null, FormPanel.WITH_SCROLLING);
        content.setBorder(BorderFactory.createEmptyBorder(16,16,16,16));

        content.addWideToForm(UIUtils.createJLabel("Unsatisfied dependencies", UIUtils.getIcon32FromResources("dialog-warning.png"), 28));
        content.addWideToForm(UIUtils.makeBorderlessReadonlyTextPane("The project '" + fileName.toString() + "' might not be loadable due to missing dependencies. You can choose to activate the dependencies (requires a restart of ImageJ or JIPipe) or ignore this message.", false));

        if(!dependencySet.isEmpty()) {
            content.addWideToForm(Box.createVerticalStrut(32));
            content.addWideToForm(UIUtils.createJLabel("JIPipe extensions", 22));

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
                    if(pluginManager == null) {
                        pluginManager = new JIPipePluginManager(messagePanel);
                        pluginManager.initializeUpdateSites();
                    }
                    ExtensionItemActionButton button = new ExtensionItemActionButton(pluginManager, extension);
                    button.setFont(new Font(Font.DIALOG, Font.PLAIN, 22));
                    dependencyPanel.add(button, new GridBagConstraints(1,0,1,1,0,0,GridBagConstraints.NORTHWEST, GridBagConstraints.NONE,new Insets(4,4,4,4),0,0));
                }
                else {
                    dependencyPanel.add(UIUtils.createJLabel("Extension not installed", UIUtils.getIcon32FromResources("emblems/emblem-rabbitvcs-conflicted.png")), new GridBagConstraints(1,0,1,1,0,0,GridBagConstraints.NORTHWEST, GridBagConstraints.NONE,new Insets(4,4,4,4),0,0));
                }

                content.addWideToForm(dependencyPanel);
            }
        }
        if(!missingUpdateSites.isEmpty()) {
            content.addWideToForm(Box.createVerticalStrut(32));
            content.addWideToForm(UIUtils.createJLabel("ImageJ update sites", 22));

            if(pluginManager == null) {
                pluginManager = new JIPipePluginManager(messagePanel);
                pluginManager.initializeUpdateSites();
            }
        }


//        // Generate message
//        StringBuilder stringBuilder = new StringBuilder();
//        stringBuilder.append("# Unsatisfied dependencies\n\n");
//        stringBuilder.append("The project `").append(fileName.toString()).append("` might not be loadable due to missing dependencies:\n\n\n");
//        for (JIPipeDependency dependency : dependencySet) {
//            stringBuilder.append("<div style=\"border: 1px solid gray; border-radius: 4px; margin: 4px; padding: 4px;\">");
//            stringBuilder.append(JIPipeDependency.toHtmlElement(dependency));
//            stringBuilder.append("</div>\n");
//        }
//
//        if (!missingUpdateSites.isEmpty()) {
//            stringBuilder.append("Following ImageJ update sites " +
//                    "were requested, but are not activated:\n\n");
//            for (JIPipeImageJUpdateSiteDependency site : missingUpdateSites) {
//                stringBuilder.append("* `").append(site.getName()).append("` (").append(site.getUrl()).append(")\n");
//            }
//            stringBuilder.append("\nClick 'Resolve' to install them.");
//        }
//
//        MarkdownDocument document = new MarkdownDocument(stringBuilder.toString());
//        MarkdownReader markdownReader = new MarkdownReader(false);
//        markdownReader.setDocument(document);
//        content.add(markdownReader, BorderLayout.CENTER);

        content.addVerticalGlue();
        getContentPane().add(content, BorderLayout.CENTER);

        initializeButtonPanel();
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

        if (!missingUpdateSites.isEmpty()) {
            JButton resolveButton = new JButton("Resolve", UIUtils.getIconFromResources("emblems/vcs-normal.png"));
            resolveButton.addActionListener(e -> {
                continueLoading = true;
                showResolver();
            });
            buttonPanel.add(resolveButton);
        }

        JButton confirmButton = new JButton("Ignore and load project anyways", UIUtils.getIconFromResources("actions/document-open-folder.png"));
        confirmButton.addActionListener(e -> {
            continueLoading = true;
            setVisible(false);
        });
        buttonPanel.add(confirmButton);
        getContentPane().add(buttonPanel, BorderLayout.SOUTH);
    }

    private void showResolver() {
        JPanel content = new JPanel(new BorderLayout());

        JIPipeImageJPluginManager pluginManager = new JIPipeImageJPluginManager(workbench, false);
        content.add(pluginManager, BorderLayout.CENTER);
        pluginManager.setUpdateSitesToAddAndActivate(missingUpdateSites.stream().map(JIPipeImageJUpdateSiteDependency::toUpdateSite).collect(Collectors.toList()));
        pluginManager.refreshUpdater();

        setContentPane(content);
        revalidate();
        repaint();
    }

    public boolean isContinueLoading() {
        return continueLoading;
    }
}
