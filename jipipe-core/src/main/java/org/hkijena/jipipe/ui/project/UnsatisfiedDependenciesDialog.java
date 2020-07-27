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

import org.hkijena.jipipe.JIPipeDependency;
import org.hkijena.jipipe.JIPipeImageJUpdateSiteDependency;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.components.MarkdownDocument;
import org.hkijena.jipipe.ui.components.MarkdownReader;
import org.hkijena.jipipe.ui.ijupdater.JIPipeImageJPluginManager;
import org.hkijena.jipipe.utils.UIUtils;

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
    private Path fileName;
    private Set<JIPipeDependency> dependencySet;
    private final Set<JIPipeImageJUpdateSiteDependency> missingUpdateSites;
    private boolean continueLoading = false;

    /**
     * @param workbench the workbench
     * @param fileName      the project file or folder. Only for informational purposes
     * @param dependencySet the unsatisfied dependencies
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

    private void initialize() {
        setTitle("Missing dependencies detected");
        JPanel content = new JPanel(new BorderLayout(8, 8));

        // Generate message
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("# Unsatisfied dependencies\n\n");
        stringBuilder.append("The project `").append(fileName.toString()).append("` might not be loadable due to missing dependencies:\n\n\n");
        for (JIPipeDependency dependency : dependencySet) {
            stringBuilder.append("<div style=\"border: 1px solid gray; border-radius: 4px; margin: 4px; padding: 4px;\">");
            stringBuilder.append(JIPipeDependency.toHtmlElement(dependency));
            stringBuilder.append("</div>\n");
        }

        if(!missingUpdateSites.isEmpty()) {
            stringBuilder.append("Following ImageJ update sites " +
                    "were requested, but are not activated:\n\n");
            for (JIPipeImageJUpdateSiteDependency site : missingUpdateSites) {
                stringBuilder.append("* `").append(site.getName()).append("` (").append(site.getUrl()).append(")\n");
            }
            stringBuilder.append("\nClick 'Resolve' to install them.");
        }

        MarkdownDocument document = new MarkdownDocument(stringBuilder.toString());
        MarkdownReader markdownReader = new MarkdownReader(false);
        markdownReader.setDocument(document);
        content.add(markdownReader, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        buttonPanel.add(Box.createHorizontalGlue());

        JButton cancelButton = new JButton("Cancel", UIUtils.getIconFromResources("actions/cancel.png"));
        cancelButton.addActionListener(e -> {
            continueLoading = false;
            setVisible(false);
        });
        buttonPanel.add(cancelButton);

        if(!missingUpdateSites.isEmpty()) {
            JButton resolveButton = new JButton("Resolve", UIUtils.getIconFromResources("emblems/vcs-normal.png"));
            resolveButton.addActionListener(e -> {
                continueLoading = true;
                showResolver();
            });
            buttonPanel.add(resolveButton);
        }

        JButton confirmButton = new JButton("Load anyways", UIUtils.getIconFromResources("actions/document-open-folder.png"));
        confirmButton.addActionListener(e -> {
            continueLoading = true;
            setVisible(false);
        });
        buttonPanel.add(confirmButton);
        content.add(buttonPanel, BorderLayout.SOUTH);

        setContentPane(content);
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

    /**
     * Shows the dialog
     *
     * @param workbench        the parent
     * @param fileName      the project file or folder. Only for informational purposes
     * @param dependencySet the unsatisfied dependencies
     * @param missingUpdateSites missing update sites
     * @return if loading should be continued anyways
     */
    public static boolean showDialog(JIPipeWorkbench workbench, Path fileName, Set<JIPipeDependency> dependencySet, Set<JIPipeImageJUpdateSiteDependency> missingUpdateSites) {
        UnsatisfiedDependenciesDialog dialog = new UnsatisfiedDependenciesDialog(workbench, fileName, dependencySet, missingUpdateSites);
        dialog.setModal(true);
        dialog.pack();
        dialog.setSize(640, 480);
        dialog.setLocationRelativeTo(workbench.getWindow());
        dialog.setVisible(true);
        return dialog.continueLoading;
    }
}
