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

package org.hkijena.jipipe.desktop.commons.components.renderers;

import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.database.JIPipeNodeDatabaseEntry;
import org.hkijena.jipipe.api.nodes.taxonomy.TaxonomyView;
import org.hkijena.jipipe.api.nodes.database.entries.CreateNewNodeByExampleDatabaseEntry;
import org.hkijena.jipipe.api.nodes.database.entries.CreateNewNodeByInfoDatabaseEntry;
import org.hkijena.jipipe.desktop.commons.components.icons.SolidColorIcon;
import org.hkijena.jipipe.utils.ThemeUtils;

import javax.swing.*;
import java.awt.*;

/**
 * Renderer for {@link JIPipeNodeDatabaseEntry} (supports only non-existing nodes fully)
 */
public class JIPipeDesktopNodeDatabaseEntryListCellRenderer extends JPanel implements ListCellRenderer<JIPipeNodeDatabaseEntry> {

    private SolidColorIcon nodeColor;
    private JLabel nodeIcon;
    private JLabel nameLabel;
    private JPanel stateChips;
    private JLabel pathLabel;

    private JLabel alternativeLabel;

    /**
     * Creates a new renderer
     */
    public JIPipeDesktopNodeDatabaseEntryListCellRenderer() {
        setOpaque(true);
        setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));
        initialize();
    }

    private void initialize() {
        setLayout(new GridBagLayout());
        nodeColor = new SolidColorIcon(16, 50);
        nodeIcon = new JLabel();
        nameLabel = new JLabel();
        stateChips = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 0));
        stateChips.setOpaque(false);
        pathLabel = new JLabel();
        pathLabel.setForeground(Color.GRAY);
        alternativeLabel = new JLabel();
        alternativeLabel.setForeground(ThemeUtils.getCurrentStyle().getSecondaryColor());
        alternativeLabel.setFont(new Font(Font.DIALOG, Font.ITALIC, ThemeUtils.getCurrentStyle().getFontSizeNormal()));

        add(new JLabel(nodeColor), new GridBagConstraints() {
            {
                gridx = 0;
                gridy = 0;
                gridheight = 3;
            }
        });
        add(nodeIcon, new GridBagConstraints() {
            {
                gridx = 1;
                gridy = 0;
                insets = new Insets(0, 4, 0, 4);
            }
        });
        add(nameLabel, new GridBagConstraints() {
            {
                gridx = 2;
                gridy = 0;
                fill = HORIZONTAL;
                weightx = 1;
            }
        });
        add(stateChips, new GridBagConstraints() {
            {
                gridx = 3;
                gridy = 0;
                anchor = WEST;
            }
        });
        add(pathLabel, new GridBagConstraints() {
            {
                gridx = 2;
                gridy = 1;
                fill = HORIZONTAL;
                weightx = 1;
            }
        });
        add(alternativeLabel, new GridBagConstraints() {
            {
                gridx = 2;
                gridy = 2;
                fill = HORIZONTAL;
                weightx = 1;
            }
        });
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends JIPipeNodeDatabaseEntry> list, JIPipeNodeDatabaseEntry obj, int index, boolean isSelected, boolean cellHasFocus) {

        setFont(list.getFont());

        if (obj != null) {
            nameLabel.setText(obj.getName());
            nodeColor.setFillColor(obj.getFillColor());
            pathLabel.setText(locationOf(obj));
            nodeIcon.setIcon(obj.getIcon());
            // The node's state (imported / unstable / deprecated), per the
            // state label system
            stateChips.removeAll();
            stateChips.add(JIPipeNodeStateChips.panel(obj));
            // The template badge (Pattern / Custom), read from the entry —
            // spec §7.2. Non-templates get no badge.
            JComponent badge = JIPipeNodeStateChips.templateBadge(obj);
            if (badge != null) {
                stateChips.add(badge);
            }
            stateChips.revalidate();
        }

        if (obj instanceof CreateNewNodeByInfoDatabaseEntry) {
            alternativeLabel.setForeground(ThemeUtils.getCurrentStyle().getSecondaryColor());
            // Task 26a: alias rows removed (converted nodes have no aliases).
            alternativeLabel.setText("");
        } else if (obj instanceof CreateNewNodeByExampleDatabaseEntry) {
            alternativeLabel.setForeground(ThemeUtils.getCurrentStyle().getPrimaryColor());
            alternativeLabel.setText("Example");
        }

        if (isSelected) {
            setBackground(UIManager.getColor("List.selectionBackground"));
        } else {
            setBackground(UIManager.getColor("List.background"));
        }

        return this;
    }

    /**
     * The location row for the given entry (Task 21b).
     * <p>
     * Node-info-carrying entries that wrap a <em>converted</em> node (a
     * non-null primary function) show the taxonomy function path
     * ({@link TaxonomyView#locationPath}). Every other entry — unconverted
     * node entries, existing pipeline nodes, compartments, and templates —
     * keeps the stored {@link JIPipeNodeDatabaseEntry#getLocationInfos()
     * location info} byte-equivalent to before, so no unconverted node's
     * visible location changes.
     */
    private static String locationOf(JIPipeNodeDatabaseEntry obj) {
        JIPipeNodeInfo info = null;
        if (obj instanceof CreateNewNodeByInfoDatabaseEntry) {
            info = ((CreateNewNodeByInfoDatabaseEntry) obj).getNodeInfo();
        } else if (obj instanceof CreateNewNodeByExampleDatabaseEntry) {
            info = ((CreateNewNodeByExampleDatabaseEntry) obj).getExample().getNodeInfo();
        }
        if (info != null && info.getPrimaryFunction() != null) {
            return TaxonomyView.locationPath(info);
        }
        return obj.getLocationInfos().get(0).replace("\n", " > ");
    }
}
