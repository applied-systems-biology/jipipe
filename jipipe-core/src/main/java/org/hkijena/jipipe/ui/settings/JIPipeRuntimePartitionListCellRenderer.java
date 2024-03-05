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

package org.hkijena.jipipe.ui.settings;

import org.hkijena.jipipe.api.grouping.JIPipeGraphWrapperAlgorithm;
import org.hkijena.jipipe.api.runtimepartitioning.JIPipeRuntimePartition;
import org.hkijena.jipipe.api.runtimepartitioning.JIPipeRuntimePartitionConfiguration;
import org.hkijena.jipipe.ui.components.icons.SolidColorIcon;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;

public class JIPipeRuntimePartitionListCellRenderer extends JPanel implements ListCellRenderer<JIPipeRuntimePartition> {

    private final JIPipeRuntimePartitionConfiguration runtimePartitionList;
    private final SolidColorIcon colorIcon = new SolidColorIcon(12, 44, Color.BLACK, Color.DARK_GRAY);
    private final JLabel nameLabel = new JLabel();
    private final JPanel indicatorPanel = new JPanel();

    public JIPipeRuntimePartitionListCellRenderer(JIPipeRuntimePartitionConfiguration runtimePartitionList) {
        this.runtimePartitionList = runtimePartitionList;
        setOpaque(true);
        setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        initialize();
    }

    private void initialize() {
        setLayout(new GridBagLayout());
        Insets insets = new Insets(4,4,4,4);
        add(new JLabel(colorIcon), new GridBagConstraints(0,
                0,
                1,
                2,
                0,
                0,
                GridBagConstraints.NORTHWEST,
                GridBagConstraints.NONE,
                insets,
                0,
                0));
        nameLabel.setFont(new Font(Font.DIALOG, Font.BOLD, 12));
        add(nameLabel, new GridBagConstraints(1,
                0,
                1,
                1,
                1,
                0,
                GridBagConstraints.NORTHWEST,
                GridBagConstraints.HORIZONTAL,
                insets,
                0,
                0));

        indicatorPanel.setOpaque(false);
        indicatorPanel.setLayout(new BoxLayout(indicatorPanel, BoxLayout.X_AXIS));
        add(indicatorPanel, new GridBagConstraints(1,
                1,
                1,
                1,
                1,
                0,
                GridBagConstraints.NORTHWEST,
                GridBagConstraints.HORIZONTAL,
                insets,
                0,
                0));
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends JIPipeRuntimePartition> list, JIPipeRuntimePartition value, int index, boolean isSelected, boolean cellHasFocus) {
        setFont(list.getFont());
        indicatorPanel.removeAll();
        if(value != null) {
            String name = runtimePartitionList.getFullName(value);
            int idx = runtimePartitionList.indexOf(value);
            String nameText;
            if(idx >= 0 ) {
                nameText = idx == 0 ? StringUtils.orElse(value.getName(), "Default") : StringUtils.orElse(value.getName(), "Unnamed");
            }
            else {
                nameText = StringUtils.orElse(value.getName(), "Unnamed");
            }
            nameLabel.setText(nameText + " (Partition " + idx + ")");
            colorIcon.setFillColor(value.getColor().isEnabled() ? value.getColor().getContent() : Color.WHITE);

            if(idx == 0) {
                indicatorPanel.add(new JLabel(UIUtils.getIconFromResources("actions/lock.png")));
            }
            if(value.getOutputSettings().isExportHeavyData() || value.getOutputSettings().isExportLightweightData()) {
                indicatorPanel.add(new JLabel(UIUtils.getIconFromResources("actions/document-export.png")));
            }
            if(value.getIterationMode() == JIPipeGraphWrapperAlgorithm.IterationMode.IteratingDataBatch) {
                indicatorPanel.add(new JLabel( UIUtils.getIconFromResources("actions/media-playlist-normal.png")));
            }
            if(value.getIterationMode() == JIPipeGraphWrapperAlgorithm.IterationMode.MergingDataBatch) {
                indicatorPanel.add(new JLabel(UIUtils.getIconFromResources("actions/rabbitvcs-merge.png")));
            }
        }

        if (isSelected) {
            setBackground(UIManager.getColor("List.selectionBackground"));
        } else {
            setBackground(UIManager.getColor("List.background"));
        }
        return this;
    }

    public JIPipeRuntimePartitionConfiguration getRuntimePartitionList() {
        return runtimePartitionList;
    }
}
