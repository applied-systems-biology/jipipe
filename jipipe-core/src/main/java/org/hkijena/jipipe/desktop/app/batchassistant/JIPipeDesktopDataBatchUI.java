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

import org.apache.commons.text.WordUtils;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeDataTable;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeMultiIterationStep;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWorkbenchPanel;
import org.hkijena.jipipe.desktop.app.datatable.JIPipeDesktopExtendedDataTableUI;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopFormPanel;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.data.OwningStore;

import javax.swing.*;
import java.awt.*;
import java.util.Collections;
import java.util.Set;

public class JIPipeDesktopDataBatchUI extends JIPipeDesktopProjectWorkbenchPanel {

    private final JIPipeGraphNode node;
    private final JIPipeMultiIterationStep batch;

    /**
     * @param workbenchUI The workbench UI
     * @param node        the node
     * @param batch       the batch to be displayed
     */
    public JIPipeDesktopDataBatchUI(JIPipeDesktopProjectWorkbench workbenchUI, JIPipeGraphNode node, JIPipeMultiIterationStep batch) {
        super(workbenchUI);
        this.node = node;
        this.batch = batch;
        initialize();
    }

    private void initialize() {
        setBorder(UIUtils.createControlBorder());
        setLayout(new BorderLayout());
        JLabel iterationStepName = new JLabel("Data batch");
        iterationStepName.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        iterationStepName.setFont(new Font(Font.DIALOG, Font.BOLD, 16));
        add(iterationStepName, BorderLayout.NORTH);

        JIPipeDesktopFormPanel metaDataList = new JIPipeDesktopFormPanel(null, JIPipeDesktopFormPanel.NONE);
        for (JIPipeTextAnnotation annotation : batch.getMergedTextAnnotations().values()) {
            JLabel title = new JLabel(annotation.getName(), UIUtils.getIconFromResources("data-types/annotation.png"), JLabel.LEFT);
            JTextField content = UIUtils.makeReadonlyBorderlessTextField(WordUtils.abbreviate(annotation.getValue(), 50, 70, " ..."));
            content.setToolTipText(annotation.getValue());
            metaDataList.addToForm(content, title, null);
        }
        metaDataList.addVerticalGlue();
        add(metaDataList, BorderLayout.CENTER);

        JIPipeDesktopFormPanel dataList = new JIPipeDesktopFormPanel(null, JIPipeDesktopFormPanel.NONE);
        for (JIPipeDataSlot slot : node.getDataInputSlots()) {
            Set<Integer> rows = batch.getInputSlotRows().getOrDefault(slot, Collections.emptySet());
            Icon dataTypeIcon = JIPipe.getDataTypes().getIconFor(slot.getAcceptedDataType());
            JButton displayButton = new JButton(slot.getName(), dataTypeIcon);
            UIUtils.setStandardButtonBorder(displayButton);
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
            copySlot.addData(slot.getDataItemStore(row), slot.getTextAnnotations(row), JIPipeTextAnnotationMergeMode.Merge, slot.getDataContext(row), new JIPipeProgressInfo());
        }
        JIPipeDesktopExtendedDataTableUI tableUI = new JIPipeDesktopExtendedDataTableUI(getDesktopProjectWorkbench(), new OwningStore<>(copySlot), true);
//        DataSlotTableUI tableUI = new DataSlotTableUI(getProjectWorkbench(), copySlot);
        JFrame frame = new JFrame("Data batch contents");
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        frame.setIconImage(UIUtils.getJIPipeIcon128());
        frame.setContentPane(tableUI);
        frame.pack();
        frame.setSize(800, 600);
        frame.setLocationRelativeTo(getDesktopWorkbench().getWindow());
        frame.setVisible(true);
    }
}
