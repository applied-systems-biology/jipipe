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

package org.hkijena.jipipe.ui.components.renderers;

import org.hkijena.jipipe.api.parameters.JIPipeParameterTypeInfo;
import org.hkijena.jipipe.ui.components.icons.SolidColorIcon;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;

public class JIPipeParameterTypeInfoListCellRenderer extends JPanel implements ListCellRenderer<JIPipeParameterTypeInfo> {

    private final JLabel iconLabel = new JLabel();
    private final JLabel nameLabel = new JLabel();
    private final JLabel descriptionLabel = new JLabel();
    private final JLabel infoLabel = new JLabel();

    public JIPipeParameterTypeInfoListCellRenderer() {
       initialize();
    }

    private void initialize() {
        setOpaque(true);
        setLayout(new GridBagLayout());
        setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        iconLabel.setIcon(UIUtils.getIconFromResources("data-types/parameters.png"));
        descriptionLabel.setFont(new Font(Font.DIALOG, Font.ITALIC, 10));
        infoLabel.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 10));
        infoLabel.setForeground(Color.GRAY);
        add(iconLabel, new GridBagConstraints() {
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
                weightx = 1;
                anchor = WEST;
            }
        });
        add(descriptionLabel, new GridBagConstraints() {
            {
                gridx = 1;
                gridy = 1;
                weightx = 1;
                anchor = WEST;
            }
        });
        add(infoLabel, new GridBagConstraints() {
            {
                gridx = 1;
                gridy = 2;
                weightx = 1;
                anchor = WEST;
            }
        });
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends JIPipeParameterTypeInfo> list, JIPipeParameterTypeInfo value, int index, boolean isSelected, boolean cellHasFocus) {
        if (value != null) {
            nameLabel.setText(value.getName());
            descriptionLabel.setText(value.getDescription());
            infoLabel.setText(value.getId());
        } else {
            nameLabel.setText("<Null>");
            descriptionLabel.setText("");
            infoLabel.setText("");
        }

        if (isSelected) {
            setBackground(UIManager.getColor("List.selectionBackground"));
        } else {
            setBackground(UIManager.getColor("List.background"));
        }
        return this;
    }
}
