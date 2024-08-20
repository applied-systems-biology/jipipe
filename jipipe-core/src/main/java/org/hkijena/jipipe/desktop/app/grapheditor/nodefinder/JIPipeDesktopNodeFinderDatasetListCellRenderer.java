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

package org.hkijena.jipipe.desktop.app.grapheditor.nodefinder;

import org.hkijena.jipipe.api.nodes.database.JIPipeNodeDatabaseEntry;
import org.hkijena.jipipe.api.notifications.JIPipeNotificationAction;
import org.hkijena.jipipe.desktop.commons.theme.JIPipeDesktopRoundedButtonUI;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.ui.RoundedLineBorder;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;

public class JIPipeDesktopNodeFinderDatasetListCellRenderer extends JPanel implements ListCellRenderer<JIPipeNodeDatabaseEntry> {
    public static final int CELL_HEIGHT = 100;
    private final JIPipeDesktopNodeFinderDialogUI parent;
    private final JLabel iconLabel = new JLabel();
    private final JLabel nameLabel = new JLabel();
    private final JLabel categoryLabel = new JLabel();
    private final JLabel descriptionLabel = new JLabel();
    private final JButton addButton = new JButton();
    private final Border defaultBorder;
    private final Border selectedBorder;

    public JIPipeDesktopNodeFinderDatasetListCellRenderer(JIPipeDesktopNodeFinderDialogUI parent) {
        this.parent = parent;
        this.defaultBorder = BorderFactory.createCompoundBorder(UIUtils.createEmptyBorder(4),
                UIUtils.createControlBorder());
        this.selectedBorder = BorderFactory.createCompoundBorder(UIUtils.createEmptyBorder(4),
                UIUtils.createControlBorder(UIUtils.COLOR_SUCCESS));
        initialize();
    }

    private void initialize() {
        setLayout(new GridBagLayout());

        nameLabel.setFont(new Font(Font.DIALOG, Font.PLAIN, 12));

        descriptionLabel.setFont(new Font(Font.DIALOG, Font.PLAIN, 10));
        descriptionLabel.setForeground(Color.GRAY);

        categoryLabel.setFont(new Font(Font.DIALOG, Font.ITALIC, 10));
        categoryLabel.setForeground(Color.GRAY);

        Insets insets = new Insets(2, 2, 2, 2);
//        add(indicator, new GridBagConstraints(0, 0, 1, 4, 0, 0, GridBagConstraints.NORTHWEST, GridBagConstraints.VERTICAL, insets, 0, 0));
        add(iconLabel, new GridBagConstraints(1, 0, 1, 1, 0, 0, GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, insets, 0, 0));
        add(nameLabel, new GridBagConstraints(2, 0, 1, 1, 1, 0, GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL, insets, 0, 0));
        add(categoryLabel, new GridBagConstraints(2, 1, 1, 1, 1, 0, GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL, insets, 0, 0));
        add(descriptionLabel, new GridBagConstraints(2, 2, 1, 1, 0, 0, GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL, insets, 0, 0));

        addButton.setOpaque(false);
        addButton.setUI(new JIPipeDesktopRoundedButtonUI(3, JIPipeNotificationAction.Style.Success.getBackground().brighter(), JIPipeNotificationAction.Style.Success.getBackground().darker()));
        addButton.setIcon(UIUtils.getIconFromResources("actions/add.png"));
//        addButton.setFont(new Font(Font.DIALOG, Font.PLAIN, 12));
//        addButton.setBackground(JIPipeNotificationAction.Style.Success.getBackground());
//        addButton.setForeground(JIPipeNotificationAction.Style.Success.getText());
        addButton.setBorder(BorderFactory.createCompoundBorder(new RoundedLineBorder(new Color(0x5CB85C), 1, 3),
                BorderFactory.createEmptyBorder(3, 3, 3, 3)));
        add(addButton, new GridBagConstraints(3, 0, 1, 1, 0, 0, GridBagConstraints.NORTHWEST, GridBagConstraints.BOTH, new Insets(2, 16, 2, 2), 0, 0));
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends JIPipeNodeDatabaseEntry> list, JIPipeNodeDatabaseEntry value, int index, boolean isSelected, boolean cellHasFocus) {
        setPreferredSize(new Dimension(list.getWidth() - 16, CELL_HEIGHT));
        iconLabel.setIcon(value.getIcon());
        nameLabel.setText(value.getName());
        categoryLabel.setText(value.getLocationInfos().get(0).trim().replace("\n", " > "));
        String descriptionText = value.getDescription().toPlainText();
        if (descriptionText.length() > 200) {
            descriptionText = descriptionText.substring(0, 200) + " ...";
        }
        descriptionLabel.setText(descriptionText);

        if (parent.getQuerySlot() != null) {
            if (value.exists()) {
                addButton.setText("Connect");
            } else {
                addButton.setText("Add");
            }
        } else {
            addButton.setText("Add");
        }

        setBorder(isSelected ? selectedBorder : defaultBorder);

        return this;
    }
}
