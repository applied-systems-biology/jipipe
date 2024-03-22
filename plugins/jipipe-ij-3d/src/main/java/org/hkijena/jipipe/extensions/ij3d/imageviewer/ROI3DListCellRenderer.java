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

package org.hkijena.jipipe.extensions.ij3d.imageviewer;

import org.apache.commons.math3.util.Precision;
import org.hkijena.jipipe.desktop.commons.components.icons.SolidJIPipeDesktopColorIcon;
import org.hkijena.jipipe.extensions.ij3d.datatypes.ROI3D;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;

public class ROI3DListCellRenderer extends JPanel implements ListCellRenderer<ROI3D> {

    private final SolidJIPipeDesktopColorIcon strokeFillPreview = new SolidJIPipeDesktopColorIcon(16, 16);
    private final JLabel iconLabel = new JLabel();
    private final JLabel nameLabel = new JLabel();
    private final JLabel infoLabel = new JLabel();

    public ROI3DListCellRenderer() {
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
    public Component getListCellRendererComponent(JList<? extends ROI3D> list, ROI3D value, int index, boolean isSelected, boolean cellHasFocus) {

        if (!StringUtils.isNullOrEmpty(value.getName()))
            nameLabel.setText(value.getName());
        else
            nameLabel.setText("Unnamed [" + index + "]");
        if (value.getFillColor() != null)
            strokeFillPreview.setFillColor(value.getFillColor());
        else
            strokeFillPreview.setFillColor(Color.WHITE);

        iconLabel.setText("" + index);
        infoLabel.setText("x: " + Precision.round(value.getObject3D().getCenterX(), 3) + ", y: " + Precision.round(value.getObject3D().getCenterY(), 3) + ", z: " + Precision.round(value.getObject3D().getCenterZ(), 3) +
                ", c: " + (value.getChannel() != 0 ? value.getChannel() : "*") + ", t: " + (value.getFrame() != 0 ? value.getFrame() : "*"));

        if (isSelected) {
            setBackground(UIManager.getColor("List.selectionBackground"));
        } else {
            setBackground(UIManager.getColor("List.background"));
        }
        return this;
    }
}
