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

package org.hkijena.jipipe.ui.grapheditor.general.properties;

import com.google.common.html.HtmlEscapers;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeCellRenderer;
import java.awt.*;
import java.util.Set;

/**
 * Renders an {@link JIPipeDataSlot} in a {@link JTree}
 */
public class JIPipeDataSlotTreeCellRenderer extends JPanel implements TreeCellRenderer {

    private final JLabel slotLabel;
    private final JLabel slotName;
    private final JLabel slotEdges;

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
            slotName.setIcon(JIPipe.getDataTypes().getIconFor(slot.getAcceptedDataType()));
            if (!StringUtils.isNullOrEmpty(slot.getInfo().getCustomName())) {
                slotLabel.setText(slot.getInfo().getCustomName());
            } else {
                slotLabel.setText(null);
            }
            if (slot.isInput()) {
                Set<JIPipeDataSlot> sourceSlots = slot.getNode().getParentGraph().getInputIncomingSourceSlots(slot);
                if (sourceSlots.isEmpty()) {
                    slotEdges.setText("No connections");
                } else {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("<html>");
                    for (JIPipeDataSlot targetSlot : sourceSlots) {
                        stringBuilder.append("Receives data from '<i>").append(HtmlEscapers.htmlEscaper().escape(targetSlot.getDisplayName())).append("</i>'<br/>");
                    }
                    stringBuilder.append("</html>");
                    slotEdges.setText(stringBuilder.toString());
                }
            } else {
                Set<JIPipeDataSlot> targetSlots = slot.getNode().getParentGraph().getOutputOutgoingTargetSlots(slot);
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
            setBackground(UIManager.getColor("List.selectionBackground"));
        } else {
            setBackground(UIManager.getColor("List.background"));
        }

        return this;
    }
}
