package org.hkijena.jipipe.extensions.nodetoolboxtool;

import com.google.common.html.HtmlEscapers;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.data.JIPipeDataInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.InternalNodeTypeCategory;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbench;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.JIPipeWorkbenchPanel;
import org.hkijena.jipipe.ui.components.markdown.MarkdownDocument;
import org.hkijena.jipipe.ui.components.markdown.MarkdownReader;
import org.hkijena.jipipe.ui.components.renderers.JIPipeNodeInfoOrExamplesListCellRenderer;
import org.hkijena.jipipe.ui.components.search.SearchTextField;
import org.hkijena.jipipe.ui.components.window.AlwaysOnTopToggle;
import org.hkijena.jipipe.utils.AutoResizeSplitPane;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.search.RankedData;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class NodeToolBox extends JIPipeWorkbenchPanel {

    private final MarkdownReader documentationReader = new MarkdownReader(false);
    private final JToolBar toolBar = new JToolBar();
    private final boolean isDocked;
    private JList<Object> algorithmList;
    private SearchTextField searchField;

    public NodeToolBox(JIPipeWorkbench workbench, boolean isDocked) {
        super(workbench);
        this.isDocked = isDocked;
        initialize();
        reloadAlgorithmList();
    }

    public static void openNewToolBoxWindow(JIPipeWorkbench workbench, Component parent) {
        NodeToolBox toolBox = new NodeToolBox(workbench, false);
        JFrame window = new JFrame();
        toolBox.getToolBar().add(new AlwaysOnTopToggle(window));
        window.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        window.setAlwaysOnTop(true);
        window.setTitle("Available nodes");
        window.setIconImage(UIUtils.getIcon128FromResources("jipipe.png").getImage());
        window.setContentPane(toolBox);
        window.pack();
        window.setSize(300, 700);
        window.setLocationRelativeTo(parent);
        window.setVisible(true);
    }

    private static int[] rankNavigationEntry(JIPipeNodeInfo info, String[] searchStrings) {
        if (searchStrings == null || searchStrings.length == 0)
            return new int[0];
        String nameHayStack;
        String menuHayStack;
        String descriptionHayStack;
        if (info.isHidden())
            return null;
        nameHayStack = StringUtils.orElse(info.getName(), "").toLowerCase();
        menuHayStack = info.getCategory().getName() + "\n" + info.getMenuPath();
        descriptionHayStack = StringUtils.orElse(info.getDescription().getBody(), "").toLowerCase();

        for (JIPipeNodeMenuLocation location : info.getAliases()) {
            if (!StringUtils.isNullOrEmpty(location.getAlternativeName())) {
                nameHayStack += location.getAlternativeName().toLowerCase();
            }
            menuHayStack += location.getMenuPath();
        }

        nameHayStack = nameHayStack.toLowerCase();
        menuHayStack = menuHayStack.toLowerCase();
        descriptionHayStack = descriptionHayStack.toLowerCase();

        int[] ranks = new int[3];

        for (int i = 0; i < searchStrings.length; i++) {
            String string = searchStrings[i];
            if (nameHayStack.contains(string.toLowerCase()))
                --ranks[0];
            if (i == 0 && nameHayStack.startsWith(string.toLowerCase()))
                ranks[0] -= 2;
            if (menuHayStack.contains(string.toLowerCase()))
                --ranks[1];
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
        searchField.addActionListener(e -> reloadAlgorithmList());
        toolBar.add(searchField);

        if (isDocked) {
            JButton openWindowButton = new JButton(UIUtils.getIconFromResources("actions/open-in-new-window.png"));
            openWindowButton.setToolTipText("Open in new window");
            openWindowButton.addActionListener(e -> openNewToolBoxWindow(getWorkbench(), SwingUtilities.getWindowAncestor(this)));
            toolBar.add(openWindowButton);
        }

        algorithmList = new JList<>();
        algorithmList.setToolTipText("Drag one or multiple entries from the list into the graph to create nodes.");
        algorithmList.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        algorithmList.setBorder(BorderFactory.createEtchedBorder());
        algorithmList.setCellRenderer(new JIPipeNodeInfoOrExamplesListCellRenderer());
        algorithmList.setModel(new DefaultListModel<>());
        algorithmList.addListSelectionListener(e -> {
            if (algorithmList.getSelectedValue() instanceof JIPipeNodeInfo) {
                selectNodeInfo((JIPipeNodeInfo) algorithmList.getSelectedValue());
            } else if (algorithmList.getSelectedValue() instanceof JIPipeNodeExample) {
                selectNodeExample((JIPipeNodeExample) algorithmList.getSelectedValue());
            }
        });
        algorithmList.setDragEnabled(true);
        algorithmList.setTransferHandler(new NodeToolBoxTransferHandler());
        JScrollPane scrollPane = new JScrollPane(algorithmList);

        AutoResizeSplitPane splitPane = new AutoResizeSplitPane(JSplitPane.VERTICAL_SPLIT, scrollPane, documentationReader, AutoResizeSplitPane.RATIO_3_TO_1);
        add(splitPane, BorderLayout.CENTER);
    }

    private void reloadAlgorithmList() {
        List<JIPipeNodeInfo> infos = getFilteredAndSortedInfos();
        DefaultListModel<Object> model = new DefaultListModel<>();
        for (JIPipeNodeInfo info : infos) {
            if (info.isHidden() || info.getCategory() == null || info.getCategory() instanceof InternalNodeTypeCategory) {
                continue;
            }
            model.addElement(info);
            if (getWorkbench() instanceof JIPipeProjectWorkbench) {
                for (JIPipeNodeExample example : ((JIPipeProjectWorkbench) getWorkbench()).getProject().getNodeExamples(info.getId())) {
                    model.addElement(example);
                }
            }
        }
        algorithmList.setModel(model);

        if (!model.isEmpty())
            algorithmList.setSelectedIndex(0);
        else
            selectNodeInfo(null);
    }

    private void selectNodeExample(JIPipeNodeExample example) {
        if (example != null) {
            JIPipeNodeInfo info = example.getNodeInfo();

            StringBuilder builder = new StringBuilder();
            builder.append("# ").append(info.getName()).append("\n\n");

            if (!StringUtils.isNullOrEmpty(example.getNodeTemplate().getDescription().getBody())) {
                builder.append(example.getNodeTemplate().getDescription().getBody()).append("<br/>");
            }
            if (!StringUtils.isNullOrEmpty(info.getDescription().getBody())) {
                builder.append(info.getDescription().getBody()).append("</br>");
            }

            // Write algorithm slot info
            builder.append("<table style=\"margin-top: 10px;\">");
            for (JIPipeInputSlot slot : info.getInputSlots()) {
                builder.append("<tr>");
                builder.append("<td><p style=\"background-color:#27ae60; color:white;border:3px solid #27ae60;border-radius:5px;text-align:center;\">Input</p></td>");
                builder.append("<td>").append("<img src=\"").append(JIPipe.getDataTypes().getIconURLFor(slot.value())).append("\"/></td>");
                builder.append("<td>").append(HtmlEscapers.htmlEscaper().escape(StringUtils.orElse(slot.slotName(), "-"))).append("</td>");
                builder.append("<td><i>(").append(HtmlEscapers.htmlEscaper().escape(JIPipeDataInfo.getInstance(slot.value()).getName())).append(")</i></td>");
                builder.append("</tr>");
            }
            for (JIPipeOutputSlot slot : info.getOutputSlots()) {
                builder.append("<tr>");
                builder.append("<td><p style=\"background-color:#da4453; color:white;border:3px solid #da4453;border-radius:5px;text-align:center;\">Output</p></td>");
                builder.append("<td>").append("<img src=\"").append(JIPipe.getDataTypes().getIconURLFor(slot.value())).append("\"/></td>");
                builder.append("<td>").append(HtmlEscapers.htmlEscaper().escape(StringUtils.orElse(slot.slotName(), "-"))).append("</td>");
                builder.append("<td><i>(").append(HtmlEscapers.htmlEscaper().escape(JIPipeDataInfo.getInstance(slot.value()).getName())).append(")</i></td>");
                builder.append("</tr>");
            }
            builder.append("</table>\n\n");

            documentationReader.setDocument(new MarkdownDocument(builder.toString()));
        } else {
            documentationReader.setDocument(new MarkdownDocument(""));
        }
    }

    private void selectNodeInfo(JIPipeNodeInfo info) {
        if (info != null) {
            StringBuilder builder = new StringBuilder();
            builder.append("# ").append(info.getName()).append("\n\n");

            // Write description
            String description = info.getDescription().getBody();
            if (description != null && !description.isEmpty())
                builder.append(description).append("</br>");

            // Write algorithm slot info
            builder.append("<table style=\"margin-top: 10px;\">");
            for (JIPipeInputSlot slot : info.getInputSlots()) {
                builder.append("<tr>");
                builder.append("<td><p style=\"background-color:#27ae60; color:white;border:3px solid #27ae60;border-radius:5px;text-align:center;\">Input</p></td>");
                builder.append("<td>").append("<img src=\"").append(JIPipe.getDataTypes().getIconURLFor(slot.value())).append("\"/></td>");
                builder.append("<td>").append(HtmlEscapers.htmlEscaper().escape(StringUtils.orElse(slot.slotName(), "-"))).append("</td>");
                builder.append("<td><i>(").append(HtmlEscapers.htmlEscaper().escape(JIPipeDataInfo.getInstance(slot.value()).getName())).append(")</i></td>");
                builder.append("</tr>");
            }
            for (JIPipeOutputSlot slot : info.getOutputSlots()) {
                builder.append("<tr>");
                builder.append("<td><p style=\"background-color:#da4453; color:white;border:3px solid #da4453;border-radius:5px;text-align:center;\">Output</p></td>");
                builder.append("<td>").append("<img src=\"").append(JIPipe.getDataTypes().getIconURLFor(slot.value())).append("\"/></td>");
                builder.append("<td>").append(HtmlEscapers.htmlEscaper().escape(StringUtils.orElse(slot.slotName(), "-"))).append("</td>");
                builder.append("<td><i>(").append(HtmlEscapers.htmlEscaper().escape(JIPipeDataInfo.getInstance(slot.value()).getName())).append(")</i></td>");
                builder.append("</tr>");
            }
            builder.append("</table>\n\n");

            documentationReader.setDocument(new MarkdownDocument(builder.toString()));
        } else {
            documentationReader.setDocument(new MarkdownDocument(""));
        }
    }

    private List<JIPipeNodeInfo> getFilteredAndSortedInfos() {
        return RankedData.getSortedAndFilteredData(JIPipe.getNodes().getRegisteredNodeInfos().values(),
                JIPipeNodeInfo::getName,
                NodeToolBox::rankNavigationEntry,
                searchField.getSearchStrings());
    }
}
