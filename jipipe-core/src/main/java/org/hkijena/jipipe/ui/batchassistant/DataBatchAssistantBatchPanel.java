package org.hkijena.jipipe.ui.batchassistant;

import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataTable;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbench;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbenchPanel;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;

public class DataBatchAssistantBatchPanel extends JIPipeProjectWorkbenchPanel {

    private final DataBatchAssistantUI dataBatchAssistantUI;
    private JLabel batchPreviewNumberLabel;
    private JLabel batchPreviewMissingLabel;
    private JLabel batchPreviewDuplicateLabel;
    private DataBatchAssistantDataTableUI batchTable;

    public DataBatchAssistantBatchPanel(JIPipeProjectWorkbench workbench, DataBatchAssistantUI dataBatchAssistantUI) {
        super(workbench);
        this.dataBatchAssistantUI = dataBatchAssistantUI;
        initialize();
    }

    private void initialize() {
        JToolBar batchPreviewOverview = new JToolBar();

        batchPreviewNumberLabel = new JLabel();
        batchPreviewOverview.add(batchPreviewNumberLabel);
        batchPreviewOverview.add(Box.createHorizontalGlue());

        batchPreviewMissingLabel = new JLabel("Missing items found!", UIUtils.getIconFromResources("emblems/warning.png"), JLabel.LEFT);
        batchPreviewOverview.add(batchPreviewMissingLabel);

        batchPreviewDuplicateLabel = new JLabel("Multiple items per batch", UIUtils.getIconFromResources("emblems/emblem-information.png"), JLabel.LEFT);
        batchPreviewOverview.add(batchPreviewDuplicateLabel);

        batchPreviewOverview.setFloatable(false);
        add(batchPreviewOverview, BorderLayout.NORTH);

        this.batchTable = new DataBatchAssistantDataTableUI(getWorkbench(), new JIPipeDataTable(JIPipeData.class));
        add(batchTable, BorderLayout.CENTER);
    }

    public DataBatchAssistantUI getDataBatchAssistantUI() {
        return dataBatchAssistantUI;
    }

    public void setDataTable(JIPipeDataTable dataTable) {
        batchTable.setDataTable(dataTable);
    }
}
