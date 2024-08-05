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

package org.hkijena.jipipe.plugins.nodetemplate;

import org.hkijena.jipipe.api.JIPipeNodeTemplate;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.desktop.commons.components.icons.SolidColorIcon;

import javax.swing.*;
import java.awt.*;
import java.util.Set;

/**
 * Renderer for {@link JIPipeNodeInfo}
 */
public class JIPipeNodeTemplateListCellRenderer extends JPanel implements ListCellRenderer<JIPipeNodeTemplate> {

    public static final Color COLOR_GLOBAL =
            new Color(0x2372BE);
    public static final Color COLOR_PROJECT =
            new Color(0x6B40AA);
    public static final Color COLOR_EXTENSION =
            new Color(0x4098AA);

    private final Set<JIPipeNodeTemplate> projectTemplateList;
    private SolidColorIcon nodeColor;
    private JLabel nodeIcon;
    private JLabel nameLabel;
    private JLabel nodeNameLabel;
    private JLabel storageLabel;

    /**
     * Creates a new renderer
     *
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
        nodeColor = new SolidColorIcon(16, 40);
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

        nodeColor.setFillColor(template.getFillColor());
        nodeColor.setBorderColor(template.getBorderColor());
        nameLabel.setText(template.getName());
        nodeNameLabel.setText(("Templates\n" + String.join("\n", template.getMenuPath())).replace("\n\n", "\n").trim().replace("\n", " > "));
        nodeIcon.setIcon(template.getIconImage());

        if (template.isFromExtension()) {
            storageLabel.setForeground(COLOR_EXTENSION);
            storageLabel.setText("Plugin");
        } else if (projectTemplateList.contains(template)) {
            storageLabel.setForeground(COLOR_PROJECT);
            storageLabel.setText("Project");
        } else {
            storageLabel.setForeground(COLOR_GLOBAL);
            storageLabel.setText("Global");
        }

        if (isSelected) {
            setBackground(UIManager.getColor("List.selectionBackground"));
        } else {
            setBackground(UIManager.getColor("List.background"));
        }
        return this;
    }
}
