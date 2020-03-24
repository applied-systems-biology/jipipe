package org.hkijena.acaq5.ui.project;

import org.hkijena.acaq5.ACAQDependency;
import org.hkijena.acaq5.ui.components.MarkdownDocument;
import org.hkijena.acaq5.ui.components.MarkdownReader;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Path;
import java.util.Set;

public class UnsatisfiedDependenciesDialog extends JDialog {
    private Path fileName;
    private Set<ACAQDependency> dependencySet;
    private boolean continueLoading = false;

    public UnsatisfiedDependenciesDialog(Component parent, Path fileName, Set<ACAQDependency> dependencySet) {
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
        for (ACAQDependency dependency : dependencySet) {
            stringBuilder.append("<div style=\"border: 1px solid gray; border-radius: 4px; margin: 4px; padding: 4px;\">");
            stringBuilder.append(ACAQDependency.toHtmlElement(dependency));
            stringBuilder.append("</div>");
        }
        MarkdownDocument document = new MarkdownDocument(stringBuilder.toString());
        MarkdownReader markdownReader = new MarkdownReader(false);
        markdownReader.setDocument(document);
        content.add(markdownReader, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        buttonPanel.add(Box.createHorizontalGlue());

        JButton cancelButton = new JButton("Cancel", UIUtils.getIconFromResources("remove.png"));
        cancelButton.addActionListener(e -> {
            continueLoading = false;
            setVisible(false);
        });
        buttonPanel.add(cancelButton);

        JButton confirmButton = new JButton("Load anyways", UIUtils.getIconFromResources("open.png"));
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
     * @param parent
     * @param fileName
     * @param dependencySet
     * @return if loading should be continued anyways
     */
    public static boolean showDialog(Component parent, Path fileName, Set<ACAQDependency> dependencySet) {
        UnsatisfiedDependenciesDialog dialog = new UnsatisfiedDependenciesDialog(parent, fileName, dependencySet);
        dialog.setModal(true);
        dialog.pack();
        dialog.setSize(640, 480);
        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);
        return dialog.continueLoading;
    }
}
