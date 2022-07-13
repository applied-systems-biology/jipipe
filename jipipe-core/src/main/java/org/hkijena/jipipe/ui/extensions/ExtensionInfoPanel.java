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

import com.google.common.html.HtmlEscapers;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.JIPipeDependency;
import org.hkijena.jipipe.JIPipeExtension;
import org.hkijena.jipipe.JIPipeImageJUpdateSiteDependency;
import org.hkijena.jipipe.JIPipeJavaExtension;
import org.hkijena.jipipe.JIPipeJsonExtension;
import org.hkijena.jipipe.api.JIPipeAuthorMetadata;
import org.hkijena.jipipe.ui.components.FormPanel;
import org.hkijena.jipipe.ui.components.tabs.DocumentTabPane;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.ui.RoundedLineBorder;
import org.jdesktop.swingx.JXPanel;
import org.jdesktop.swingx.ScrollableSizeHint;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class ExtensionInfoPanel extends JPanel {

    private final JIPipeModernPluginManagerUI pluginManagerUI;
    private final JIPipeExtension extension;

    public ExtensionInfoPanel(JIPipeModernPluginManagerUI pluginManagerUI, JIPipeExtension extension) {
        this.pluginManagerUI = pluginManagerUI;
        this.extension = extension;
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        DocumentTabPane tabPane = new DocumentTabPane();
//        tabPane.getTabbedPane().setTabPlacement(SwingConstants.LEFT);
        add(tabPane, BorderLayout.CENTER);
        initializeOverview(tabPane);
        if(extension.isActivated() && !(extension instanceof UpdateSiteExtension)) {
            initializeNodeCompendium(tabPane);
            initializeDataTypeCompendium(tabPane);
        }
        initializeTechnicalInfo(tabPane);
    }

    private void initializeDataTypeCompendium(DocumentTabPane tabPane) {
        ExtensionDataTypeCompendiumUI compendiumUI = new ExtensionDataTypeCompendiumUI(extension);
        compendiumUI.reloadList();
        tabPane.addTab("Registered data types", UIUtils.getIconFromResources("data-types/data-type.png"), compendiumUI, DocumentTabPane.CloseMode.withoutCloseButton);
    }

    private void initializeNodeCompendium(DocumentTabPane tabPane) {
        ExtensionAlgorithmCompendiumUI compendiumUI = new ExtensionAlgorithmCompendiumUI(extension);
        compendiumUI.reloadList();
        tabPane.addTab("Registered nodes", UIUtils.getIconFromResources("data-types/node.png"), compendiumUI, DocumentTabPane.CloseMode.withoutCloseButton);
    }

    private void initializeTechnicalInfo(DocumentTabPane tabPane) {
        FormPanel panel = new FormPanel(null, FormPanel.WITH_SCROLLING);
        // Technical info
        panel.addGroupHeader("Overview", UIUtils.getIconFromResources("actions/code-context.png"));
        panel.addToForm(UIUtils.makeReadonlyTextField(extension.getDependencyId()), new JLabel("ID"), null);
        panel.addToForm(UIUtils.makeReadonlyTextField(extension.getDependencyVersion()), new JLabel("Version"), null);
        panel.addToForm(UIUtils.makeReadonlyTextField(extension.getMetadata().getLicense()), new JLabel("License"), null);
        panel.addToForm(UIUtils.makeReadonlyTextField("" + extension.getDependencyLocation()), new JLabel("Defining file"), null);
        String typeName;
        if(extension instanceof UpdateSiteExtension) {
            typeName = "ImageJ update site";
        }
        else if(extension.isCoreExtension()) {
            typeName = "Core extension [" + extension.getClass() + "]";
        }
        else {
            typeName = "Standard extension [" + extension.getClass() + "]";
        }
        panel.addToForm(new JLabel(typeName), new JLabel("Type"), null);

        if(extension.isActivated()) {
            panel.addToForm(UIUtils.makeReadonlyTextField(JIPipe.getDataTypes().getDeclaredBy(extension).size() + " data types, " + JIPipe.getNodes().getDeclaredBy(extension).size() + " nodes"), new JLabel("Registered functions"), null);
        }

        // Dependencies
        Set<JIPipeDependency> dependencies = new HashSet<>();
        if(extension instanceof JIPipeJsonExtension) {
            dependencies = ((JIPipeJsonExtension) extension).getDependencies();
        }
        if(!dependencies.isEmpty() || !extension.getImageJUpdateSiteDependencies().isEmpty()) {
            panel.addGroupHeader("Dependencies", UIUtils.getIconFromResources("actions/distribute-graph-directed.png"));
            for (JIPipeDependency dependency : dependencies) {
                panel.addToForm(UIUtils.makeReadonlyTextField("id=" + dependency.getDependencyId() + ", version=" + dependency.getDependencyVersion()), new JLabel("Dependency"), null);
            }
            for (JIPipeImageJUpdateSiteDependency dependency : extension.getImageJUpdateSiteDependencies()) {
                panel.addToForm(UIUtils.makeReadonlyTextField("name=" + dependency.getName() + ", url=" + dependency.getUrl()), new JLabel("ImageJ update site"), null);
            }
        }

        panel.addVerticalGlue();
        tabPane.addTab("Technical information", UIUtils.getIconFromResources("actions/code-context.png"), panel, DocumentTabPane.CloseMode.withoutCloseButton);
    }

    private void initializeOverview(DocumentTabPane tabPane) {
        FormPanel panel = new FormPanel(null, FormPanel.WITH_SCROLLING);

        // Title panel
        JPanel titlePanel = new JPanel();
        titlePanel.setBorder(BorderFactory.createCompoundBorder(new RoundedLineBorder(UIManager.getColor("Button.borderColor"), 1, 2), BorderFactory.createEmptyBorder(8,8,8,8)));
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.X_AXIS));
        if(extension instanceof JIPipeJavaExtension && !((JIPipeJavaExtension) extension).getSplashIcons().isEmpty()) {
            for (ImageIcon splashIcon : ((JIPipeJavaExtension) extension).getSplashIcons()) {
                titlePanel.add(new JLabel(splashIcon));
                titlePanel.add(Box.createHorizontalStrut(4));
            }
        }
        else if(extension instanceof UpdateSiteExtension)  {
            titlePanel.add(new JLabel(UIUtils.getIcon32FromResources("module-imagej.png")));
        }
        else if(extension instanceof JIPipeJavaExtension) {
            titlePanel.add(new JLabel(UIUtils.getIcon32FromResources("module-java.png")));
        }
        else {
            titlePanel.add(new JLabel(UIUtils.getIcon32FromResources("module-json.png")));
        }
        titlePanel.add(Box.createHorizontalStrut(8));
        JLabel titleLabel = new JLabel(extension.getMetadata().getName());
        titleLabel.setFont(new Font(Font.DIALOG, Font.PLAIN, 20));
        titlePanel.add(titleLabel);

        titlePanel.add(Box.createHorizontalGlue());
        ExtensionItemActionButton actionButton = new ExtensionItemActionButton(pluginManagerUI, extension);
        actionButton.setFont(new Font(Font.DIALOG, Font.PLAIN, 20));
        titlePanel.add(actionButton);

        panel.addWideToForm(titlePanel, null);

        // Description
        panel.addWideToForm(UIUtils.makeBorderlessReadonlyTextPane(extension.getMetadata().getSummary().getHtml(), false), null);
        if(!StringUtils.isNullOrEmpty(extension.getMetadata().getWebsite())) {
            JTextPane pane = UIUtils.makeBorderlessReadonlyTextPane("<html><a href=\"" + HtmlEscapers.htmlEscaper().escape(extension.getMetadata().getWebsite()) + "\">" + HtmlEscapers.htmlEscaper().escape(extension.getMetadata().getWebsite()) + "</a></html>", false);
            pane.addHyperlinkListener(e -> {
                if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                    {
                        if (Desktop.isDesktopSupported()) {
                            try {
                                Desktop.getDesktop().browse(e.getURL().toURI());
                            } catch (Exception e1) {
                                throw new RuntimeException(e1);
                            }
                        }
                    }
                }
            });
            panel.addWideToForm(pane, null);
        }
        if(!Objects.equals(extension.getMetadata().getSummary().getHtml(), extension.getMetadata().getDescription().getHtml())) {
            panel.addWideToForm(UIUtils.makeBorderlessReadonlyTextPane(extension.getMetadata().getDescription().getHtml(), false), null);
        }
        panel.addWideToForm(Box.createVerticalStrut(32), null);

        // Authors
        if(!extension.getMetadata().getAuthors().isEmpty() || !extension.getMetadata().getAcknowledgements().isEmpty()) {
            panel.addGroupHeader("Authors/Acknowledgements", UIUtils.getIconFromResources("actions/im-user.png"));
            if (!extension.getMetadata().getAuthors().isEmpty()) {
                panel.addToForm(createAuthorPanel(this, extension.getMetadata().getAuthors(), UIUtils.getIconFromResources("actions/im-user.png")), new JLabel("Extension authors"), null);
            }
            if (!extension.getMetadata().getAcknowledgements().isEmpty()) {
                panel.addToForm(createAuthorPanel(this, extension.getMetadata().getAcknowledgements(), UIUtils.getIconFromResources("actions/im-user.png")), new JLabel("Acknowledgements"), null);
            }
        }

        // Citations
        if(!StringUtils.isNullOrEmpty(extension.getMetadata().getCitation()) || !extension.getMetadata().getDependencyCitations().isEmpty()) {
            panel.addGroupHeader("Citations", UIUtils.getIconFromResources("actions/contents.png"));
            if(!StringUtils.isNullOrEmpty(extension.getMetadata().getCitation())) {
                panel.addToForm(UIUtils.makeBorderlessReadonlyTextPane(extension.getMetadata().getCitation(), false), new JLabel("Extension citation"), null);
            }
            for (String citation : extension.getMetadata().getDependencyCitations()) {
                panel.addToForm(UIUtils.makeBorderlessReadonlyTextPane(citation, false), new JLabel("Also cite"), null);
            }
        }

        panel.addVerticalGlue();
        tabPane.addTab("Overview", UIUtils.getIconFromResources("actions/plugins.png"), panel, DocumentTabPane.CloseMode.withoutCloseButton);
    }

    public static Component createAuthorPanel(Component parent, Collection<JIPipeAuthorMetadata> authors, Icon icon) {
        JXPanel projectAuthors = new JXPanel(new GridLayout((int)Math.ceil(authors.size() / 4.0), 4));
        projectAuthors.setScrollableWidthHint(ScrollableSizeHint.FIT);
        projectAuthors.setScrollableHeightHint(ScrollableSizeHint.VERTICAL_STRETCH);
        for (JIPipeAuthorMetadata author : authors) {
            JButton authorButton = new JButton(author.toString(), icon);
            authorButton.setHorizontalAlignment(SwingConstants.LEFT);
            authorButton.setToolTipText("Click to show more information");
            authorButton.addActionListener(e -> {
                JIPipeAuthorMetadata.openAuthorInfoWindow(parent, authors, author);
            });
            authorButton.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
            projectAuthors.add(authorButton);
        }
        while(projectAuthors.getComponents().length % 4 != 0) {
            projectAuthors.add(new JPanel());
        }
        projectAuthors.revalidate();
        projectAuthors.repaint();
        return projectAuthors;
    }

    private void initializeButtonPanel() {
    }
}
