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

package org.hkijena.acaq5.ui;

import com.google.common.eventbus.Subscribe;
import com.google.common.html.HtmlEscapers;
import ij.IJ;
import ij.ImagePlus;
import ij.plugin.filter.GaussianBlur;
import org.hkijena.acaq5.api.ACAQAuthorMetadata;
import org.hkijena.acaq5.api.events.ParameterChangedEvent;
import org.hkijena.acaq5.api.parameters.ACAQParameterTree;
import org.hkijena.acaq5.extensions.parameters.table.ParameterTable;
import org.hkijena.acaq5.extensions.settings.ProjectsSettings;
import org.hkijena.acaq5.ui.components.FormPanel;
import org.hkijena.acaq5.ui.components.MarkdownDocument;
import org.hkijena.acaq5.ui.components.MarkdownReader;
import org.hkijena.acaq5.ui.grapheditor.ACAQAlgorithmGraphCanvasUI;
import org.hkijena.acaq5.ui.parameters.ParameterPanel;
import org.hkijena.acaq5.ui.settings.ACAQProjectInfoParameters;
import org.hkijena.acaq5.utils.ReflectionUtils;
import org.hkijena.acaq5.utils.ResourceUtils;
import org.hkijena.acaq5.utils.StringUtils;
import org.hkijena.acaq5.utils.UIUtils;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.Attributes;

/**
 * UI that gives an overview of a pipeline (shows parameters, etc.)
 */
public class ACAQProjectInfoUI extends ACAQProjectWorkbenchPanel {

    private final MarkdownReader descriptionReader;
    private final ParameterPanel parameterPanel;
    private JTextField licenseInfo;
    private JTextField projectName;
    private JTextField projectStats;
    private JTextPane projectAuthors;
    private BufferedImage headerBackground;
    private JButton openWebsiteButton;
    private JButton copyCitationButton;
    private JButton copyDependencyCitationsButton;

    /**
     * Creates a new instance
     *
     * @param workbenchUI The workbench UI
     */
    public ACAQProjectInfoUI(ACAQProjectWorkbench workbenchUI) {
        super(workbenchUI);
        descriptionReader = new MarkdownReader(false);
        parameterPanel = new ParameterPanel(getWorkbench(),
                getPipelineParameters(),
                MarkdownDocument.fromPluginResource("documentation/project-info-parameters.md"),
                ParameterPanel.WITH_SEARCH_BAR | ParameterPanel.WITH_SCROLLING
                        | ParameterPanel.NO_EMPTY_GROUP_HEADERS | ParameterPanel.WITH_DOCUMENTATION |
                        ParameterPanel.DOCUMENTATION_BELOW);
        initialize();
        refreshAll();
        getProject().getMetadata().getEventBus().register(this);

    }

    private void refreshAll() {
        renderBackgroundPanel();
        refreshDescription();
        refreshHeaderText();
        refreshTechnicalInfo();
        refreshHeaderButtons();
    }

    private void refreshHeaderText() {
        projectName.setText(StringUtils.orElse(getProject().getMetadata().getName(), "Unnamed project"));
        StringBuilder authors = new StringBuilder();
        StringBuilder affiliations = new StringBuilder();
        authors.append("<html>");
        affiliations.append("<html>");
        for (ACAQAuthorMetadata author : getProject().getMetadata().getAuthors()) {
            authors.append(HtmlEscapers.htmlEscaper().escape(author.getFirstName()));
            authors.append(" ");
            authors.append(HtmlEscapers.htmlEscaper().escape(author.getLastName()));
            authors.append("<br/>");

            affiliations.append("<u>");
            affiliations.append(HtmlEscapers.htmlEscaper().escape(author.getFirstName()));
            affiliations.append(" ");
            affiliations.append(HtmlEscapers.htmlEscaper().escape(author.getLastName()));
            affiliations.append("</u>");
            affiliations.append("<br/>");
            affiliations.append(HtmlEscapers.htmlEscaper().escape(author.getAffiliations()).replace("\n", "<br/>"));
            affiliations.append("<br/>");
            affiliations.append("<br/>");
        }
        affiliations.append("</html>");
        authors.append("</html>");
        projectAuthors.setText(authors.toString());
        projectAuthors.setToolTipText(affiliations.toString());
    }

    private void refreshHeaderButtons() {
        if(!StringUtils.isNullOrEmpty(getProject().getMetadata().getWebsite())) {
            openWebsiteButton.setToolTipText(getProject().getMetadata().getWebsite());
            openWebsiteButton.setEnabled(true);
        }
        else {
            openWebsiteButton.setToolTipText("No website provided");
            openWebsiteButton.setEnabled(false);
        }
        if(!StringUtils.isNullOrEmpty(getProject().getMetadata().getCitation())) {
            copyCitationButton.setToolTipText(getProject().getMetadata().getCitation());
            copyCitationButton.setEnabled(true);
        }
        else {
            copyCitationButton.setToolTipText("No citation provided");
            copyCitationButton.setEnabled(false);
        }
        if(getProject().getMetadata().getDependencyCitations().isEmpty()) {
            copyDependencyCitationsButton.setToolTipText("No cited sources provided");
            copyDependencyCitationsButton.setEnabled(false);
        }
        else {
            StringBuilder stringBuilder = new StringBuilder();
            for (String dependencyCitation : getProject().getMetadata().getDependencyCitations()) {
                stringBuilder.append(dependencyCitation).append("\n\n");
            }
            copyDependencyCitationsButton.setToolTipText(stringBuilder.toString());
            copyDependencyCitationsButton.setEnabled(true);
        }
    }

    private void refreshTechnicalInfo() {
        licenseInfo.setText(StringUtils.orElse(getProject().getMetadata().getLicense(), "No license"));
        projectStats.setText(getProject().getGraph().getAlgorithmCount() + " nodes in " + getProject().getCompartments().size() + " compartments");
    }

    private void renderBackgroundPanel() {
        ACAQAlgorithmGraphCanvasUI canvasUI = new ACAQAlgorithmGraphCanvasUI(getWorkbench(),getProject().getGraph(), null);
        canvasUI.setCurrentViewMode(ACAQAlgorithmGraphCanvasUI.ViewMode.Horizontal);
        canvasUI.autoLayoutAll();
        try {
            BufferedImage screenshot = canvasUI.createScreenshotPNG();
            ImagePlus img = new ImagePlus("screenshot", screenshot);
            GaussianBlur blur = new GaussianBlur();
            blur.blurGaussian(img.getProcessor(), 8);
            headerBackground = img.getBufferedImage();
        }
        catch (Exception e) {
        }
        if(headerBackground == null) {
            try {
                headerBackground = ImageIO.read(ResourceUtils.getPluginResource("infoui-background.png"));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        repaint();
    }

    private void refreshDescription() {
        descriptionReader.setDocument(new MarkdownDocument(getProject().getMetadata().getDescription()));
    }

    private void initialize() {
        setLayout(new BorderLayout());

        initializeHeaderPanel();

        descriptionReader.getScrollPane().setBorder(null);
        parameterPanel.getScrollPane().setBorder(null);
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, descriptionReader, parameterPanel);
        splitPane.setBorder(null);
        splitPane.setDividerSize(3);
        splitPane.setResizeWeight(0.66);
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                super.componentResized(e);
                splitPane.setDividerLocation(0.66);
            }
        });
        add(splitPane, BorderLayout.CENTER);
    }

    @Subscribe
    public void onRecentProjectsChanged(ParameterChangedEvent event) {
        if ("description".equals(event.getKey())) {
            refreshDescription();
        }
        else if("name".equals(event.getKey()) || "authors".equals(event.getKey())) {
            refreshHeaderText();
        }
        else {
            refreshTechnicalInfo();
            refreshHeaderButtons();
        }
    }

    private void initializeHeaderPanel() {
        JPanel headerPanel = new BackgroundPanel();
        headerPanel.setLayout(new BorderLayout());
        headerPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.DARK_GRAY));
        headerPanel.setPreferredSize(new Dimension(headerPanel.getPreferredSize().width, 200));

        FormPanel nameAndAuthorPanel = new FormPanel(null, FormPanel.NONE);
        nameAndAuthorPanel.setOpaque(false);
        nameAndAuthorPanel.getContentPanel().setOpaque(false);
        nameAndAuthorPanel.setLayout(new BoxLayout(nameAndAuthorPanel, BoxLayout.Y_AXIS));

        projectName = UIUtils.makeReadonlyBorderlessTextField("Unnamed project");
        projectName.setOpaque(false);
        projectName.setFont(new Font(Font.DIALOG, Font.PLAIN, 40));
        projectName.setBorder(BorderFactory.createEmptyBorder(8,8,8,8));
        nameAndAuthorPanel.addWideToForm(projectName, null);

        projectAuthors = UIUtils.makeBorderlessReadonlyTextPane("");
        projectAuthors.setOpaque(false);
        projectAuthors.setBorder(BorderFactory.createEmptyBorder(8,8,8,8));
        nameAndAuthorPanel.addWideToForm(projectAuthors, null);

        nameAndAuthorPanel.addVerticalGlue();
        headerPanel.add(nameAndAuthorPanel, BorderLayout.WEST);

        FormPanel technicalInfo = new FormPanel(null, FormPanel.NONE);
        technicalInfo.setOpaque(false);
        technicalInfo.getContentPanel().setOpaque(false);

        licenseInfo = UIUtils.makeReadonlyBorderlessTextField("No license");
        technicalInfo.addToForm(licenseInfo, new JLabel("Licensed under"), null);
        projectStats = UIUtils.makeReadonlyBorderlessTextField("No information");
        technicalInfo.addToForm(projectStats, new JLabel("Project statistics"), null);
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

        openWebsiteButton = new JButton("Visit website", UIUtils.getIconFromResources("filetype-html.png"));
        openWebsiteButton.addActionListener(e -> UIUtils.openWebsite(getProject().getMetadata().getWebsite()));
        openWebsiteButton.setOpaque(false);
        openWebsiteButton.setBackground(new Color(0, 0, 0, 0));
        toolBar.add(openWebsiteButton);
        toolBar.add(Box.createHorizontalStrut(4));

        copyCitationButton = new JButton("Copy citation", UIUtils.getIconFromResources("copy.png"));
        copyCitationButton.addActionListener(e ->{
            StringSelection selection = new StringSelection(getProject().getMetadata().getCitation());
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(selection, selection);
        });
        copyCitationButton.setOpaque(false);
        copyCitationButton.setBackground(new Color(0, 0, 0, 0));
        toolBar.add(copyCitationButton);
        toolBar.add(Box.createHorizontalStrut(4));

        copyDependencyCitationsButton = new JButton("Copy cited sources", UIUtils.getIconFromResources("copy.png"));
        copyDependencyCitationsButton.addActionListener(e ->{
            StringBuilder stringBuilder = new StringBuilder();
            for (String dependencyCitation : getProject().getMetadata().getDependencyCitations()) {
                stringBuilder.append(dependencyCitation).append("\n\n");
            }
            StringSelection selection = new StringSelection(stringBuilder.toString());
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(selection, selection);
        });
        copyDependencyCitationsButton.setOpaque(false);
        copyDependencyCitationsButton.setBackground(new Color(0, 0, 0, 0));
        toolBar.add(copyDependencyCitationsButton);
        toolBar.add(Box.createHorizontalStrut(4));

        toolBar.add(Box.createHorizontalGlue());

        JButton refreshButton = new JButton("Refresh", UIUtils.getIconFromResources("refresh.png"));
        refreshButton.setOpaque(false);
        refreshButton.setBackground(new Color(0, 0, 0, 0));
        refreshButton.setToolTipText("Updates the contents of this page.");
        refreshButton.addActionListener(e -> refreshAll());
        toolBar.add(refreshButton);

        JButton openSettingsButton = new JButton("Edit", UIUtils.getIconFromResources("edit.png"));
        openSettingsButton.setOpaque(false);
        openSettingsButton.setBackground(new Color(0, 0, 0, 0));
        openSettingsButton.setToolTipText("Opens the project settings that allow you to change this page.");
        openSettingsButton.addActionListener(e -> getProjectWorkbench().getDocumentTabPane().selectSingletonTab(ACAQProjectWorkbench.TAB_PROJECT_SETTINGS));
        toolBar.add(openSettingsButton);

        topPanel.add(toolBar, BorderLayout.SOUTH);
    }

    private ACAQProjectInfoParameters getPipelineParameters() {
        Object existing = getProject().getAdditionalMetadata().getOrDefault(ACAQProjectInfoParameters.METADATA_KEY, null);
        ACAQProjectInfoParameters result;
        if(existing instanceof ACAQProjectInfoParameters) {
            result = (ACAQProjectInfoParameters) existing;
        }
        else {
            result = new ACAQProjectInfoParameters();
            getProject().getAdditionalMetadata().put(ACAQProjectInfoParameters.METADATA_KEY, result);
        }
        result.setProject(getProject());
        return result;
    }

    private class BackgroundPanel extends JPanel {

        public BackgroundPanel() {
            setOpaque(false);
        }

        @Override
        public void paint(Graphics g) {
            double factor = 1.0 * getHeight() / headerBackground.getHeight();
            g.setColor(Color.WHITE);
            g.fillRect(0,0,getWidth(), getHeight());
            g.drawImage(headerBackground, 0, 0, (int) (headerBackground.getWidth() * factor), getHeight(), null);
            super.paint(g);
        }
    }

}
