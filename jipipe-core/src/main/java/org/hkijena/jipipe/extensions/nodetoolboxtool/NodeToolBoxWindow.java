package org.hkijena.jipipe.extensions.nodetoolboxtool;

import com.google.common.html.HtmlEscapers;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.compat.SingleImageJAlgorithmRun;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.ui.components.JIPipeNodeInfoListCellRenderer;
import org.hkijena.jipipe.ui.components.MarkdownDocument;
import org.hkijena.jipipe.ui.components.MarkdownReader;
import org.hkijena.jipipe.ui.components.SearchTextField;
import org.hkijena.jipipe.ui.grapheditor.JIPipeNodeUI;
import org.hkijena.jipipe.utils.RankedData;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class NodeToolBoxWindow extends JFrame {

    private JList<JIPipeNodeInfo> algorithmList;
    private SearchTextField searchField;
    private MarkdownReader documentationReader = new MarkdownReader(false);

    public NodeToolBoxWindow() {
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setAlwaysOnTop(true);
        initialize();
        reloadAlgorithmList();
    }

    public static NodeToolBoxWindow openNewToolBox() {
        NodeToolBoxWindow window = new NodeToolBoxWindow();
        window.setTitle("Available nodes");
        window.setIconImage(UIUtils.getIcon128FromResources("jipipe.png").getImage());
        window.pack();
        window.setSize(300,700);
        window.setVisible(true);
        return window;
    }

    private void initialize() {
        setContentPane(new JPanel(new BorderLayout()));

        JToolBar toolBar = new JToolBar();
        getContentPane().add(toolBar, BorderLayout.NORTH);

        searchField = new SearchTextField();
        searchField.addActionListener(e -> reloadAlgorithmList());
        toolBar.add(searchField);

        JToggleButton alwaysOnTopToggle = new JToggleButton(UIUtils.getIconFromResources("actions/window-pin.png"));
        alwaysOnTopToggle.setSelected(isAlwaysOnTop());
        alwaysOnTopToggle.addActionListener(e -> setAlwaysOnTop(alwaysOnTopToggle.isSelected()));
        toolBar.add(alwaysOnTopToggle);

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

        getContentPane().add(splitPane, BorderLayout.CENTER);
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
        if(info != null) {
            StringBuilder builder = new StringBuilder();
            builder.append("# ").append(info.getName()).append("\n\n");
            // Write algorithm slot info
            builder.append("<table>");
            {
                List<JIPipeInputSlot> inputSlots = info.getInputSlots();
                List<JIPipeOutputSlot> outputSlots = info.getOutputSlots();

                int displayedSlots = Math.max(inputSlots.size(), outputSlots.size());
                if (displayedSlots > 0) {
                    builder.append("<tr><td><i>Input</i></td><td><i>Output</i></td></tr>");
                    for (int i = 0; i < displayedSlots; ++i) {
                        Class<? extends JIPipeData> inputSlot = i < inputSlots.size() ? inputSlots.get(i).value() : null;
                        Class<? extends JIPipeData> outputSlot = i < outputSlots.size() ? outputSlots.get(i).value() : null;
                        builder.append("<tr>");
                        builder.append("<td>");
                        if (inputSlot != null) {
                            builder.append(StringUtils.createIconTextHTMLTableElement(JIPipeData.getNameOf(inputSlot), JIPipe.getDataTypes().getIconURLFor(inputSlot)));
                        }
                        builder.append("</td>");
                        builder.append("<td>");
                        if (outputSlot != null) {
                            builder.append(StringUtils.createRightIconTextHTMLTableElement(JIPipeData.getNameOf(outputSlot), JIPipe.getDataTypes().getIconURLFor(outputSlot)));
                        }
                        builder.append("</td>");
                        builder.append("</tr>");
                    }
                }
            }
            builder.append("</table>\n\n");

            // Write description
            String description = info.getDescription();
            if (description != null && !description.isEmpty())
                builder.append(HtmlEscapers.htmlEscaper().escape(description)).append("</br>");


            documentationReader.setDocument(new MarkdownDocument(builder.toString()));
        }
        else
            documentationReader.setDocument(new MarkdownDocument(""));
    }

    private List<JIPipeNodeInfo> getFilteredAndSortedInfos() {
        return RankedData.getSortedAndFilteredData( JIPipe.getNodes().getRegisteredNodeInfos().values(),
                JIPipeNodeInfo::getName,
                NodeToolBoxWindow::rankNavigationEntry,
                searchField.getSearchStrings());
    }

    private static int[] rankNavigationEntry(JIPipeNodeInfo info, String[] searchStrings) {
        if (searchStrings == null || searchStrings.length == 0)
            return new int[0];
        String nameHayStack;
        String descriptionHayStack;
        if (info.isHidden())
            return null;
        nameHayStack = StringUtils.orElse(info.getName(), "").toLowerCase();
        descriptionHayStack = StringUtils.orElse(info.getDescription(), "").toLowerCase();

        nameHayStack = nameHayStack.toLowerCase();
        descriptionHayStack = descriptionHayStack.toLowerCase();

        int[] ranks = new int[2];

        for (String string : searchStrings) {
            if (nameHayStack.contains(string.toLowerCase()))
                --ranks[0];
            if (descriptionHayStack.contains(string.toLowerCase()))
                --ranks[1];
        }

        if (ranks[0] == 0 && ranks[1] == 0)
            return null;

        return ranks;
    }
}
