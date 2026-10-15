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

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.database.JIPipeNodeDatabase;
import org.hkijena.jipipe.api.nodes.database.JIPipeNodeDatabaseEntry;
import org.hkijena.jipipe.api.nodes.database.entries.CreateNewNodeByInfoDatabaseEntry;
import org.hkijena.jipipe.api.nodes.taxonomy.JIPipeNodeTaxonomyFramework;
import org.hkijena.jipipe.api.nodes.taxonomy.JIPipeNodeTaxonomySpatialDimension;
import org.hkijena.jipipe.api.nodes.taxonomy.NoFramework;
import org.hkijena.jipipe.desktop.app.documentation.JIPipeDesktopAlgorithmCompendiumUI;
import org.hkijena.jipipe.desktop.commons.components.markup.JIPipeDesktopMarkdownReader;
import org.hkijena.jipipe.desktop.commons.components.renderers.JIPipeNodeStateChips;
import org.hkijena.jipipe.desktop.commons.components.tabs.JIPipeDesktopTabPane;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui.JIPipeDesktopGraphNodeUI;
import org.hkijena.jipipe.plugins.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.plugins.parameters.library.markup.MarkdownText;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.ThemeUtils;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The right-hand detail rail of the rebuilt node finder dialog (Task 17).
 * <p>
 * For the entry selected in the center list it shows, in order:
 * <ul>
 * <li>the node's name, framework badge (a rounded chip filled with the
 * {@link JIPipeNodeTaxonomyFramework#getColor() framework color}, labeled
 * with the framework's abbreviation; the {@link NoFramework} sentinel
 * renders as a neutral chip with a dash) and the entry's tags,</li>
 * <li>a one-line summary of the spatial dimensions and the input profile
 * (e.g. "Spatial: 2D, 3D · Input: 3D volume"),</li>
 * <li>a tab bar — the shared {@code JIPipeDesktopTabPane} in the AI
 * monitor's TopPill style, so the tabs stay consistent with the rest of
 * the application — switching between the detail pages:
 * <ul>
 * <li><b>Documentation</b> (default, help/info icon) — the canonical
 * node documentation: the same markdown the algorithm compendium and
 * the graph editor's node settings show (title, description,
 * input/output slot table, per-parameter documentation);</li>
 * <li><b>Similar</b> — the {@code database.relations(entry, 6)}
 * {@link org.hkijena.jipipe.api.nodes.database.relations.NodeRelations#similar() similar}
 * list (same function → framework → tags, workflow neighbors excluded);</li>
 * <li><b>Previous steps</b> (diagram-predecessor icon) — the
 * {@link org.hkijena.jipipe.api.nodes.database.relations.NodeRelations#previous() previous}
 * list (nodes that run before this one);</li>
 * <li><b>Next steps</b> (diagram-successor icon, matching the node browser's
 * relation strip) — the
 * {@link org.hkijena.jipipe.api.nodes.database.relations.NodeRelations#next() next}
 * list (nodes that run after this one).</li>
 * </ul>
 * Clicking a similar/previous/next item tells the dialog (via the
 * {@link EntrySelector} callback) to select that entry in the center list,
 * so the rail cannot recurse into itself.
 * <p>
 * <b>Follow-up (deferred, see work item comment):</b> the rail previously
 * offered "▶ Insert setup" and "Open example" actions that inserted nodes
 * into the graph. Both were removed from the finder — "Open example"
 * inserting a node is not what users expect there, and both actions are to
 * be reworked into a dedicated feature based on node templates (which
 * support multiple nodes).
 * </li>
 * </ul>
 * <p>
 * When no entry is selected the rail shows an empty-state label.
 * <p>
 * All data comes from the shared node database layer (Task 15/16):
 * {@code relations()} resolves the entry through its
 * {@link JIPipeNodeDatabaseEntry#getOriginNodeId() origin node id}.
 */
public class NodeFinderDetailRail extends JPanel {

    /**
     * Callback for entry clicks in the rail's "Similar" / "Previous steps" /
     * "Next steps" lists: the dialog selects the entry in the center list.
     */
    @FunctionalInterface
    public interface EntrySelector {
        void select(JIPipeNodeDatabaseEntry entry);
    }

    private static final int FRAMEWORK_CHIP_WIDTH = 52;
    private static final int FRAMEWORK_CHIP_HEIGHT = 16;
    private static final String CARD_EMPTY = "empty";
    private static final String CARD_CONTENT = "content";

    private final JIPipeNodeDatabase database;
    private final EntrySelector entrySelector;
    private final CardLayout cardLayout = new CardLayout();
    private JPanel cards;

    private final JLabel emptyLabel = new JLabel("Select a node to see details, similar nodes and workflow steps.",
        SwingConstants.CENTER);
    private final JPanel contentPanel = new JPanel();

    private final JLabel nameLabel = new JLabel();
    /**
     * The framework badge: a small non-opaque panel that paints a rounded
     * rectangle filled with {@link #frameworkChipColor}.
     */
    private final JPanel frameworkChip = new JPanel() {
        @Override
        protected void paintComponent(Graphics g) {
            if (frameworkChipColor != null) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setColor(frameworkChipColor);
                g2d.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2d.dispose();
            }
            super.paintComponent(g);
        }
    };
    private Color frameworkChipColor;
    private final JLabel frameworkChipLabel = new JLabel();
    private final JLabel tagsLabel = new JLabel();
    private final JLabel profileLabel = new JLabel();
    private final JPanel stateChips = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 0));

    // --- Tabs + pages (the shared tab pane, same component + style as the
    // AI monitor's tabs — see JIPipeDesktopAIMonitorWindow) ---
    private final JIPipeDesktopTabPane tabPane = new JIPipeDesktopTabPane(true, JIPipeDesktopTabPane.Style.TopPill);
    private final JIPipeDesktopMarkdownReader documentationReader = new JIPipeDesktopMarkdownReader(false);
    private final JList<JIPipeNodeDatabaseEntry> similarList = new JList<>();
    private final JList<JIPipeNodeDatabaseEntry> previousList = new JList<>();
    private final JList<JIPipeNodeDatabaseEntry> nextList = new JList<>();
    private final Map<JList<JIPipeNodeDatabaseEntry>, JPanel> listPanels = new IdentityHashMap<>();
    private final Map<JList<JIPipeNodeDatabaseEntry>, CardLayout> listCards = new IdentityHashMap<>();
    private final Map<JList<JIPipeNodeDatabaseEntry>, JPanel> listBodies = new IdentityHashMap<>();
    private final JLabel similarEmpty = new JLabel("No similar nodes", SwingConstants.CENTER);
    private final JLabel previousEmpty = new JLabel("No previous steps", SwingConstants.CENTER);
    private final JLabel nextEmpty = new JLabel("No next steps", SwingConstants.CENTER);

    private JIPipeNodeDatabaseEntry currentEntry;
    /**
     * Guards the similar/previous/next list selection listeners while the
     * rail programmatically swaps their contents, so the rail cannot recurse
     * (center list → rail → center list).
     */
    private boolean updatingLists;

    /**
     * Creates a new detail rail.
     *
     * @param database      the node database to query for the entry's
     *                      similar / previous / next relations
     * @param entrySelector called when the user clicks a similar/previous/next
     *                      entry (the dialog selects it in the center list)
     */
    public NodeFinderDetailRail(JIPipeNodeDatabase database, EntrySelector entrySelector) {
        this.database = database;
        this.entrySelector = entrySelector;
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());

        // Empty state
        emptyLabel.setForeground(Color.GRAY);
        JPanel emptyPanel = new JPanel(new GridBagLayout());
        emptyPanel.add(emptyLabel, new GridBagConstraints(0, 0, 1, 1, 1, 1, GridBagConstraints.CENTER,
            GridBagConstraints.BOTH, new Insets(40, 20, 40, 20), 0, 0));

        // Content state
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        initializeTitleBlock();
        initializeEntryList(similarList, similarEmpty);
        initializeEntryList(previousList, previousEmpty);
        initializeEntryList(nextList, nextEmpty);
        initializeTabs();
        contentPanel.add(titleBlock());
        contentPanel.add(Box.createVerticalStrut(8));
        // Minimum footprint 160 px, grows with the dialog (the tab content
        // is internally scrollable)
        tabPane.setTabPanelBorder(BorderFactory.createEmptyBorder(12, 4, 12, 4));
        tabPane.setPreferredSize(new Dimension(0, 160));
        tabPane.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        contentPanel.add(tabPane);

        this.cards = new JPanel(cardLayout);
        cards.add(emptyPanel, CARD_EMPTY);
        cards.add(new JScrollPane(contentPanel), CARD_CONTENT);
        add(cards, BorderLayout.CENTER);
        cardLayout.show(cards, CARD_EMPTY);
    }

    /**
     * Shows the details of the given entry, or the empty state if
     * {@code entry} is {@code null}.
     *
     * @param entry the selected entry, or {@code null} for the empty state
     */
    public void setEntry(@Nullable JIPipeNodeDatabaseEntry entry) {
        if (entry == null) {
            currentEntry = null;
            cardLayout.show(cards, CARD_EMPTY);
            return;
        }
        currentEntry = entry;
        cardLayout.show(cards, CARD_CONTENT);
        fillContent(entry);
    }

    /**
     * @return the entry currently shown in the rail
     */
    public JIPipeNodeDatabaseEntry getCurrentEntry() {
        return currentEntry;
    }

    // ------------------------------------------------------------------
    // Content
    // ------------------------------------------------------------------

    private void fillContent(JIPipeNodeDatabaseEntry entry) {
        // Title block: name + state chips + framework chip + tags + profile line
        nameLabel.setText(entry.getName());
        stateChips.removeAll();
        stateChips.add(JIPipeNodeStateChips.panel(entry));
        stateChips.revalidate();
        JIPipeNodeTaxonomyFramework framework = entry.getFramework();
        frameworkChipLabel.setText(framework.getAbbreviation());
        boolean noFramework = NoFramework.ID.equals(framework.getId());
        frameworkChipColor = noFramework ? Color.GRAY : framework.getColor();
        frameworkChip.setToolTipText(noFramework ? "No external framework" : "Framework: " + framework.getName());
        frameworkChip.revalidate();

        Set<String> tags = entry.getTags();
        tagsLabel.setText(tags.isEmpty() ? "" : "Tags: " + String.join(", ", tags.stream().sorted().toList()));
        tagsLabel.setVisible(!tags.isEmpty());
        profileLabel.setText(buildProfileLine(entry));

        // Documentation page: the same markdown the compendium and the
        // graph editor's node documentation use (see buildDocumentation)
        documentationReader.setDocument(buildDocumentation(entry));

        // Similar + previous + next (synchronous; the database caches all
        // three in one NodeRelations). The updatingLists guard suppresses
        // the list selection listeners so programmatic list data changes
        // do not fire the EntrySelector.
        updatingLists = true;
        try {
            var relations = database.relations(entry, 6);
            similarList.setListData(relations.similar().toArray(new JIPipeNodeDatabaseEntry[0]));
            previousList.setListData(relations.previous().toArray(new JIPipeNodeDatabaseEntry[0]));
            nextList.setListData(relations.next().toArray(new JIPipeNodeDatabaseEntry[0]));
        } finally {
            updatingLists = false;
        }
        updateListEmptyStates();

        // Back to the documentation tab (the default) on every entry change
        tabPane.switchToContent(documentationReader);

        contentPanel.revalidate();
        contentPanel.repaint();
    }

    /**
     * The spatial + input profile summary line, e.g.
     * "Spatial: 2D, 3D · Input: 3D volume".
     */
    private String buildProfileLine(JIPipeNodeDatabaseEntry entry) {
        StringBuilder builder = new StringBuilder();
        Set<JIPipeNodeTaxonomySpatialDimension> spatial = entry.getSpatialDimensions();
        builder.append("Spatial: ");
        if (spatial.isEmpty()) {
            builder.append("N/A");
        } else if (spatial.contains(JIPipeNodeTaxonomySpatialDimension.SPATIAL_2D)
            && spatial.contains(JIPipeNodeTaxonomySpatialDimension.SPATIAL_3D)) {
            builder.append("2D, 3D");
        } else if (spatial.contains(JIPipeNodeTaxonomySpatialDimension.SPATIAL_3D)) {
            builder.append("3D");
        } else {
            builder.append("2D");
        }
        builder.append("  ·  Input: ");
        switch (entry.getInputProfile()) {
            case PLANE_2D -> builder.append("2D plane");
            case VOLUME_3D -> builder.append("3D volume");
            case ANY -> builder.append("any");
            default -> builder.append("N/A");
        }
        return builder.toString();
    }

    /**
     * Shows the empty-state label (instead of the list box) when a
     * similar/previous/next list has no entries.
     */
    private void updateListEmptyStates() {
        for (JList<JIPipeNodeDatabaseEntry> list : new JList[]{similarList, previousList, nextList}) {
            // The list body is a CardLayout: the empty-state card shows only
            // when the list has no entries (two components cannot share a
            // BorderLayout CENTER — only the last-added one would lay out)
            listCards.get(list).show(listBodies.get(list),
                list.getModel().getSize() == 0 ? "empty" : "list");
        }
    }

    // ------------------------------------------------------------------
    // Title block
    // ------------------------------------------------------------------

    private void initializeTitleBlock() {
        nameLabel.setFont(new Font(Font.DIALOG, Font.BOLD, ThemeUtils.getCurrentStyle().getFontSizeNormal() + 2));
        tagsLabel.setForeground(Color.GRAY);
        tagsLabel.setFont(new Font(Font.DIALOG, Font.PLAIN, ThemeUtils.getCurrentStyle().getFontSizeTiny()));
        profileLabel.setForeground(Color.GRAY);
        profileLabel.setFont(new Font(Font.DIALOG, Font.PLAIN, ThemeUtils.getCurrentStyle().getFontSizeTiny()));

        frameworkChip.setOpaque(false);
        frameworkChip.setPreferredSize(new Dimension(FRAMEWORK_CHIP_WIDTH, FRAMEWORK_CHIP_HEIGHT));
        frameworkChip.setMaximumSize(new Dimension(FRAMEWORK_CHIP_WIDTH, FRAMEWORK_CHIP_HEIGHT));
        frameworkChip.setLayout(new GridBagLayout());
        frameworkChipLabel.setForeground(Color.WHITE);
        frameworkChipLabel.setHorizontalAlignment(SwingConstants.CENTER);
        frameworkChipLabel.setFont(new Font(Font.DIALOG, Font.BOLD, ThemeUtils.getCurrentStyle().getFontSizeTiny()));
        frameworkChip.add(frameworkChipLabel, new GridBagConstraints());
        frameworkChipColor = Color.GRAY;
    }

    private JPanel titleBlock() {
        JPanel block = new JPanel();
        block.setLayout(new BoxLayout(block, BoxLayout.Y_AXIS));
        stateChips.setOpaque(false);
        JPanel nameRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        nameRow.add(nameLabel);
        nameRow.add(stateChips);
        nameRow.add(frameworkChip);
        block.add(nameRow);
        block.add(tagsLabel);
        block.add(profileLabel);
        return block;
    }

    // ------------------------------------------------------------------
    // Tabs (the shared tab pane, same component as the AI monitor)
    // ------------------------------------------------------------------

    private void initializeTabs() {
        tabPane.addTab("Documentation", JIPipe.RESOURCES.getIcon16("actions/help-info.png"),
            documentationReader, JIPipeDesktopTabPane.CloseMode.withoutCloseButton);
        tabPane.addTab("Similar", JIPipe.RESOURCES.getIcon16("actions/tag.png"),
            listPanels.get(similarList), JIPipeDesktopTabPane.CloseMode.withoutCloseButton);
        tabPane.addTab("Previous steps", JIPipe.RESOURCES.getIcon16("actions/diagram-predecessor.png"),
            listPanels.get(previousList), JIPipeDesktopTabPane.CloseMode.withoutCloseButton);
        tabPane.addTab("Next steps", JIPipe.RESOURCES.getIcon16("actions/diagram-successor.png"),
            listPanels.get(nextList), JIPipeDesktopTabPane.CloseMode.withoutCloseButton);
    }

    /**
     * The Documentation tab content. For node entries this is the canonical
     * node documentation — the same markdown the algorithm compendium and
     * the graph editor's node settings show (title, description, input/output
     * slot table, parameter documentation). Non-node entries (templates,
     * examples, existing graph nodes) fall back to name + description.
     */
    private MarkdownText buildDocumentation(JIPipeNodeDatabaseEntry entry) {
        JIPipeNodeInfo info = entryNodeInfo(entry);
        if (info != null) {
            return JIPipeDesktopAlgorithmCompendiumUI.buildAlgorithmDocumentation(info);
        }
        StringBuilder builder = new StringBuilder();
        builder.append("# ").append(entry.getName()).append("\n\n");
        HTMLText description = entry.getDescription();
        if (description != null && !StringUtils.isNullOrEmpty(description.getBody())) {
            builder.append(description.getBody());
        }
        return new MarkdownText(builder.toString());
    }

    // ------------------------------------------------------------------
    // Similar / previous / next lists
    // ------------------------------------------------------------------

    private void initializeEntryList(JList<JIPipeNodeDatabaseEntry> list, JLabel emptyState) {
        list.setOpaque(false);
        listCards.put(list, new CardLayout());
        list.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> view, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(view, value, index, isSelected, cellHasFocus);
                if (value instanceof JIPipeNodeDatabaseEntry entry) {
                    label.setIcon(entry.getIcon());
                    label.setText(entry.getName());
                    label.setToolTipText(entry.getDescription().toPlainText());
                }
                return label;
            }
        });
        list.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (updatingLists || !SwingUtilities.isLeftMouseButton(e)) {
                    return;
                }
                int index = list.locationToIndex(e.getPoint());
                if (index >= 0 && index < list.getModel().getSize()) {
                    JIPipeNodeDatabaseEntry clicked = list.getModel().getElementAt(index);
                    if (clicked != null) {
                        entrySelector.select(clicked);
                    }
                }
            }
        });
        emptyState.setForeground(Color.GRAY);

        JPanel body = new JPanel(listCards.get(list));
        listBodies.put(list, body);
        JScrollPane scroll = new JScrollPane(list);
        scroll.setBorder(null);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        body.add(scroll, "list");
        emptyState.setOpaque(false);
        body.add(emptyState, "empty");
        listCards.get(list).show(body, "empty");

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
        panel.add(body, BorderLayout.CENTER);
        listPanels.put(list, panel);
    }

    // ------------------------------------------------------------------
    // Actions
    // ------------------------------------------------------------------

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    @Nullable
    private static JIPipeNodeInfo entryNodeInfo(JIPipeNodeDatabaseEntry entry) {
        if (entry instanceof CreateNewNodeByInfoDatabaseEntry byInfo) {
            return byInfo.getNodeInfo();
        }
        return null;
    }
}
