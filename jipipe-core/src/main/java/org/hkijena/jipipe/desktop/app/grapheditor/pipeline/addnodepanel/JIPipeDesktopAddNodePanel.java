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

package org.hkijena.jipipe.desktop.app.grapheditor.pipeline.addnodepanel;

import com.google.common.html.HtmlEscapers;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.AbstractJIPipeRunnable;
import org.hkijena.jipipe.api.data.JIPipeDataInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.database.*;
import org.hkijena.jipipe.api.run.JIPipeRunnableQueue;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbenchPanel;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphCanvasUI;
import org.hkijena.jipipe.desktop.commons.components.layouts.JIPipeDesktopWrapLayout;
import org.hkijena.jipipe.desktop.commons.components.markup.JIPipeDesktopMarkdownReader;
import org.hkijena.jipipe.desktop.commons.components.renderers.JIPipeDesktopNodeDatabaseEntryListCellRenderer;
import org.hkijena.jipipe.desktop.commons.components.search.JIPipeDesktopSearchTextField;
import org.hkijena.jipipe.desktop.commons.components.window.JIPipeDesktopAlwaysOnTopToggle;
import org.hkijena.jipipe.plugins.parameters.library.markup.MarkdownText;
import org.hkijena.jipipe.utils.JIPipeDesktopSplitPane;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Comparator;

/**
 * New and improved node tool box
 */
public class JIPipeDesktopAddNodePanel extends JIPipeDesktopWorkbenchPanel {

    private final JToolBar toolBar = new JToolBar();
    private final JIPipeNodeDatabase database;
    private final JIPipeRunnableQueue queue = new JIPipeRunnableQueue("Node toolbox");
    private JList<JIPipeNodeDatabaseEntry> algorithmList;
    private JIPipeDesktopSearchTextField searchField;
    private JScrollPane scrollPane;
    private final JPanel mainCategoriesPanel = new JPanel();
    private final JIPipeDesktopGraphCanvasUI canvasUI;

    public JIPipeDesktopAddNodePanel(JIPipeDesktopWorkbench workbench, JIPipeDesktopGraphCanvasUI canvasUI) {
        super(workbench);
        this.database = workbench instanceof JIPipeDesktopProjectWorkbench ?
                ((JIPipeDesktopProjectWorkbench) workbench).getNodeDatabase() : JIPipeNodeDatabase.getInstance();
        this.canvasUI = canvasUI;
        initialize();
        reloadAlgorithmList();
    }

    private void reloadAlgorithmList() {
        queue.cancelAll();
        queue.enqueue(new ReloadListRun(this));
    }

    public JToolBar getToolBar() {
        return toolBar;
    }

    private void initialize() {
        setLayout(new GridBagLayout());

        initializeToolbar();
        initializeMainCategoryPanel();
        initializeAlgorithmList();
    }

    private void initializeMainCategoryPanel() {
        mainCategoriesPanel.setLayout(new JIPipeDesktopWrapLayout(FlowLayout.LEFT));
        JIPipe.getNodes().getRegisteredCategories().values().stream().sorted(Comparator.comparing(JIPipeNodeTypeCategory::getUIOrder)).forEach(category -> {
            if(category.isVisibleInGraphCompartment()) {
                JButton categoryButton = new JButton(category.getName(), category.getIcon());
                categoryButton.setFont(new Font(Font.DIALOG, Font.PLAIN, 11));
                mainCategoriesPanel.add(categoryButton);
            }
        });
        add(mainCategoriesPanel, new GridBagConstraints(0,
                1,
                1,
                1,
                1,
                0,
                GridBagConstraints.NORTHWEST,
                GridBagConstraints.HORIZONTAL,
                new Insets(8, 8, 8, 8),
                0,
                0));
    }

    private void initializeToolbar() {
        add(toolBar, new GridBagConstraints(0,
                0,
                1,
                1,
                1,
                0,
                GridBagConstraints.NORTHWEST,
                GridBagConstraints.HORIZONTAL,
                new Insets(0, 0, 0, 0),
                0,
                0));

        searchField = new JIPipeDesktopSearchTextField();
        searchField.addActionListener(e -> reloadAlgorithmList());
        toolBar.add(searchField);
    }

    private void initializeAlgorithmList() {
        algorithmList = new JList<>();
        algorithmList.setToolTipText("Drag one or multiple entries from the list into the graph to create nodes.");
        algorithmList.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        algorithmList.setBorder(UIUtils.createEmptyBorder(8));
        algorithmList.setOpaque(false);
        algorithmList.setModel(new DefaultListModel<>());
        algorithmList.setDragEnabled(true);
        algorithmList.setTransferHandler(new JIPipeDesktopAddNodeTransferHandler());
        scrollPane = new JScrollPane(algorithmList);
        algorithmList.setCellRenderer(new JIPipeDesktopAddNodePanelEntryListCellRenderer(scrollPane));
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        add(scrollPane, new GridBagConstraints(0,
                5,
                1,
                1,
                1,
                1,
                GridBagConstraints.NORTHWEST,
                GridBagConstraints.BOTH,
                new Insets(8, 8, 8, 8),
                0,
                0));

        algorithmList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if(SwingUtilities.isLeftMouseButton(e)) {
                    if(e.getClickCount() == 2) {
                        insertAtCursor(algorithmList.getSelectedValue());
                    }
                }
            }
        });
    }

    private void insertAtCursor(JIPipeNodeDatabaseEntry entry) {
        entry.addToGraph(canvasUI);
    }

    public static class ReloadListRun extends AbstractJIPipeRunnable {

        private final JIPipeDesktopAddNodePanel toolBox;

        public ReloadListRun(JIPipeDesktopAddNodePanel toolBox) {
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

            if (!model.isEmpty()) {
                SwingUtilities.invokeLater(() -> {
                    toolBox.algorithmList.setSelectedIndex(0);
                    toolBox.scrollPane.getVerticalScrollBar().setValue(0);
                });
            }
        }
    }
}
