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

package org.hkijena.jipipe.desktop.app.batchassistant;

import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataTable;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWorkbenchPanel;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopFormPanel;
import org.hkijena.jipipe.utils.UIUtils;

import java.awt.*;

public class JIPipeDesktopDataBatchAssistantBatchPanel extends JIPipeDesktopProjectWorkbenchPanel {

    private final JIPipeDesktopDataBatchAssistantUI iterationStepAssistantUI;
    private JIPipeDesktopDataBatchAssistantDataTableUI batchTable;

    public JIPipeDesktopDataBatchAssistantBatchPanel(JIPipeDesktopProjectWorkbench workbench, JIPipeDesktopDataBatchAssistantUI iterationStepAssistantUI) {
        super(workbench);
        this.iterationStepAssistantUI = iterationStepAssistantUI;
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());

        JIPipeDesktopFormPanel.GroupHeaderPanel groupHeaderPanel = new JIPipeDesktopFormPanel.GroupHeaderPanel("Preview iteration steps", UIUtils.getIconFromResources("actions/format-list-ordered.png"), 4);
        groupHeaderPanel.addToTitlePanel(UIUtils.createBalloonHelpButton("The node will be executed for each of the following steps (1 step per row). Please review if the data is assigned as expected."));

        add(groupHeaderPanel, BorderLayout.NORTH);

        this.batchTable = new JIPipeDesktopDataBatchAssistantDataTableUI(getDesktopWorkbench(), new JIPipeDataTable(JIPipeData.class));
        add(batchTable, BorderLayout.CENTER);
//        add(new JLabel("test"), BorderLayout.CENTER);
    }

    public JIPipeDesktopDataBatchAssistantUI getDataBatchAssistantUI() {
        return iterationStepAssistantUI;
    }

    public void setDataTable(JIPipeDataTable dataTable) {
        batchTable.setDataTable(dataTable);
    }
}
