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

package org.hkijena.jipipe.extensions.nodeexamples;

import org.hkijena.jipipe.api.nodes.JIPipeNodeExample;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.ui.components.icons.SolidColorIcon;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;

/**
 * Renderer for {@link JIPipeNodeInfo}
 */
public class JIPipeNodeExampleListCellRenderer extends JPanel implements ListCellRenderer<JIPipeNodeExample> {

    private SolidColorIcon nodeColor;
    private JLabel nodeIcon;
    private JLabel nameLabel;
    private JLabel descriptionLabel;

    /**
     * Creates a new renderer
     *
     */
    public JIPipeNodeExampleListCellRenderer() {
        setOpaque(true);
        setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));
        initialize();
    }

    private void initialize() {
        setLayout(new GridBagLayout());
        nodeColor = new SolidColorIcon(16, 40);
        nodeIcon = new JLabel();
        nameLabel = new JLabel();
        descriptionLabel = new JLabel();
        descriptionLabel.setFont(descriptionLabel.getFont().deriveFont(Font.ITALIC));
        descriptionLabel.setForeground(Color.GRAY);

        add(new JLabel(nodeColor), new GridBagConstraints() {
            {
                gridx = 0;
                gridy = 0;
                gridheight = 2;
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
        add(descriptionLabel, new GridBagConstraints() {
            {
                gridx = 2;
                gridy = 1;
                fill = HORIZONTAL;
                weightx = 1;
            }
        });
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends JIPipeNodeExample> list, JIPipeNodeExample example, int index, boolean isSelected, boolean cellHasFocus) {
        setFont(list.getFont());
        nodeColor.setFillColor(example.getNodeTemplate().getFillColor());
        nodeColor.setBorderColor(example.getNodeTemplate().getBorderColor());
        nameLabel.setText(example.getNodeTemplate().getName());
        descriptionLabel.setText(example.getNodeTemplate().getDescription().getHtml());
        nodeIcon.setIcon(UIUtils.getIconFromResources(StringUtils.orElse(example.getNodeTemplate().getIcon().getIconName(), "actions/configure.png")));

        if (isSelected) {
            setBackground(UIManager.getColor("List.selectionBackground"));
        } else {
            setBackground(UIManager.getColor("List.background"));
        }
        return this;
    }
}
