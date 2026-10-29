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
import org.hkijena.jipipe.api.nodes.database.entries.CreateNewNodeByExampleDatabaseEntry;
import org.hkijena.jipipe.api.nodes.database.entries.CreateNewNodeByInfoDatabaseEntry;
import org.hkijena.jipipe.api.nodes.taxonomy.TaxonomyView;
import org.hkijena.jipipe.desktop.commons.components.icons.NodeListEntryIcon;
import org.hkijena.jipipe.utils.ThemeUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;

/**
 * Renderer for {@link JIPipeNodeDatabaseEntry} (supports only non-existing nodes fully)
 */
public class JIPipeDesktopNodeDatabaseEntryListCellRenderer extends JPanel implements ListCellRenderer<JIPipeNodeDatabaseEntry> {

    private NodeListEntryIcon nodeColor;
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

    private void initialize() {
        setBorder(UIUtils.createEmptyBorder(2));
        setLayout(new GridBagLayout());
        // The unified list icon: a function-colored bar with the framework
        // pattern and the node icon in the upper region (one column, covering
        // all three text rows).
        nodeColor = new NodeListEntryIcon(50);
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
        add(nameLabel, new GridBagConstraints() {
            {
                gridx = 1;
                gridy = 0;
                fill = HORIZONTAL;
                weightx = 1;
                insets = new Insets(0, 4, 0, 0);
            }
        });
        add(stateChips, new GridBagConstraints() {
            {
                gridx = 2;
                gridy = 0;
                anchor = WEST;
            }
        });
        add(pathLabel, new GridBagConstraints() {
            {
                gridx = 1;
                gridy = 1;
                fill = HORIZONTAL;
                weightx = 1;
                insets = new Insets(0, 4, 0, 0);
            }
        });
        add(alternativeLabel, new GridBagConstraints() {
            {
                gridx = 1;
                gridy = 2;
                fill = HORIZONTAL;
                weightx = 1;
                insets = new Insets(0, 4, 0, 0);
            }
        });
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends JIPipeNodeDatabaseEntry> list, JIPipeNodeDatabaseEntry obj, int index, boolean isSelected, boolean cellHasFocus) {

        setFont(list.getFont());

        if (obj != null) {
            nameLabel.setText(obj.getName());
            // Unified list icon: fill + framework pattern + node icon on top.
            nodeColor.setFillColor(obj.getFillColor());
            nodeColor.setFramework(obj.getFramework());
            nodeColor.setNodeIcon(obj.getIcon());
            pathLabel.setText(locationOf(obj));
            // The node's state (imported / unstable / deprecated), per the
            // state label system
            stateChips.removeAll();
            stateChips.add(JIPipeNodeStateChips.panel(obj));
            stateChips.revalidate();
        }

        // The entry kind in the bottom-row slot: Example / Pattern / Custom
        // (plain nodes get no label). Same text + colors as the find-node
        // dialog's bottom row.
        String kind = JIPipeNodeStateChips.entryKind(obj);
        alternativeLabel.setForeground(JIPipeNodeStateChips.entryKindColor(kind));
        alternativeLabel.setText(kind);

        if (isSelected) {
            setBackground(UIManager.getColor("List.selectionBackground"));
        } else {
            setBackground(UIManager.getColor("List.background"));
        }

        return this;
    }
}
