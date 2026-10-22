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

package org.hkijena.jipipe.desktop.commons.components.renderers;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.nodes.JIPipeNodeExample;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.taxonomy.TaxonomyView;
import org.hkijena.jipipe.desktop.commons.components.icons.NodeFrameworkPatternIcon;
import org.hkijena.jipipe.utils.ThemeUtils;

import javax.swing.*;
import java.awt.*;

/**
 * Renderer for {@link JIPipeNodeInfo} and {@link org.hkijena.jipipe.api.nodes.JIPipeNodeExample}
 */
public class JIPipeNodeInfoOrExamplesListCellRenderer extends JPanel implements ListCellRenderer<Object> {

    private NodeFrameworkPatternIcon nodeColor;
    private JLabel nodeIcon;
    private JLabel nameLabel;
    private JLabel pathLabel;

    private JLabel alternativeLabel;

    /**
     * Creates a new renderer
     */
    public JIPipeNodeInfoOrExamplesListCellRenderer() {
        setOpaque(true);
        setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));
        initialize();
    }

    private void initialize() {
        setLayout(new GridBagLayout());
        nodeColor = new NodeFrameworkPatternIcon(16, 50);
        nodeIcon = new JLabel();
        nameLabel = new JLabel();
        pathLabel = new JLabel();
        pathLabel.setForeground(Color.GRAY);
        alternativeLabel = new JLabel();
        alternativeLabel.setForeground(ThemeUtils.getCurrentStyle().getSecondaryColor());
        alternativeLabel.setFont(new Font(Font.DIALOG, Font.ITALIC, ThemeUtils.getCurrentStyle().getFontSizeNormal()));

        add(new JLabel(nodeColor), new GridBagConstraints() {
            {
                gridx = 0;
                gridy = 0;
                gridheight = 3;
            }
        });
        add(nodeIcon, new GridBagConstraints() {
            {
                gridx = 1;
                gridy = 0;
                insets = new Insets(0, 4, 0, 4);
            }
        });
        add(nameLabel, new GridBagConstraints() {
            {
                gridx = 2;
                gridy = 0;
                fill = HORIZONTAL;
                weightx = 1;
            }
        });
        add(pathLabel, new GridBagConstraints() {
            {
                gridx = 2;
                gridy = 1;
                fill = HORIZONTAL;
                weightx = 1;
            }
        });
        add(alternativeLabel, new GridBagConstraints() {
            {
                gridx = 2;
                gridy = 2;
                fill = HORIZONTAL;
                weightx = 1;
            }
        });
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends Object> list, Object obj, int index, boolean isSelected, boolean cellHasFocus) {

        setFont(list.getFont());

        if (obj instanceof JIPipeNodeInfo) {
            JIPipeNodeInfo info = (JIPipeNodeInfo) obj;
            nodeColor.setFillColor(ThemeUtils.getNodeFillColor(info));
            // Framework signature pattern over the swatch (the node's framework).
            nodeColor.setFramework(info.getFramework());
            // Location (Task 21b): converted ⇒ "function chain > node", unconverted
            // ⇒ legacy "category [> menuPath] > node".
            pathLabel.setText(TaxonomyView.locationPath(info));
            nameLabel.setText(info.getName());
            nodeIcon.setIcon(JIPipe.getNodes().getIconFor(info));

            alternativeLabel.setForeground(ThemeUtils.getCurrentStyle().getSecondaryColor());
            // Task 26a: alias rows removed (converted nodes have no aliases).
            alternativeLabel.setText("");
        } else if (obj instanceof JIPipeNodeExample) {
            JIPipeNodeExample example = (JIPipeNodeExample) obj;
            JIPipeNodeInfo info = example.getNodeInfo();
            nodeColor.setFillColor(ThemeUtils.getNodeFillColor(info));
            // Framework signature pattern over the swatch (the node's framework).
            nodeColor.setFramework(info.getFramework());
            // Location (Task 21b): converted ⇒ "function chain > node", unconverted
            // ⇒ legacy "category [> menuPath] > node".
            pathLabel.setText(TaxonomyView.locationPath(info));
            nameLabel.setText(example.getNodeTemplate().getName() + ": " + info.getName());
            nodeIcon.setIcon(JIPipe.getNodes().getIconFor(info));

            alternativeLabel.setForeground(ThemeUtils.getCurrentStyle().getPrimaryColor());
            alternativeLabel.setText("Example");
        } else {
            nameLabel.setText("<Null>");
        }

        if (isSelected) {
            setBackground(UIManager.getColor("List.selectionBackground"));
        } else {
            setBackground(UIManager.getColor("List.background"));
        }
        return this;
    }
}
