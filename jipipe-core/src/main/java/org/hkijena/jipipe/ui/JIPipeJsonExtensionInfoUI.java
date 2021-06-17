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

package org.hkijena.jipipe.ui;

import com.google.common.eventbus.Subscribe;
import ij.IJ;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.extensions.settings.ProjectsSettings;
import org.hkijena.jipipe.ui.components.BackgroundPanel;
import org.hkijena.jipipe.ui.components.FormPanel;
import org.hkijena.jipipe.ui.components.MarkdownDocument;
import org.hkijena.jipipe.ui.components.MarkdownReader;
import org.hkijena.jipipe.ui.components.RecentProjectListCellRenderer;
import org.hkijena.jipipe.utils.ReflectionUtils;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.jar.Attributes;

/**
 * UI that shows some introduction
 */
public class JIPipeJsonExtensionInfoUI extends JIPipeJsonExtensionWorkbenchPanel {

    private final JList<Path> recentProjectsList = new JList<>();

    /**
     * Creates a new instance
     *
     * @param workbenchUI The workbench UI
     */
    public JIPipeJsonExtensionInfoUI(JIPipeJsonExtensionWorkbench workbenchUI) {
        super(workbenchUI);
        initialize();
        refreshRecentProjects();
        ProjectsSettings.getInstance().getEventBus().register(this);
    }

    private void refreshRecentProjects() {
        DefaultListModel<Path> model = new DefaultListModel<>();
        for (Path path : ProjectsSettings.getInstance().getRecentJsonExtensionProjects()) {
            if (Files.exists(path)) {
                model.addElement(path);
            }
        }
        if (model.getSize() == 0) {
            model.addElement(null);
        }
        recentProjectsList.setModel(model);
    }

    private void initialize() {
        setLayout(new BorderLayout());

        initializeHeaderPanel();
        initRecentProjects();
        initContent();
    }

    private void initContent() {
        MarkdownReader markdownReader = new MarkdownReader(false, MarkdownDocument.fromPluginResource("documentation/introduction-extension-builder.md", new HashMap<>()));
        markdownReader.setBorder(null);
        markdownReader.getScrollPane().setBorder(null);
        add(markdownReader, BorderLayout.CENTER);
    }

    private void initRecentProjects() {
        recentProjectsList.setCellRenderer(new RecentProjectListCellRenderer());
        JScrollPane scrollPane = new JScrollPane(recentProjectsList);
        scrollPane.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, Color.DARK_GRAY));
        add(scrollPane, BorderLayout.WEST);

        recentProjectsList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    Path value = recentProjectsList.getSelectedValue();
                    if (value != null) {
                        ((JIPipeJsonExtensionWindow) getExtensionWorkbenchUI().getWindow()).openProject(value);
                    }
                } else {
                    if (recentProjectsList.getMousePosition().x > recentProjectsList.getWidth() - 50) {
                        Path value = recentProjectsList.getSelectedValue();
                        if (value != null) {
                            ((JIPipeJsonExtensionWindow) getExtensionWorkbenchUI().getWindow()).openProject(value);
                        }
                    }
                }
            }
        });
    }

    @Subscribe
    public void onRecentProjectsChanged(JIPipeParameterCollection.ParameterChangedEvent event) {
        if ("recent-json-extension-projects".equals(event.getKey())) {
            refreshRecentProjects();
        }
    }

    private void initializeHeaderPanel() {
        JPanel headerPanel;
        headerPanel = new BackgroundPanel(UIUtils.getHeaderPanelBackground(), true);
        headerPanel.setLayout(new BorderLayout());
        headerPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.DARK_GRAY));
        headerPanel.setPreferredSize(new Dimension(headerPanel.getPreferredSize().width, 200));
        JLabel logo = new JLabel(new ImageIcon(UIUtils.getExtensionBuilderLogo400()));
        logo.setBorder(BorderFactory.createEmptyBorder(0, 32, 0, 0));
        headerPanel.add(logo, BorderLayout.WEST);

        FormPanel technicalInfo = new FormPanel(null, FormPanel.NONE);
        technicalInfo.setOpaque(false);
        technicalInfo.getContentPanel().setOpaque(false);

        technicalInfo.addToForm(UIUtils.makeReadonlyBorderlessTextField(StringUtils.orElse(getClass().getPackage().getImplementationVersion(), "Development")), new JLabel("Version"), null);
        Attributes manifestAttributes = ReflectionUtils.getManifestAttributes();
        if (manifestAttributes != null) {
            String implementationDateString = manifestAttributes.getValue("Implementation-Date");
            technicalInfo.addToForm(UIUtils.makeReadonlyBorderlessTextField(StringUtils.orElse(implementationDateString, "N/A")), new JLabel("Build time"), null);
        }
        technicalInfo.addToForm(UIUtils.makeReadonlyBorderlessTextField(StringUtils.orElse(IJ.getVersion(), "N/A")), new JLabel("ImageJ"), null);
        technicalInfo.addToForm(UIUtils.makeReadonlyBorderlessTextField(StringUtils.orElse(System.getProperty("java.version"), "N/A")), new JLabel("Java"), null);
        technicalInfo.addVerticalGlue();

        headerPanel.add(technicalInfo, BorderLayout.EAST);

        initializeToolbar(headerPanel);

        add(headerPanel, BorderLayout.NORTH);
    }

    private void initializeToolbar(JPanel topPanel) {
        JPanel toolBar = new JPanel();
        toolBar.setBorder(BorderFactory.createEmptyBorder(0, 32, 8, 0));
        toolBar.setLayout(new BoxLayout(toolBar, BoxLayout.X_AXIS));
        toolBar.setOpaque(false);

        JButton openWebsiteButton = new JButton("Visit our website", UIUtils.getIconFromResources("actions/web-browser.png"));
        openWebsiteButton.setToolTipText("https://www.jipipe.org/");
        openWebsiteButton.addActionListener(e -> UIUtils.openWebsite("https://www.jipipe.org/"));
        openWebsiteButton.setOpaque(false);
        openWebsiteButton.setBackground(new Color(0, 0, 0, 0));
        toolBar.add(openWebsiteButton);
        toolBar.add(Box.createHorizontalStrut(4));

        JButton openTutorialsButton = new JButton("Tutorial", UIUtils.getIconFromResources("actions/graduation-cap.png"));
        openTutorialsButton.setToolTipText("https://www.jipipe.org/tutorials/extension");
        openTutorialsButton.addActionListener(e -> UIUtils.openWebsite("https://www.jipipe.org/tutorial/extension"));
        openTutorialsButton.setOpaque(false);
        openTutorialsButton.setBackground(new Color(0, 0, 0, 0));
        toolBar.add(openTutorialsButton);
        toolBar.add(Box.createHorizontalStrut(4));

        JButton openDocumentationButton = new JButton("Documentation", UIUtils.getIconFromResources("actions/help-info.png"));
        openDocumentationButton.setToolTipText("https://www.jipipe.org/documentation/create-json-extensions");
        openDocumentationButton.addActionListener(e -> UIUtils.openWebsite("https://www.jipipe.org/documentation/create-json-extensions"));
        openDocumentationButton.setOpaque(false);
        openDocumentationButton.setBackground(new Color(0, 0, 0, 0));
        toolBar.add(openDocumentationButton);
        toolBar.add(Box.createHorizontalStrut(4));

        JButton openSourceCodeButton = new JButton("Source code", UIUtils.getIconFromResources("actions/dialog-xml-editor.png"));
        openSourceCodeButton.setToolTipText("https://github.com/applied-systems-biology/jipipe/");
        openSourceCodeButton.addActionListener(e -> UIUtils.openWebsite("https://github.com/applied-systems-biology/jipipe/"));
        openSourceCodeButton.setOpaque(false);
        openSourceCodeButton.setBackground(new Color(0, 0, 0, 0));
        toolBar.add(openSourceCodeButton);
        toolBar.add(Box.createHorizontalStrut(4));

        topPanel.add(toolBar, BorderLayout.SOUTH);
    }
}
