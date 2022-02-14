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

package org.hkijena.jipipe.ui.bookmarks;

import org.hkijena.jipipe.api.compartments.algorithms.JIPipeProjectCompartment;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;

/**
 * Renderer for {@link JIPipeGraphNode}
 */
public class BookmarkedNodeListCellRenderer extends JPanel implements ListCellRenderer<JIPipeGraphNode> {

    private JLabel nodeIcon;
    private JLabel compartmentLabel;
    private JLabel nameLabel;
    private JLabel descriptionLabel;

    /**
     * Creates a new renderer
     */
    public BookmarkedNodeListCellRenderer() {
        setOpaque(true);
        setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        initialize();
    }

    private void initialize() {
        setLayout(new GridBagLayout());
        nodeIcon = new JLabel();
        nameLabel = new JLabel();
        compartmentLabel = new JLabel();
        descriptionLabel = new JLabel();
        descriptionLabel.setForeground(Color.GRAY);

        add(nodeIcon, new GridBagConstraints() {
            {
                gridx = 0;
                gridy = 0;
                insets = new Insets(0, 4, 0, 4);
            }
        });
        add(nameLabel, new GridBagConstraints() {
            {
                gridx = 1;
                gridy = 0;
                fill = HORIZONTAL;
                weightx = 1;
            }
        });
        add(compartmentLabel, new GridBagConstraints() {
            {
                gridx = 1;
                gridy = 1;
                fill = HORIZONTAL;
                weightx = 1;
            }
        });
        add(descriptionLabel, new GridBagConstraints() {
            {
                gridx = 1;
                gridy = 2;
                fill = HORIZONTAL;
                weightx = 1;
            }
        });
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends JIPipeGraphNode> list, JIPipeGraphNode node, int index, boolean isSelected, boolean cellHasFocus) {

        setFont(list.getFont());

        if (node instanceof JIPipeProjectCompartment) {
            descriptionLabel.setText("<html>" + StringUtils.orElse(node.getCustomDescription().getBody(), "No description given") + "</html>");
            nameLabel.setText(node.getName());
            nodeIcon.setIcon(UIUtils.getIconFromResources("data-types/graph-compartment.png"));
            compartmentLabel.setIcon(null);
            compartmentLabel.setText("Compartment");
        } else if (node != null) {
            descriptionLabel.setText("<html>" + StringUtils.orElse(node.getCustomDescription().getBody(), "No description given") + "</html>");
            nameLabel.setText(node.getName());
            JIPipeProjectCompartment compartment = node.getProjectCompartment();
            if (compartment != null) {
                compartmentLabel.setIcon(UIUtils.getIconFromResources("data-types/graph-compartment.png"));
                compartmentLabel.setText(compartment.getName());
            } else {
                compartmentLabel.setIcon(null);
                compartmentLabel.setText("");
            }
            nodeIcon.setIcon(node.getInfo().getIcon());
        } else {
            compartmentLabel.setText("<Null>");
            nameLabel.setText("<Null>");
            descriptionLabel.setText("<Null>");
        }

        if (isSelected) {
            setBackground(UIManager.getColor("List.selectionBackground"));
        } else {
            setBackground(UIManager.getColor("List.background"));
        }
        return this;
    }
}
