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

package org.hkijena.jipipe.ui.grapheditor.settings;

import com.google.common.html.HtmlEscapers;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.ui.registries.JIPipeUIDatatypeRegistry;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeCellRenderer;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.Set;

/**
 * Renders an {@link JIPipeDataSlot} in a {@link JTree}
 */
public class JIPipeDataSlotTreeCellRenderer extends JPanel implements TreeCellRenderer {

    private JLabel slotLabel;
    private JLabel slotName;
    private JLabel slotEdges;

    /**
     * Creates a new renderer
     */
    public JIPipeDataSlotTreeCellRenderer() {
        setOpaque(true);
        setLayout(new GridBagLayout());
        slotName = new JLabel();
        slotName.setFont(new Font(Font.DIALOG, Font.BOLD, 12));
        add(slotName, new GridBagConstraints() {
            {
                gridx = 0;
                gridy = 0;
                insets = UIUtils.UI_PADDING;
            }
        });

        slotLabel = new JLabel();
        slotLabel.setFont(new Font(Font.DIALOG, Font.ITALIC, 12));
        add(slotLabel, new GridBagConstraints() {
            {
                gridx = 1;
                gridy = 0;
                insets = UIUtils.UI_PADDING;
            }
        });

        slotEdges = new JLabel();
        slotEdges.setFont(new Font(Font.DIALOG, Font.PLAIN, 12));
        add(slotEdges, new GridBagConstraints() {
            {
                gridx = 0;
                gridy = 1;
                gridwidth = 2;
                insets = UIUtils.UI_PADDING;
            }
        });

    }

    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {

        Object o = ((DefaultMutableTreeNode) value).getUserObject();
        if (o instanceof JIPipeDataSlot) {
            JIPipeDataSlot slot = (JIPipeDataSlot) o;

            slotName.setText(slot.getName());
            slotName.setIcon(JIPipeUIDatatypeRegistry.getInstance().getIconFor(slot.getAcceptedDataType()));
            if (!StringUtils.isNullOrEmpty(slot.getDefinition().getCustomName())) {
                slotLabel.setText(slot.getDefinition().getCustomName());
            } else {
                slotLabel.setText(null);
            }
            if (slot.isInput()) {
                JIPipeDataSlot sourceSlot = slot.getNode().getGraph().getSourceSlot(slot);
                if (sourceSlot != null) {
                    slotEdges.setText("<html>Receives data from '<i>" + sourceSlot.getDisplayName() + "</i>'</html>");
                } else {
                    slotEdges.setText("No connections");
                }
            } else {
                Set<JIPipeDataSlot> targetSlots = slot.getNode().getGraph().getTargetSlots(slot);
                if (targetSlots.isEmpty()) {
                    slotEdges.setText("No connections");
                } else {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("<html>");
                    for (JIPipeDataSlot targetSlot : targetSlots) {
                        stringBuilder.append("Sends data to '<i>").append(HtmlEscapers.htmlEscaper().escape(targetSlot.getDisplayName())).append("</i>'<br/>");
                    }
                    stringBuilder.append("</html>");
                    slotEdges.setText(stringBuilder.toString());
                }
            }
        } else {
            slotName.setText("" + o);
            slotName.setIcon(null);
            slotLabel.setText(null);
            slotEdges.setText(null);
        }

        // Update status
        // Update status
        if (selected) {
            setBackground(new Color(184, 207, 229));
        } else {
            setBackground(new Color(255, 255, 255));
        }

        return this;
    }
}
