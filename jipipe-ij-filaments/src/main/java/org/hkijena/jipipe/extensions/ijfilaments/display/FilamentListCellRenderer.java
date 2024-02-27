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

package org.hkijena.jipipe.extensions.ijfilaments.display;

import org.hkijena.jipipe.extensions.ijfilaments.datatypes.Filaments3DData;
import org.hkijena.jipipe.ui.components.icons.SolidColorIcon;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;

public class FilamentListCellRenderer extends JPanel implements ListCellRenderer<Filaments3DData> {

    private final SolidColorIcon strokeFillPreview = new SolidColorIcon(16, 16);
    private final JLabel iconLabel = new JLabel();
    private final JLabel nameLabel = new JLabel();
    private final JLabel infoLabel = new JLabel();

    public FilamentListCellRenderer() {
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
    public Component getListCellRendererComponent(JList<? extends Filaments3DData> list, Filaments3DData value, int index, boolean isSelected, boolean cellHasFocus) {
        nameLabel.setText("Filament " + (index + 1));
        strokeFillPreview.setFillColor(value.getAverageVertexColor());
        strokeFillPreview.setBorderColor(value.getAverageEdgeColor());
        infoLabel.setText(value.vertexSet().size() + " vertices, " + value.edgeSet().size() + " edges");

        if (isSelected) {
            setBackground(UIManager.getColor("List.selectionBackground"));
        } else {
            setBackground(UIManager.getColor("List.background"));
        }
        return this;
    }
}
