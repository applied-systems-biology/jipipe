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
import org.hkijena.jipipe.api.registries.JIPipeProjectTemplateRegistry;
import org.hkijena.jipipe.ui.JIPipeProjectWindow;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbench;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbenchPanel;
import org.hkijena.jipipe.ui.components.FormPanel;
import org.hkijena.jipipe.ui.components.ImageFrame;
import org.hkijena.jipipe.ui.components.renderers.TemplateProjectListCellRenderer;
import org.hkijena.jipipe.ui.components.search.SearchTextField;
import org.hkijena.jipipe.ui.components.tabs.DocumentTabPane;
import org.hkijena.jipipe.ui.project.templatedownloader.ProjectTemplateDownloaderRun;
import org.hkijena.jipipe.ui.running.JIPipeRunExecuterUI;
import org.hkijena.jipipe.utils.*;

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
        }
        catch (Throwable e) {
            backgroundImage = null;
        }
        JPanel heroPanel = new ImageFrame(backgroundImage, false, ImageFrame.Mode.Cover, true);
        heroPanel.setLayout(new BoxLayout(heroPanel, BoxLayout.Y_AXIS));

        heroPanel.add(Box.createVerticalGlue());
        initializeHeroLogo(heroPanel);
        heroPanel.add(Box.createVerticalGlue());
        initializeHeroTechnicalInfo(heroPanel);

        splitPane.setRightComponent(heroPanel);
    }

    private void initializeHeroTechnicalInfo(JPanel heroPanel) {
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
        technicalInfo.addToForm(UIUtils.makeReadonlyBorderlessTextField(JIPipe.getNodes().getRegisteredNodeInfos().size() + " algorithms"), new JLabel("Registered node types"), null);
        technicalInfo.addToForm(UIUtils.makeReadonlyBorderlessTextField(JIPipe.getDataTypes().getRegisteredDataTypes().size() + " types"), new JLabel("Registered data types"), null);
        technicalInfo.addVerticalGlue();

        JPanel technicalInfoPanel = new JPanel(new BorderLayout());
        technicalInfoPanel.setOpaque(false);
        technicalInfoPanel.add(technicalInfo, BorderLayout.EAST);
        technicalInfoPanel.setMaximumSize(new Dimension(Short.MAX_VALUE, 200));

        technicalInfoPanel.setBorder(BorderFactory.createMatteBorder(1,1,1,1,Color.RED));

        heroPanel.add(technicalInfoPanel);
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
