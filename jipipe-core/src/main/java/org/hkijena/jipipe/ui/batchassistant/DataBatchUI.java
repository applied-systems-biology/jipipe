/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.ui.batchassistant;

import org.hkijena.jipipe.api.algorithm.JIPipeGraphNode;
import org.hkijena.jipipe.api.algorithm.JIPipeMergingDataBatch;
import org.hkijena.jipipe.api.data.JIPipeAnnotation;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbench;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbenchPanel;
import org.hkijena.jipipe.ui.components.FormPanel;
import org.hkijena.jipipe.ui.registries.JIPipeUIDatatypeRegistry;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.util.Collections;
import java.util.Comparator;
import java.util.Set;
import java.util.stream.Collectors;

public class DataBatchUI extends JIPipeProjectWorkbenchPanel {

    private final JIPipeGraphNode node;
    private final JIPipeMergingDataBatch batch;
    private ButtonGroup dataDisplayToggle;
    private JPanel currentDataDisplay;

    /**
     * @param workbenchUI The workbench UI
     * @param node the node
     * @param batch the batch to be displayed
     */
    public DataBatchUI(JIPipeProjectWorkbench workbenchUI, JIPipeGraphNode node, JIPipeMergingDataBatch batch) {
        super(workbenchUI);
        this.node = node;
        this.batch = batch;
        dataDisplayToggle = new ButtonGroup() {
            @Override
            public void setSelected(ButtonModel model, boolean selected) {
                if (selected) {
                    super.setSelected(model, selected);
                } else {
                    clearSelection();
                    DataBatchUI.this.remove(currentDataDisplay);
                    DataBatchUI.this.revalidate();
                    DataBatchUI.this.repaint();
                }
            }
        };
        initialize();
    }

    private void initialize() {
        setBorder(BorderFactory.createLineBorder(Color.GRAY, 1, true));
        setLayout(new BorderLayout());
        JLabel dataBatchName = new JLabel("Data batch");
        dataBatchName.setBorder(BorderFactory.createEmptyBorder(4,4,4,4));
        dataBatchName.setFont(new Font(Font.DIALOG, Font.BOLD, 16));
        add(dataBatchName, BorderLayout.NORTH);

        FormPanel metaDataList = new FormPanel(null, FormPanel.NONE);
        for (JIPipeAnnotation annotation : batch.getAnnotations().values()) {
            JLabel title = new JLabel(annotation.getName(), UIUtils.getIconFromResources("annotation.png"), JLabel.LEFT);
            JTextField content = UIUtils.makeReadonlyBorderlessTextField(annotation.getValue());
            metaDataList.addToForm(content, title, null);
        }
        metaDataList.addVerticalGlue();
        add(metaDataList, BorderLayout.CENTER);

        FormPanel dataList = new FormPanel(null, FormPanel.NONE);
        for (JIPipeDataSlot slot : node.getInputSlots()) {
            Set<Integer> rows = batch.getInputSlotRows().getOrDefault(slot, Collections.emptySet());
            Icon dataTypeIcon = JIPipeUIDatatypeRegistry.getInstance().getIconFor(slot.getAcceptedDataType());
            JToggleButton toggleButton = new JToggleButton(slot.getName(), dataTypeIcon);
            UIUtils.makeFlat(toggleButton);
            if(!rows.isEmpty()) {
                toggleButton.setHorizontalAlignment(SwingConstants.LEFT);
                toggleButton.addActionListener(e -> {
                    if (toggleButton.isSelected())
                        displayData(slot, batch.getInputSlotRows().get(slot));
                });
            }
            else {
                toggleButton.setEnabled(false);
            }
            dataDisplayToggle.add(toggleButton);

            JLabel statusLabel;
            if(rows.isEmpty()) {
                statusLabel = new JLabel("Missing!", UIUtils.getIconFromResources("error.png"), JLabel.LEFT);
            }
            else {
                statusLabel = new JLabel(rows.size() == 1 ? "1 item" : rows.size() + " items", UIUtils.getIconFromResources("check-circle-green.png"), JLabel.LEFT);
            }

            dataList.addToForm(toggleButton, statusLabel, null);
        }
        add(dataList, BorderLayout.EAST);
    }

    private void displayData(JIPipeDataSlot slot, Set<Integer> rows) {
        if(currentDataDisplay != null) {
            remove(currentDataDisplay);
        }

        currentDataDisplay = new JPanel(new BorderLayout());
        JPanel separator = new JPanel();
        separator.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(4,0,0,0),
                BorderFactory.createMatteBorder(1,0,0,0, Color.GRAY)));
        currentDataDisplay.add(separator, BorderLayout.NORTH);

        JIPipeDataSlot copySlot = new JIPipeDataSlot(slot.getDefinition(), slot.getNode());
        for (int row : rows) {
            copySlot.addData(slot.getData(row, JIPipeData.class), slot.getAnnotations(row));
        }

        DataSlotTableUI tableUI = new DataSlotTableUI(getProjectWorkbench(), copySlot);
        currentDataDisplay.add(tableUI, BorderLayout.CENTER);
        add(currentDataDisplay, BorderLayout.SOUTH);
        revalidate();
        repaint();
    }
}
