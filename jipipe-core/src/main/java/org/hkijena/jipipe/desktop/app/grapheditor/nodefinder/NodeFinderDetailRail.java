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
import org.hkijena.jipipe.desktop.commons.components.panels.JIPipeDesktopFormPanel;
import org.hkijena.jipipe.desktop.commons.components.renderers.JIPipeNodeStateChips;
import org.hkijena.jipipe.desktop.commons.components.tabs.JIPipeDesktopTabPane;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui.JIPipeDesktopGraphNodeUI;
import org.hkijena.jipipe.plugins.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.plugins.parameters.library.markup.MarkdownText;
import org.hkijena.jipipe.utils.CustomStateLessToggleButton;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.ThemeUtils;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.desktop.commons.theme.JIPipeDesktopModernThemeStyle;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
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
 * <li><b>Related</b> (link icon) — the
 * {@code database.relations(entry, 6)} lists as three collapsible
 * sections in one scrollable page:
 * {@link org.hkijena.jipipe.api.nodes.database.relations.NodeRelations#similar() Similar}
 * (same function → framework → tags, workflow neighbors excluded),
 * {@link org.hkijena.jipipe.api.nodes.database.relations.NodeRelations#previous() Previous steps}
 * (nodes that run before this one) and
 * {@link org.hkijena.jipipe.api.nodes.database.relations.NodeRelations#next() Next steps}
 * (nodes that run after this one). The sections reuse the
 * {@code GroupHeaderPanel} headers of the "Configure node templates"
 * manager, and each section's rows are that manager's rounded cards —
 * adapted from its wrapping grid to a single-column list (icon + bold
 * name + one-line description). Non-empty sections are expanded, empty
 * ones collapse to their header. Clicking a card tells the dialog (via
 * the {@link EntrySelector} callback) to select that entry in the
 * center list, so the rail cannot recurse into itself.</li>
 * </ul>
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
    // --- Related page: one scrollable form with the three relation lists as
    // collapsible sections (the "Configure node templates" manager's
    // GroupHeaderPanel + card rows, adapted from a wrapping grid to a
    // single-column list) ---
    private JList<JIPipeNodeDatabaseEntry> similarList;
    private JList<JIPipeNodeDatabaseEntry> previousList;
    private JList<JIPipeNodeDatabaseEntry> nextList;
    private final RelatedSection similarSection =
        new RelatedSection("Similar", JIPipe.RESOURCES.getIcon16("actions/tag.png"));
    private final RelatedSection previousSection =
        new RelatedSection("Previous steps", JIPipe.RESOURCES.getIcon16("actions/diagram-predecessor.png"));
    private final RelatedSection nextSection =
        new RelatedSection("Next steps", JIPipe.RESOURCES.getIcon16("actions/diagram-successor.png"));

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

        // Content state: the header block (name + chips + tags + profile)
        // stays fixed above the tab bar; the tab pages scroll internally.
        contentPanel.setLayout(new BorderLayout(0, 8));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        initializeTitleBlock();
        similarList = similarSection.initializeList(this::selectListEntry);
        previousList = previousSection.initializeList(this::selectListEntry);
        nextList = nextSection.initializeList(this::selectListEntry);
        // Set the pill border BEFORE the tabs are added: each tab component
        // captures the pane's border at construction (the AI monitor — the
        // design reference — does the same order). Setting it after
        // initializeTabs() left the pills with the thin default border
        // (4,0,4,0 — no horizontal padding, pills touching: the
        // "no padding and margin" report).
        tabPane.setTabPanelBorder(BorderFactory.createEmptyBorder(12, 4, 12, 4));
        initializeTabs();
        contentPanel.add(titleBlock(), BorderLayout.NORTH);
        // Minimum footprint 160 px, grows with the dialog (the tab content
        // is internally scrollable)
        tabPane.setPreferredSize(new Dimension(0, 160));
        contentPanel.add(tabPane, BorderLayout.CENTER);

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
    // Test accessors (headless UI tests; no production callers)
    // ------------------------------------------------------------------

    JIPipeDesktopTabPane tabPaneForTest() {
        return tabPane;
    }

    /** The Similar section's card list (the rail's relation lists). */
    JList<JIPipeNodeDatabaseEntry> similarListForTest() {
        return similarList;
    }

    /** The Previous steps section's card list. */
    JList<JIPipeNodeDatabaseEntry> previousListForTest() {
        return previousList;
    }

    /** The Next steps section's card list. */
    JList<JIPipeNodeDatabaseEntry> nextListForTest() {
        return nextList;
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
        // Section expand/collapse follows the content on every entry change
        // (non-empty sections expanded, empty ones collapsed to their
        // header) — a manual toggle stays valid until the next entry.
        similarSection.applyContentState(similarList.getModel().getSize() > 0);
        previousSection.applyContentState(previousList.getModel().getSize() > 0);
        nextSection.applyContentState(nextList.getModel().getSize() > 0);

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

    // ------------------------------------------------------------------
    // Related page (the merged Similar / Previous steps / Next steps)
    // ------------------------------------------------------------------

    /**
     * Builds the "Related" tab page: one scrollable form panel (the same
     * {@code WITH_SCROLLING} form machinery the "Configure node templates"
     * manager uses) holding the three relation sections — {@code Similar},
     * {@code Previous steps}, {@code Next steps} — each a collapsible
     * {@code GroupHeaderPanel} over a single-column list of the manager's
     * rounded card rows (grid → list).
     * <p>
     * The form is NOT wrapped in an island: the tab content sits inside the
     * dialog's split pane, and the form's opaque panel background already
     * provides the surface (the template manager wraps in an island only
     * because it floats on a window background).
     */
    private JComponent buildRelatedPage() {
        JIPipeDesktopFormPanel formPanel = new JIPipeDesktopFormPanel(JIPipeDesktopFormPanel.WITH_SCROLLING);
        similarSection.buildSectionOn(formPanel);
        previousSection.buildSectionOn(formPanel);
        nextSection.buildSectionOn(formPanel);
        formPanel.addVerticalGlue();
        return formPanel;
    }

    /**
     * Selects the clicked list entry in the center list — guarded so the
     * rail's own programmatic list swaps cannot recurse.
     */
    private void selectListEntry(JIPipeNodeDatabaseEntry entry) {
        if (updatingLists) {
            return;
        }
        entrySelector.select(entry);
    }

    // ------------------------------------------------------------------
    // Tabs (the shared tab pane, same component as the AI monitor)
    // ------------------------------------------------------------------

    private void initializeTabs() {
        tabPane.addTab("Documentation", JIPipe.RESOURCES.getIcon16("actions/help-info.png"),
            documentationReader, JIPipeDesktopTabPane.CloseMode.withoutCloseButton);
        // The Similar / Previous steps / Next steps lists are merged into one
        // "Related" page (three collapsible sections) — the three separate
        // tabs cluttered the narrow rail's pill row and hid the groups from
        // each other.
        tabPane.addTab("Related", JIPipe.RESOURCES.getIcon16("actions/link.png"),
            buildRelatedPage(), JIPipeDesktopTabPane.CloseMode.withoutCloseButton);
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
    // One collapsible Related section (GroupHeaderPanel + card list)
    // ------------------------------------------------------------------

    /**
     * A section of the "Related" page: the shared {@code GroupHeaderPanel}
     * header (title + icon, the flat collapse caret of the parameter forms
     * at the start, the live entry count at the end — the "Configure node
     * templates" manager's section look) over a single-column list of that
     * manager's rounded card rows.
     * <p>
     * Unlike the manager's wrapping card GRID ({@code HORIZONTAL_WRAP} +
     * fixed cells), this list is a plain vertical list: full section width,
     * one card per row (the "list, not grid" adaptation of the manager's
     * layout).
     */
    private class RelatedSection {
        /** The relation list's row height (the manager's grid cards are 66 px). */
        private static final int CARD_ROW_HEIGHT = 46;

        private final String title;
        private final Icon icon;
        private JList<JIPipeNodeDatabaseEntry> list;
        private JPanel listHost;
        private JIPipeDesktopFormPanel.GroupHeaderPanel header;
        private CustomStateLessToggleButton collapseButton;
        private JLabel countLabel;

        RelatedSection(String title, Icon icon) {
            this.title = title;
            this.icon = icon;
        }

        /**
         * Creates and configures this section's list (card renderer + click
         * handler) — called before {@link #buildSectionOn} so the section
         * can be added to the form afterwards.
         */
        JList<JIPipeNodeDatabaseEntry> initializeList(java.util.function.Consumer<JIPipeNodeDatabaseEntry> onEntryClick) {
            this.list = new JList<>(new DefaultListModel<>());
            this.list.setOpaque(false);
            this.list.setFixedCellHeight(CARD_ROW_HEIGHT);
            this.list.setCellRenderer(new CardRowRenderer());
            this.list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            // The card renderer paints its OWN rounded selection; suppress
            // the list's default rectangular selection fill (the manager's
            // cards do the same).
            this.list.setSelectionBackground(ThemeUtils.getCurrentStyle().getPanelBackground());
            this.list.setBorder(BorderFactory.createEmptyBorder(2, 4, 6, 4));
            this.list.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (!SwingUtilities.isLeftMouseButton(e)) {
                        return;
                    }
                    int index = list.locationToIndex(e.getPoint());
                    if (index >= 0 && index < list.getModel().getSize()) {
                        JIPipeNodeDatabaseEntry clicked = list.getModel().getElementAt(index);
                        if (clicked != null) {
                            onEntryClick.accept(clicked);
                        }
                    }
                }
            });
            return this.list;
        }

        /**
         * Adds this section to the form panel: a full-width group header and
         * the full-width card list right below it — the same
         * {@code addGroupHeader} + {@code addWideToForm} pattern and the
         * same collapse mechanism (the form's GridBag skips invisible
         * components) the "Configure node templates" manager uses.
         */
        void buildSectionOn(JIPipeDesktopFormPanel formPanel) {
            this.header = formPanel.addGroupHeader(title, icon);

            // A plain width-forcing host, NOT a scroll pane: the form's
            // single outer scroll (WITH_SCROLLING) scrolls the whole page;
            // an inner scroll pane would swallow the mouse wheel (the
            // manager's documented wheel re-targeting fix).
            this.listHost = new JPanel(new BorderLayout());
            this.listHost.setOpaque(false);
            this.listHost.add(this.list, BorderLayout.CENTER);
            formPanel.addWideToForm(this.listHost);

            this.collapseButton = new CustomStateLessToggleButton("",
                JIPipe.RESOURCES.getIcon16("actions/caret-right.png"),
                "",
                JIPipe.RESOURCES.getIcon16("actions/caret-down.png"),
                true);
            UIUtils.makeButtonFlat25x25(collapseButton);
            collapseButton.setOpaque(false); // no toggled/pressed background
            collapseButton.setFocusPainted(false);
            collapseButton.setToolTipText("Collapse");
            header.addToStartOfTitlePanel(collapseButton);

            this.countLabel = new JLabel();
            countLabel.setOpaque(false);
            countLabel.setForeground(ThemeUtils.getCurrentStyle().getTextMuted());
            countLabel.setFont(countLabel.getFont().deriveFont(Font.PLAIN, ThemeUtils.getCurrentStyle().getFontSizeTiny()));
            header.addToEndOfTitlePanel(countLabel);

            header.setCollapsed(false);
            collapseButton.getToggledEventEmitter().subscribeLambda((e, listener) -> {
                boolean nowExpanded = collapseButton.getState();
                header.setCollapsed(!nowExpanded);
                listHost.setVisible(nowExpanded);
                collapseButton.setToolTipText(nowExpanded ? "Collapse" : "Expand");
                formPanel.revalidate();
            });
            updateCount();
        }

        /**
         * Expands/collapses this section to match its content and refreshes
         * the header count — called on every entry change, so the expand
         * state is always "non-empty = expanded" for the new entry (a
         * manual toggle stays valid until the next entry is selected).
         */
        void applyContentState(boolean hasContent) {
            if (listHost == null) {
                return;
            }
            collapseButton.setState(hasContent);
            header.setCollapsed(!hasContent);
            listHost.setVisible(hasContent);
            collapseButton.setToolTipText(hasContent ? "Collapse" : "Expand");
            updateCount();
        }

        private void updateCount() {
            if (countLabel == null) {
                return;
            }
            int n = list.getModel().getSize();
            countLabel.setText(n == 0 ? "" : "(" + n + ")");
        }
    }

    // ------------------------------------------------------------------
    // The card row renderer (the manager's rounded card, single-column)
    // ------------------------------------------------------------------

    /**
     * A rounded card row — the "Configure node templates" manager's card
     * ({@code CardRenderer}) adapted to a single-column list: the card is
     * painted inset by a GUTTER within its full-width row, with the entry
     * icon, the bold name and a one-line muted description. The row is
     * transparent; the card is painted in {@link #paintComponent} so the
     * corners are rounded and the inter-row spacing is a real gap.
     */
    private class CardRowRenderer extends JPanel implements ListCellRenderer<JIPipeNodeDatabaseEntry> {
        private static final int CARD_INSET = 4;
        private static final int CARD_ARC = 10;

        private final JLabel icon = new JLabel();
        private final JLabel name = new JLabel();
        private final JLabel desc = new JLabel();
        private boolean cardSelected = false;

        CardRowRenderer() {
            setLayout(new BorderLayout(8, 1));
            setOpaque(false);
            // The CONTENT sits inside the painted card: gutter + inner pad.
            setBorder(BorderFactory.createEmptyBorder(CARD_INSET + 4, CARD_INSET + 8, CARD_INSET + 4, CARD_INSET + 8));

            icon.setVerticalAlignment(SwingConstants.CENTER);
            name.setOpaque(false);
            name.setFont(name.getFont().deriveFont(Font.BOLD));
            desc.setOpaque(false);
            desc.setForeground(ThemeUtils.getCurrentStyle().getTextMuted());

            JPanel text = new JPanel();
            text.setOpaque(false);
            text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
            name.setAlignmentX(LEFT_ALIGNMENT);
            desc.setAlignmentX(LEFT_ALIGNMENT);
            text.add(name);
            text.add(desc);
            add(icon, BorderLayout.WEST);
            add(text, BorderLayout.CENTER);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int x = CARD_INSET;
            int y = CARD_INSET;
            int w = getWidth() - 2 * CARD_INSET - 1;
            int h = getHeight() - 2 * CARD_INSET - 1;
            JIPipeDesktopModernThemeStyle style = ThemeUtils.getCurrentStyle();
            g2.setColor(cardSelected ? style.getSelectionBackground() : style.getPanelBackground());
            g2.fillRoundRect(x, y, w, h, CARD_ARC, CARD_ARC);
            g2.setColor(cardSelected ? style.getSelectionHighlight() : style.getBorderColor());
            g2.drawRoundRect(x, y, w, h, CARD_ARC, CARD_ARC);
            g2.dispose();
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends JIPipeNodeDatabaseEntry> view, JIPipeNodeDatabaseEntry value,
                                                      int index, boolean isSelected, boolean cellHasFocus) {
            // Full-width row: the list stretches the cell, so only the
            // HEIGHT is pinned (width from the view).
            setPreferredSize(new Dimension(Math.max(view.getWidth(), 1), RelatedSection.CARD_ROW_HEIGHT));
            cardSelected = isSelected;
            if (value != null) {
                icon.setIcon(value.getIcon());
                name.setText(value.getName());
                name.setToolTipText(value.getName());
                String d = shortDescription(value);
                desc.setText(d);
                desc.setToolTipText(value.getDescription() != null ? value.getDescription().getBody() : "");
            } else {
                name.setText("<null>");
                desc.setText("");
            }
            JIPipeDesktopModernThemeStyle style = ThemeUtils.getCurrentStyle();
            if (isSelected) {
                Color fg = style.getSelectionForeground();
                setForeground(fg);
                name.setForeground(fg);
                desc.setForeground(fg);
            } else {
                setForeground(style.getTextForeground());
                name.setForeground(style.getTextForeground());
                desc.setForeground(style.getTextMuted());
            }
            return this;
        }

        private String shortDescription(JIPipeNodeDatabaseEntry entry) {
            HTMLText description = entry.getDescription();
            if (description != null) {
                String body = description.getBody().replace('\n', ' ').trim();
                if (!body.isEmpty()) {
                    return body.length() > 120 ? body.substring(0, 119) + "…" : body;
                }
            }
            return "";
        }
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
