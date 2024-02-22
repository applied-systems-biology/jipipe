package org.hkijena.jipipe.ui.components;

import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Path;

/**
 * Renders a recent project
 */
public class RecentProjectListCellRenderer extends JPanel implements ListCellRenderer<Path> {

    private JLabel iconLabel;
    private JLabel nameLabel;
    private JLabel pathLabel;
    private JButton openButton;

    public RecentProjectListCellRenderer() {
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
        openButton = new JButton("Open");
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
    public Component getListCellRendererComponent(JList<? extends Path> list, Path value, int index, boolean isSelected, boolean cellHasFocus) {

        if (value != null) {
            nameLabel.setText(value.getFileName().toString());
            pathLabel.setText(value.getParent().toString());
            openButton.setVisible(isSelected);
        } else {
            nameLabel.setText("No recent projects");
            pathLabel.setText("Your recent projects will appear here");
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
