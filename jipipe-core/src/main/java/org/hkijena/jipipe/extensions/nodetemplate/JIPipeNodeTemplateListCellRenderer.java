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

package org.hkijena.jipipe.extensions.nodetemplate;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeNodeTemplate;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.ui.components.ColorIcon;
import org.hkijena.jipipe.ui.theme.ModernMetalTheme;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import javax.swing.plaf.ColorUIResource;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Set;

/**
 * Renderer for {@link JIPipeNodeInfo}
 */
public class JIPipeNodeTemplateListCellRenderer extends JPanel implements ListCellRenderer<JIPipeNodeTemplate> {

    public static final Color COLOR_GLOBAL =
            new Color(0x2372BE);
    public static final Color COLOR_PROJECT =
            new Color(0x6B40AA);

    private final Set<JIPipeNodeTemplate> projectTemplateList;
    private ColorIcon nodeColor;
    private JLabel nodeIcon;
    private JLabel nameLabel;
    private JLabel nodeNameLabel;
    private JLabel storageLabel;

    /**
     * Creates a new renderer
     * @param projectTemplateList templates that are in project
     */
    public JIPipeNodeTemplateListCellRenderer(Set<JIPipeNodeTemplate> projectTemplateList) {
        this.projectTemplateList = projectTemplateList;
        setOpaque(true);
        setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));
        initialize();
    }

    private void initialize() {
        setLayout(new GridBagLayout());
        nodeColor = new ColorIcon(16, 40);
        nodeIcon = new JLabel();
        nameLabel = new JLabel();
        nodeNameLabel = new JLabel();
        nodeNameLabel.setFont(nodeNameLabel.getFont().deriveFont(Font.ITALIC));
        nodeNameLabel.setForeground(Color.GRAY);
        storageLabel = new JLabel();
        storageLabel.setForeground(Color.BLUE);

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
        add(nodeNameLabel, new GridBagConstraints() {
            {
                gridx = 2;
                gridy = 1;
                fill = HORIZONTAL;
                weightx = 1;
            }
        });
        add(storageLabel, new GridBagConstraints() {
            {
                gridx = 3;
                gridy = 0;
            }
        });
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends JIPipeNodeTemplate> list, JIPipeNodeTemplate template, int index, boolean isSelected, boolean cellHasFocus) {

        setFont(list.getFont());

        JIPipeNodeInfo info = template.getNodeInfo();
        if (info != null) {
            nodeColor.setFillColor(UIUtils.getFillColorFor(info));
            nodeNameLabel.setText((info.getCategory().getName() + "\n" + info.getMenuPath() + "\n" + info.getName()).replace("\n\n", "\n").replace("\n", " > "));
            nameLabel.setText(template.getName());
            nodeIcon.setIcon(JIPipe.getNodes().getIconFor(info));
        } else {
            nameLabel.setText(template.getName());
            nodeNameLabel.setText("<Invalid node type>");
        }
        if(projectTemplateList.contains(template)) {
            storageLabel.setForeground(COLOR_PROJECT);
            storageLabel.setText("Project storage");
        }
        else {
            storageLabel.setForeground(COLOR_GLOBAL);
            storageLabel.setText("Global storage");
        }

        if (isSelected) {
            setBackground(UIManager.getColor("List.selectionBackground"));
        } else {
            setBackground(UIManager.getColor("List.background"));
        }
        return this;
    }
}
