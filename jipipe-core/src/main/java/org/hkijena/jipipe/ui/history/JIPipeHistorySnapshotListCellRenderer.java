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

package org.hkijena.jipipe.ui.history;

import org.hkijena.jipipe.api.history.JIPipeHistoryJournalSnapshot;
import org.hkijena.jipipe.utils.StringUtils;

import javax.swing.*;
import java.awt.*;

/**
 * Renderer for {@link JIPipeHistoryJournalSnapshot}
 */
public class JIPipeHistorySnapshotListCellRenderer extends JPanel implements ListCellRenderer<JIPipeHistoryJournalSnapshot> {

    private JLabel snapshotIcon;
    private JLabel dateLabel;
    private JLabel nameLabel;
    private JLabel descriptionLabel;

    /**
     * Creates a new renderer
     */
    public JIPipeHistorySnapshotListCellRenderer() {
        setOpaque(true);
        setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        initialize();
    }

    private void initialize() {
        setLayout(new GridBagLayout());
        snapshotIcon = new JLabel();
        nameLabel = new JLabel();
        dateLabel = new JLabel();
        descriptionLabel = new JLabel();
        descriptionLabel.setForeground(Color.GRAY);

        add(snapshotIcon, new GridBagConstraints() {
            {
                gridx = 0;
                gridy = 0;
                insets = new Insets(0, 4, 0, 4);
            }
        });
        add(dateLabel, new GridBagConstraints() {
            {
                gridx = 1;
                gridy = 0;
                fill = HORIZONTAL;
                weightx = 1;
            }
        });
        add(nameLabel, new GridBagConstraints() {
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
    public Component getListCellRendererComponent(JList<? extends JIPipeHistoryJournalSnapshot> list, JIPipeHistoryJournalSnapshot snapshot, int index, boolean isSelected, boolean cellHasFocus) {

        setFont(list.getFont());

        if (snapshot != null) {
            descriptionLabel.setText(snapshot.getDescription());
            nameLabel.setText(snapshot.getName());
            dateLabel.setText(StringUtils.formatDateTime(snapshot.getCreationTime()));
            snapshotIcon.setIcon(snapshot.getIcon());
        } else {
            dateLabel.setText("<Null>");
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
