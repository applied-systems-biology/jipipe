package org.hkijena.jipipe.ui.batchassistant;

import com.google.common.collect.ImmutableList;
import org.hkijena.jipipe.api.JIPipeDataBatchGenerationResult;
import org.hkijena.jipipe.api.data.JIPipeInputDataSlot;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.JIPipeWorkbenchPanel;
import org.hkijena.jipipe.ui.components.FormPanel;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;

public class DataBatchAssistantInputPreviewPanel extends JIPipeWorkbenchPanel {

    private static boolean SHOW_ALL_INPUT_DATA = true;

    private final DataBatchAssistantUI iterationStepAssistantUI;
    private final JCheckBox showAllInputsCheck = new JCheckBox("Show all");
    private final FormPanel contentPanel = new FormPanel(FormPanel.WITH_SCROLLING);
    private JIPipeDataBatchGenerationResult lastResults;

    public DataBatchAssistantInputPreviewPanel(JIPipeWorkbench workbench, DataBatchAssistantUI iterationStepAssistantUI) {
        super(workbench);
        this.iterationStepAssistantUI = iterationStepAssistantUI;
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

        groupHeaderPanel.addColumn(UIUtils.createBalloonHelpButton("An overview of the available input data tables and which columns are considered for assigning data into iteration steps.\n" +
                "A green column indicates that the text annotation is used for determining which rows are put together into an iteration step.\n" +
                "If a row has a unique iteration step, the associated column and its values are highlighted with a color."));

        add(groupHeaderPanel, BorderLayout.NORTH);

        add(contentPanel, BorderLayout.CENTER);
    }

    public void updateStatus() {
        contentPanel.clear();
        for (JIPipeInputDataSlot inputSlot : iterationStepAssistantUI.getAlgorithm().getInputSlots()) {
            contentPanel.addWideToForm(new DataBatchAssistantInputPreviewPanelTable(this, inputSlot, !showAllInputsCheck.isSelected()));
        }
        contentPanel.addVerticalGlue();
        applyHighlight();
    }

    public DataBatchAssistantUI getDataBatchAssistantUI() {
        return iterationStepAssistantUI;
    }

    public void highlightResults(JIPipeDataBatchGenerationResult iterationStepGenerationResult) {
        lastResults = iterationStepGenerationResult;
        applyHighlight();
    }

    private void applyHighlight() {
        if(lastResults != null) {
            for (FormPanel.FormPanelEntry entry : ImmutableList.copyOf(contentPanel.getEntries())) {
                Component component = entry.getContent();
                if (component instanceof DataBatchAssistantInputPreviewPanelTable) {
                    ((DataBatchAssistantInputPreviewPanelTable) component).highlightResults(lastResults);
                }
            }
        }
    }
}
