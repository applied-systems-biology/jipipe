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
import org.hkijena.jipipe.ui.components.MarkdownDocument;
import org.hkijena.jipipe.ui.components.MarkdownReader;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Path;
import java.util.Set;

/**
 * Shown when unsatisfied dependencies are found
 */
public class UnsatisfiedDependenciesDialog extends JDialog {
    private Path fileName;
    private Set<JIPipeDependency> dependencySet;
    private boolean continueLoading = false;

    /**
     * @param parent        Parent component
     * @param fileName      the project file or folder. Only for informational purposes
     * @param dependencySet the unsatisfied dependencies
     */
    public UnsatisfiedDependenciesDialog(Component parent, Path fileName, Set<JIPipeDependency> dependencySet) {
        super(SwingUtilities.getWindowAncestor(parent));
        this.fileName = fileName;
        this.dependencySet = dependencySet;

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
            stringBuilder.append("</div>");
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

        JButton confirmButton = new JButton("Load anyways", UIUtils.getIconFromResources("actions/document-open-folder.png"));
        confirmButton.addActionListener(e -> {
            continueLoading = true;
            setVisible(false);
        });
        buttonPanel.add(confirmButton);
        content.add(buttonPanel, BorderLayout.SOUTH);

        setContentPane(content);
    }

    public boolean isContinueLoading() {
        return continueLoading;
    }

    /**
     * Shows the dialog
     *
     * @param parent        the parent
     * @param fileName      the project file or folder. Only for informational purposes
     * @param dependencySet the unsatisfied dependencies
     * @return if loading should be continued anyways
     */
    public static boolean showDialog(Component parent, Path fileName, Set<JIPipeDependency> dependencySet) {
        UnsatisfiedDependenciesDialog dialog = new UnsatisfiedDependenciesDialog(parent, fileName, dependencySet);
        dialog.setModal(true);
        dialog.pack();
        dialog.setSize(640, 480);
        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);
        return dialog.continueLoading;
    }
}
