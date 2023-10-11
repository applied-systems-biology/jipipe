package org.hkijena.jipipe.ui.datatracer;

import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.JIPipeRunnable;
import org.hkijena.jipipe.api.cache.JIPipeCache;
import org.hkijena.jipipe.api.data.JIPipeDataTable;
import org.hkijena.jipipe.api.nodes.JIPipeAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntry;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntryLevel;
import org.hkijena.jipipe.api.validation.contexts.InternalErrorValidationReportContext;
import org.hkijena.jipipe.api.validation.contexts.UnspecifiedValidationReportContext;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbench;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbenchPanel;
import org.hkijena.jipipe.ui.components.FormPanel;
import org.hkijena.jipipe.ui.components.JIPipeValidityReportUI;
import org.hkijena.jipipe.ui.components.MessagePanel;
import org.hkijena.jipipe.ui.components.window.AlwaysOnTopToggle;
import org.hkijena.jipipe.ui.running.JIPipeRunnerQueue;
import org.hkijena.jipipe.ui.running.JIPipeRunnerQueueButton;
import org.hkijena.jipipe.utils.UIUtils;
import org.jdesktop.swingx.JXPanel;
import org.scijava.Disposable;

import javax.swing.*;
import java.awt.*;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

public class DataTracerUI extends JIPipeProjectWorkbenchPanel implements JIPipeRunnable.InterruptedEventListener, JIPipeRunnable.FinishedEventListener, Disposable, JIPipeCache.ModifiedEventListener {
    private final JIPipeRunnerQueue queue = new JIPipeRunnerQueue("Data tracer");
    private final JIPipeGraphNode targetNode;

    private final String targetSlotName;
    private final UUID targetNodeUUID;
    private final JIPipeDataTable targetTable;
    private final int targetTableRow;
    private final JPanel contentPanel = new JPanel();
    private final JToolBar toolBar = new JToolBar();
    private final MessagePanel messagePanel = new MessagePanel();
    private Map<Integer, Map<String, Map<String, JIPipeDataTable>>> resultMap = Collections.emptyMap();

    public DataTracerUI(JIPipeProjectWorkbench workbench, JIPipeGraphNode targetNode, String targetSlotName, UUID targetNodeUUID, JIPipeDataTable targetTable, int targetTableRow) {
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

    private void initialize() {
        setLayout(new BorderLayout());

        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
        topPanel.add(toolBar);
        topPanel.add(messagePanel);

        toolBar.setFloatable(false);

        add(topPanel, BorderLayout.NORTH);
        initializeToolbar(toolBar);

        contentPanel.setLayout(new GridBagLayout());
        add(new JScrollPane(contentPanel), BorderLayout.CENTER);
    }

    private void initializeToolbar(JToolBar toolBar) {
        toolBar.add(new JIPipeRunnerQueueButton(getWorkbench(), queue));
        toolBar.add(Box.createHorizontalGlue());
        toolBar.add(UIUtils.createButton("Refresh", UIUtils.getIconFromResources("actions/view-refresh.png"), this::rebuildContent));
    }

    private void rebuildContent() {
        queue.cancelAll();
        queue.enqueue(new CollectTraceRun(getProjectWorkbench(), targetNode, targetSlotName, targetNodeUUID, targetTable, targetTableRow));
    }

    private void refreshContent() {
        contentPanel.removeAll();
        List<Integer> levels = resultMap.keySet().stream().sorted().collect(Collectors.toList());
        int numRows = 0;
        for (int i = 0; i < levels.size(); i++) {
            int level = levels.get(i);
            if(i != 0) {
                JLabel separatorLabel = new JLabel(UIUtils.getIconFromResources("actions/merge-down.png"));
                contentPanel.add(separatorLabel, new GridBagConstraints(0,
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

//            int tableCount = 0;
            for (Map.Entry<String, Map<String, JIPipeDataTable>> nodeEntry : nodes.entrySet()) {
                String nodeUUID = nodeEntry.getKey();
                for (Map.Entry<String, JIPipeDataTable> outputSlotEntry : nodeEntry.getValue().entrySet()) {
                    String outputSlotName = outputSlotEntry.getKey();
                    JIPipeDataTable dataTable = outputSlotEntry.getValue();

                    DataTrackerNodeOutputUI outputUI = new DataTrackerNodeOutputUI(getProjectWorkbench(), nodeUUID, outputSlotName, dataTable, level == 0);
                    levelPanel.add(outputUI);
                }
            }

//            levelPanel.setLayout(new GridLayout(1, tableCount, 8,8));
            levelPanel.setLayout(new BoxLayout(levelPanel, BoxLayout.X_AXIS));

            contentPanel.add(levelPanel, new GridBagConstraints(0,
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

        contentPanel.add(new JPanel(), new GridBagConstraints(0,
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
        if(numRows <= 1) {
            messagePanel.addMessage(MessagePanel.MessageType.InfoLight,
                    "The trace function cannot follow data if predecessors are missing. Use 'Cache intermediate results' instead of 'Update cache'.",
                    true,
                    true);
        }
    }

    public static void openWindow(JIPipeProjectWorkbench workbench, String targetId) {
        JFrame frame = new JFrame();
        frame.setTitle("JIPipe - Data trace");
        frame.setIconImage(UIUtils.getJIPipeIcon128());

        JIPipeDataTable targetTable = null;
        int targetTableRow = -1;
        String targetSlotName = null;
        JIPipeGraphNode targetNode = null;
        UUID targetNodeUUID = null;
        outer: for (JIPipeGraphNode graphNode : workbench.getProject().getGraph().getGraphNodes()) {
            if(graphNode instanceof JIPipeAlgorithm) {
                Map<String, JIPipeDataTable> cachedData = workbench.getProject().getCache().query(graphNode, graphNode.getUUIDInParentGraph(), new JIPipeProgressInfo());
                if(cachedData != null) {
                    for (Map.Entry<String, JIPipeDataTable> entry : cachedData.entrySet()) {
                        JIPipeDataTable dataTable = entry.getValue();
                        for (int row = 0; row < dataTable.getRowCount(); row++) {
                            if(Objects.equals(dataTable.getDataContext(row).getId(), targetId)) {
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

        if(targetTable != null) {
            frame.setTitle("JIPipe - Data trace - " + targetNode.getCompartmentDisplayName() + "/" + targetNode.getName() + "/" + targetSlotName + "/" + targetTableRow);
            DataTracerUI tracerUI = new DataTracerUI(workbench, targetNode, targetSlotName, targetNodeUUID, targetTable, targetTableRow);
            tracerUI.toolBar.add(new AlwaysOnTopToggle(frame));
            frame.setContentPane(tracerUI);
        }
        else {
            JIPipeValidationReport report = new JIPipeValidationReport();
            report.add(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Error, new UnspecifiedValidationReportContext(), "Unable to find data!",
                    "The cache does not contain the specified data. Data tracing is not available.",
                    "Please update the cache",
                    "Requested ID: " + targetId));
            JIPipeValidityReportUI reportUI = new JIPipeValidityReportUI(workbench, false);
            reportUI.setReport(report);
            frame.setContentPane(reportUI);
        }

        frame.pack();
        frame.setSize(1024,768);
        frame.setLocationRelativeTo(workbench.getWindow());
        frame.setVisible(true);
    }

    @Override
    public void onRunnableInterrupted(JIPipeRunnable.InterruptedEvent event) {
        contentPanel.removeAll();
        JIPipeValidityReportUI reportUI = new JIPipeValidityReportUI(getWorkbench(), false);
        JIPipeValidationReport report = new JIPipeValidationReport();
        report.add(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Error,
                new InternalErrorValidationReportContext(),
                "Error while tracing data",
                "The data tracing process was interrupted",
                "See details",
                event.getException().toString()));
        reportUI.setReport(report);
        contentPanel.add(reportUI);
    }

    @Override
    public void onRunnableFinished(JIPipeRunnable.FinishedEvent event) {
        if(event.getRun() instanceof CollectTraceRun) {
            this.resultMap = ((CollectTraceRun) event.getRun()).getResultMap();
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
