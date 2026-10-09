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
import org.hkijena.jipipe.api.JIPipeNodeTemplate;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeNodeExample;
import org.hkijena.jipipe.api.nodes.database.JIPipeNodeDatabase;
import org.hkijena.jipipe.api.nodes.database.JIPipeNodeDatabaseEntry;
import org.hkijena.jipipe.api.nodes.database.entries.CreateNewNodeByInfoAliasDatabaseEntry;
import org.hkijena.jipipe.api.nodes.database.entries.CreateNewNodeByInfoDatabaseEntry;
import org.hkijena.jipipe.api.nodes.database.entries.CreateNewNodesByTemplateDatabaseEntry;
import org.hkijena.jipipe.api.nodes.taxonomy.JIPipeNodeTaxonomyFamily;
import org.hkijena.jipipe.api.nodes.taxonomy.JIPipeNodeTaxonomyFramework;
import org.hkijena.jipipe.api.nodes.taxonomy.JIPipeNodeTaxonomyInputProfile;
import org.hkijena.jipipe.api.nodes.taxonomy.JIPipeNodeTaxonomySpatialDimension;
import org.hkijena.jipipe.api.nodes.taxonomy.NoFramework;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphCanvasUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui.JIPipeDesktopGraphNodeUI;
import org.hkijena.jipipe.plugins.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.utils.DocumentationUtils;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.ThemeUtils;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
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
 * <li>the description (the entry's {@code HTMLText} in a non-editable
 * {@link JEditorPane}),</li>
 * <li>a "Related" list ({@code database.related(entry, 6)}) and a
 * "Follow-ups" list ({@code database.followUps(entry)}); clicking an item
 * tells the dialog (via the {@link EntrySelector} callback) to select that
 * entry in the center list, so the rail cannot recurse into itself,</li>
 * <li>the actions: <b>▶ Insert setup</b> — when the entry's node belongs to
 * a workflow family with a non-null {@link JIPipeNodeTaxonomyFamily#getTemplate()
 * template}, the template is inserted via
 * {@link CreateNewNodesByTemplateDatabaseEntry#addToGraph(JIPipeDesktopGraphCanvasUI)}
 * (the same path the dialog uses for template entries); otherwise the
 * entry's follow-ups are each inserted unconnected via
 * {@code entry.addToGraph(canvasUI)} — <b>Open example</b>, enabled only
 * when the family declares a non-null example (it is disabled for all
 * seed families; Phase 3 templates will light it up), which inserts the
 * example's node template — and <b>Documentation</b>, which swaps the
 * description panel for the entry's full documentation text
 * ({@link DocumentationUtils#getDocumentationDescription(Class)} on the
 * entry's node class) with a Back button to return.</li>
 * </ul>
 * <p>
 * When no entry is selected the rail shows an empty-state label. The rail
 * must remain usable when the node belongs to <em>no</em> workflow family
 * (the common case today): the related/follow-up lists and the actions
 * then show their empty states rather than errors.
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
    private static final String CARD_EMPTY = "empty";
    private static final String CARD_CONTENT = "content";

    private final JIPipeNodeDatabase database;
    private final JIPipeDesktopGraphCanvasUI canvasUI;
    private final EntrySelector entrySelector;
    private final CardLayout cardLayout = new CardLayout();
    private JPanel cards;

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
    private final JEditorPane descriptionPane = new JEditorPane();
    private final JEditorPane documentationPane = new JEditorPane();
    private final JPanel descriptionSection = new JPanel();
    private final JPanel documentationSection = new JPanel();
    private final JButton backButton = new JButton("< Back");
    private final JList<JIPipeNodeDatabaseEntry> relatedList = new JList<>();
    private final JList<JIPipeNodeDatabaseEntry> followUpsList = new JList<>();
    private final Map<JList<JIPipeNodeDatabaseEntry>, JPanel> listPanels = new IdentityHashMap<>();

    private final JButton insertSetupButton = new JButton("▶ Insert setup");
    private final JButton openExampleButton = new JButton("Open example");
    private final JButton documentationButton = new JButton("Documentation");

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
        initializeDescriptionSections();
        initializeEntryList("Related", relatedList);
        initializeEntryList("Follow-ups", followUpsList);
        initializeActions();
        contentPanel.add(titleBlock());
        contentPanel.add(descriptionSection);
        contentPanel.add(Box.createVerticalStrut(8));
        contentPanel.add(listPanels.get(relatedList));
        contentPanel.add(Box.createVerticalStrut(8));
        contentPanel.add(listPanels.get(followUpsList));
        contentPanel.add(Box.createVerticalStrut(8));
        contentPanel.add(insertSetupButton);
        contentPanel.add(openExampleButton);
        contentPanel.add(documentationButton);

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
        // Reset the docs toggle
        documentationSection.setVisible(false);
        descriptionSection.setVisible(true);

        // Title block: name + framework chip + tags + profile line
        nameLabel.setText(entry.getName());
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

        // Description
        descriptionPane.setText(entry.getDescription().getHtml());

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

        // Actions: family resolution
        JIPipeNodeTaxonomyFamily family = resolveFamily(entry);
        String familyName = family != null ? family.getName() : null;
        if (family == null || family.getTemplate() == null) {
            insertSetupButton.setToolTipText(familyName == null
                ? "Inserts the follow-up nodes of this node, unconnected"
                : "Inserts the follow-up nodes of this node, unconnected (the '" + familyName + "' family has no template)");
        } else {
            insertSetupButton.setToolTipText("Inserts the '" + familyName + "' family's setup template");
        }
        boolean hasExample = family != null && family.getExample() != null;
        openExampleButton.setEnabled(hasExample);
        openExampleButton.setToolTipText(hasExample
            ? "Inserts the '" + familyName + "' family's example"
            : "This node's family has no example");

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
        JPanel nameRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        nameRow.add(nameLabel);
        nameRow.add(frameworkChip);
        block.add(nameRow);
        block.add(tagsLabel);
        block.add(profileLabel);
        return block;
    }

    // ------------------------------------------------------------------
    // Description + documentation sections
    // ------------------------------------------------------------------

    private void initializeDescriptionSections() {
        // Description
        descriptionPane.setEditable(false);
        descriptionPane.setContentType("text/html");
        descriptionPane.setOpaque(false);
        descriptionPane.setBackground(getBackground());
        JScrollPane descriptionScroll = new JScrollPane(descriptionPane);
        descriptionScroll.setBorder(null);
        descriptionScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        descriptionScroll.setPreferredSize(new Dimension(0, 120));
        descriptionScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));
        descriptionSection.setLayout(new BorderLayout());
        descriptionSection.add(new JLabel("Description", JIPipe.RESOURCES.getIcon16("actions/help-contents.png"),
            SwingConstants.LEFT), BorderLayout.NORTH);
        descriptionSection.add(descriptionScroll, BorderLayout.CENTER);

        // Documentation
        documentationPane.setEditable(false);
        documentationPane.setContentType("text/html");
        documentationPane.setOpaque(false);
        documentationPane.setBackground(getBackground());
        JScrollPane documentationScroll = new JScrollPane(documentationPane);
        documentationScroll.setBorder(null);
        documentationScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        documentationScroll.setPreferredSize(new Dimension(0, 120));
        documentationScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));
        documentationSection.setLayout(new BorderLayout());
        JPanel docsHeader = new JPanel(new BorderLayout());
        docsHeader.add(backButton, BorderLayout.WEST);
        docsHeader.add(new JLabel("Documentation", JIPipe.RESOURCES.getIcon16("actions/help-contents.png"),
            SwingConstants.LEFT), BorderLayout.CENTER);
        documentationSection.add(docsHeader, BorderLayout.NORTH);
        documentationSection.add(documentationScroll, BorderLayout.CENTER);
        documentationSection.setVisible(false);

        backButton.addActionListener(e -> {
            documentationSection.setVisible(false);
            descriptionSection.setVisible(true);
            contentPanel.revalidate();
            contentPanel.repaint();
        });
    }

    private void showDocumentation(JIPipeNodeDatabaseEntry entry) {
        StringBuilder builder = new StringBuilder();
        builder.append("<h3>").append(escapeHtml(entry.getName())).append("</h3>");
        HTMLText description = entry.getDescription();
        if (description != null && !StringUtils.isNullOrEmpty(description.getBody())) {
            builder.append("<p>").append(description.getBody()).append("</p>");
        }
        JIPipeNodeInfo info = entryNodeInfo(entry);
        if (info != null) {
            String docs = DocumentationUtils.getDocumentationDescription(info.getInstanceClass());
            if (!StringUtils.isNullOrEmpty(docs)) {
                // The class documentation is plain text: escape it and
                // convert newlines to line breaks
                builder.append("<p>").append(escapeHtml(docs).replace("\n", "<br>")).append("</p>");
            }
        }
        documentationPane.setText(builder.toString());
        documentationPane.setCaretPosition(0);
        descriptionSection.setVisible(false);
        documentationSection.setVisible(true);
        contentPanel.revalidate();
        contentPanel.repaint();
    }

    private static String escapeHtml(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    // ------------------------------------------------------------------
    // Related / follow-up lists
    // ------------------------------------------------------------------

    private void initializeEntryList(String title, JList<JIPipeNodeDatabaseEntry> list) {
        list.setOpaque(false);
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

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
        panel.add(new JLabel(title, JIPipe.RESOURCES.getIcon16("actions/caret-right.png"), SwingConstants.LEFT),
            BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(list);
        scroll.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
        scroll.setPreferredSize(new Dimension(0, 90));
        scroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        panel.add(scroll, BorderLayout.CENTER);
        listPanels.put(list, panel);
    }

    // ------------------------------------------------------------------
    // Actions
    // ------------------------------------------------------------------

    private void initializeActions() {
        Font buttonFont = new Font(Font.DIALOG, Font.PLAIN, ThemeUtils.getCurrentStyle().getFontSizeNormal());
        for (JButton button : new JButton[]{insertSetupButton, openExampleButton, documentationButton}) {
            button.setFont(buttonFont);
            button.setAlignmentX(LEFT_ALIGNMENT);
            button.setMaximumSize(new Dimension(Integer.MAX_VALUE, button.getPreferredSize().height));
        }
        insertSetupButton.setToolTipText("Inserts this node's family setup (its template) or its follow-up nodes");
        insertSetupButton.addActionListener(e -> onInsertSetup());
        openExampleButton.setEnabled(false);
        openExampleButton.setToolTipText("This node's family has no example");
        openExampleButton.addActionListener(e -> onOpenExample());
        documentationButton.setToolTipText("Shows the full documentation of this node");
        documentationButton.addActionListener(e -> {
            if (currentEntry != null) {
                showDocumentation(currentEntry);
            }
        });
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
        } else {
            // No template (or no family): insert each follow-up unconnected
            List<JIPipeNodeDatabaseEntry> followUps = database.followUps(entry);
            Set<JIPipeDesktopGraphNodeUI> added = new HashSet<>();
            for (JIPipeNodeDatabaseEntry followUp : followUps) {
                added.addAll(followUp.addToGraph(canvasUI));
            }
            if (!added.isEmpty()) {
                canvasUI.getSelectionManager().setSelection(added);
            }
        }
    }

    private void onOpenExample() {
        JIPipeNodeDatabaseEntry entry = currentEntry;
        if (entry == null) {
            return;
        }
        JIPipeNodeTaxonomyFamily family = resolveFamily(entry);
        if (family == null) {
            return;
        }
        JIPipeNodeExample example = family.getExample();
        if (example == null) {
            return;
        }
        // Insert the example's node template, mirroring the dialog's
        // template-entry path (CreateNewNodesByTemplateDatabaseEntry)
        JIPipeNodeTemplate template = example.getNodeTemplate();
        if (template == null) {
            return;
        }
        JIPipeDesktopGraphNodeUI nodeUI = new CreateNewNodesByTemplateDatabaseEntry(template)
            .addToGraph(canvasUI).iterator().next();
        canvasUI.getSelectionManager().setSelection(Set.of(nodeUI));
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    @Nullable
    private static JIPipeNodeInfo entryNodeInfo(JIPipeNodeDatabaseEntry entry) {
        if (entry instanceof CreateNewNodeByInfoDatabaseEntry byInfo) {
            return byInfo.getNodeInfo();
        }
        if (entry instanceof CreateNewNodeByInfoAliasDatabaseEntry alias) {
            return alias.getNodeInfo();
        }
        return null;
    }
}
