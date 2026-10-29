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

package org.hkijena.jipipe.desktop.app.grapheditor.nodebrowser;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.nodes.database.JIPipeNodeDatabaseEntry;
import org.hkijena.jipipe.api.nodes.taxonomy.*;
import org.hkijena.jipipe.desktop.commons.components.icons.ColoredCircleIcon;
import org.hkijena.jipipe.desktop.commons.components.renderers.JIPipeNodeStateChips;
import org.hkijena.jipipe.desktop.commons.theme.JIPipeDesktopModernThemeStyle;
import org.hkijena.jipipe.utils.ThemeUtils;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.util.Set;

/**
 * Shared tree cell renderer of the node browser and the node finder dialog.
 * <p>
 * Function branch rows keep the original look (name + icon, generic icon
 * for icon-less leaves, bold when a search matches under them); node-leaf
 * rows (the browser's compact single-pane) show the entry's icon, name, a
 * framework badge (a rounded chip in the framework's color, mirroring the
 * node finder's detail rail) and spatial chips ("2D" / "3D").
 */
public class FunctionTreeCellRenderer implements TreeCellRenderer {

    private static final Icon GENERIC_NODE_ICON = JIPipe.RESOURCES.getIcon16("actions/tag.png");
    private static final Icon TEMPLATE_ICON = JIPipe.RESOURCES.getIcon16("actions/template.png");
    private static final int CHIP_ARC = 6;
    private final NodeBrowserTreeController controller;

    public FunctionTreeCellRenderer(NodeBrowserTreeController controller) {
        this.controller = controller;
    }

    /**
     * The 24×24 wrapper for template rows: the template icon centered on a circle
     * with NEUTRAL theme colors (panel-background fill, highlight-border ring) -
     * the look of a "node of no function family," so templates read as distinct
     * from the function rows' colored circles.
     */
    private Icon templateCircleIcon() {
        JIPipeDesktopModernThemeStyle style = ThemeUtils.getCurrentStyle();
        return new ColoredCircleIcon(TEMPLATE_ICON,
                style.getPanelBackground(), style.getBorderColor());
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
            // The template badge (Pattern / Custom), read from the entry -
            // spec §7.2. Non-templates get no badge.
            JComponent badge = JIPipeNodeStateChips.templateBadge(entry);
            if (badge != null) {
                rowPanel.add(badge);
            }
            return rowPanel;
        }

        JLabel label = new JLabel();
        label.setOpaque(true);
        label.setBorder(BorderFactory.createEmptyBorder(3, 4, 3, 4));
        if (userObject instanceof JIPipeNodeTaxonomyFunction function) {
            label.setText(function.getName());
            Icon icon = function.getIcon();
            // The function icon wrapped in a circle painted with the function's
            // node fill/border color (the exact color of its canvas nodes) -
            // the tree doubles as a legend for what node colors mean.
            JIPipeDesktopModernThemeStyle style = ThemeUtils.getCurrentStyle();
            FunctionRegistry registry = JIPipe.getNodes().getFunctions();
            label.setIcon(new ColoredCircleIcon(icon != null ? icon : GENERIC_NODE_ICON,
                    ThemeUtils.functionNodeFillColor(function, style, registry),
                    null));
            label.setToolTipText(function.getDescription());
            // Search highlight: the search matched a node filed under this
            // leaf (or one of its descendant leaves)
            if (controller.getSearchHighlightedLeafPaths().contains(new TreePath(node.getPath()))) {
                label.setFont(label.getFont().deriveFont(Font.BOLD));
            }
        } else if (userObject instanceof TemplateScopeTreeNode scopeNode) {
            // The synthetic "Templates" branch (spec §7.2): the header row and
            // its System/Global/Project children. Pure UI grouping - distinct
            // template icon, not a taxonomy function.
            label.setText(scopeNode.isRoot()
                    ? "Templates"
                    : scopeNode.getScope().name());
            label.setIcon(templateCircleIcon());
            label.setToolTipText(scopeNode.isRoot()
                    ? "Node templates grouped by storage scope"
                    : "Node templates stored in the " + scopeNode.getScope().name().toLowerCase() + " scope");
        } else if (userObject instanceof PatternsTreeNode patterns) {
            // The synthetic <function>/Patterns row: the function's node
            // templates, grouped below the function level (pure UI grouping,
            // not a taxonomy function - the function level itself shows only
            // real nodes).
            label.setText("Patterns");
            label.setIcon(templateCircleIcon());
            label.setToolTipText("Node template patterns filed under "
                    + (patterns.getFunction() != null ? patterns.getFunction().getName() : "this function"));
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
