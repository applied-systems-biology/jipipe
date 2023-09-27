package org.hkijena.jipipe.ui.batchassistant;

import org.hkijena.jipipe.api.JIPipeDataBatchGenerationResult;
import org.hkijena.jipipe.api.data.JIPipeInputDataSlot;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.JIPipeWorkbenchPanel;
import org.hkijena.jipipe.ui.components.FormPanel;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;

public class DataBatchAssistantInputPreviewPanel extends JIPipeWorkbenchPanel {

    private static boolean SHOW_ALL_INPUT_DATA = false;

    private final DataBatchAssistantUI dataBatchAssistantUI;
    private final JCheckBox showAllInputsCheck = new JCheckBox("Show all");
    private final FormPanel contentPanel = new FormPanel(FormPanel.WITH_SCROLLING);
    private JIPipeDataBatchGenerationResult lastResults;

    public DataBatchAssistantInputPreviewPanel(JIPipeWorkbench workbench, DataBatchAssistantUI dataBatchAssistantUI) {
        super(workbench);
        this.dataBatchAssistantUI = dataBatchAssistantUI;
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());

        FormPanel.GroupHeaderPanel groupHeaderPanel = new FormPanel.GroupHeaderPanel("Input data", UIUtils.getIconFromResources("actions/insert-table.png"), 4);


        showAllInputsCheck.setOpaque(false);
        showAllInputsCheck.setSelected(SHOW_ALL_INPUT_DATA);
        showAllInputsCheck.addActionListener(e -> {
            SHOW_ALL_INPUT_DATA = showAllInputsCheck.isSelected();
            updateStatus();
        });
        groupHeaderPanel.addColumn(showAllInputsCheck);

        groupHeaderPanel.addColumn(UIUtils.createBalloonHelpButton("An overview of the available input data tables and which columns are considered for assigning data into iteration steps"));

        add(groupHeaderPanel, BorderLayout.NORTH);

        add(contentPanel, BorderLayout.CENTER);
    }

    public void updateStatus() {
        contentPanel.clear();
        for (JIPipeInputDataSlot inputSlot : dataBatchAssistantUI.getAlgorithm().getInputSlots()) {
            contentPanel.addWideToForm(new DataBatchAssistantInputPreviewPanelTable(this, inputSlot, !showAllInputsCheck.isSelected()));
        }
        contentPanel.addVerticalGlue();
        applyHighlight();
    }

    public DataBatchAssistantUI getDataBatchAssistantUI() {
        return dataBatchAssistantUI;
    }

    public void highlightResults(JIPipeDataBatchGenerationResult dataBatchGenerationResult) {
        lastResults = dataBatchGenerationResult;
        applyHighlight();
    }

    private void applyHighlight() {
        if(lastResults != null) {
            for (FormPanel.FormPanelEntry entry : contentPanel.getEntries()) {
                Component component = entry.getContent();
                if (component instanceof DataBatchAssistantInputPreviewPanelTable) {
                    ((DataBatchAssistantInputPreviewPanelTable) component).highlightResults(lastResults);
                }
            }
        }
    }
}
