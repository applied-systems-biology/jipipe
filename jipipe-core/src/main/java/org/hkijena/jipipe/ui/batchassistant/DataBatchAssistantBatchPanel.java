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

    private final DataBatchAssistantUI iterationStepAssistantUI;
    private DataBatchAssistantDataTableUI batchTable;

    public DataBatchAssistantBatchPanel(JIPipeProjectWorkbench workbench, DataBatchAssistantUI iterationStepAssistantUI) {
        super(workbench);
        this.iterationStepAssistantUI = iterationStepAssistantUI;
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
        return iterationStepAssistantUI;
    }

    public void setDataTable(JIPipeDataTable dataTable) {
        batchTable.setDataTable(dataTable);
    }
}
