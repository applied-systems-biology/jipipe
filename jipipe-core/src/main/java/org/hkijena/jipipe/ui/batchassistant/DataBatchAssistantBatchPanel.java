package org.hkijena.jipipe.ui.batchassistant;

import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataTable;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbench;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbenchPanel;
import org.hkijena.jipipe.ui.components.FormPanel;
import org.hkijena.jipipe.utils.TooltipUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;

public class DataBatchAssistantBatchPanel extends JIPipeProjectWorkbenchPanel {

    private final DataBatchAssistantUI dataBatchAssistantUI;
    private DataBatchAssistantDataTableUI batchTable;

    public DataBatchAssistantBatchPanel(JIPipeProjectWorkbench workbench, DataBatchAssistantUI dataBatchAssistantUI) {
        super(workbench);
        this.dataBatchAssistantUI = dataBatchAssistantUI;
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());

        FormPanel.GroupHeaderPanel groupHeaderPanel = new FormPanel.GroupHeaderPanel("Preview iteration steps", UIUtils.getIconFromResources("actions/format-list-ordered.png"), 4);
        groupHeaderPanel.addColumn(UIUtils.createBalloonHelpButton("The node will be executed for each of the following steps (1 step per row). Please review if the data is assigned as expected."));

        add(groupHeaderPanel, BorderLayout.NORTH);

        this.batchTable = new DataBatchAssistantDataTableUI(getWorkbench(), new JIPipeDataTable(JIPipeData.class));
        add(batchTable, BorderLayout.CENTER);
//        add(new JLabel("test"), BorderLayout.CENTER);
    }

    public DataBatchAssistantUI getDataBatchAssistantUI() {
        return dataBatchAssistantUI;
    }

    public void setDataTable(JIPipeDataTable dataTable) {
        batchTable.setDataTable(dataTable);
    }
}
