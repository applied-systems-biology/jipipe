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

import org.hkijena.jipipe.api.project.JIPipeProjectTemplate;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;

/**
 * Renders a recent project
 */
public class JIPipeDesktopTemplateProjectListCellRenderer extends JPanel implements ListCellRenderer<JIPipeProjectTemplate> {

    private JLabel iconLabel;
    private JLabel nameLabel;
    private JLabel pathLabel;
    private JButton openButton;

    public JIPipeDesktopTemplateProjectListCellRenderer() {
        setOpaque(true);
        initialize();
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
    }

    private void initialize() {
        setLayout(new GridBagLayout());
        iconLabel = new JLabel(UIUtils.getIcon32FromResources("jipipe-file.png"));
        nameLabel = new JLabel();
        nameLabel.setFont(new Font(Font.DIALOG, Font.PLAIN, 12));
        pathLabel = new JLabel();
        pathLabel.setForeground(Color.GRAY);
        pathLabel.setFont(new Font(Font.DIALOG, Font.ITALIC, 12));
        openButton = new JButton("New");
        UIUtils.setStandardButtonBorder(openButton);
        openButton.setBorder(UIUtils.createControlBorder());

        add(iconLabel, new GridBagConstraints() {
            {
                gridx = 0;
                gridy = 0;
                gridheight = 2;
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
        add(pathLabel, new GridBagConstraints() {
            {
                gridx = 1;
                gridy = 1;
                fill = HORIZONTAL;
                weightx = 1;
            }
        });
        add(openButton, new GridBagConstraints() {
            {
                gridx = 2;
                gridy = 0;
                gridheight = 2;
                insets = new Insets(0, 4, 0, 0);
            }
        });
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends JIPipeProjectTemplate> list, JIPipeProjectTemplate value, int index, boolean isSelected, boolean cellHasFocus) {

        if (value != null) {
            nameLabel.setText(value.getFixedName());
            pathLabel.setText(value.getMetadata().getTemplateDescription());
            openButton.setVisible(isSelected);
        } else {
            nameLabel.setText("No templates");
            pathLabel.setText("");
            openButton.setVisible(false);
        }

        if (isSelected) {
            setBackground(UIManager.getColor("List.selectionBackground"));
        } else {
            setBackground(UIManager.getColor("List.background"));
        }
        return this;
    }
}
