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

package org.hkijena.jipipe.ui.grapheditor.general.properties;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;

/**
 * Renders a {@link JIPipeDataSlot}
 */
public class JIPipeDataSlotListCellRenderer extends JPanel implements ListCellRenderer<JIPipeDataSlot> {

    private final JLabel nodeLabel = new JLabel();
    private final JLabel slotNameLabel = new JLabel();
    private final JLabel dataTypeLabel = new JLabel();

    public JIPipeDataSlotListCellRenderer() {
        initialize();
    }

    private void initialize() {
        setLayout(new GridBagLayout());
        setOpaque(true);
        setFont(new Font(Font.DIALOG, Font.PLAIN, 12));
        setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

        Insets border = new Insets(2, 4, 2, 2);

        dataTypeLabel.setForeground(Color.GRAY);
        nodeLabel.setFont(new Font(Font.DIALOG, Font.PLAIN, 11));

        add(slotNameLabel, new GridBagConstraints(0,0,1,1,1,0,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,border,0,0));
        add(dataTypeLabel, new GridBagConstraints(1,0,1,1,0,0,GridBagConstraints.EAST, GridBagConstraints.NONE,border,0,0));
        add(nodeLabel, new GridBagConstraints(0,1,2,1,1,0,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,border,0,0));

        setBorder(BorderFactory.createMatteBorder(0,0,1,0, Color.LIGHT_GRAY));
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends JIPipeDataSlot> list, JIPipeDataSlot value, int index, boolean isSelected, boolean cellHasFocus) {

        nodeLabel.setText(value.getNode().getDisplayName());
        nodeLabel.setIcon(JIPipe.getNodes().getIconFor(value.getNode().getInfo()));
        slotNameLabel.setText("<html>" + value.getName() + " (<i>" + value.getSlotType() + ")</i></html>");
        slotNameLabel.setIcon(JIPipe.getDataTypes().getIconFor(value.getAcceptedDataType()));
        dataTypeLabel.setText(JIPipeData.getNameOf(value.getAcceptedDataType()));
        dataTypeLabel.setIcon(JIPipe.getDataTypes().getIconFor(value.getAcceptedDataType()));

        if (isSelected) {
            setBackground(UIManager.getColor("List.selectionBackground"));
        } else {
            setBackground(UIManager.getColor("List.background"));
        }
        return this;
    }
}
