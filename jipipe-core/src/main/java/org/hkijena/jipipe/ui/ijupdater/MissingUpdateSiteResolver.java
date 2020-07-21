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

package org.hkijena.jipipe.ui.ijupdater;

import org.hkijena.jipipe.JIPipeImageJUpdateSiteDependency;
import org.hkijena.jipipe.JIPipeRegistryIssues;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.components.DocumentTabPane;
import org.hkijena.jipipe.ui.components.MarkdownDocument;
import org.hkijena.jipipe.ui.components.MarkdownReader;
import org.hkijena.jipipe.utils.UIUtils;
import org.scijava.Context;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.Window;
import java.util.stream.Collectors;

public class MissingUpdateSiteResolver extends JDialog implements JIPipeWorkbench {

    private final Context context;
    private final JIPipeRegistryIssues issues;

    public MissingUpdateSiteResolver(Context context, JIPipeRegistryIssues issues) {
        this.context = context;
        this.issues = issues;
        setSize(1024,768);
        setTitle("Missing ImageJ dependencies");
        setModal(true);
        getContentPane().setLayout(new BorderLayout());
        showInitialMessage();
    }

    private void showInitialMessage() {
        JPanel content = new JPanel(new BorderLayout());

        JLabel titleLabel = new JLabel("Missing dependencies detected", UIUtils.getIcon32FromResources("dialog-warning.png"), JLabel.LEFT);
        titleLabel.setFont(new Font(Font.DIALOG, Font.BOLD, 16));
        content.add(titleLabel, BorderLayout.NORTH);

        StringBuilder builder = new StringBuilder();
        builder.append("There are some extensions that requested the presence of ImageJ plugins (via the Update Manager). " +
                "Those dependencies could not be found, which could cause some problems while loading the extensions. ");
        if(!issues.getErroneousNodes().isEmpty() || issues.getErroneousPlugins().isEmpty()) {
            builder.append("There are ").append(issues.getErroneousPlugins().size())
                    .append(" plugins that reported errors and ")
                    .append(issues.getErroneousNodes().size())
                    .append(" nodes that failed in basic tests. The ")
                    .append("reason behind this might be the missing dependencies.");
        }
        builder.append("Following ImageJ update sites " +
                "were requested, but are not activated:\n\n");
        for (JIPipeImageJUpdateSiteDependency site : issues.getMissingImageJSites()) {
            builder.append("* `").append(site.getName()).append("` (").append(site.getURL()).append(")\n");
        }
        builder.append("\n\nYou can ignore this or install the missing dependencies by clicking 'Resolve'. " +
                "Please note that the 'Resolve' tool will add the listed URLs to your list of update sites if the name is not present.");
        MarkdownReader reader = new MarkdownReader(false, new MarkdownDocument(builder.toString()));
        reader.getContent().setOpaque(false);
        content.add(reader, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        buttonPanel.add(Box.createHorizontalGlue());

        JButton ignoreButton = new JButton("Ignore", UIUtils.getIconFromResources("actions/cancel.png"));
        ignoreButton.addActionListener(e -> setVisible(false));
        buttonPanel.add(ignoreButton);

        JButton resolveButton = new JButton("Resolve", UIUtils.getIconFromResources("emblems/vcs-normal.png"));
        resolveButton.addActionListener(e -> showResolver());
        buttonPanel.add(resolveButton);
        content.add(buttonPanel, BorderLayout.SOUTH);

        getContentPane().add(content, BorderLayout.CENTER);
    }

    private void showResolver() {
        getContentPane().removeAll();

        JIPipeImageJPluginManager pluginManager = new JIPipeImageJPluginManager(this, false);
        getContentPane().add(pluginManager, BorderLayout.CENTER);
        pluginManager.setUpdateSitesToAddAndActivate(issues.getMissingImageJSites().stream().map(JIPipeImageJUpdateSiteDependency::toUpdateSite).collect(Collectors.toList()));
        pluginManager.refreshUpdater();

        getContentPane().revalidate();
        getContentPane().repaint();
    }

    @Override
    public Window getWindow() {
        return this;
    }

    @Override
    public void sendStatusBarText(String text) {

    }

    @Override
    public Context getContext() {
        return context;
    }

    @Override
    public DocumentTabPane getDocumentTabPane() {
        return null;
    }
}
