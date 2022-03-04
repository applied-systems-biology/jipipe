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

package org.hkijena.jipipe.ui.documentation;

import com.google.common.eventbus.Subscribe;
import ij.IJ;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeProjectTemplate;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.extensions.settings.GeneralUISettings;
import org.hkijena.jipipe.extensions.settings.ProjectsSettings;
import org.hkijena.jipipe.ui.JIPipeProjectWindow;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbench;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbenchAccess;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbenchPanel;
import org.hkijena.jipipe.ui.components.BackgroundPanel;
import org.hkijena.jipipe.ui.components.FormPanel;
import org.hkijena.jipipe.ui.components.RecentProjectListCellRenderer;
import org.hkijena.jipipe.ui.components.markdown.MarkdownDocument;
import org.hkijena.jipipe.ui.components.markdown.MarkdownReader;
import org.hkijena.jipipe.ui.components.renderers.TemplateProjectListCellRenderer;
import org.hkijena.jipipe.ui.components.search.SearchTextField;
import org.hkijena.jipipe.ui.components.tabs.DocumentTabPane;
import org.hkijena.jipipe.utils.ReflectionUtils;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.ui.DotSlideshow;
import org.hkijena.jipipe.utils.ui.RoundedLineBorder;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.jar.Attributes;

/**
 * UI that shows some introduction
 */
public class WelcomePanel extends JIPipeProjectWorkbenchPanel {

    private final SearchTextField templateSearch = new SearchTextField();
    private final JList<JIPipeProjectTemplate> templateList = new JList<>();

    /**
     * Creates a new instance
     *
     * @param workbenchUI The workbench UI
     */
    public WelcomePanel(JIPipeProjectWorkbench workbenchUI) {
        super(workbenchUI);
        initialize();
        refreshTemplateProjects();
    }

    private void refreshTemplateProjects() {
        DefaultListModel<JIPipeProjectTemplate> model = new DefaultListModel<>();
        for (JIPipeProjectTemplate template : JIPipeProjectTemplate.listTemplates()) {
            if (templateSearch.test(template.getMetadata().getName() + " " + template.getMetadata().getTemplateDescription())) {
                model.addElement(template);
            }
        }
        if (model.getSize() == 0 && templateSearch.getSearchStrings().length == 0) {
            model.addElement(null);
        }
        templateList.setModel(model);
    }



    private void initialize() {
        setLayout(new BorderLayout());

        initializeHeaderPanel();
        initRecentProjectsAndTemplates();
        initContent();
    }

    private void initContent() {
        if (!GeneralUISettings.getInstance().isShowIntroductionTour())
            return;

        JPanel tourPanel = new JPanel();
        tourPanel.setLayout(new BoxLayout(tourPanel, BoxLayout.Y_AXIS));
        JPanel tourContentPanel = new JPanel(new BorderLayout());
        tourContentPanel.setAlignmentY(JComponent.CENTER_ALIGNMENT);
        tourContentPanel.setAlignmentX(JComponent.CENTER_ALIGNMENT);
        tourContentPanel.setBorder(new RoundedLineBorder(Color.GRAY, 1, 3, true));
        tourContentPanel.setMaximumSize(new Dimension(800, 650));
//        tourContentPanel.setMinimumSize(new Dimension(800,600));
        tourContentPanel.setPreferredSize(new Dimension(800, 650));
        tourContentPanel.setBackground(UIManager.getColor("TextArea.background"));
        tourPanel.add(Box.createVerticalGlue());
        tourPanel.add(tourContentPanel);
        tourPanel.add(Box.createVerticalGlue());
        add(tourPanel, BorderLayout.CENTER);

        DotSlideshow slideshow = new DotSlideshow();
        MarkdownReader slideWelcome = new MarkdownReader(false, MarkdownDocument.fromPluginResource("documentation/introduction_welcome.md", new HashMap<>()));
        slideWelcome.setBorder(BorderFactory.createLineBorder(UIManager.getColor("TextArea.background"), 16));
        slideshow.addSlide(slideWelcome, "Welcome to JIPipe");

        MarkdownReader slideOrganization = new MarkdownReader(false, MarkdownDocument.fromPluginResource("documentation/introduction_organization.md", new HashMap<>()));
        slideOrganization.setBorder(BorderFactory.createLineBorder(UIManager.getColor("TextArea.background"), 16));
        slideshow.addSlide(slideOrganization, "Organizing your pipeline");

        MarkdownReader slideNodes = new MarkdownReader(false, MarkdownDocument.fromPluginResource("documentation/introduction_nodes.md", new HashMap<>()));
        slideNodes.setBorder(BorderFactory.createLineBorder(UIManager.getColor("TextArea.background"), 16));
        slideshow.addSlide(slideNodes, "Adding nodes");

        MarkdownReader slidesRunning = new MarkdownReader(false, MarkdownDocument.fromPluginResource("documentation/introduction_running.md", new HashMap<>()));
        slidesRunning.setBorder(BorderFactory.createLineBorder(UIManager.getColor("TextArea.background"), 16));
        slideshow.addSlide(slidesRunning, "Running your pipeline");

        slideshow.showSlide("Welcome to JIPipe");

        tourContentPanel.add(slideshow, BorderLayout.CENTER);
    }

    private void initRecentProjectsAndTemplates() {
        DocumentTabPane tabPane = new DocumentTabPane();

        // Recent projects list
        initRecentProjects(tabPane);

        // Template list
        initTemplateList(tabPane);

        tabPane.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, Color.DARK_GRAY));

        if (!GeneralUISettings.getInstance().isShowIntroductionTour())
            add(tabPane, BorderLayout.CENTER);
        else
            add(tabPane, BorderLayout.WEST);
    }

    private void initTemplateList(DocumentTabPane tabPane) {
        JPanel panel = new JPanel(new BorderLayout());

        templateList.setCellRenderer(new TemplateProjectListCellRenderer());
        JScrollPane templateListScrollPane = new JScrollPane(templateList);
        templateList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    JIPipeProjectTemplate value = templateList.getSelectedValue();
                    if (value != null) {
                        ((JIPipeProjectWindow) getProjectWorkbench().getWindow()).newProjectFromTemplate(value);
                    }
                } else {
                    if (templateList.getMousePosition().x > templateList.getWidth() - 50) {
                        JIPipeProjectTemplate value = templateList.getSelectedValue();
                        if (value != null) {
                            ((JIPipeProjectWindow) getProjectWorkbench().getWindow()).newProjectFromTemplate(value);
                        }
                    }
                }
            }
        });

        // Init search
        templateSearch.addActionListener(e -> refreshTemplateProjects());

        panel.add(templateListScrollPane, BorderLayout.CENTER);
        panel.add(templateSearch, BorderLayout.NORTH);

        tabPane.addTab("Example projects & templates",
                UIUtils.getIconFromResources("actions/graduation-cap.png"),
                panel,
                DocumentTabPane.CloseMode.withoutCloseButton);
    }

    private void initRecentProjects(DocumentTabPane tabPane) {
        tabPane.addTab("Recent projects",
                UIUtils.getIconFromResources("actions/view-calendar-time-spent.png"),
                new RecentProjectsListPanel(getProjectWorkbench()),
                DocumentTabPane.CloseMode.withoutCloseButton);
    }

    private void initializeHeaderPanel() {
        JPanel headerPanel = new BackgroundPanel(UIUtils.getHeaderPanelBackground(), false);
        headerPanel.setLayout(new BorderLayout());
        headerPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.DARK_GRAY));
        headerPanel.setPreferredSize(new Dimension(headerPanel.getPreferredSize().width, 200));
        JLabel logo = new JLabel(new ImageIcon(UIUtils.getLogo400()));
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
        technicalInfo.addToForm(UIUtils.makeReadonlyBorderlessTextField(JIPipe.getNodes().getRegisteredNodeInfos().size() + " algorithms"), new JLabel("Registered algorithms"), null);
        technicalInfo.addToForm(UIUtils.makeReadonlyBorderlessTextField(JIPipe.getDataTypes().getRegisteredDataTypes().size() + " types"), new JLabel("Registered data types"), null);
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

        JButton openTutorialsButton = new JButton("Tutorials", UIUtils.getIconFromResources("actions/graduation-cap.png"));
        openTutorialsButton.setToolTipText("https://www.jipipe.org/tutorials");
        openTutorialsButton.addActionListener(e -> UIUtils.openWebsite("https://www.jipipe.org/tutorials"));
        openTutorialsButton.setOpaque(false);
        openTutorialsButton.setBackground(new Color(0, 0, 0, 0));
        toolBar.add(openTutorialsButton);
        toolBar.add(Box.createHorizontalStrut(4));

        JButton openExamplesButton = new JButton("Examples", UIUtils.getIconFromResources("actions/flask.png"));
        openExamplesButton.setToolTipText("https://www.jipipe.org/examples");
        openExamplesButton.addActionListener(e -> UIUtils.openWebsite("https://www.jipipe.org/examples"));
        openExamplesButton.setOpaque(false);
        openExamplesButton.setBackground(new Color(0, 0, 0, 0));
        toolBar.add(openExamplesButton);
        toolBar.add(Box.createHorizontalStrut(4));

        JButton openDocumentationButton = new JButton("Documentation", UIUtils.getIconFromResources("actions/help-info.png"));
        openDocumentationButton.setToolTipText("https://www.jipipe.org/documentation");
        openDocumentationButton.addActionListener(e -> UIUtils.openWebsite("https://www.jipipe.org/documentation"));
        openDocumentationButton.setOpaque(false);
        openDocumentationButton.setBackground(new Color(0, 0, 0, 0));
        toolBar.add(openDocumentationButton);
        toolBar.add(Box.createHorizontalStrut(4));

        JButton openCommunityButton = new JButton("Community", UIUtils.getIconFromResources("actions/im-irc.png"));
        openCommunityButton.setToolTipText("https://forum.image.sc/tag/jipipe");
        openCommunityButton.addActionListener(e -> UIUtils.openWebsite("https://forum.image.sc/tag/jipipe"));
        openCommunityButton.setOpaque(false);
        openCommunityButton.setBackground(new Color(0, 0, 0, 0));
        toolBar.add(openCommunityButton);
        toolBar.add(Box.createHorizontalStrut(4));

        JButton openSourceCodeButton = new JButton("Source code", UIUtils.getIconFromResources("actions/dialog-xml-editor.png"));
        openSourceCodeButton.setToolTipText("https://github.com/applied-systems-biology/jipipe/");
        openSourceCodeButton.addActionListener(e -> UIUtils.openWebsite("https://github.com/applied-systems-biology/jipipe/"));
        openSourceCodeButton.setOpaque(false);
        openSourceCodeButton.setBackground(new Color(0, 0, 0, 0));
        toolBar.add(openSourceCodeButton);
        toolBar.add(Box.createHorizontalStrut(4));

        JButton reportIssueButton = new JButton("Report issue", UIUtils.getIconFromResources("actions/bug.png"));
        reportIssueButton.setToolTipText("https://github.com/applied-systems-biology/jipipe/issues");
        reportIssueButton.addActionListener(e -> UIUtils.openWebsite("https://github.com/applied-systems-biology/jipipe/issues"));
        reportIssueButton.setOpaque(false);
        reportIssueButton.setBackground(new Color(0, 0, 0, 0));
        toolBar.add(reportIssueButton);
        toolBar.add(Box.createHorizontalStrut(4));

        topPanel.add(toolBar, BorderLayout.SOUTH);
    }

}
