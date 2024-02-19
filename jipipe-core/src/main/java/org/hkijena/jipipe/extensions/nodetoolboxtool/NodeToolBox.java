package org.hkijena.jipipe.extensions.nodetoolboxtool;

import com.google.common.html.HtmlEscapers;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.AbstractJIPipeRunnable;
import org.hkijena.jipipe.api.data.JIPipeDataInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.database.*;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbench;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.JIPipeWorkbenchPanel;
import org.hkijena.jipipe.ui.components.markdown.MarkdownDocument;
import org.hkijena.jipipe.ui.components.markdown.MarkdownReader;
import org.hkijena.jipipe.ui.components.renderers.JIPipeNodeDatabaseEntryListCellRenderer;
import org.hkijena.jipipe.ui.components.search.SearchTextField;
import org.hkijena.jipipe.ui.components.window.AlwaysOnTopToggle;
import org.hkijena.jipipe.ui.running.JIPipeRunnerQueue;
import org.hkijena.jipipe.utils.AutoResizeSplitPane;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;

public class NodeToolBox extends JIPipeWorkbenchPanel {

    private final MarkdownReader documentationReader = new MarkdownReader(false);
    private final JToolBar toolBar = new JToolBar();
    private final boolean isDocked;
    private JList<JIPipeNodeDatabaseEntry> algorithmList;
    private SearchTextField searchField;
    private final JIPipeNodeDatabase database;
    private final JIPipeRunnerQueue queue = new JIPipeRunnerQueue("Node toolbox");

    public NodeToolBox(JIPipeWorkbench workbench, boolean isDocked) {
        super(workbench);
        this.isDocked = isDocked;
        this.database = workbench instanceof JIPipeProjectWorkbench ?
                ((JIPipeProjectWorkbench) workbench).getNodeDatabase() : JIPipeNodeDatabase.getInstance();
        initialize();
        reloadAlgorithmList();
    }

    private void reloadAlgorithmList() {
        queue.cancelAll();
        queue.enqueue(new ReloadListRun(this));
    }

    public static void openNewToolBoxWindow(JIPipeWorkbench workbench, Component parent) {
        NodeToolBox toolBox = new NodeToolBox(workbench, false);
        JFrame window = new JFrame();
        toolBox.getToolBar().add(new AlwaysOnTopToggle(window));
        window.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        window.setAlwaysOnTop(true);
        window.setTitle("Available nodes");
        window.setIconImage(UIUtils.getJIPipeIcon128());
        window.setContentPane(toolBox);
        window.pack();
        window.setSize(300, 700);
        window.setLocationRelativeTo(parent);
        window.setVisible(true);
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
        algorithmList.setCellRenderer(new JIPipeNodeDatabaseEntryListCellRenderer());
        algorithmList.setModel(new DefaultListModel<>());
        algorithmList.addListSelectionListener(e -> {
            if (algorithmList.getSelectedValue() instanceof CreateNewNodeByInfoDatabaseEntry) {
                selectNodeInfo(((CreateNewNodeByInfoDatabaseEntry) algorithmList.getSelectedValue()).getNodeInfo());
            } else if (algorithmList.getSelectedValue() instanceof CreateNewNodeByExampleDatabaseEntry) {
                selectNodeExample(((CreateNewNodeByExampleDatabaseEntry) algorithmList.getSelectedValue()).getExample());
            }
        });
        algorithmList.setDragEnabled(true);
        algorithmList.setTransferHandler(new NodeToolBoxTransferHandler());
        JScrollPane scrollPane = new JScrollPane(algorithmList);

        AutoResizeSplitPane splitPane = new AutoResizeSplitPane(JSplitPane.VERTICAL_SPLIT, scrollPane, documentationReader, AutoResizeSplitPane.RATIO_3_TO_1);
        add(splitPane, BorderLayout.CENTER);
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
            for (AddJIPipeInputSlot slot : info.getInputSlots()) {
                builder.append("<tr>");
                builder.append("<td><p style=\"background-color:#27ae60; color:white;border:3px solid #27ae60;border-radius:5px;text-align:center;\">Input</p></td>");
                builder.append("<td>").append("<img src=\"").append(JIPipe.getDataTypes().getIconURLFor(slot.value())).append("\"/></td>");
                builder.append("<td>").append(HtmlEscapers.htmlEscaper().escape(StringUtils.orElse(slot.slotName(), "-"))).append("</td>");
                builder.append("<td><i>(").append(HtmlEscapers.htmlEscaper().escape(JIPipeDataInfo.getInstance(slot.value()).getName())).append(")</i></td>");
                builder.append("</tr>");
            }
            for (AddJIPipeOutputSlot slot : info.getOutputSlots()) {
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
            for (AddJIPipeInputSlot slot : info.getInputSlots()) {
                builder.append("<tr>");
                builder.append("<td><p style=\"background-color:#27ae60; color:white;border:3px solid #27ae60;border-radius:5px;text-align:center;\">Input</p></td>");
                builder.append("<td>").append("<img src=\"").append(JIPipe.getDataTypes().getIconURLFor(slot.value())).append("\"/></td>");
                builder.append("<td>").append(HtmlEscapers.htmlEscaper().escape(StringUtils.orElse(slot.slotName(), "-"))).append("</td>");
                builder.append("<td><i>(").append(HtmlEscapers.htmlEscaper().escape(JIPipeDataInfo.getInstance(slot.value()).getName())).append(")</i></td>");
                builder.append("</tr>");
            }
            for (AddJIPipeOutputSlot slot : info.getOutputSlots()) {
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

    public static class ReloadListRun extends AbstractJIPipeRunnable {

        private final NodeToolBox toolBox;

        public ReloadListRun(NodeToolBox toolBox) {
            this.toolBox = toolBox;
        }

        @Override
        public String getTaskLabel() {
            return "Reload list";
        }

        @Override
        public void run() {
            DefaultListModel<JIPipeNodeDatabaseEntry> model = new DefaultListModel<>();
            for (JIPipeNodeDatabaseEntry entry : toolBox.database.query(toolBox.searchField.getText(),
                    JIPipeNodeDatabaseRole.PipelineNode,
                    false,
                    true)) {
                model.addElement(entry);
            }
            toolBox.algorithmList.setModel(model);

            if (!model.isEmpty())
                toolBox.algorithmList.setSelectedIndex(0);
            else
                toolBox.selectNodeInfo(null);
        }
    }
}
