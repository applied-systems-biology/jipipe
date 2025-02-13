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

package org.hkijena.jipipe.desktop.app.datatracer;

import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.cache.JIPipeCache;
import org.hkijena.jipipe.api.data.JIPipeDataTable;
import org.hkijena.jipipe.api.nodes.JIPipeAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.run.JIPipeRunnable;
import org.hkijena.jipipe.api.run.JIPipeRunnableQueue;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntry;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntryLevel;
import org.hkijena.jipipe.api.validation.contexts.InternalErrorValidationReportContext;
import org.hkijena.jipipe.api.validation.contexts.UnspecifiedValidationReportContext;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWorkbenchPanel;
import org.hkijena.jipipe.desktop.app.datatable.JIPipeDesktopExtendedDataTableUI;
import org.hkijena.jipipe.desktop.app.running.JIPipeDesktopRunnableQueueButton;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopMessagePanel;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopValidityReportUI;
import org.hkijena.jipipe.desktop.commons.components.tabs.JIPipeDesktopTabPane;
import org.hkijena.jipipe.desktop.commons.components.window.JIPipeDesktopAlwaysOnTopToggle;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.data.OwningStore;
import org.jdesktop.swingx.JXStatusBar;
import org.scijava.Disposable;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

public class JIPipeDesktopDataTracerUI extends JIPipeDesktopProjectWorkbenchPanel implements JIPipeRunnable.InterruptedEventListener, JIPipeRunnable.FinishedEventListener, Disposable, JIPipeCache.ModifiedEventListener {
    private final JIPipeRunnableQueue queue = new JIPipeRunnableQueue("Data tracer");
    private final JIPipeGraphNode targetNode;

    private final String targetSlotName;
    private final UUID targetNodeUUID;
    private final JIPipeDataTable targetTable;
    private final int targetTableRow;
    private final JPanel graphContentPanel = new JPanel();
    private final JToolBar toolBar = new JToolBar();
    private final JToolBar statusBar = new JToolBar();
    private final JIPipeDesktopMessagePanel messagePanel = new JIPipeDesktopMessagePanel();
    private Map<Integer, Map<String, Map<String, JIPipeDataTable>>> resultMap = Collections.emptyMap();
    private final JIPipeDesktopTabPane tabPane = new JIPipeDesktopTabPane();

    private JScrollPane graphScrollPane;

    public JIPipeDesktopDataTracerUI(JIPipeDesktopProjectWorkbench workbench, JIPipeGraphNode targetNode, String targetSlotName, UUID targetNodeUUID, JIPipeDataTable targetTable, int targetTableRow) {
        super(workbench);
        this.targetNode = targetNode;
        this.targetSlotName = targetSlotName;
        this.targetNodeUUID = targetNodeUUID;
        this.targetTable = targetTable;
        this.targetTableRow = targetTableRow;
        initialize();

        // Run tracer
        queue.getInterruptedEventEmitter().subscribe(this);
        queue.getFinishedEventEmitter().subscribe(this);

        workbench.getProject().getCache().getModifiedEventEmitter().subscribe(this);

        rebuildContent();
    }

    public static void openWindow(JIPipeDesktopProjectWorkbench workbench, String targetId) {
        JFrame frame = new JFrame();
        frame.setTitle("JIPipe - Data trace");
        frame.setIconImage(UIUtils.getJIPipeIcon128());

        JIPipeDataTable targetTable = null;
        int targetTableRow = -1;
        String targetSlotName = null;
        JIPipeGraphNode targetNode = null;
        UUID targetNodeUUID = null;
        outer:
        for (JIPipeGraphNode graphNode : workbench.getProject().getGraph().getGraphNodes()) {
            if (graphNode instanceof JIPipeAlgorithm) {
                Map<String, JIPipeDataTable> cachedData = workbench.getProject().getCache().query(graphNode, graphNode.getUUIDInParentGraph(), new JIPipeProgressInfo());
                if (cachedData != null) {
                    for (Map.Entry<String, JIPipeDataTable> entry : cachedData.entrySet()) {
                        JIPipeDataTable dataTable = entry.getValue();
                        for (int row = 0; row < dataTable.getRowCount(); row++) {
                            if (Objects.equals(dataTable.getDataContext(row).getId(), targetId)) {
                                targetTable = dataTable;
                                targetTableRow = row;
                                targetSlotName = entry.getKey();
                                targetNode = graphNode;
                                targetNodeUUID = graphNode.getUUIDInParentGraph();
                                break outer;
                            }
                        }
                    }
                }
            }
        }

        if (targetTable != null) {
            frame.setTitle("JIPipe - Data trace - " + targetNode.getCompartmentDisplayName() + "/" + targetNode.getName() + "/" + targetSlotName + "/" + targetTableRow);
            JIPipeDesktopDataTracerUI tracerUI = new JIPipeDesktopDataTracerUI(workbench, targetNode, targetSlotName, targetNodeUUID, targetTable, targetTableRow);
            tracerUI.statusBar.add(new JIPipeDesktopAlwaysOnTopToggle(frame));
            frame.setContentPane(tracerUI);
        } else {
            JIPipeValidationReport report = new JIPipeValidationReport();
            report.add(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Error, new UnspecifiedValidationReportContext(), "Unable to find data!",
                    "The cache does not contain the specified data. Data tracing is not available.",
                    "Please update the cache",
                    "Requested ID: " + targetId));
            JIPipeDesktopValidityReportUI reportUI = new JIPipeDesktopValidityReportUI(workbench, false);
            reportUI.setReport(report);
            frame.setContentPane(reportUI);
        }

        frame.pack();
        frame.setSize(1024, 768);
        frame.setLocationRelativeTo(workbench.getWindow());
        frame.setVisible(true);
    }

    private void initialize() {
        setLayout(new BorderLayout());

        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
        topPanel.add(toolBar);
        topPanel.add(messagePanel);

        toolBar.setFloatable(false);

        add(topPanel, BorderLayout.NORTH);
        initializeToolbar(toolBar);

        graphContentPanel.setLayout(new GridBagLayout());
        graphContentPanel.setBackground(UIManager.getColor("EditorPane.background"));
        graphScrollPane = new JScrollPane(graphContentPanel);
        graphScrollPane.getVerticalScrollBar().setUnitIncrement(25);
        UIUtils.addPanningToScrollPane(graphScrollPane);

        add(tabPane, BorderLayout.CENTER);

        statusBar.setFloatable(false);
        statusBar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UIManager.getColor("MenuBar.borderColor")));
        add(statusBar, BorderLayout.SOUTH);

        statusBar.add(Box.createHorizontalGlue());

        JIPipeDesktopRunnableQueueButton tracerQueueButton = new JIPipeDesktopRunnableQueueButton(getDesktopWorkbench(), queue);
        tracerQueueButton.makeFlat();
        tracerQueueButton.setReadyLabel("Trace");
        tracerQueueButton.setTasksFinishedLabel("Trace");
        statusBar.add(tracerQueueButton);
        statusBar.addSeparator();
        JIPipeDesktopRunnableQueueButton globalQueueButton = new JIPipeDesktopRunnableQueueButton(getDesktopWorkbench());
        globalQueueButton.makeFlat();
        statusBar.add(globalQueueButton);
        statusBar.addSeparator();
    }

    private void initializeToolbar(JToolBar toolBar) {
        toolBar.add(Box.createHorizontalGlue());
        toolBar.add(UIUtils.createButton("Refresh", UIUtils.getIconFromResources("actions/view-refresh.png"), this::rebuildContent));
    }

    private void rebuildContent() {
        queue.cancelAll();
        queue.enqueue(new JIPipeDesktopCollectTraceRun(getDesktopProjectWorkbench(), targetNode, targetSlotName, targetNodeUUID, targetTable, targetTableRow));
    }

    private void refreshContent() {
        tabPane.closeAllTabs(true);

        buildGraphView();
        buildTableView();
    }

    private void buildTableView() {
        List<Integer> levels = resultMap.keySet().stream().sorted().collect(Collectors.toList());
        JIPipeDataTable tracedDataTable = new JIPipeDataTable();
        for (int i = 0; i < levels.size(); i++) {
            int level = levels.get(i);
            Map<String, Map<String, JIPipeDataTable>> nodes = resultMap.get(level);
            for (Map.Entry<String, Map<String, JIPipeDataTable>> nodeEntry : nodes.entrySet()) {
                String nodeUUID = nodeEntry.getKey();
                JIPipeGraphNode node = getDesktopProjectWorkbench().getProject().getGraph().getNodeByUUID(UUID.fromString(nodeUUID));
                for (Map.Entry<String, JIPipeDataTable> outputSlotEntry : nodeEntry.getValue().entrySet()) {
                    String outputSlotName = outputSlotEntry.getKey();
                    JIPipeDataTable dataTable = outputSlotEntry.getValue();
                    tracedDataTable.addDataFromTable(dataTable, JIPipeProgressInfo.SILENT);
                    for (int j = tracedDataTable.getRowCount() - dataTable.getRowCount(); j < tracedDataTable.getRowCount(); j++) {
                        tracedDataTable.setTextAnnotation(j, "jipipe:trace:level", String.valueOf(level));
                        tracedDataTable.setTextAnnotation(j, "jipipe:trace:node-uuid", nodeUUID);
                        tracedDataTable.setTextAnnotation(j, "jipipe:trace:node-name", node != null ? node.getName() : "N/A");
                        tracedDataTable.setTextAnnotation(j, "jipipe:trace:slot-name", outputSlotName);
                    }
                }
            }
        }

        JIPipeDesktopExtendedDataTableUI dataTableUI = new JIPipeDesktopExtendedDataTableUI(getDesktopWorkbench(),
                new OwningStore<>(tracedDataTable),
                false,
                false);
        tabPane.addTab("Table view", UIUtils.getIconFromResources("/actions/table-list.png"), dataTableUI, JIPipeDesktopTabPane.CloseMode.withoutCloseButton);

    }

    private void buildGraphView() {
        List<Integer> levels = resultMap.keySet().stream().sorted().collect(Collectors.toList());
        int numRows = 0;
        JPanel level0Panel = null;
        for (int i = 0; i < levels.size(); i++) {
            int level = levels.get(i);
            if (i != 0) {
                JLabel separatorLabel = new JLabel(UIUtils.getIconFromResources("actions/merge-down.png"));
                graphContentPanel.add(separatorLabel, new GridBagConstraints(0,
                        numRows++,
                        1,
                        1,
                        1,
                        0,
                        GridBagConstraints.NORTHWEST,
                        GridBagConstraints.HORIZONTAL,
                        UIUtils.UI_PADDING,
                        0,
                        0));
            }

            Map<String, Map<String, JIPipeDataTable>> nodes = resultMap.get(level);
            JPanel levelPanel = new JPanel();

            if (level == 0) {
                level0Panel = levelPanel;
            }

//            int tableCount = 0;

            for (Map.Entry<String, Map<String, JIPipeDataTable>> nodeEntry : nodes.entrySet()) {
                String nodeUUID = nodeEntry.getKey();
                for (Map.Entry<String, JIPipeDataTable> outputSlotEntry : nodeEntry.getValue().entrySet()) {
                    String outputSlotName = outputSlotEntry.getKey();
                    JIPipeDataTable dataTable = outputSlotEntry.getValue();

                    JIPipeDesktopDataTracerNodeOutputUI outputUI = new JIPipeDesktopDataTracerNodeOutputUI(getDesktopProjectWorkbench(), nodeUUID, outputSlotName, dataTable, level == 0);
                    levelPanel.add(outputUI);
                }
            }

//            levelPanel.setLayout(new GridLayout(1, tableCount, 8,8));
            levelPanel.setLayout(new BoxLayout(levelPanel, BoxLayout.X_AXIS));

            graphContentPanel.add(levelPanel, new GridBagConstraints(0,
                    numRows++,
                    1,
                    1,
                    1,
                    0,
                    GridBagConstraints.NORTHWEST,
                    GridBagConstraints.HORIZONTAL,
                    UIUtils.UI_PADDING,
                    0,
                    0));
        }

        graphContentPanel.add(new JPanel(), new GridBagConstraints(0,
                numRows++,
                1,
                1,
                1,
                1,
                GridBagConstraints.NORTHWEST,
                GridBagConstraints.BOTH,
                UIUtils.UI_PADDING,
                0,
                0));

        messagePanel.clear();
        if (numRows <= 1 && !targetNode.getInputSlots().isEmpty()) {
            messagePanel.addMessage(JIPipeDesktopMessagePanel.MessageType.InfoLight,
                    "The trace function cannot follow data if predecessors are missing. Use 'Cache intermediate results' instead of 'Update cache'.",
                    true,
                    true);
        }

        tabPane.addTab("Graph view", UIUtils.getIconFromResources("/actions/distribute-graph-directed.png"), graphScrollPane, JIPipeDesktopTabPane.CloseMode.withoutCloseButton);

        if (level0Panel != null) {
            JPanel finalLevel0Panel = level0Panel;
            SwingUtilities.invokeLater(() -> {
                graphScrollPane.getVerticalScrollBar().setValue(finalLevel0Panel.getY());
            });
        }
    }

    @Override
    public void onRunnableInterrupted(JIPipeRunnable.InterruptedEvent event) {
        graphContentPanel.removeAll();
        JIPipeDesktopValidityReportUI reportUI = new JIPipeDesktopValidityReportUI(getDesktopWorkbench(), false);
        JIPipeValidationReport report = new JIPipeValidationReport();
        report.add(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Error,
                new InternalErrorValidationReportContext(),
                "Error while tracing data",
                "The data tracing process was interrupted",
                "See details",
                event.getException().toString()));
        reportUI.setReport(report);
        graphContentPanel.add(reportUI);
    }

    @Override
    public void onRunnableFinished(JIPipeRunnable.FinishedEvent event) {
        if (event.getRun() instanceof JIPipeDesktopCollectTraceRun) {
            this.resultMap = ((JIPipeDesktopCollectTraceRun) event.getRun()).getResultMap();
            refreshContent();
        }
    }

    @Override
    public void dispose() {
        Disposable.super.dispose();

        queue.cancelAll();
        resultMap.clear();
    }

    @Override
    public void onCacheModified(JIPipeCache.ModifiedEvent event) {

    }
}
