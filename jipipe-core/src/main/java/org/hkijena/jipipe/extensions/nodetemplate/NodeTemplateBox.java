/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.extensions.nodetemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableList;
import com.google.common.primitives.Ints;
import ij.IJ;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeNodeTemplate;
import org.hkijena.jipipe.api.JIPipeProject;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.extensions.nodetemplate.templatedownloader.NodeTemplateDownloaderRun;
import org.hkijena.jipipe.extensions.settings.FileChooserSettings;
import org.hkijena.jipipe.extensions.settings.NodeTemplateSettings;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbench;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.JIPipeWorkbenchPanel;
import org.hkijena.jipipe.ui.components.markdown.MarkdownDocument;
import org.hkijena.jipipe.ui.components.markdown.MarkdownReader;
import org.hkijena.jipipe.ui.components.search.SearchTextField;
import org.hkijena.jipipe.ui.components.window.AlwaysOnTopToggle;
import org.hkijena.jipipe.ui.grapheditor.general.JIPipeGraphCanvasUI;
import org.hkijena.jipipe.ui.parameters.ParameterPanel;
import org.hkijena.jipipe.ui.running.JIPipeRunExecuterUI;
import org.hkijena.jipipe.utils.AutoResizeSplitPane;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.TooltipUtils;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.json.JsonUtils;
import org.hkijena.jipipe.utils.search.RankedData;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class NodeTemplateBox extends JIPipeWorkbenchPanel implements NodeTemplatesRefreshedEventListener {

    private final JIPipeProject project;
    private final Set<JIPipeNodeTemplate> projectTemplateList = new HashSet<>();
    private final MarkdownReader documentationReader = new MarkdownReader(false);
    private final JToolBar toolBar = new JToolBar();
    private final boolean isDocked;
    private final JIPipeGraphCanvasUI canvasUI;
    private final Set<JIPipeGraphNode> nodesToAdd;
    private final JPopupMenu manageMenu = new JPopupMenu();
    private JList<JIPipeNodeTemplate> templateList;
    private SearchTextField searchField;

    public NodeTemplateBox(JIPipeWorkbench workbench, boolean isDocked, JIPipeGraphCanvasUI canvasUI, Set<JIPipeGraphNode> nodesToAdd) {
        super(workbench);
        this.isDocked = isDocked;
        this.canvasUI = canvasUI;
        this.nodesToAdd = nodesToAdd;
        if (workbench instanceof JIPipeProjectWorkbench) {
            this.project = ((JIPipeProjectWorkbench) workbench).getProject();
        } else {
            this.project = null;
        }
        initialize();
        reloadTemplateList();
        JIPipe.getInstance().getNodeTemplatesRefreshedEventEmitter().subscribeWeak(this);
    }

    public static void openNewToolBoxWindow(JIPipeWorkbench workbench) {
        NodeTemplateBox toolBox = new NodeTemplateBox(workbench, false, null, null);
        JFrame window = new JFrame();
        toolBox.getToolBar().add(new AlwaysOnTopToggle(window));
        window.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        window.setAlwaysOnTop(true);
        window.setTitle("Node templates");
        window.setIconImage(UIUtils.getJIPipeIcon128());
        window.setContentPane(toolBox);
        window.pack();
        window.setSize(300, 700);
        window.setLocationRelativeTo(workbench.getWindow());
        window.setVisible(true);
    }

    private static int[] rankNavigationEntry(JIPipeNodeTemplate template, String[] searchStrings) {
        if (searchStrings == null || searchStrings.length == 0)
            return new int[0];
        String nameHayStack;
        String name2HayStack;
        String descriptionHayStack;
        nameHayStack = StringUtils.orElse(template.getName(), "").toLowerCase();
//        name2HayStack = StringUtils.orElse(template.getNodeInfo().getName(), "").toLowerCase();
        descriptionHayStack = StringUtils.orElse(template.getDescription().getBody(), "").toLowerCase();

        nameHayStack = nameHayStack.toLowerCase();
//        name2HayStack = name2HayStack.toLowerCase();
        descriptionHayStack = descriptionHayStack.toLowerCase();

        int[] ranks = new int[3];

        for (int i = 0; i < searchStrings.length; i++) {
            String string = searchStrings[i];
            if (nameHayStack.contains(string.toLowerCase()))
                --ranks[0];
            if (i == 0 && nameHayStack.startsWith(string.toLowerCase()))
                ranks[0] -= 2;
//            if (name2HayStack.contains(string.toLowerCase()))
//                --ranks[1];
            if (descriptionHayStack.contains(string.toLowerCase()))
                --ranks[2];
        }

        if (ranks[0] == 0 && ranks[1] == 0 && ranks[2] == 0)
            return null;

        return ranks;
    }

    public JToolBar getToolBar() {
        return toolBar;
    }

    private void initialize() {
        setLayout(new BorderLayout());
        add(toolBar, BorderLayout.NORTH);

        searchField = new SearchTextField();
        searchField.addActionListener(e -> reloadTemplateList());
        toolBar.add(searchField);

        templateList = new JList<>();
        templateList.setToolTipText("Drag one or multiple entries from the list into the graph to create nodes.");
        templateList.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        templateList.setBorder(UIUtils.createControlBorder());
        templateList.setCellRenderer(new JIPipeNodeTemplateListCellRenderer(projectTemplateList));
        templateList.setModel(new DefaultListModel<>());
        templateList.addListSelectionListener(e -> {
            selectNodeTemplate(templateList.getSelectedValue());
        });
        templateList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {

                    int i = templateList.locationToIndex(e.getPoint());
                    if (i >= 0) {
                        if (templateList.getSelectedValuesList().isEmpty()) {
                            templateList.addSelectionInterval(i, i);
                        }
                        else if(!Ints.contains(templateList.getSelectedIndices(), i)) {
                            templateList.clearSelection();
                            templateList.addSelectionInterval(i, i);
                        }
                    }
                    reloadManageMenu();
                    manageMenu.show(templateList, e.getX(), e.getY());
                } else if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2) {
                    addSelectedTemplatesToPipeline();
                }
            }
        });
        templateList.setDragEnabled(true);
        templateList.setTransferHandler(new NodeTemplateBoxTransferHandler());
        JScrollPane scrollPane = new JScrollPane(templateList);

        AutoResizeSplitPane splitPane = new AutoResizeSplitPane(JSplitPane.VERTICAL_SPLIT, scrollPane, documentationReader, AutoResizeSplitPane.RATIO_3_TO_1);
        add(splitPane, BorderLayout.CENTER);

        JButton manageButton = new JButton("Manage", UIUtils.getIconFromResources("actions/wrench.png"));
        toolBar.add(manageButton);

        if (nodesToAdd != null && !nodesToAdd.isEmpty() && canvasUI != null) {
            JButton addButton = new JButton("Create", UIUtils.getIconFromResources("actions/add.png"));
            addButton.addActionListener(e -> addTemplate());
            toolBar.add(addButton);
        }

        UIUtils.addReloadablePopupMenuToButton(manageButton, manageMenu, this::reloadManageMenu);


        if (isDocked) {
            JButton openWindowButton = new JButton(UIUtils.getIconFromResources("actions/open-in-new-window.png"));
            openWindowButton.setToolTipText("Open in new window");
            openWindowButton.addActionListener(e -> openNewToolBoxWindow(getWorkbench()));
            toolBar.add(openWindowButton);
        }
    }

    private void addSelectedTemplatesToPipeline() {
        if (canvasUI != null) {
            for (JIPipeNodeTemplate nodeTemplate : templateList.getSelectedValuesList()) {
                try {
                    canvasUI.pasteNodes(nodeTemplate.getGraph());
                } catch (JsonProcessingException e) {
                    IJ.handleException(e);
                }
            }
        }
    }

    private void addTemplate() {
        JIPipeNodeTemplate.create(canvasUI, nodesToAdd);
    }

    private void downloadTemplates() {
        JIPipeRunExecuterUI.runInDialog(getWorkbench(), getWorkbench().getWindow(), new NodeTemplateDownloaderRun(getWorkbench()));
    }

    private void reloadManageMenu() {

        manageMenu.removeAll();

        if (!templateList.getSelectedValuesList().isEmpty()) {
            if (canvasUI != null) {
                manageMenu.add(UIUtils.createMenuItem("Insert", "Adds the selected nodes to the pipeline", UIUtils.getIconFromResources("actions/add.png"), this::addSelectedTemplatesToPipeline));
            }

            JMenuItem editItem = new JMenuItem("Edit", UIUtils.getIconFromResources("actions/document-edit.png"));
            editItem.addActionListener(e -> editSelected());
            manageMenu.add(editItem);

            if (project != null) {
                JMenuItem copyToProjectItem = new JMenuItem("Copy to project", UIUtils.getIconFromResources("actions/edit-copy.png"));
                copyToProjectItem.addActionListener(e -> copySelectionToProject());
                manageMenu.add(copyToProjectItem);

                JMenuItem copyToGlobalItem = new JMenuItem("Copy to global storage", UIUtils.getIconFromResources("actions/edit-copy.png"));
                copyToGlobalItem.addActionListener(e -> copySelectionToGlobal());
                manageMenu.add(copyToGlobalItem);
            }

            JMenuItem exportItem = new JMenuItem("Export", UIUtils.getIconFromResources("actions/document-export.png"));
            exportItem.addActionListener(e -> exportTemplates());
            manageMenu.add(exportItem);

            JMenuItem deleteItem = new JMenuItem("Delete", UIUtils.getIconFromResources("actions/edit-delete.png"));
            deleteItem.addActionListener(e -> deleteSelection());
            manageMenu.add(deleteItem);
        }

        manageMenu.addSeparator();

        JMenuItem selectAllItem = new JMenuItem("Select all", UIUtils.getIconFromResources("actions/edit-select-all.png"));
        selectAllItem.addActionListener(e -> {
            if (templateList.getModel().getSize() > 0) {
                templateList.setSelectionInterval(0, templateList.getModel().getSize() - 1);
            }
        });
        manageMenu.add(selectAllItem);

        manageMenu.addSeparator();

        JMenuItem importItem = new JMenuItem("Import from file", UIUtils.getIconFromResources("actions/document-import.png"));
        importItem.addActionListener(e -> importTemplates());
        manageMenu.add(importItem);

        JMenuItem downloadTemplatesItem = new JMenuItem("Download more templates", UIUtils.getIconFromResources("actions/download.png"));
        downloadTemplatesItem.addActionListener(e -> downloadTemplates());
        manageMenu.add(downloadTemplatesItem);

        manageMenu.addSeparator();

        JMenuItem refreshItem = new JMenuItem("Reload list", UIUtils.getIconFromResources("actions/view-refresh.png"));
        refreshItem.addActionListener(e -> reloadTemplateList());
        manageMenu.add(refreshItem);
    }

    private void editSelected() {
        if (templateList.getSelectedValuesList().isEmpty()) {
            JOptionPane.showMessageDialog(this, "You did not select any templates!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        JIPipeNodeTemplate template = templateList.getSelectedValue();
        if (template.isFromExtension()) {
            JOptionPane.showMessageDialog(this, "Extension-provided templates cannot be edited!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        JIPipeNodeTemplate copy = new JIPipeNodeTemplate(template);
        if (ParameterPanel.showDialog(getWorkbench(), copy, new MarkdownDocument("# Node templates\n\nUse this user interface to modify node templates."), "Edit template",
                ParameterPanel.WITH_SCROLLING | ParameterPanel.WITH_SEARCH_BAR | ParameterPanel.WITH_DOCUMENTATION)) {
            template.copyFrom(copy);
            template.setSource(JIPipeNodeTemplate.SOURCE_USER);
            if (project != null) {
                project.getMetadata().emitParameterChangedEvent("node-templates");
            }
            NodeTemplateSettings templateSettings = NodeTemplateSettings.getInstance();
            templateSettings.emitParameterChangedEvent("node-templates");
            NodeTemplateSettings.triggerRefreshedEvent();
        }
    }

    private void exportTemplates() {
        if (templateList.getSelectedValuesList().isEmpty()) {
            return;
        }
        Path path = FileChooserSettings.saveFile(this, FileChooserSettings.LastDirectoryKey.Projects, "Export templates", UIUtils.EXTENSION_FILTER_JSON);
        if (path != null) {
            try {
                Files.write(path, JsonUtils.toPrettyJsonString(templateList.getSelectedValuesList()).getBytes(StandardCharsets.UTF_8));
            } catch (IOException e) {
                UIUtils.openErrorDialog(getWorkbench(), this, e);
            }
        }
    }

    private void importTemplates() {
        Path path = FileChooserSettings.openFile(this, FileChooserSettings.LastDirectoryKey.Projects, "Import templates", UIUtils.EXTENSION_FILTER_JSON);
        if (path != null) {
            try {
                JsonNode node = JsonUtils.getObjectMapper().readerFor(JsonNode.class).readValue(path.toFile());
                List<JIPipeNodeTemplate> templates = new ArrayList<>();
                for (JsonNode element : ImmutableList.copyOf(node.elements())) {
                    templates.add(JsonUtils.getObjectMapper().readerFor(JIPipeNodeTemplate.class).readValue(element));
                }
                boolean writeToProject = false;
                if (project != null) {
                    int result = JOptionPane.showOptionDialog(this,
                            "Node templates can be stored globally or inside the project. Where should the templates be stored?",
                            "Import node templates",
                            JOptionPane.YES_NO_CANCEL_OPTION,
                            JOptionPane.QUESTION_MESSAGE,
                            null,
                            new Object[]{"Globally", "Inside project", "Cancel"},
                            "Globally");
                    if (result == JOptionPane.CANCEL_OPTION)
                        return;
                    writeToProject = result == JOptionPane.NO_OPTION;
                }
                if (writeToProject) {
                    for (JIPipeNodeTemplate template : templates) {
                        if (!project.getMetadata().getNodeTemplates().contains(template)) {
                            project.getMetadata().getNodeTemplates().add(template);
                        }
                    }
                    project.getMetadata().emitParameterChangedEvent("node-templates");
                } else {
                    NodeTemplateSettings templateSettings = NodeTemplateSettings.getInstance();
                    for (JIPipeNodeTemplate template : templates) {
                        if (!templateSettings.getNodeTemplates().contains(template)) {
                            templateSettings.getNodeTemplates().add(template);
                        }
                    }
                    templateSettings.emitParameterChangedEvent("node-templates");
                }
                NodeTemplateSettings.triggerRefreshedEvent();
            } catch (Exception e) {
                UIUtils.openErrorDialog(getWorkbench(), this, e);
            }
        }
    }

    private void copySelectionToGlobal() {
        if (project == null) {
            return;
        }
        if (templateList.getSelectedValuesList().isEmpty()) {
            JOptionPane.showMessageDialog(this, "You did not select any templates!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (JOptionPane.showConfirmDialog(this, "Do you really want to copy " + templateList.getSelectedValuesList().size() + " templates into the global storage?",
                "Copy selection to global storage", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.NO_OPTION) {
            return;
        }
        NodeTemplateSettings templateSettings = NodeTemplateSettings.getInstance();
        Set<JIPipeNodeTemplate> copied = new HashSet<>();
        for (JIPipeNodeTemplate template : ImmutableList.copyOf(templateList.getSelectedValuesList())) {
            if (!templateSettings.getNodeTemplates().contains(template)) {
                templateSettings.getNodeTemplates().add(template);
                copied.add(template);
            }
        }
        if (!copied.isEmpty()) {
            if (JOptionPane.showConfirmDialog(this, "Successfully copied " + copied.size() + " templates into the global storage.\n" +
                    "Do you want to remove these from the project storage (if applicable)?", "Copy selection to global storage", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION) {
                for (JIPipeNodeTemplate template : copied) {
                    project.getMetadata().getNodeTemplates().remove(template);
                }
                project.getMetadata().emitParameterChangedEvent("node-templates");
            }
        }
        NodeTemplateSettings.triggerRefreshedEvent();
    }

    private void copySelectionToProject() {
        if (project == null) {
            return;
        }
        if (templateList.getSelectedValuesList().isEmpty()) {
            JOptionPane.showMessageDialog(this, "You did not select any templates!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (JOptionPane.showConfirmDialog(this, "Do you really want to copy " + templateList.getSelectedValuesList().size() + " templates into the project storage?",
                "Copy selection to project", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.NO_OPTION) {
            return;
        }
        NodeTemplateSettings templateSettings = NodeTemplateSettings.getInstance();
        Set<JIPipeNodeTemplate> copied = new HashSet<>();
        for (JIPipeNodeTemplate template : ImmutableList.copyOf(templateList.getSelectedValuesList())) {
            if (!project.getMetadata().getNodeTemplates().contains(template)) {
                project.getMetadata().getNodeTemplates().add(template);
                copied.add(template);
            }
        }
        if (!copied.isEmpty()) {
            if (JOptionPane.showConfirmDialog(this, "Successfully copied " + copied.size() + " templates into the project storage.\n" +
                    "Do you want to remove these from the global storage (if applicable)?", "Copy selection to project", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION) {
                for (JIPipeNodeTemplate template : copied) {
                    templateSettings.getNodeTemplates().remove(template);
                }
                templateSettings.emitParameterChangedEvent("node-templates");
            }
        }
        NodeTemplateSettings.triggerRefreshedEvent();
    }

    private void deleteSelection() {
        if (templateList.getSelectedValuesList().isEmpty()) {
            JOptionPane.showMessageDialog(this, "You did not select any templates!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (JOptionPane.showConfirmDialog(this, "Do you really want to delete " + templateList.getSelectedValuesList().size() + " templates?",
                "Delete selected templates", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.NO_OPTION) {
            return;
        }
        boolean modifiedProject = false;
        boolean modifiedGlobal = false;
        boolean triedModifyExtension = false;
        NodeTemplateSettings templateSettings = NodeTemplateSettings.getInstance();
        for (JIPipeNodeTemplate template : ImmutableList.copyOf(templateList.getSelectedValuesList())) {
            if (template.isFromExtension()) {
                triedModifyExtension = true;
                continue;
            }
            if (templateSettings.getNodeTemplates().remove(template)) {
                modifiedGlobal = true;
            }
            if (project != null && project.getMetadata().getNodeTemplates().remove(template)) {
                modifiedProject = true;
            }
        }
        if (modifiedGlobal) {
            templateSettings.emitParameterChangedEvent("node-templates");
        }
        if (modifiedProject) {
            project.getMetadata().emitParameterChangedEvent("node-templates");
        }
        if (modifiedProject || modifiedGlobal) {
            NodeTemplateSettings.triggerRefreshedEvent();
        }
        if (triedModifyExtension) {
            JOptionPane.showMessageDialog(this,
                    "Extension-provided templates cannot be deleted.",
                    "Delete templates",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void reloadTemplateList() {
        List<JIPipeNodeTemplate> infos = getFilteredAndSortedInfos();
        DefaultListModel<JIPipeNodeTemplate> model = new DefaultListModel<>();
        for (JIPipeNodeTemplate info : infos) {
            if (info.getGraph() == null) {
                continue;
            }
            model.addElement(info);
        }
        templateList.setModel(model);

        if (!model.isEmpty())
            templateList.setSelectedIndex(0);
        else
            selectNodeTemplate(null);
    }

    private void selectNodeTemplate(JIPipeNodeTemplate template) {
        if (template != null) {
            documentationReader.setDocument(new MarkdownDocument("# " + template.getName() + "\n\n" + TooltipUtils.getAlgorithmTooltip(template, false)));
        } else
            documentationReader.setDocument(new MarkdownDocument(""));
    }

    private List<JIPipeNodeTemplate> getFilteredAndSortedInfos() {
        List<JIPipeNodeTemplate> templates = new ArrayList<>(NodeTemplateSettings.getInstance().getNodeTemplates());
        this.projectTemplateList.clear();
        if (project != null) {
            templates.addAll(project.getMetadata().getNodeTemplates());
            this.projectTemplateList.addAll(project.getMetadata().getNodeTemplates());
        }
        templates.addAll(JIPipe.getNodes().getRegisteredTemplates());
        return RankedData.getSortedAndFilteredData(templates,
                JIPipeNodeTemplate::getName,
                NodeTemplateBox::rankNavigationEntry,
                searchField.getSearchStrings());
    }

    public JIPipeProject getProject() {
        return project;
    }

    @Override
    public void onJIPipeNodeTemplatesRefreshed(NodeTemplatesRefreshedEvent event) {
        reloadTemplateList();
    }
}
