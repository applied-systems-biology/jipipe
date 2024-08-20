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

package org.hkijena.jipipe.desktop.app.grapheditor.addnodepanel;

import org.hkijena.jipipe.api.nodes.database.JIPipeNodeDatabaseEntry;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.util.Set;

/**
 * Renderer for {@link JIPipeNodeDatabaseEntry} (supports only non-existing nodes fully)
 */
public class JIPipeDesktopAddNodePanelEntryListCellRenderer extends JPanel implements ListCellRenderer<JIPipeNodeDatabaseEntry> {

    private final Border defaultBorder;
    private final Border selectedBorder;
    private final JComponent parent;
    private final JIPipeDesktopAddNodesPanel addNodePanel;
    private JLabel nodeIcon;
    private JLabel nameLabel;
    private JLabel descriptionLabel;
    private JLabel pathLabel;
    private JLabel pinLabel;

    /**
     * Creates a new renderer
     */
    public JIPipeDesktopAddNodePanelEntryListCellRenderer(JComponent parent, JIPipeDesktopAddNodesPanel addNodePanel) {
        this.parent = parent;
        this.addNodePanel = addNodePanel;
        this.defaultBorder = BorderFactory.createCompoundBorder(UIUtils.createEmptyBorder(4),
                UIUtils.createControlBorder());
        this.selectedBorder = BorderFactory.createCompoundBorder(UIUtils.createEmptyBorder(4),
                UIUtils.createControlBorder(UIUtils.COLOR_SUCCESS));
        setOpaque(true);
        setBorder(defaultBorder);
        initialize();
    }

    private void initialize() {
        setLayout(new GridBagLayout());
        nodeIcon = new JLabel();
        nameLabel = new JLabel();
        descriptionLabel = new JLabel();
        descriptionLabel.setForeground(Color.GRAY);
        descriptionLabel.setFont(new Font(Font.DIALOG, Font.PLAIN, 10));
        pathLabel = new JLabel();
        pathLabel.setForeground(Color.GRAY);
        pathLabel.setFont(new Font(Font.DIALOG, Font.ITALIC, 10));
        pinLabel = new JLabel();

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
        add(pinLabel, new GridBagConstraints() {
            {
                anchor = GridBagConstraints.NORTHWEST;
                gridx = 2;
                gridy = 0;
                fill = NONE;
            }
        });
        add(descriptionLabel, new GridBagConstraints() {
            {
                anchor = GridBagConstraints.NORTHWEST;
                gridx = 1;
                gridy = 2;
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

        boolean showDescriptions = addNodePanel == null || addNodePanel.isShowDescriptions();
        int availableWidth = parent.getWidth() - list.getInsets().left - list.getInsets().right;
        setMinimumSize(new Dimension(availableWidth, 16));

        if (showDescriptions) {
            setMaximumSize(new Dimension(availableWidth, 75));
        } else {
            setMaximumSize(new Dimension(availableWidth, 50));
        }
        setPreferredSize(getMaximumSize());
        setFont(list.getFont());

        if (obj != null) {
            setTruncatedText(nameLabel, obj.getName(), list);
            if (showDescriptions) {
                setTruncatedText(descriptionLabel, obj.getDescription().toPlainText(), list);
            } else {
                descriptionLabel.setText(null);
            }
            setTruncatedText(pathLabel, obj.getLocationInfos().get(0).replace("\n", " > "), list);
            nodeIcon.setIcon(obj.getIcon());

            // Pinned items
            Set<String> pinnedNodeDatabaseEntries = addNodePanel.getPinnedNodeDatabaseEntries();
            if (pinnedNodeDatabaseEntries.contains(obj.getId())) {
                pinLabel.setIcon(UIUtils.getIconInvertedFromResources("actions/window-pin.png"));
            } else {
                pinLabel.setIcon(null);
            }
        }

        setBorder(isSelected ? selectedBorder : defaultBorder);

        return this;
    }

    private void setTruncatedText(JLabel label, String text, JList<? extends JIPipeNodeDatabaseEntry> list) {
        if (text.length() > 100) {
            text = text.substring(0, 100) + " ...";
        }
        FontMetrics fm = label.getFontMetrics(label.getFont());
        int availableWidth = parent.getWidth() - list.getInsets().left - list.getInsets().right;
        label.setText(StringUtils.limitWithEllipsis(text, availableWidth, fm));
    }
}
