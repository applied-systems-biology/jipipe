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

package org.hkijena.jipipe.desktop.app.customizer;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.desktop.commons.theme.JIPipeDesktopModernThemeStyle;
import org.hkijena.jipipe.utils.ThemeUtils;

import javax.swing.*;
import java.awt.*;

public class JIPipeDesktopModernThemeStyleListCellRenderer extends JPanel implements javax.swing.ListCellRenderer<JIPipeDesktopModernThemeStyle> {

    private JLabel iconLabel;
    private JLabel nameLabel;
    private JLabel infoLabel;

    public JIPipeDesktopModernThemeStyleListCellRenderer() {
        setOpaque(true);
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        initialize();
    }

    private void initialize() {
        setLayout(new GridBagLayout());
        iconLabel = new JLabel();
        nameLabel = new JLabel();
        infoLabel = new JLabel();
        infoLabel.setForeground(Color.GRAY);

        add(iconLabel, new GridBagConstraints() {
            {
                gridx = 0;
                gridy = 0;
                gridheight = 3;
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
        add(infoLabel, new GridBagConstraints() {
            {
                gridx = 1;
                gridy = 1;
                fill = HORIZONTAL;
                weightx = 1;
            }
        });
    }


    @Override
    public Component getListCellRendererComponent(JList<? extends JIPipeDesktopModernThemeStyle> list, JIPipeDesktopModernThemeStyle value, int index, boolean isSelected, boolean cellHasFocus) {

        if (value != null) {
            nameLabel.setText(value.getName());
            boolean isCurrentTheme = ThemeUtils.getCurrentStyle().getId().equals(value.getId());
            infoLabel.setText((value.isBuiltIn() ? "Built-in" : "User-defined") +  (isCurrentTheme ? " - current theme" : ""));
            iconLabel.setIcon(new JIPipeDesktopModernThemeStyleIcon(value));
        } else {
            nameLabel.setText("Null");
            infoLabel.setText("Null");
            iconLabel.setIcon(JIPipe.RESOURCES.getIcon32("missing.png"));
        }

        if (isSelected) {
            setBackground(UIManager.getColor("List.selectionBackground"));
        } else {
            setBackground(UIManager.getColor("List.background"));
        }
        return this;
    }
}
