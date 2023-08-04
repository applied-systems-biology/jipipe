package org.hkijena.jipipe.ui.grapheditor.nodefinder;

import org.hkijena.jipipe.api.nodes.database.JIPipeNodeDatabase;
import org.hkijena.jipipe.api.nodes.database.JIPipeNodeDatabaseEntry;
import org.hkijena.jipipe.api.notifications.JIPipeNotificationAction;
import org.hkijena.jipipe.ui.components.RoundedButtonUI;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.ui.RoundedLineBorder;

import javax.swing.*;
import java.awt.*;
import java.util.Collections;

public class NodeFinderDatasetListCellRenderer extends JPanel implements ListCellRenderer<JIPipeNodeDatabaseEntry> {
    private final JIPipeNodeDatabase database;
    private final JLabel iconLabel = new JLabel();
    private final JLabel nameLabel = new JLabel();
    private final JLabel categoryLabel = new JLabel();
    private final JLabel descriptionLabel = new JLabel();
    private final JButton addButton = new JButton();
    private final Color borderColorDefault = UIManager.getColor("Button.borderColor");
    private final Color borderColorSelected = JIPipeNotificationAction.Style.Success.getBackground();
    private final RoundedLineBorder border = new RoundedLineBorder(borderColorDefault, 1, 3);
    private final JPanel indicator = new JPanel();
    private final RoundedLineBorder indicatorBorder = new RoundedLineBorder(Color.GRAY, 2, 1);

    public NodeFinderDatasetListCellRenderer(JIPipeNodeDatabase database) {
        this.database = database;
        initialize();
    }

    private void initialize() {
        setLayout(new GridBagLayout());

        nameLabel.setFont(new Font(Font.DIALOG, Font.BOLD, 14));

        categoryLabel.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        categoryLabel.setForeground(Color.GRAY);

        indicator.setBorder(indicatorBorder);

        Insets insets = new Insets(2,2,2,2);
        add(indicator, new GridBagConstraints(0,0,1,4,0,0,GridBagConstraints.NORTHWEST,GridBagConstraints.VERTICAL,insets,0,0));
        add(iconLabel, new GridBagConstraints(1,0,1,1,0,0,GridBagConstraints.NORTHWEST, GridBagConstraints.NONE,insets,0,0));
        add(nameLabel, new GridBagConstraints(2,0,1,1,1,0,GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL,insets,0,0));
        add(categoryLabel, new GridBagConstraints(2,1,1,1,1,0,GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL,insets,0,0));
        add(descriptionLabel, new GridBagConstraints(2,2,1,1,0,0,GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL,insets,0,0));

        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.X_AXIS));
        bottomPanel.add(Box.createHorizontalGlue());
        bottomPanel.add(addButton);
        bottomPanel.setOpaque(false);
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(8,0,0,0));
        add(bottomPanel, new GridBagConstraints(1,3,2,1,1,0,GridBagConstraints.NORTHWEST,GridBagConstraints.HORIZONTAL,insets,0,0));

        addButton.setOpaque(true);
        addButton.setUI(new RoundedButtonUI(3, JIPipeNotificationAction.Style.Success.getBackground().brighter(), JIPipeNotificationAction.Style.Success.getBackground().darker()));
        addButton.setIcon(UIUtils.getIconInvertedFromResources("actions/add.png"));
        addButton.setFont(new Font(Font.DIALOG, Font.PLAIN, 16));
        addButton.setBackground(JIPipeNotificationAction.Style.Success.getBackground());
        addButton.setForeground(JIPipeNotificationAction.Style.Success.getText());

        setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(4,4,4,4),
                BorderFactory.createCompoundBorder(border,
                        BorderFactory.createEmptyBorder(4,4,4,4))));
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends JIPipeNodeDatabaseEntry> list, JIPipeNodeDatabaseEntry value, int index, boolean isSelected, boolean cellHasFocus) {
        setPreferredSize(new Dimension(list.getWidth() - 16, 128));
        iconLabel.setIcon(value.getIcon());
        nameLabel.setText(value.getName());
        categoryLabel.setText(value.getCategory().trim().replace("\n", " > "));
        descriptionLabel.setText(value.getDescriptionPlain());

        if(value.exists()) {
            indicator.setBackground(value.getFillColor());
            indicatorBorder.setFill(value.getBorderColor());
            indicator.setOpaque(true);
            addButton.setText("Connect");
        }
        else {
            indicatorBorder.setFill(value.getFillColor());
            indicator.setOpaque(false);
            addButton.setText("Add");
        }
        if (isSelected) {
            border.setFill(borderColorSelected);
        } else {
            border.setFill(borderColorDefault);
        }

        return this;
    }
}
