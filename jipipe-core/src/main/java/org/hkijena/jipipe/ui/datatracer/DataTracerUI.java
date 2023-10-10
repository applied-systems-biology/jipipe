package org.hkijena.jipipe.ui.datatracer;

import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeDataTable;
import org.hkijena.jipipe.api.nodes.JIPipeAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntry;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntryLevel;
import org.hkijena.jipipe.api.validation.contexts.UnspecifiedValidationReportContext;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbench;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.JIPipeWorkbenchPanel;
import org.hkijena.jipipe.ui.components.JIPipeValidityReportUI;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class DataTracerUI extends JIPipeWorkbenchPanel {
    private final JIPipeGraphNode targetNode;
    private final UUID targetNodeUUID;
    private final JIPipeDataTable targetTable;
    private final int targetTableRow;

    public DataTracerUI(JIPipeWorkbench workbench, JIPipeGraphNode targetNode, UUID targetNodeUUID, JIPipeDataTable targetTable, int targetTableRow) {
        super(workbench);
        this.targetNode = targetNode;
        this.targetNodeUUID = targetNodeUUID;
        this.targetTable = targetTable;
        this.targetTableRow = targetTableRow;
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
            frame.setContentPane(new DataTracerUI(workbench, targetNode, targetNodeUUID, targetTable, targetTableRow));
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
}
