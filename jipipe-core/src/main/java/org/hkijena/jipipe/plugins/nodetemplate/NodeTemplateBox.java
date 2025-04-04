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

package org.hkijena.jipipe.plugins.nodetemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import com.google.common.primitives.Ints;
import ij.IJ;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeNodeTemplate;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.desktop.JIPipeDesktop;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbenchPanel;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphCanvasUI;
import org.hkijena.jipipe.desktop.app.running.JIPipeDesktopRunExecuteUI;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopParameterFormPanel;
import org.hkijena.jipipe.desktop.commons.components.search.JIPipeDesktopSearchTextField;
import org.hkijena.jipipe.desktop.commons.components.window.JIPipeDesktopAlwaysOnTopToggle;
import org.hkijena.jipipe.plugins.nodetemplate.templatedownloader.NodeTemplateDownloaderRun;
import org.hkijena.jipipe.plugins.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.plugins.parameters.library.markup.MarkdownText;
import org.hkijena.jipipe.plugins.settings.JIPipeFileChooserApplicationSettings;
import org.hkijena.jipipe.plugins.settings.JIPipeNodeTemplateApplicationSettings;
import org.hkijena.jipipe.utils.StringUtils;
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
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class NodeTemplateBox extends JIPipeDesktopWorkbenchPanel implements NodeTemplatesRefreshedEventListener {

    private final JIPipeProject project;
    private final Set<JIPipeNodeTemplate> projectOwnedTemplates = Sets.newIdentityHashSet();
    //    private final JIPipeDesktopMarkdownReader documentationReader = new JIPipeDesktopMarkdownReader(false);
    private final JToolBar toolBar = new JToolBar();
    private final boolean isDocked;
    private final JIPipeDesktopGraphCanvasUI canvasUI;
    private final Set<JIPipeGraphNode> nodesToAdd;
    private final JPopupMenu manageMenu = new JPopupMenu();
    private JList<JIPipeNodeTemplate> templateJList;
    private JIPipeDesktopSearchTextField searchField;

    public NodeTemplateBox(JIPipeDesktopWorkbench workbench, boolean isDocked, JIPipeDesktopGraphCanvasUI canvasUI, Set<JIPipeGraphNode> nodesToAdd) {
        super(workbench);
        this.isDocked = isDocked;
        this.canvasUI = canvasUI;
        this.nodesToAdd = nodesToAdd;
        if (workbench instanceof JIPipeDesktopProjectWorkbench) {
            this.project = workbench.getProject();
        } else {
            this.project = null;
        }
        initialize();
        reloadTemplateList();
        JIPipe.getNodeTemplates().getNodeTemplatesRefreshedEventEmitter().subscribeWeak(this);
    }

    public static void openNewToolBoxWindow(JIPipeDesktopWorkbench workbench) {
        NodeTemplateBox toolBox = new NodeTemplateBox(workbench, false, null, null);
        JFrame window = new JFrame();
        toolBox.getToolBar().add(new JIPipeDesktopAlwaysOnTopToggle(window));
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
        nameHayStack = StringUtils.orElse(template.getName(), "").toLowerCase(Locale.ROOT);
//        name2HayStack = StringUtils.orElse(template.getNodeInfo().getName(), "").toLowerCase(Locale.ROOT);
        descriptionHayStack = StringUtils.orElse(template.getDescription().getBody(), "").toLowerCase(Locale.ROOT);

        nameHayStack = nameHayStack.toLowerCase(Locale.ROOT);
//        name2HayStack = name2HayStack.toLowerCase(Locale.ROOT);
        descriptionHayStack = descriptionHayStack.toLowerCase(Locale.ROOT);

        int[] ranks = new int[3];

        for (int i = 0; i < searchStrings.length; i++) {
            String string = searchStrings[i];
            if (nameHayStack.contains(string.toLowerCase(Locale.ROOT)))
                --ranks[0];
            if (i == 0 && nameHayStack.startsWith(string.toLowerCase(Locale.ROOT)))
                ranks[0] -= 2;
//            if (name2HayStack.contains(string.toLowerCase(Locale.ROOT)))
//                --ranks[1];
            if (descriptionHayStack.contains(string.toLowerCase(Locale.ROOT)))
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

        searchField = new JIPipeDesktopSearchTextField();
        searchField.addActionListener(e -> reloadTemplateList());
        toolBar.add(searchField);

        templateJList = new JList<>();
        JScrollPane scrollPane = new JScrollPane(templateJList);
//        templateJList.setToolTipText("Drag one or multiple entries from the list into the graph to create nodes.");
        templateJList.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        templateJList.setBorder(UIUtils.createControlBorder());
        templateJList.setOpaque(false);
        templateJList.setBorder(UIUtils.createEmptyBorder(8));
        templateJList.setCellRenderer(new JIPipeNodeTemplateListCellRenderer(scrollPane, projectOwnedTemplates));
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        templateJList.setModel(new DefaultListModel<>());
//        templateJList.addListSelectionListener(e -> {
//            selectNodeTemplate(templateJList.getSelectedValue());
//        });
        templateJList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {

                    int i = templateJList.locationToIndex(e.getPoint());
                    if (i >= 0) {
                        if (templateJList.getSelectedValuesList().isEmpty()) {
                            templateJList.addSelectionInterval(i, i);
                        } else if (!Ints.contains(templateJList.getSelectedIndices(), i)) {
                            templateJList.clearSelection();
                            templateJList.addSelectionInterval(i, i);
                        }
                    }
                    reloadManageMenu();
                    manageMenu.show(templateJList, e.getX(), e.getY());
                } else if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2) {
                    addSelectedTemplatesToPipeline();
                }
            }
        });
        templateJList.setDragEnabled(true);
        templateJList.setTransferHandler(new NodeTemplateBoxTransferHandler());

        add(scrollPane, BorderLayout.CENTER);

        JButton manageButton = new JButton(UIUtils.getIconFromResources("actions/hamburger-menu.png"));
        UIUtils.makeButtonFlat25x25(manageButton);
        toolBar.add(manageButton);
        UIUtils.addReloadablePopupMenuToButton(manageButton, manageMenu, this::reloadManageMenu);

//        if (nodesToAdd != null && !nodesToAdd.isEmpty() && canvasUI != null) {
//            JButton addButton = new JButton("Create", UIUtils.getIconFromResources("actions/add.png"));
//            addButton.addActionListener(e -> addTemplate());
//            toolBar.add(addButton);
//        }


//        if (isDocked) {
//            JButton openWindowButton = new JButton(UIUtils.getIconFromResources("actions/open-in-new-window.png"));
//            openWindowButton.setToolTipText("Open in new window");
//            openWindowButton.addActionListener(e -> openNewToolBoxWindow(getDesktopWorkbench()));
//            toolBar.add(openWindowButton);
//        }
    }

    private void addSelectedTemplatesToPipeline() {
        if (canvasUI != null) {
            for (JIPipeNodeTemplate nodeTemplate : templateJList.getSelectedValuesList()) {
                try {
                    Map<UUID, JIPipeGraphNode> nodeMap = canvasUI.pasteNodes(nodeTemplate.getGraph());
                    canvasUI.setSelection(nodeMap.values().stream().map(node -> canvasUI.getNodeUIs().get(node)).collect(Collectors.toSet()));
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
        JIPipeDesktopRunExecuteUI.runInDialog(getDesktopWorkbench(), getDesktopWorkbench().getWindow(), new NodeTemplateDownloaderRun(getDesktopWorkbench()));
    }

    private void reloadManageMenu() {

        manageMenu.removeAll();

        if (!templateJList.getSelectedValuesList().isEmpty()) {
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
            if (templateJList.getModel().getSize() > 0) {
                templateJList.setSelectionInterval(0, templateJList.getModel().getSize() - 1);
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
        if (templateJList.getSelectedValuesList().isEmpty()) {
            JOptionPane.showMessageDialog(this, "You did not select any templates!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        JIPipeNodeTemplate template = templateJList.getSelectedValue();
        if (template.isFromExtension()) {
            JOptionPane.showMessageDialog(this, "Extension-provided templates cannot be edited!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        JIPipeNodeTemplate copy = new JIPipeNodeTemplate(template);
        if (JIPipeDesktopParameterFormPanel.showDialog(getDesktopWorkbench(), copy, new MarkdownText("# Node templates\n\nUse this user interface to modify node templates."), "Edit template",
                JIPipeDesktopParameterFormPanel.WITH_SCROLLING | JIPipeDesktopParameterFormPanel.WITH_SEARCH_BAR | JIPipeDesktopParameterFormPanel.WITH_DOCUMENTATION)) {
            JIPipe.getNodeTemplates().editTemplate(template, copy, project);
        }
    }

    private void exportTemplates() {
        if (templateJList.getSelectedValuesList().isEmpty()) {
            return;
        }
        Path path = JIPipeDesktop.saveFile(this, getDesktopWorkbench(), JIPipeFileChooserApplicationSettings.LastDirectoryKey.Projects, "Export templates", HTMLText.EMPTY, UIUtils.EXTENSION_FILTER_JSON);
        if (path != null) {
            try {
                Files.write(path, JsonUtils.toPrettyJsonString(templateJList.getSelectedValuesList()).getBytes(StandardCharsets.UTF_8));
            } catch (IOException e) {
                UIUtils.showErrorDialog(getDesktopWorkbench(), this, e);
            }
        }
    }

    private void importTemplates() {
        Path path = JIPipeDesktop.openFile(this, getDesktopWorkbench(), JIPipeFileChooserApplicationSettings.LastDirectoryKey.Projects, "Import templates", HTMLText.EMPTY, UIUtils.EXTENSION_FILTER_JSON);
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
                    JIPipe.getNodeTemplates().addToProject(templates, project);
                } else {
                    JIPipe.getNodeTemplates().addToGlobal(templates);
                }
            } catch (Exception e) {
                UIUtils.showErrorDialog(getDesktopWorkbench(), this, e);
            }
        }
    }

    private void copySelectionToGlobal() {
        if (project == null) {
            return;
        }
        if (templateJList.getSelectedValuesList().isEmpty()) {
            JOptionPane.showMessageDialog(this, "You did not select any templates!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (JOptionPane.showConfirmDialog(this, "Do you really want to copy " + templateJList.getSelectedValuesList().size() + " templates into the global storage?",
                "Copy selection to global storage", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.NO_OPTION) {
            return;
        }
        Set<JIPipeNodeTemplate> copied = new HashSet<>();
        for (JIPipeNodeTemplate template : ImmutableList.copyOf(templateJList.getSelectedValuesList())) {
            if (!JIPipe.getNodeTemplates().isInGlobal(template)) {
                if (JIPipe.getNodeTemplates().addToGlobal(new JIPipeNodeTemplate(template))) {
                    copied.add(template);
                }
            }
        }
        if (!copied.isEmpty()) {
            if (JOptionPane.showConfirmDialog(this, "Successfully copied " + copied.size() + " templates into the global storage.\n" +
                    "Do you want to remove these from the project storage (if applicable)?", "Copy selection to global storage", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION) {
                for (JIPipeNodeTemplate template : copied) {
                    JIPipe.getNodeTemplates().removeFromProject(template, project);
                }
            }
        }
        JIPipe.getNodeTemplates().emitRefreshedEvent();
    }

    private void copySelectionToProject() {
        if (project == null) {
            return;
        }
        if (templateJList.getSelectedValuesList().isEmpty()) {
            JOptionPane.showMessageDialog(this, "You did not select any templates!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (JOptionPane.showConfirmDialog(this, "Do you really want to copy " + templateJList.getSelectedValuesList().size() + " templates into the project storage?",
                "Copy selection to project", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.NO_OPTION) {
            return;
        }
        Set<JIPipeNodeTemplate> copied = new HashSet<>();
        for (JIPipeNodeTemplate template : ImmutableList.copyOf(templateJList.getSelectedValuesList())) {
            if (!JIPipe.getNodeTemplates().isInProject(template, project)) {
                if (JIPipe.getNodeTemplates().addToProject(new JIPipeNodeTemplate(template), project)) {
                    copied.add(template);
                }
            }
        }
        if (!copied.isEmpty()) {
            if (JOptionPane.showConfirmDialog(this, "Successfully copied " + copied.size() + " templates into the project storage.\n" +
                    "Do you want to remove these from the global storage (if applicable)?", "Copy selection to project", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION) {
                for (JIPipeNodeTemplate template : copied) {
                    JIPipe.getNodeTemplates().removeFromGlobal(template);
                }
            }
        }
        JIPipe.getNodeTemplates().emitRefreshedEvent();
    }

    private void deleteSelection() {
        if (templateJList.getSelectedValuesList().isEmpty()) {
            JOptionPane.showMessageDialog(this, "You did not select any templates!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (JOptionPane.showConfirmDialog(this, "Do you really want to delete " + templateJList.getSelectedValuesList().size() + " templates?",
                "Delete selected templates", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.NO_OPTION) {
            return;
        }
        boolean modifiedProject = false;
        boolean modifiedGlobal = false;
        boolean triedModifyExtension = false;
        JIPipeNodeTemplateApplicationSettings templateSettings = JIPipeNodeTemplateApplicationSettings.getInstance();
        for (JIPipeNodeTemplate template : ImmutableList.copyOf(templateJList.getSelectedValuesList())) {
            if (template.isFromExtension()) {
                triedModifyExtension = true;
                continue;
            }
            if (JIPipe.getNodeTemplates().removeFromGlobal(template)) {
                modifiedGlobal = true;
            }
            if (project != null && JIPipe.getNodeTemplates().removeFromProject(template, project)) {
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
            JIPipe.getNodeTemplates().emitRefreshedEvent();
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
        templateJList.setModel(model);

        if (!model.isEmpty()) {
            templateJList.setSelectedIndex(0);
        } else {
//            selectNodeTemplate(null);
        }
    }

//    private void selectNodeTemplate(JIPipeNodeTemplate template) {
//        if (template != null) {
//            documentationReader.setDocument(new MarkdownText("# " + template.getName() + "\n\n" + TooltipUtils.getAlgorithmTooltip(template, false)));
//        } else
//            documentationReader.setDocument(new MarkdownText(""));
//    }

    private List<JIPipeNodeTemplate> getFilteredAndSortedInfos() {
        List<JIPipeNodeTemplate> templates = new ArrayList<>(JIPipe.getNodeTemplates().getGlobalTemplates());
        this.projectOwnedTemplates.clear();
        if (project != null) {
            templates.addAll(project.getMetadata().getNodeTemplates());
            this.projectOwnedTemplates.addAll(project.getMetadata().getNodeTemplates());
        }
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
