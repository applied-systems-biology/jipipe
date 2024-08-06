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

package org.hkijena.jipipe.desktop.app.grapheditor.pipeline.addnodepanel;

import org.hkijena.jipipe.api.nodes.database.JIPipeNodeDatabaseEntry;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;

/**
 * Renderer for {@link JIPipeNodeDatabaseEntry} (supports only non-existing nodes fully)
 */
public class JIPipeDesktopAddNodePanelEntryListCellRenderer extends JPanel implements ListCellRenderer<JIPipeNodeDatabaseEntry> {

    private JLabel nodeIcon;
    private JLabel nameLabel;
    private JLabel pathLabel;
    private final JScrollPane scrollPane;

    /**
     * Creates a new renderer
     */
    public JIPipeDesktopAddNodePanelEntryListCellRenderer(JScrollPane scrollPane) {
        this.scrollPane = scrollPane;
        setOpaque(true);
        setBorder(BorderFactory.createCompoundBorder(UIUtils.createEmptyBorder(4),
                UIUtils.createControlBorder()));
        initialize();
    }

    private void initialize() {
        setLayout(new GridBagLayout());
        nodeIcon = new JLabel();
        nameLabel = new JLabel();
        pathLabel = new JLabel();
        pathLabel.setForeground(Color.GRAY);
        pathLabel.setFont(new Font(Font.DIALOG, Font.PLAIN, 10));

        add(nodeIcon, new GridBagConstraints() {
            {
                anchor = GridBagConstraints.NORTHWEST;
                gridx = 0;
                gridy = 0;
                insets = new Insets(0, 4, 0, 4);
            }
        });
        add(nameLabel, new GridBagConstraints() {
            {
                anchor = GridBagConstraints.NORTHWEST;
                gridx = 1;
                gridy = 0;
                fill = HORIZONTAL;
                weightx = 1;
            }
        });
        add(pathLabel, new GridBagConstraints() {
            {
                anchor = GridBagConstraints.NORTHWEST;
                gridx = 1;
                gridy = 1;
                fill = HORIZONTAL;
                weightx = 1;
            }
        });
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends JIPipeNodeDatabaseEntry> list, JIPipeNodeDatabaseEntry obj, int index, boolean isSelected, boolean cellHasFocus) {

        int availableWidth = scrollPane.getWidth() - list.getInsets().left - list.getInsets().right;
        setMinimumSize(new Dimension(availableWidth, 16));
        setMaximumSize(new Dimension(availableWidth, 50));
        setPreferredSize(new Dimension(availableWidth, 50));
        setFont(list.getFont());

        if (obj != null) {
            setTruncatedText(nameLabel, obj.getName(), list);
            setTruncatedText(pathLabel, obj.getLocationInfo().replace("\n", " > "), list);
            nodeIcon.setIcon(obj.getIcon());
        }

//        if (isSelected) {
//            setBackground(UIManager.getColor("List.selectionBackground"));
//        } else {
//            setBackground(UIManager.getColor("List.background"));
//        }

        return this;
    }

    private void setTruncatedText(JLabel label, String text, JList<? extends JIPipeNodeDatabaseEntry> list) {
        FontMetrics fm = label.getFontMetrics(label.getFont());
        int availableWidth = scrollPane.getWidth() - list.getInsets().left - list.getInsets().right;
        label.setText(StringUtils.limitWithEllipsis(text, availableWidth, fm));
    }
}
