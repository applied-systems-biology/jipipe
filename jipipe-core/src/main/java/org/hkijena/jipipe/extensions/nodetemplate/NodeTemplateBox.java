package org.hkijena.jipipe.extensions.nodetemplate;

import com.google.common.eventbus.Subscribe;
import com.google.common.html.HtmlEscapers;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeNodeTemplate;
import org.hkijena.jipipe.api.JIPipeProject;
import org.hkijena.jipipe.api.data.JIPipeDataInfo;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.nodes.categories.InternalNodeTypeCategory;
import org.hkijena.jipipe.extensions.settings.NodeTemplateSettings;
import org.hkijena.jipipe.ui.components.AlwaysOnTopToggle;
import org.hkijena.jipipe.ui.components.MarkdownDocument;
import org.hkijena.jipipe.ui.components.MarkdownReader;
import org.hkijena.jipipe.ui.components.SearchTextField;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.search.RankedData;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class NodeTemplateBox extends JPanel {

    private final JIPipeProject project;
    private JList<JIPipeNodeTemplate> templateList;
    private final Set<JIPipeNodeTemplate> projectTemplateList = new HashSet<>();
    private SearchTextField searchField;
    private final MarkdownReader documentationReader = new MarkdownReader(false);
    private final JToolBar toolBar = new JToolBar();

    public NodeTemplateBox(JIPipeProject project) {
        this.project = project;
        initialize();
        reloadAlgorithmList();
        NodeTemplateSettings.getInstance().getEventBus().register(this);
    }

    public JToolBar getToolBar() {
        return toolBar;
    }

    @Subscribe
    public void onNodeTemplatesRefreshed(NodeTemplateSettings.NodeTemplatesRefreshedEvent event) {
        reloadAlgorithmList();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        add(toolBar, BorderLayout.NORTH);

        searchField = new SearchTextField();
        searchField.addActionListener(e -> reloadAlgorithmList());
        toolBar.add(searchField);

        templateList = new JList<>();
        templateList.setToolTipText("Drag one or multiple entries from the list into the graph to create nodes.");
        templateList.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        templateList.setBorder(BorderFactory.createEtchedBorder());
        templateList.setCellRenderer(new JIPipeNodeTemplateListCellRenderer(projectTemplateList));
        templateList.setModel(new DefaultListModel<>());
        templateList.addListSelectionListener(e -> {
            selectNodeTemplate(templateList.getSelectedValue());
        });
        templateList.setDragEnabled(true);
        templateList.setTransferHandler(new NodeTemplateBoxTransferHandler());
        JScrollPane scrollPane = new JScrollPane(templateList);

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, scrollPane, documentationReader);
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

        JButton reloadButton = new JButton("Refresh", UIUtils.getIconFromResources("actions/view-refresh.png"));
        reloadButton.setToolTipText("Refresh list");
        reloadButton.addActionListener(e -> reloadAlgorithmList());
        toolBar.add(reloadButton);

        JButton manageButton = new JButton("Manage", UIUtils.getIconFromResources("actions/wrench.png"));
        toolBar.add(manageButton);

        JPopupMenu manageMenu = UIUtils.addPopupMenuToComponent(manageButton);

        JMenuItem copyToProjectItem = new JMenuItem("Copy selection to project", UIUtils.getIconFromResources("actions/edit-copy.png"));
        copyToProjectItem.addActionListener(e -> copySelectionToProject());
        manageMenu.add(copyToProjectItem);

        JMenuItem copyToGlobalItem = new JMenuItem("Copy selection to global storage", UIUtils.getIconFromResources("actions/edit-copy.png"));
        copyToGlobalItem.addActionListener(e -> copySelectionToGlobal());
        manageMenu.add(copyToGlobalItem);

        JMenuItem deleteItem = new JMenuItem("Delete selection", UIUtils.getIconFromResources("actions/edit-delete.png"));
        deleteItem.addActionListener(e -> deleteSelection());
        manageMenu.add(deleteItem);
    }

    private void copySelectionToGlobal() {

    }

    private void copySelectionToProject() {

    }

    private void deleteSelection() {

    }

    private void reloadAlgorithmList() {
        List<JIPipeNodeTemplate> infos = getFilteredAndSortedInfos();
        DefaultListModel<JIPipeNodeTemplate> model = new DefaultListModel<>();
        for (JIPipeNodeTemplate info : infos) {
            if (info.getNodeInfo() == null || info.getNodeInfo().isHidden() || info.getNodeInfo().getCategory() == null
                    || info.getNodeInfo().getCategory() instanceof InternalNodeTypeCategory) {
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
            StringBuilder builder = new StringBuilder();
            builder.append("# ").append(template.getName()).append("\n\n");

            // Write description
            String description = template.getDescription().getBody();
            if (description != null && !description.isEmpty())
                builder.append(description).append("</br>");

            JIPipeGraphNode node = template.newInstance();
            if(node != null) {
                // Write description
                description = node.getInfo().getDescription().getBody();
                if (description != null && !description.isEmpty())
                    builder.append(description).append("</br>");

                // Write algorithm slot info
                builder.append("<table style=\"margin-top: 10px;\">");
                for (JIPipeDataSlot slot : node.getInputSlots()) {
                    builder.append("<tr>");
                    builder.append("<td><p style=\"background-color:#27ae60; color:white;border:3px solid #27ae60;border-radius:5px;text-align:center;\">Input</p></td>");
                    builder.append("<td>").append("<img src=\"").append(JIPipe.getDataTypes().getIconURLFor(slot.getAcceptedDataType())).append("\"/></td>");
                    builder.append("<td>").append(HtmlEscapers.htmlEscaper().escape(StringUtils.orElse(slot.getName(), "-"))).append("</td>");
                    builder.append("<td><i>(").append(HtmlEscapers.htmlEscaper().escape(JIPipeDataInfo.getInstance(slot.getAcceptedDataType()).getName())).append(")</i></td>");
                    builder.append("</tr>");
                }
                for (JIPipeDataSlot slot : node.getOutputSlots()) {
                    builder.append("<tr>");
                    builder.append("<td><p style=\"background-color:#da4453; color:white;border:3px solid #da4453;border-radius:5px;text-align:center;\">Output</p></td>");
                    builder.append("<td>").append("<img src=\"").append(JIPipe.getDataTypes().getIconURLFor(slot.getAcceptedDataType())).append("\"/></td>");
                    builder.append("<td>").append(HtmlEscapers.htmlEscaper().escape(StringUtils.orElse(slot.getName(), "-"))).append("</td>");
                    builder.append("<td><i>(").append(HtmlEscapers.htmlEscaper().escape(JIPipeDataInfo.getInstance(slot.getAcceptedDataType()).getName())).append(")</i></td>");
                    builder.append("</tr>");
                }
                builder.append("</table>\n\n");
            }
            documentationReader.setDocument(new MarkdownDocument(builder.toString()));
        } else
            documentationReader.setDocument(new MarkdownDocument(""));
    }

    private List<JIPipeNodeTemplate> getFilteredAndSortedInfos() {
        List<JIPipeNodeTemplate> templates = new ArrayList<>(NodeTemplateSettings.getInstance().getNodeTemplates());
        this.projectTemplateList.clear();
        if(project != null) {
            templates.addAll(project.getMetadata().getNodeTemplates());
            this.projectTemplateList.addAll(project.getMetadata().getNodeTemplates());
        }
        return RankedData.getSortedAndFilteredData(templates,
                JIPipeNodeTemplate::getName,
                NodeTemplateBox::rankNavigationEntry,
                searchField.getSearchStrings());
    }

    public JIPipeProject getProject() {
        return project;
    }

    public static void openNewToolBoxWindow(JIPipeProject project) {
        NodeTemplateBox toolBox = new NodeTemplateBox(project);
        JFrame window = new JFrame();
        toolBox.getToolBar().add(new AlwaysOnTopToggle(window));
        window.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        window.setAlwaysOnTop(true);
        window.setTitle("Node templates");
        window.setIconImage(UIUtils.getIcon128FromResources("jipipe.png").getImage());
        window.setContentPane(toolBox);
        window.pack();
        window.setSize(300, 700);
        window.setVisible(true);
    }

    private static int[] rankNavigationEntry(JIPipeNodeTemplate template, String[] searchStrings) {
        if (searchStrings == null || searchStrings.length == 0)
            return new int[0];
        String nameHayStack;
        String name2HayStack;
        String descriptionHayStack;
        if (template.getNodeInfo() == null || template.getNodeInfo().isHidden())
            return null;
        nameHayStack = StringUtils.orElse(template.getName(), "").toLowerCase();
        name2HayStack = StringUtils.orElse(template.getNodeInfo().getName(), "").toLowerCase();
        descriptionHayStack = StringUtils.orElse(template.getDescription().getBody(), "").toLowerCase();

        nameHayStack = nameHayStack.toLowerCase();
        name2HayStack = name2HayStack.toLowerCase();
        descriptionHayStack = descriptionHayStack.toLowerCase();

        int[] ranks = new int[3];

        for (String string : searchStrings) {
            if (nameHayStack.contains(string.toLowerCase()))
                --ranks[0];
            if (name2HayStack.contains(string.toLowerCase()))
                --ranks[1];
            if (descriptionHayStack.contains(string.toLowerCase()))
                --ranks[2];
        }

        if (ranks[0] == 0 && ranks[1] == 0 && ranks[2] == 0)
            return null;

        return ranks;
    }
}
