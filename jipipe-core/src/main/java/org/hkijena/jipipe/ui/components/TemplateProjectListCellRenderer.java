package org.hkijena.jipipe.ui.components;

import org.hkijena.jipipe.api.JIPipeProjectTemplate;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;

/**
 * Renders a recent project
 */
public class TemplateProjectListCellRenderer extends JPanel implements ListCellRenderer<JIPipeProjectTemplate> {

    private JLabel iconLabel;
    private JLabel nameLabel;
    private JLabel pathLabel;

    public TemplateProjectListCellRenderer() {
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
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends JIPipeProjectTemplate> list, JIPipeProjectTemplate value, int index, boolean isSelected, boolean cellHasFocus) {

        if (value != null) {
            nameLabel.setText(value.getMetadata().getName());
            pathLabel.setText(value.getMetadata().getTemplateDescription());
        }

        if (isSelected) {
            setBackground(UIManager.getColor("List.selectionBackground"));
        } else {
            setBackground(UIManager.getColor("List.background"));
        }
        return this;
    }
}
