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

import com.google.common.collect.ImmutableList;
import org.hkijena.jipipe.api.JIPipeDataBatchGenerationResult;
import org.hkijena.jipipe.api.data.JIPipeInputDataSlot;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbenchPanel;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopFormPanel;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;

public class JIPipeDesktopDataBatchAssistantInputPreviewPanel extends JIPipeDesktopWorkbenchPanel {

    private static boolean SHOW_ALL_INPUT_DATA = true;

    private final JIPipeDesktopDataBatchAssistantUI iterationStepAssistantUI;
    private final JCheckBox showAllInputsCheck = new JCheckBox("Show all");
    private final JIPipeDesktopFormPanel contentPanel = new JIPipeDesktopFormPanel(JIPipeDesktopFormPanel.WITH_SCROLLING);
    private JIPipeDataBatchGenerationResult lastResults;

    public JIPipeDesktopDataBatchAssistantInputPreviewPanel(JIPipeDesktopWorkbench workbench, JIPipeDesktopDataBatchAssistantUI iterationStepAssistantUI) {
        super(workbench);
        this.iterationStepAssistantUI = iterationStepAssistantUI;
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());

        JIPipeDesktopFormPanel.GroupHeaderPanel groupHeaderPanel = new JIPipeDesktopFormPanel.GroupHeaderPanel("Input data", UIUtils.getIconFromResources("actions/insert-table.png"), 4);


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
            contentPanel.addWideToForm(new JIPipeDesktopDataBatchAssistantInputPreviewPanelTable(this, inputSlot, !showAllInputsCheck.isSelected()));
        }
        contentPanel.addVerticalGlue();
        applyHighlight();
    }

    public JIPipeDesktopDataBatchAssistantUI getDataBatchAssistantUI() {
        return iterationStepAssistantUI;
    }

    public void highlightResults(JIPipeDataBatchGenerationResult iterationStepGenerationResult) {
        lastResults = iterationStepGenerationResult;
        applyHighlight();
    }

    private void applyHighlight() {
        if (lastResults != null) {
            for (JIPipeDesktopFormPanel.FormPanelEntry entry : ImmutableList.copyOf(contentPanel.getEntries())) {
                Component component = entry.getContent();
                if (component instanceof JIPipeDesktopDataBatchAssistantInputPreviewPanelTable) {
                    ((JIPipeDesktopDataBatchAssistantInputPreviewPanelTable) component).highlightResults(lastResults);
                }
            }
        }
    }
}
