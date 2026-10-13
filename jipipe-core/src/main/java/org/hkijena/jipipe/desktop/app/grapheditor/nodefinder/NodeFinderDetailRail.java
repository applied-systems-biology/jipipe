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
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.JIPipeNodeExample;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.database.JIPipeNodeDatabase;
import org.hkijena.jipipe.api.nodes.database.JIPipeNodeDatabaseEntry;
import org.hkijena.jipipe.api.nodes.database.entries.CreateNewNodeByInfoDatabaseEntry;
import org.hkijena.jipipe.api.nodes.database.entries.CreateNewNodeByExampleDatabaseEntry;
import org.hkijena.jipipe.api.nodes.database.entries.CreateNewNodesByTemplateDatabaseEntry;
import org.hkijena.jipipe.api.nodes.taxonomy.JIPipeNodeTaxonomyFamily;
import org.hkijena.jipipe.api.nodes.taxonomy.JIPipeNodeTaxonomyFramework;
import org.hkijena.jipipe.api.nodes.taxonomy.JIPipeNodeTaxonomySpatialDimension;
import org.hkijena.jipipe.api.nodes.taxonomy.NoFramework;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphCanvasUI;
import org.hkijena.jipipe.desktop.commons.components.renderers.JIPipeNodeStateChips;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui.JIPipeDesktopGraphNodeUI;
import org.hkijena.jipipe.plugins.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.plugins.parameters.library.markup.MarkdownText;
import org.hkijena.jipipe.utils.DocumentationUtils;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.ThemeUtils;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.lang.reflect.Method;
import java.util.HashSet;
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
 * <li>a <b>custom pill tab bar</b> (styled like the AI monitor's top pills)
 * switching between the detail pages:
 * <ul>
 * <li><b>Documentation</b> (default, help/info icon) — the entry's
 * description rendered as markdown, then per-parameter documentation
 * (the same {@link SetJIPipeDocumentation} scan the node settings UI
 * performs) and the class documentation when present;</li>
 * <li><b>Related</b> — the {@code database.related(entry, 6)} list;</li>
 * <li><b>Follow-ups</b> (diagram-next icon, matching the node browser's
 * follow-up strip) — the {@code database.followUps(entry)} list.</li>
 * </ul>
 * Clicking a related/follow-up item tells the dialog (via the
 * {@link EntrySelector} callback) to select that entry in the center list,
 * so the rail cannot recurse into itself.</li>
 * <li>the actions: <b>▶ Insert setup</b> — when the entry's node belongs to
 * a workflow family with a non-null
 * {@link JIPipeNodeTaxonomyFamily#getTemplate() template}, the template is
 * inserted via
 * {@link CreateNewNodesByTemplateDatabaseEntry#addToGraph(JIPipeDesktopGraphCanvasUI)};
 * otherwise the entry's follow-ups are each inserted unconnected; when the
 * node has neither (the common case — most nodes belong to no workflow
 * family), the node <em>itself</em> is inserted, so the button always does
 * something. <b>Open example</b> — inserts the node's registered example
 * ({@code JIPipe.getNodes().getNodeExamples(originNodeId)}); when none is
 * registered the button stays enabled and its tooltip says so.
 * </li>
 * </ul>
 * <p>
 * When no entry is selected the rail shows an empty-state label.
 * <p>
 * All data comes from the shared node database layer (Task 15/16):
 * {@code related}/{@code followUps} resolve the entry through its
 * {@link JIPipeNodeDatabaseEntry#getOriginNodeId() origin node id}; the
 * family used for the actions is looked up by walking the node registry's
 * family map against
 * {@code JIPipe.getNodes().getFamilyAssembler().familyIdsOf(originNodeId)}.
 */
public class NodeFinderDetailRail extends JPanel {

    /**
     * Callback for entry clicks in the rail's "Related" / "Follow-ups"
     * lists: the dialog selects the entry in the center list.
     */
    @FunctionalInterface
    public interface EntrySelector {
        void select(JIPipeNodeDatabaseEntry entry);
    }

    private static final int FRAMEWORK_CHIP_WIDTH = 52;
    private static final int FRAMEWORK_CHIP_HEIGHT = 16;
    private static final int PILL_H = 26;
    private static final int PILL_ARC = 13;
    private static final String CARD_EMPTY = "empty";
    private static final String CARD_CONTENT = "content";

    private final JIPipeNodeDatabase database;
    private final JIPipeDesktopGraphCanvasUI canvasUI;
    private final EntrySelector entrySelector;
    private final CardLayout cardLayout = new CardLayout();
    private final CardLayout pageLayout = new CardLayout();
    private JPanel cards;
    private JPanel pages;

    private final JLabel emptyLabel = new JLabel("Select a node to see details, related nodes and follow-ups.",
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

    // --- Pill tabs + pages ---
    private final JPanel pillBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
    private PillButton docsPill;
    private PillButton relatedPill;
    private PillButton followUpsPill;
    private final JEditorPane documentationPane = new JEditorPane();
    private final JList<JIPipeNodeDatabaseEntry> relatedList = new JList<>();
    private final JList<JIPipeNodeDatabaseEntry> followUpsList = new JList<>();
    private final Map<JList<JIPipeNodeDatabaseEntry>, JPanel> listPanels = new IdentityHashMap<>();
    private final Map<JList<JIPipeNodeDatabaseEntry>, CardLayout> listCards = new IdentityHashMap<>();
    private final Map<JList<JIPipeNodeDatabaseEntry>, JPanel> listBodies = new IdentityHashMap<>();
    private final JLabel relatedEmpty = new JLabel("No related nodes", SwingConstants.CENTER);
    private final JLabel followUpsEmpty = new JLabel("No follow-ups", SwingConstants.CENTER);

    private final JButton insertSetupButton = new JButton("▶ Insert setup");
    private final JButton openExampleButton = new JButton("Open example");

    private JIPipeNodeDatabaseEntry currentEntry;
    /**
     * Guards the related/follow-up list selection listeners while the rail
     * programmatically swaps their contents, so the rail cannot recurse
     * (center list → rail → center list).
     */
    private boolean updatingLists;

    /**
     * Creates a new detail rail.
     *
     * @param database      the node database to query for related entries
     *                      and follow-ups
     * @param canvasUI      the graph canvas UI that inserted nodes are
     *                      added to
     * @param entrySelector called when the user clicks a related/follow-up
     *                      entry (the dialog selects it in the center list)
     */
    public NodeFinderDetailRail(JIPipeNodeDatabase database, JIPipeDesktopGraphCanvasUI canvasUI, EntrySelector entrySelector) {
        this.database = database;
        this.canvasUI = canvasUI;
        this.entrySelector = entrySelector;

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
        initializePillBar();
        initializeDocumentationPage();
        initializeEntryList("Related", relatedList, relatedEmpty);
        initializeEntryList("Follow-ups", followUpsList, followUpsEmpty);
        initializeActions();
        contentPanel.add(titleBlock());
        contentPanel.add(Box.createVerticalStrut(8));
        contentPanel.add(pillBar);
        contentPanel.add(Box.createVerticalStrut(4));
        pages = new JPanel(pageLayout);
        pages.add(new JScrollPane(documentationPane), "docs");
        pages.add(listPanels.get(relatedList), "related");
        pages.add(listPanels.get(followUpsList), "followups");
        pages.setPreferredSize(new Dimension(0, 160));
        pages.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        contentPanel.add(pages);
        contentPanel.add(Box.createVerticalStrut(8));
        contentPanel.add(insertSetupButton);
        contentPanel.add(openExampleButton);
        selectPill(docsPill);

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

        // Documentation page (markdown-rendered description + parameter
        // docs + class docs)
        documentationPane.setText(buildDocumentationHtml(entry));
        documentationPane.setCaretPosition(0);

        // Related + follow-ups (synchronous; the database caches both).
        // The updatingLists guard suppresses the list selection listeners
        // so programmatic list data changes do not fire the EntrySelector.
        updatingLists = true;
        try {
            relatedList.setListData(database.related(entry, 6).toArray(new JIPipeNodeDatabaseEntry[0]));
            followUpsList.setListData(database.followUps(entry).toArray(new JIPipeNodeDatabaseEntry[0]));
        } finally {
            updatingLists = false;
        }
        updateListEmptyStates();

        // Actions: family resolution
        JIPipeNodeTaxonomyFamily family = resolveFamily(entry);
        String familyName = family != null ? family.getName() : null;
        List<JIPipeNodeDatabaseEntry> followUps = database.followUps(entry);
        if (family != null && family.getTemplate() != null) {
            insertSetupButton.setToolTipText("Inserts the '" + familyName + "' family's setup template");
        } else if (!followUps.isEmpty()) {
            insertSetupButton.setToolTipText(familyName == null
                ? "Inserts the follow-up nodes of this node, unconnected"
                : "Inserts the follow-up nodes of this node, unconnected (the '" + familyName + "' family has no template)");
        } else {
            insertSetupButton.setToolTipText("Inserts this node");
        }
        boolean hasExample = hasNodeExample(entry);
        openExampleButton.setToolTipText(hasExample
            ? "Inserts the example of this node"
            : "No example is available for this node");

        // Back to the documentation page (the default) on every entry change
        selectPill(docsPill);

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
            builder.append("—");
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
            default -> builder.append("—");
        }
        return builder.toString();
    }

    /**
     * Shows the empty-state label (instead of the list box) when a
     * related/follow-up list has no entries.
     */
    private void updateListEmptyStates() {
        for (JList<JIPipeNodeDatabaseEntry> list : new JList[]{relatedList, followUpsList}) {
            // The list body is a CardLayout: the empty-state card shows only
            // when the list has no entries (two components cannot share a
            // BorderLayout CENTER — only the last-added one would lay out)
            listCards.get(list).show(listBodies.get(list),
                list.getModel().getSize() == 0 ? "empty" : "list");
        }
    }

    /**
     * The Documentation page content as HTML: the entry's description
     * rendered from markdown (the same way the node settings and the node
     * browser's context help render it), then the per-parameter
     * documentation (every {@link SetJIPipeDocumentation} on the node
     * class's methods — the same source the settings UI shows) and the
     * class documentation when present.
     */
    private String buildDocumentationHtml(JIPipeNodeDatabaseEntry entry) {
        StringBuilder builder = new StringBuilder();
        builder.append("<h3>").append(escapeHtml(entry.getName())).append("</h3>");
        HTMLText description = entry.getDescription();
        if (description != null && !StringUtils.isNullOrEmpty(description.getBody())) {
            builder.append(new MarkdownText(description.getBody()).getRenderedHTML());
        }
        JIPipeNodeInfo info = entryNodeInfo(entry);
        if (info != null) {
            // Per-parameter documentation (same scan as the node settings UI)
            for (Method method : info.getInstanceClass().getMethods()) {
                SetJIPipeDocumentation docs = method.getAnnotation(SetJIPipeDocumentation.class);
                if (docs == null) {
                    continue;
                }
                String text = DocumentationUtils.getDocumentationDescription(docs);
                if (StringUtils.isNullOrEmpty(text)) {
                    continue;
                }
                builder.append("<p><b>").append(escapeHtml(docs.name()))
                    .append("</b>: ").append(escapeHtml(text)).append("</p>");
            }
            // Class documentation (last resort: not every node has it)
            String classDocs = DocumentationUtils.getDocumentationDescription(info.getInstanceClass());
            if (!StringUtils.isNullOrEmpty(classDocs)) {
                builder.append("<p>").append(escapeHtml(classDocs).replace("\n", "<br>")).append("</p>");
            }
        }
        return builder.toString();
    }

    private static String escapeHtml(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    /**
     * Resolves the workflow family the entry's node belongs to: the first
     * (in assembler registration order) family that has a step for the
     * entry's origin node id. Returns {@code null} when the entry wraps no
     * node type or the node belongs to no family.
     */
    @Nullable
    private JIPipeNodeTaxonomyFamily resolveFamily(JIPipeNodeDatabaseEntry entry) {
        String originNodeId = entry.getOriginNodeId();
        if (originNodeId == null) {
            return null;
        }
        Set<String> familyIds = JIPipe.getNodes().getFamilyAssembler().familyIdsOf(originNodeId);
        if (familyIds.isEmpty()) {
            return null;
        }
        Map<String, JIPipeNodeTaxonomyFamily> families = JIPipe.getNodes().getFamilies();
        for (String id : familyIds) {
            JIPipeNodeTaxonomyFamily family = families.get(id);
            if (family != null) {
                return family;
            }
        }
        return null;
    }

    private boolean hasNodeExample(JIPipeNodeDatabaseEntry entry) {
        String originNodeId = entry.getOriginNodeId();
        return originNodeId != null && !JIPipe.getNodes().getNodeExamples(originNodeId).isEmpty();
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
    // Pill tab bar + pages
    // ------------------------------------------------------------------

    private void initializePillBar() {
        pillBar.setOpaque(false);
        docsPill = new PillButton("Documentation", JIPipe.RESOURCES.getIcon16("actions/help-info.png"));
        relatedPill = new PillButton("Related", JIPipe.RESOURCES.getIcon16("actions/tag.png"));
        followUpsPill = new PillButton("Follow-ups", JIPipe.RESOURCES.getIcon16("actions/diagram-next.png"));
        for (PillButton pill : new PillButton[]{docsPill, relatedPill, followUpsPill}) {
            pill.addActionListener(e -> selectPill(pill));
            pillBar.add(pill);
        }
    }

    private void selectPill(PillButton selectedPill) {
        for (PillButton pill : new PillButton[]{docsPill, relatedPill, followUpsPill}) {
            pill.setSelectedPill(pill == selectedPill);
        }
        if (selectedPill == docsPill) {
            pageLayout.show(pages, "docs");
        } else if (selectedPill == relatedPill) {
            pageLayout.show(pages, "related");
        } else {
            pageLayout.show(pages, "followups");
        }
        contentPanel.revalidate();
        contentPanel.repaint();
    }

    /**
     * A rounded "pill" tab button (custom, matching the AI monitor's top
     * pills): accent-filled when selected, transparent + accent border
     * otherwise.
     */
    private static class PillButton extends JButton {

        private boolean selectedPill;

        PillButton(String text, Icon icon) {
            super(text, icon);
            setFocusable(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setMargin(new Insets(2, 10, 2, 10));
            setForeground(pillForeground(false));
        }

        void setSelectedPill(boolean selectedPill) {
            if (this.selectedPill == selectedPill) {
                return;
            }
            this.selectedPill = selectedPill;
            // The foreground changes with the selection state (white on the
            // accent fill), so set it here — never inside paintComponent,
            // which would re-trigger an endless repaint loop.
            setForeground(pillForeground(selectedPill));
            repaint();
        }

        private static Color pillForeground(boolean selectedPill) {
            Color accent = UIManager.getColor("TabbedPane.selected");
            return selectedPill && accent != null ? Color.WHITE : UIManager.getColor("Label.foreground");
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color accent = UIManager.getColor("TabbedPane.selected");
            if (accent == null) {
                accent = UIManager.getColor("Button.select");
            }
            if (accent == null) {
                accent = Color.GRAY;
            }
            Color fill;
            if (selectedPill) {
                fill = accent;
            } else {
                fill = new Color(Math.min(255, (int) (accent.getRed() * 0.75f)),
                    Math.min(255, (int) (accent.getGreen() * 0.75f)),
                    Math.min(255, (int) (accent.getBlue() * 0.75f)));
            }
            g2d.setColor(fill);
            g2d.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, PILL_ARC, PILL_ARC);
            g2d.dispose();
            super.paintComponent(g);
        }
    }

    // ------------------------------------------------------------------
    // Documentation page
    // ------------------------------------------------------------------

    private void initializeDocumentationPage() {
        documentationPane.setEditable(false);
        documentationPane.setContentType("text/html");
        documentationPane.setOpaque(false);
        documentationPane.setBackground(getBackground());
    }

    // ------------------------------------------------------------------
    // Related / follow-up lists
    // ------------------------------------------------------------------

    private void initializeEntryList(String title, JList<JIPipeNodeDatabaseEntry> list, JLabel emptyState) {
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
        scroll.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        body.add(scroll, "list");
        emptyState.setOpaque(false);
        body.add(emptyState, "empty");
        listCards.get(list).show(body, "empty");

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
        panel.add(new JLabel(title, JIPipe.RESOURCES.getIcon16("actions/caret-right.png"), SwingConstants.LEFT),
            BorderLayout.NORTH);
        panel.add(body, BorderLayout.CENTER);
        listPanels.put(list, panel);
    }

    // ------------------------------------------------------------------
    // Actions
    // ------------------------------------------------------------------

    private void initializeActions() {
        Font buttonFont = new Font(Font.DIALOG, Font.PLAIN, ThemeUtils.getCurrentStyle().getFontSizeNormal());
        for (JButton button : new JButton[]{insertSetupButton, openExampleButton}) {
            button.setFont(buttonFont);
            button.setAlignmentX(LEFT_ALIGNMENT);
            button.setMaximumSize(new Dimension(Integer.MAX_VALUE, button.getPreferredSize().height));
        }
        insertSetupButton.setToolTipText("Inserts this node's family setup (its template), its follow-up nodes, or the node itself");
        insertSetupButton.addActionListener(e -> onInsertSetup());
        openExampleButton.setToolTipText("No example is available for this node");
        openExampleButton.addActionListener(e -> onOpenExample());
    }

    private void onInsertSetup() {
        JIPipeNodeDatabaseEntry entry = currentEntry;
        if (entry == null) {
            return;
        }
        JIPipeNodeTaxonomyFamily family = resolveFamily(entry);
        if (family != null && family.getTemplate() != null) {
            // Template path: insert the family's setup template (the same
            // entry type the dialog's own list uses for templates)
            JIPipeDesktopGraphNodeUI nodeUI = new CreateNewNodesByTemplateDatabaseEntry(family.getTemplate())
                .addToGraph(canvasUI).iterator().next();
            canvasUI.getSelectionManager().setSelection(Set.of(nodeUI));
            return;
        }
        List<JIPipeNodeDatabaseEntry> followUps = database.followUps(entry);
        if (!followUps.isEmpty()) {
            // No template: insert each follow-up unconnected
            Set<JIPipeDesktopGraphNodeUI> added = new HashSet<>();
            for (JIPipeNodeDatabaseEntry followUp : followUps) {
                added.addAll(followUp.addToGraph(canvasUI));
            }
            if (!added.isEmpty()) {
                canvasUI.getSelectionManager().setSelection(added);
            }
            return;
        }
        // No family template and no follow-ups (the common case): insert the
        // node itself, so the button always does something visible.
        Set<JIPipeDesktopGraphNodeUI> added = entry.addToGraph(canvasUI);
        if (!added.isEmpty()) {
            canvasUI.getSelectionManager().setSelection(added);
        }
    }

    private void onOpenExample() {
        JIPipeNodeDatabaseEntry entry = currentEntry;
        if (entry == null) {
            return;
        }
        String originNodeId = entry.getOriginNodeId();
        if (originNodeId == null) {
            return;
        }
        var examples = JIPipe.getNodes().getNodeExamples(originNodeId);
        if (examples.isEmpty()) {
            // The tooltip already says no example is available; do nothing
            return;
        }
        JIPipeNodeExample example = examples.iterator().next();
        // Insert the example via the same entry type the dialog's own list
        // uses for example entries
        CreateNewNodeByExampleDatabaseEntry exampleEntry =
            new CreateNewNodeByExampleDatabaseEntry("example-" + originNodeId, example);
        Set<JIPipeDesktopGraphNodeUI> added = exampleEntry.addToGraph(canvasUI);
        if (!added.isEmpty()) {
            canvasUI.getSelectionManager().setSelection(added);
        }
    }

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
