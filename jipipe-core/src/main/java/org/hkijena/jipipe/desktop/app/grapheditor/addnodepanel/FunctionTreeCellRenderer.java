package org.hkijena.jipipe.desktop.app.grapheditor.addnodepanel;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.nodes.database.JIPipeNodeDatabaseEntry;
import org.hkijena.jipipe.api.nodes.taxonomy.JIPipeNodeTaxonomyFramework;
import org.hkijena.jipipe.api.nodes.taxonomy.JIPipeNodeTaxonomyFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.JIPipeNodeTaxonomySpatialDimension;
import org.hkijena.jipipe.api.nodes.taxonomy.NoFramework;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.util.Set;

/**
 * Tree cell renderer. Function branch rows keep the original look
 * (name + icon, generic icon for icon-less leaves); node-leaf rows
 * (compact view) show the entry's icon, name, a framework badge (a
 * rounded chip in the framework's color, mirroring the node finder's
 * detail rail), and spatial chips ("2D" / "3D").
 */
class FunctionTreeCellRenderer implements TreeCellRenderer {

    private static final Icon GENERIC_NODE_ICON = JIPipe.RESOURCES.getIcon16("actions/tag.png");
    private static final int CHIP_ARC = 6;
    private final JIPipeDesktopNodeBrowser nodeBrowser;

    public FunctionTreeCellRenderer(JIPipeDesktopNodeBrowser nodeBrowser) {
        this.nodeBrowser = nodeBrowser;
    }

    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected,
                                                  boolean expanded, boolean leaf, int row, boolean hasFocus) {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
        Object userObject = node.getUserObject();
        Color selectionBackground = UIManager.getColor("Tree.selectionBackground");
        Color selectionForeground = UIManager.getColor("Tree.selectionForeground");
        Color background = selected && selectionBackground != null ? selectionBackground : tree.getBackground();
        Color foreground = selected && selectionForeground != null ? selectionForeground : tree.getForeground();

        if (userObject instanceof JIPipeNodeDatabaseEntry entry) {
            // Node leaf row: icon + name + framework badge + spatial chips
            JPanel rowPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
            rowPanel.setOpaque(true);
            rowPanel.setBackground(background);
            rowPanel.setForeground(foreground);
            rowPanel.setBorder(BorderFactory.createEmptyBorder(3, 4, 3, 4));

            JLabel nameLabel = new JLabel();
            nameLabel.setText(entry.getName());
            nameLabel.setIcon(entry.getIcon() != null ? entry.getIcon() : GENERIC_NODE_ICON);
            nameLabel.setForeground(foreground);
            rowPanel.add(nameLabel);

            JIPipeNodeTaxonomyFramework framework = entry.getFramework();
            boolean noFramework = NoFramework.ID.equals(framework.getId());
            JPanel frameworkChip = new JPanel(new GridBagLayout()) {
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    if (!noFramework) {
                        Graphics2D g2d = (Graphics2D) g.create();
                        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        g2d.setColor(framework.getColor());
                        g2d.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, CHIP_ARC, CHIP_ARC);
                        g2d.dispose();
                    }
                }
            };
            frameworkChip.setOpaque(false);
            frameworkChip.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 2));
            frameworkChip.setToolTipText(noFramework ? "No external framework" : "Framework: " + framework.getName());
            JLabel frameworkLabel = new JLabel(framework.getAbbreviation());
            frameworkLabel.setForeground(noFramework ? Color.GRAY : Color.WHITE);
            frameworkLabel.setFont(frameworkLabel.getFont().deriveFont(Font.BOLD, 9f));
            frameworkChip.add(frameworkLabel);
            rowPanel.add(frameworkChip);

            Set<JIPipeNodeTaxonomySpatialDimension> spatial = entry.getSpatialDimensions();
            for (JIPipeNodeTaxonomySpatialDimension dimension : spatial) {
                String text = dimension == JIPipeNodeTaxonomySpatialDimension.SPATIAL_2D ? "2D" : "3D";
                JPanel chip = new JPanel(new GridBagLayout()) {
                    @Override
                    protected void paintComponent(Graphics g) {
                        super.paintComponent(g);
                        Graphics2D g2d = (Graphics2D) g.create();
                        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        g2d.setColor(Color.GRAY);
                        g2d.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, CHIP_ARC, CHIP_ARC);
                        g2d.dispose();
                    }
                };
                chip.setOpaque(false);
                chip.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 2));
                chip.setToolTipText(dimension == JIPipeNodeTaxonomySpatialDimension.SPATIAL_2D
                        ? "Operates in 2D" : "Operates in 3D");
                JLabel chipLabel = new JLabel(text);
                chipLabel.setForeground(foreground);
                chipLabel.setFont(chipLabel.getFont().deriveFont(Font.PLAIN, 9f));
                chip.add(chipLabel);
                rowPanel.add(chip);
            }
            return rowPanel;
        }

        JLabel label = new JLabel();
        label.setOpaque(true);
        label.setBorder(BorderFactory.createEmptyBorder(3, 4, 3, 4));
        if (userObject instanceof JIPipeNodeTaxonomyFunction function) {
            label.setText(function.getName());
            Icon icon = function.getIcon();
            label.setIcon(icon != null ? icon : GENERIC_NODE_ICON);
            label.setToolTipText(function.getDescription());
            // Search highlight: the search matched a node filed under this
            // leaf (or one of its descendant leaves)
            if (nodeBrowser.searchHighlightedLeafPaths.contains(new TreePath(((DefaultMutableTreeNode) value).getPath()))) {
                label.setFont(label.getFont().deriveFont(Font.BOLD));
            }
        } else {
            // The root node
            label.setText("All functions");
            label.setIcon(GENERIC_NODE_ICON);
        }
        label.setForeground(foreground);
        label.setBackground(background);
        return label;
    }
}
