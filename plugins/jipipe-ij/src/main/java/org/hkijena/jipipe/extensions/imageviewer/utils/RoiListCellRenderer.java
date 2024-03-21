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

package org.hkijena.jipipe.extensions.imageviewer.utils;

import ij.gui.Roi;
import org.apache.commons.math3.util.Precision;
import org.hkijena.jipipe.ui.components.icons.SolidColorIcon;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;

public class RoiListCellRenderer extends JPanel implements ListCellRenderer<Roi> {

    private SolidColorIcon strokeFillPreview = new SolidColorIcon(16, 16);
    private JLabel iconLabel = new JLabel();
    private JLabel nameLabel = new JLabel();
    private JLabel infoLabel = new JLabel();

    public RoiListCellRenderer() {
        initialize();
    }

    private void initialize() {
        setOpaque(true);
        setLayout(new GridBagLayout());
        iconLabel.setIcon(strokeFillPreview);
        infoLabel.setFont(new Font(Font.DIALOG, Font.PLAIN, 10));
        add(iconLabel, new GridBagConstraints() {
            {
                gridx = 0;
                gridy = 0;
                insets = UIUtils.UI_PADDING;
            }
        });
        add(nameLabel, new GridBagConstraints() {
            {
                gridx = 1;
                gridy = 0;
                weightx = 1;
                anchor = WEST;
                insets = UIUtils.UI_PADDING;
            }
        });
        add(infoLabel, new GridBagConstraints() {
            {
                gridx = 1;
                gridy = 1;
                weightx = 1;
                anchor = WEST;
                insets = UIUtils.UI_PADDING;
            }
        });
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends Roi> list, Roi value, int index, boolean isSelected, boolean cellHasFocus) {

        if (!StringUtils.isNullOrEmpty(value.getName()))
            nameLabel.setText(value.getName());
        else
            nameLabel.setText("Unnamed [" + index + "]");
        if (value.getFillColor() != null)
            strokeFillPreview.setFillColor(value.getFillColor());
        else
            strokeFillPreview.setFillColor(Color.WHITE);
        if (value.getStrokeColor() != null)
            strokeFillPreview.setBorderColor(value.getStrokeColor());
        else
            strokeFillPreview.setBorderColor(Color.YELLOW);

        iconLabel.setText("" + index);
        infoLabel.setText("x: " + Precision.round(value.getXBase(), 3) + ", y: " + Precision.round(value.getYBase(), 3) + ", z: " + (value.getZPosition() != 0 ? value.getZPosition() : "*") +
                ", c: " + (value.getCPosition() != 0 ? value.getCPosition() : "*") + ", t: " + (value.getTPosition() != 0 ? value.getTPosition() : "*"));

        if (isSelected) {
            setBackground(UIManager.getColor("List.selectionBackground"));
        } else {
            setBackground(UIManager.getColor("List.background"));
        }
        return this;
    }
}
