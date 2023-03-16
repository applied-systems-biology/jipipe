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
import org.hkijena.jipipe.api.JIPipeProject;
import org.hkijena.jipipe.api.JIPipeProjectTemplate;
import org.hkijena.jipipe.api.compartments.algorithms.JIPipeProjectCompartment;
import org.hkijena.jipipe.api.registries.JIPipeProjectTemplateRegistry;
import org.hkijena.jipipe.ui.JIPipeProjectWindow;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbench;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbenchPanel;
import org.hkijena.jipipe.ui.components.FormPanel;
import org.hkijena.jipipe.ui.components.ImageFrame;
import org.hkijena.jipipe.ui.components.RoundedButtonUI;
import org.hkijena.jipipe.ui.components.renderers.TemplateProjectListCellRenderer;
import org.hkijena.jipipe.ui.components.search.SearchTextField;
import org.hkijena.jipipe.ui.components.tabs.DocumentTabPane;
import org.hkijena.jipipe.ui.grapheditor.algorithmpipeline.JIPipePipelineGraphEditorUI;
import org.hkijena.jipipe.ui.project.templatedownloader.ProjectTemplateDownloaderRun;
import org.hkijena.jipipe.ui.running.JIPipeRunExecuterUI;
import org.hkijena.jipipe.utils.*;
import org.hkijena.jipipe.utils.ui.RoundedLineBorder;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
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
        JIPipe.getInstance().getEventBus().register(this);
    }

    @Subscribe
    public void onTemplatesUpdated(JIPipeProjectTemplateRegistry.TemplatesUpdatedEvent event) {
        refreshTemplateProjects();
    }

    private void refreshTemplateProjects() {
        DefaultListModel<JIPipeProjectTemplate> model = new DefaultListModel<>();
        for (JIPipeProjectTemplate template : JIPipe.getInstance().getProjectTemplateRegistry().getSortedRegisteredTemplates()) {
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
        AutoResizeSplitPane splitPane = new AutoResizeSplitPane(AutoResizeSplitPane.LEFT_RIGHT, AutoResizeSplitPane.RATIO_1_TO_3);

        initializeRecentProjectsAndTemplates(splitPane);
        initializeHero(splitPane);

        add(splitPane, BorderLayout.CENTER);
    }

    private void initializeHero(AutoResizeSplitPane splitPane) {
        BufferedImage backgroundImage;
        try {
            if (UIUtils.DARK_THEME) {
                backgroundImage = ImageIO.read(ResourceUtils.getPluginResource("welcome-hero-dark.png"));
            } else {
                backgroundImage = ImageIO.read(ResourceUtils.getPluginResource("welcome-hero.png"));
            }
        } catch (Throwable e) {
            backgroundImage = null;
        }
        JPanel heroPanel = new ImageFrame(backgroundImage, false, ImageFrame.Mode.Cover, true);
        heroPanel.setLayout(new BoxLayout(heroPanel, BoxLayout.Y_AXIS));

        heroPanel.add(Box.createVerticalGlue());
        initializeHeroLogo(heroPanel);
        initializeHeroActions(heroPanel);
        heroPanel.add(Box.createVerticalStrut(16));
        initializeHeroSecondaryActions(heroPanel);
        heroPanel.add(Box.createVerticalGlue());
        initializeHeroBottomPanel(heroPanel);

        splitPane.setRightComponent(heroPanel);
    }

    private void initializeHeroSecondaryActions(JPanel heroPanel) {
        JPanel actionPanel = new JPanel();
        actionPanel.setLayout(new BoxLayout(actionPanel, BoxLayout.X_AXIS));
        actionPanel.setOpaque(false);

        actionPanel.add(Box.createHorizontalGlue());

        JTextPane textPane = UIUtils.makeBorderlessReadonlyTextPane("<html>... or <a href=\"https://www.jipipe.org/tutorials/\">learn</a> how to use JIPipe (online tutorials)" +
                "</html>", false);
        textPane.setMaximumSize(new Dimension(300, 40));
        actionPanel.add(textPane);

        actionPanel.add(Box.createHorizontalGlue());

        actionPanel.setMaximumSize(new Dimension(Short.MAX_VALUE, 32));
        heroPanel.add(actionPanel);
    }

    private void initializeHeroActions(JPanel heroPanel) {
        JPanel actionPanel = new JPanel();
        actionPanel.setLayout(new BoxLayout(actionPanel, BoxLayout.X_AXIS));
        actionPanel.setOpaque(false);

        actionPanel.add(Box.createHorizontalGlue());

        Color colorSuccess = new Color(0x5CB85C);
        Color colorHover = new Color(0x4f9f4f);

        JButton startNowButton = new JButton("Start building");
        startNowButton.setBackground(colorSuccess);
        startNowButton.setForeground(Color.WHITE);
        startNowButton.setUI(new RoundedButtonUI(8, colorHover, colorHover));
        startNowButton.setFont(new Font(Font.DIALOG, Font.PLAIN, 28));
        startNowButton.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4), BorderFactory.createEmptyBorder(16, 16, 16, 16)));
        startNowButton.addActionListener(e -> doActionStartNow());
        actionPanel.add(startNowButton);

        actionPanel.add(Box.createHorizontalStrut(8));

        JButton openButton = new JButton("Open a project");
        openButton.setOpaque(false);
        openButton.setFont(new Font(Font.DIALOG, Font.PLAIN, 28));
        openButton.setBorder(BorderFactory.createCompoundBorder(new RoundedLineBorder(new Color(0xabb8c3), 1, 8), BorderFactory.createEmptyBorder(12, 12, 12, 12)));
        openButton.addActionListener(e -> doActionOpenProject());
        actionPanel.add(openButton);

        actionPanel.setMaximumSize(new Dimension(Short.MAX_VALUE, 120));

        actionPanel.add(Box.createHorizontalGlue());
        heroPanel.add(actionPanel);
    }

    private void doActionOpenProject() {
        getProjectWorkbench().getProjectWindow().openProject();
    }

    private void doActionStartNow() {
        DocumentTabPane documentTabPane = getProjectWorkbench().getDocumentTabPane();
        documentTabPane.closeTab(documentTabPane.getSingletonTabInstances().get(JIPipeProjectWorkbench.TAB_INTRODUCTION));

        // Search for a compartment tab
        for (DocumentTabPane.DocumentTab tab : documentTabPane.getTabs()) {
            if (tab.getContent() instanceof JIPipePipelineGraphEditorUI) {
                documentTabPane.switchToTab(tab);
                return;
            }
        }

        // No compartment found! Open a new one
        JIPipeProject project = getProjectWorkbench().getProject();
        JIPipeProjectCompartment compartment;
        if (project.getCompartments().isEmpty()) {
            // Create a new one
            compartment = project.addCompartment("Analysis");
        } else {
            compartment = project.getCompartments().values().iterator().next();
        }
        getProjectWorkbench().getOrOpenPipelineEditorTab(compartment, true);
    }

    private void initializeHeroBottomPanel(JPanel heroPanel) {

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setOpaque(false);
        bottomPanel.setMaximumSize(new Dimension(Short.MAX_VALUE, 200));

        initializeHeroLinksPanel(bottomPanel);
        initializeHeroTechnicalInfoPanel(bottomPanel);

        heroPanel.add(bottomPanel);
    }

    private void initializeHeroLinksPanel(JPanel bottomPanel) {

        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        toolBar.setOpaque(false);

        toolBar.add(Box.createHorizontalStrut(8));

        JButton openWebsiteButton = new JButton("Visit our website", UIUtils.getIconFromResources("actions/web-browser.png"));
        openWebsiteButton.setAlignmentY(JComponent.BOTTOM_ALIGNMENT);
        openWebsiteButton.setToolTipText("https://www.jipipe.org/");
        openWebsiteButton.addActionListener(e -> UIUtils.openWebsite("https://www.jipipe.org/"));
        openWebsiteButton.setOpaque(false);
        openWebsiteButton.setBackground(new Color(0, 0, 0, 0));
        toolBar.add(openWebsiteButton);
        toolBar.add(Box.createHorizontalStrut(4));

        JButton openCommunityButton = new JButton("Community", UIUtils.getIconFromResources("actions/im-irc.png"));
        openCommunityButton.setAlignmentY(JComponent.BOTTOM_ALIGNMENT);
        openCommunityButton.setToolTipText("https://forum.image.sc/tag/jipipe");
        openCommunityButton.addActionListener(e -> UIUtils.openWebsite("https://forum.image.sc/tag/jipipe"));
        openCommunityButton.setOpaque(false);
        openCommunityButton.setBackground(new Color(0, 0, 0, 0));
        toolBar.add(openCommunityButton);
        toolBar.add(Box.createHorizontalStrut(4));

        JButton openSourceCodeButton = new JButton("Source code", UIUtils.getIconFromResources("actions/dialog-xml-editor.png"));
        openSourceCodeButton.setAlignmentY(JComponent.BOTTOM_ALIGNMENT);
        openSourceCodeButton.setToolTipText("https://github.com/applied-systems-biology/jipipe/");
        openSourceCodeButton.addActionListener(e -> UIUtils.openWebsite("https://github.com/applied-systems-biology/jipipe/"));
        openSourceCodeButton.setOpaque(false);
        openSourceCodeButton.setBackground(new Color(0, 0, 0, 0));
        toolBar.add(openSourceCodeButton);
        toolBar.add(Box.createHorizontalStrut(4));

        JButton reportIssueButton = new JButton("Report issue", UIUtils.getIconFromResources("actions/bug.png"));
        reportIssueButton.setAlignmentY(JComponent.BOTTOM_ALIGNMENT);
        reportIssueButton.setToolTipText("https://github.com/applied-systems-biology/jipipe/issues");
        reportIssueButton.addActionListener(e -> UIUtils.openWebsite("https://github.com/applied-systems-biology/jipipe/issues"));
        reportIssueButton.setOpaque(false);
        reportIssueButton.setBackground(new Color(0, 0, 0, 0));
        toolBar.add(reportIssueButton);
        toolBar.add(Box.createHorizontalStrut(4));

        bottomPanel.add(toolBar, BorderLayout.WEST);

    }

    private void initializeHeroTechnicalInfoPanel(JPanel bottomPanel) {

        FormPanel technicalInfo = new FormPanel(null, FormPanel.NONE);
        technicalInfo.setOpaque(false);
        technicalInfo.getContentPanel().setOpaque(false);

        technicalInfo.addVerticalGlue();

        technicalInfo.addToForm(UIUtils.makeReadonlyBorderlessTextField(StringUtils.orElse(getClass().getPackage().getImplementationVersion(), "Development")), new JLabel("Version"), null);
        Attributes manifestAttributes = ReflectionUtils.getManifestAttributes();
        if (manifestAttributes != null) {
            String implementationDateString = manifestAttributes.getValue("Implementation-Date");
            technicalInfo.addToForm(UIUtils.makeReadonlyBorderlessTextField(StringUtils.orElse(implementationDateString, "N/A")), new JLabel("Build time"), null);
        }
        technicalInfo.addToForm(UIUtils.makeReadonlyBorderlessTextField(StringUtils.orElse(IJ.getVersion(), "N/A")), new JLabel("ImageJ"), null);
        technicalInfo.addToForm(UIUtils.makeReadonlyBorderlessTextField(StringUtils.orElse(System.getProperty("java.version"), "N/A")), new JLabel("Java"), null);
        technicalInfo.addToForm(UIUtils.makeReadonlyBorderlessTextField(JIPipe.getNodes().getRegisteredNodeInfos().size() + " algorithms"), new JLabel("Registered node types"), null);
        technicalInfo.addToForm(UIUtils.makeReadonlyBorderlessTextField(JIPipe.getDataTypes().getRegisteredDataTypes().size() + " types"), new JLabel("Registered data types"), null);

        technicalInfo.setMaximumSize(new Dimension(300, 200));

        bottomPanel.add(technicalInfo, BorderLayout.EAST);
    }

    private void initializeHeroLogo(JPanel heroPanel) {
        ImageFrame logoPanel = new ImageFrame(UIUtils.getLogo(), false, ImageFrame.Mode.Fit, true);
        logoPanel.setScaleFactor(0.7);
        logoPanel.setOpaque(false);
        heroPanel.add(logoPanel);
    }

    private void initializeRecentProjectsAndTemplates(AutoResizeSplitPane splitPane) {
        DocumentTabPane tabPane = new DocumentTabPane(true);

        // Recent projects list
        initRecentProjects(tabPane);

        // Template list
        initTemplateList(tabPane);

        splitPane.setLeftComponent(tabPane);
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

        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        toolBar.add(templateSearch);
        JButton downloadTemplatesButton = new JButton("Get more templates", UIUtils.getIconFromResources("actions/browser-download.png"));
        downloadTemplatesButton.addActionListener(e -> downloadTemplates());
        toolBar.add(downloadTemplatesButton);

        panel.add(templateListScrollPane, BorderLayout.CENTER);
        panel.add(toolBar, BorderLayout.NORTH);

        tabPane.addTab("Example projects & templates",
                UIUtils.getIconFromResources("actions/graduation-cap.png"),
                panel,
                DocumentTabPane.CloseMode.withoutCloseButton);
    }

    private void downloadTemplates() {
        JIPipeRunExecuterUI.runInDialog(getWorkbench().getWindow(), new ProjectTemplateDownloaderRun(getWorkbench()));
    }

    private void initRecentProjects(DocumentTabPane tabPane) {
        tabPane.addTab("Recent projects",
                UIUtils.getIconFromResources("actions/view-calendar-time-spent.png"),
                new RecentProjectsListPanel(getProjectWorkbench()),
                DocumentTabPane.CloseMode.withoutCloseButton);
    }

}
