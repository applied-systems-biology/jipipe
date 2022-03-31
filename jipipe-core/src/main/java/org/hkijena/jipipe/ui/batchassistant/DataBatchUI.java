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

import org.apache.commons.lang.WordUtils;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeDataTable;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.nodes.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeMergingDataBatch;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbench;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbenchPanel;
import org.hkijena.jipipe.ui.cache.JIPipeExtendedDataTableInfoUI;
import org.hkijena.jipipe.ui.components.FormPanel;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.util.Collections;
import java.util.Set;

public class DataBatchUI extends JIPipeProjectWorkbenchPanel {

    private final JIPipeGraphNode node;
    private final JIPipeMergingDataBatch batch;

    /**
     * @param workbenchUI The workbench UI
     * @param node        the node
     * @param batch       the batch to be displayed
     */
    public DataBatchUI(JIPipeProjectWorkbench workbenchUI, JIPipeGraphNode node, JIPipeMergingDataBatch batch) {
        super(workbenchUI);
        this.node = node;
        this.batch = batch;
        initialize();
    }

    private void initialize() {
        setBorder(BorderFactory.createLineBorder(Color.GRAY, 1, true));
        setLayout(new BorderLayout());
        JLabel dataBatchName = new JLabel("Data batch");
        dataBatchName.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        dataBatchName.setFont(new Font(Font.DIALOG, Font.BOLD, 16));
        add(dataBatchName, BorderLayout.NORTH);

        FormPanel metaDataList = new FormPanel(null, FormPanel.NONE);
        for (JIPipeTextAnnotation annotation : batch.getMergedTextAnnotations().values()) {
            JLabel title = new JLabel(annotation.getName(), UIUtils.getIconFromResources("data-types/annotation.png"), JLabel.LEFT);
            JTextField content = UIUtils.makeReadonlyBorderlessTextField(WordUtils.abbreviate(annotation.getValue(), 50, 70, " ..."));
            content.setToolTipText(annotation.getValue());
            metaDataList.addToForm(content, title, null);
        }
        metaDataList.addVerticalGlue();
        add(metaDataList, BorderLayout.CENTER);

        FormPanel dataList = new FormPanel(null, FormPanel.NONE);
        for (JIPipeDataSlot slot : node.getDataInputSlots()) {
            Set<Integer> rows = batch.getInputSlotRows().getOrDefault(slot, Collections.emptySet());
            Icon dataTypeIcon = JIPipe.getDataTypes().getIconFor(slot.getAcceptedDataType());
            JButton displayButton = new JButton(slot.getName(), dataTypeIcon);
            UIUtils.makeFlat(displayButton);
            if (!rows.isEmpty()) {
                displayButton.setHorizontalAlignment(SwingConstants.LEFT);
                displayButton.addActionListener(e -> {
                    if (displayButton.isSelected())
                        displayData(slot, batch.getInputSlotRows().get(slot));
                });
            } else {
                displayButton.setEnabled(false);
            }
            displayButton.addActionListener(e -> displayData(slot, rows));

            JLabel statusLabel;
            if (rows.isEmpty()) {
                statusLabel = new JLabel("Missing!", UIUtils.getIconFromResources("emblems/vcs-conflicting.png"), JLabel.LEFT);
            } else {
                Icon icon = (node instanceof JIPipeIteratingAlgorithm && rows.size() > 1) ? UIUtils.getIconFromResources("emblems/vcs-conflicting.png") : UIUtils.getIconFromResources("emblems/vcs-normal.png");
                statusLabel = new JLabel(rows.size() == 1 ? "1 item" : rows.size() + " items", icon, JLabel.LEFT);
            }

            dataList.addToForm(displayButton, statusLabel, null);
        }
        add(dataList, BorderLayout.EAST);
    }

    private void displayData(JIPipeDataSlot slot, Set<Integer> rows) {
        JIPipeDataTable copySlot = new JIPipeDataTable(slot.getAcceptedDataType());
        for (int row : rows) {
            copySlot.addData(slot.getVirtualData(row), slot.getTextAnnotations(row), JIPipeTextAnnotationMergeMode.Merge);
        }
        JIPipeExtendedDataTableInfoUI tableUI = new JIPipeExtendedDataTableInfoUI(getProjectWorkbench(), copySlot, true);
//        DataSlotTableUI tableUI = new DataSlotTableUI(getProjectWorkbench(), copySlot);
        JFrame frame = new JFrame("Data batch contents");
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        frame.setIconImage(UIUtils.getIcon128FromResources("jipipe.png").getImage());
        frame.setContentPane(tableUI);
        frame.pack();
        frame.setSize(800, 600);
        frame.setLocationRelativeTo(getWorkbench().getWindow());
        frame.setVisible(true);
    }
}
