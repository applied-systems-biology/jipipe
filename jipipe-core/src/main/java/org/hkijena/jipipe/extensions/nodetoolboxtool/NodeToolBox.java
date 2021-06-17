package org.hkijena.jipipe.extensions.nodetoolboxtool;

import com.google.common.html.HtmlEscapers;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.data.JIPipeDataInfo;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.ui.components.AlwaysOnTopToggle;
import org.hkijena.jipipe.ui.components.JIPipeNodeInfoListCellRenderer;
import org.hkijena.jipipe.ui.components.MarkdownDocument;
import org.hkijena.jipipe.ui.components.MarkdownReader;
import org.hkijena.jipipe.ui.components.SearchTextField;
import org.hkijena.jipipe.utils.RankedData;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.List;

public class NodeToolBox extends JPanel {

    private JList<JIPipeNodeInfo> algorithmList;
    private SearchTextField searchField;
    private MarkdownReader documentationReader = new MarkdownReader(false);
    private JToolBar toolBar = new JToolBar();

    public NodeToolBox() {
        initialize();
        reloadAlgorithmList();
    }

    public JToolBar getToolBar() {
        return toolBar;
    }

    private void initialize() {
        setLayout(new BorderLayout());

        JToolBar toolBar = new JToolBar();
        add(toolBar, BorderLayout.NORTH);

        searchField = new SearchTextField();
        searchField.addActionListener(e -> reloadAlgorithmList());
        toolBar.add(searchField);

        algorithmList = new JList<>();
        algorithmList.setToolTipText("Drag one or multiple entries from the list into the graph to create nodes.");
        algorithmList.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        algorithmList.setBorder(BorderFactory.createEtchedBorder());
        algorithmList.setCellRenderer(new JIPipeNodeInfoListCellRenderer());
        algorithmList.setModel(new DefaultListModel<>());
        algorithmList.addListSelectionListener(e -> {
            selectNodeInfo(algorithmList.getSelectedValue());
        });
        algorithmList.setDragEnabled(true);
        algorithmList.setTransferHandler(new NodeToolBoxTransferHandler());
        JScrollPane scrollPane = new JScrollPane(algorithmList);

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
    }

    private void reloadAlgorithmList() {
        List<JIPipeNodeInfo> infos = getFilteredAndSortedInfos();
        DefaultListModel<JIPipeNodeInfo> model = new DefaultListModel<>();
        for (JIPipeNodeInfo info : infos) {
            model.addElement(info);
        }
        algorithmList.setModel(model);

        if (!model.isEmpty())
            algorithmList.setSelectedIndex(0);
        else
            selectNodeInfo(null);
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
        } else
            documentationReader.setDocument(new MarkdownDocument(""));
    }

    private List<JIPipeNodeInfo> getFilteredAndSortedInfos() {
        return RankedData.getSortedAndFilteredData(JIPipe.getNodes().getRegisteredNodeInfos().values(),
                JIPipeNodeInfo::getName,
                NodeToolBox::rankNavigationEntry,
                searchField.getSearchStrings());
    }

    public static void openNewToolBoxWindow() {
        NodeToolBox toolBox = new NodeToolBox();
        JFrame window = new JFrame();
        toolBox.getToolBar().add(new AlwaysOnTopToggle(window));
        window.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        window.setAlwaysOnTop(true);
        window.setTitle("Available nodes");
        window.setIconImage(UIUtils.getIcon128FromResources("jipipe.png").getImage());
        window.setContentPane(new NodeToolBox());
        window.pack();
        window.setSize(300, 700);
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

        nameHayStack = nameHayStack.toLowerCase();
        menuHayStack = menuHayStack.toLowerCase();
        descriptionHayStack = descriptionHayStack.toLowerCase();

        int[] ranks = new int[3];

        for (String string : searchStrings) {
            if (nameHayStack.contains(string.toLowerCase()))
                --ranks[0];
            if (menuHayStack.contains(string.toLowerCase()))
                --ranks[1];
            if (descriptionHayStack.contains(string.toLowerCase()))
                --ranks[2];
        }

        if (ranks[0] == 0 && ranks[1] == 0 && ranks[2] == 0)
            return null;

        return ranks;
    }
}
