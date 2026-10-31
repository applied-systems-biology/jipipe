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

package org.hkijena.jipipe.desktop.app.settings;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.service.components.JIPipeSettingsServiceComponent;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.api.settings.JIPipeApplicationSettingsSheet;
import org.hkijena.jipipe.api.settings.JIPipeProjectSettingsSheet;
import org.hkijena.jipipe.api.settings.JIPipeSettingsSheet;
import org.hkijena.jipipe.api.settings.SettingsScope;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbenchPanel;

import org.hkijena.jipipe.desktop.commons.components.parameters.JIPipeDesktopParameterFormPanel;
import org.hkijena.jipipe.desktop.commons.components.search.JIPipeDesktopSearchTextField;
import org.hkijena.jipipe.plugins.parameters.library.markup.MarkdownText;
import org.hkijena.jipipe.plugins.settings.SettingsScopes;
import org.hkijena.jipipe.utils.JIPipeDesktopSplitPane;
import org.hkijena.jipipe.utils.UIUtils;
import org.scijava.Disposable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * The non-modal settings workbench tab (modern settings system, Task 18).
 * Replaces the former modal application-settings dialog (removed in Task 22):
 * a search field and a scope switcher ({@code Application | Project | Runtime})
 * on top, the category tree on the left, the section's
 * {@link JIPipeDesktopParameterFormPanel} on the right, and a dismissible
 * banner when another process changed the settings.
 * <p>
 * The tree is built exactly like the modal's (categories from the sheets'
 * {@code getCategory()}, "General" pinned first, category icon from the
 * first sheet of the category, sheets sorted by name) so the migration is
 * 1:1; the grouping logic lives in {@link SettingsTreeModel}. Deep links open
 * a section by its stable registered sheet id via
 * {@code JIPipeDesktopProjectWorkbench#openSettingsSection(SettingsScope, String)},
 * in the scope the sheet belongs to.
 * <p>
 * <b>No Save button.</b> Every parameter edit commits to the sheet
 * immediately (the editor UI writes through {@link org.hkijena.jipipe.api.parameters.JIPipeParameterAccess#set(Object)}),
 * the sheet bridge
 * ({@link org.hkijena.jipipe.api.settings.SettingsSheetBridge}) flushes the
 * edited sheet into the settings document on every {@code ParameterChangedEvent},
 * and the <em>engine</em>
 * ({@link JIPipeSettingsServiceComponent}) then persists that local write
 * itself: a local change schedules its debounced {@code saveLater()}, so the
 * settings file converges without any save action by this tab. Re-creating the
 * form panel on section selection therefore cannot lose an edit, and the tab
 * needs no Save button.
 * <p>
 * <b>No global Cancel or revert.</b> There is no Cancel button and no global
 * revert of settings edits (a decision taken when the modal was removed,
 * 2026-10-31): edits persist immediately, so a dialog-wide "discard" would
 * have to fight the engine that already flushed them. To undo changes, use the
 * per-section <em>Reset section</em> action (Appearance/Accessibility) or the
 * per-key reset in the raw settings editor and the changes strip. This
 * replaces the modal's Save/Cancel/Reset semantics.
 */
public class JIPipeDesktopSettingsTab extends JIPipeDesktopWorkbenchPanel implements Disposable {

    private static final Logger logger = LoggerFactory.getLogger(JIPipeDesktopSettingsTab.class);

    /**
     * The scopes shown in the switcher, in display order.
     */
    private static final SettingsScope[] SCOPES = {SettingsScope.APPLICATION, SettingsScope.PROJECT, SettingsScope.RUNTIME};

    /**
     * The registered merged sections: registered sheet id → the merged section
     * (page) its form is replaced by. A page is hosted by the first sheet that
     * registers it, so the placeholder pages need no sheet of their own.
     * <p>
     * Populated by {@link JIPipeDesktopAppearanceSection#registerCompositions()}
     * and {@link JIPipeDesktopRawSettingsSection#registerRawSection()} during
     * plugin registration.
     */
    private static final Map<String, String> MERGED_PAGES_BY_SHEET = new ConcurrentHashMap<>();

    /**
     * The sheet ids whose content is absorbed by a merged page: they keep their
     * registration and block, but get no tree entry (see
     * {@link #registerAbsorbedSheet(String)}).
     */
    private static final Set<String> ABSORBED_SHEET_IDS = ConcurrentHashMap.newKeySet();

    /**
     * Registers a merged section: the given registered sheet's form is replaced
     * by the merged section named {@code page}.
     *
     * @param sheetId the registered sheet that hosts the merged section
     * @param page    the merged section id ({@link AppearanceSectionModel#SECTION_APPEARANCE}, ...)
     */
    public static void registerMergedPage(String sheetId, String page) {
        String previous = MERGED_PAGES_BY_SHEET.putIfAbsent(sheetId, page);
        if (previous != null && !previous.equals(page)) {
            logger.warn("Settings sheet '{}' is already registered as merged page '{}'; ignoring the re-registration as '{}' (first registration wins)",
                    sheetId, previous, page);
        }
    }

    /**
     * Registers a sheet whose <em>content</em> is absorbed by a merged page: the
     * sheet stays registered and keeps its block (the engine, bindings and the
     * raw editor are unaffected), but it no longer gets a tree entry, because
     * the merged section already shows its parameters.
     * <p>
     * The absorbed set is the one extension point for "a merged section owns this
     * sheet's knobs". Deep links must therefore target the merged section's
     * sheet id, never an absorbed one.
     *
     * @param sheetId the registered sheet whose tree entry is replaced by a merged page
     */
    public static void registerAbsorbedSheet(String sheetId) {
        ABSORBED_SHEET_IDS.add(sheetId);
    }

    /**
     * @return the sheet ids whose tree entries are replaced by a merged page
     */
    public static List<String> absorbedSheetIds() {
        return List.copyOf(ABSORBED_SHEET_IDS);
    }

    /**
     * @param sheetId a registered sheet id
     * @return the merged section the sheet's form is replaced by, or
     * {@code null} for a plain section
     */
    static String mergedPageOf(String sheetId) {
        return sheetId == null ? null : MERGED_PAGES_BY_SHEET.get(sheetId);
    }

    private final JTree tree = new JTree();
    private final JTabbedPane scopeSwitcher = new JTabbedPane();
    private final JIPipeDesktopSearchTextField searchField = new JIPipeDesktopSearchTextField();
    /**
     * Sections keyed by scope-qualified identity
     * ({@link SettingsTreeModel#scopeQualifiedId(SettingsScope, String)}): a
     * section's identity is (scope, id), so the RUNTIME application sheet and
     * the PROJECT sheet {@code org.hkijena.jipipe:default-environments} are
     * distinct entries rather than one overwriting the other.
     */
    private final Map<String, SettingsTreeModel.SheetNode> nodeMap = new HashMap<>();
    private final Map<String, JIPipeParameterCollection> sheetById = new HashMap<>();
    private final Map<String, Icon> categoryIconById = new HashMap<>();
    private final JIPipeSettingsServiceComponent settingsService;
    private final JIPipeSettingsServiceComponent.SettingsChangedEventListener changedListener = this::onSettingsChanged;

    /**
     * The since-open audit of settings changes (modern settings system, Task
     * 21). Every change event — all scopes, all origins — is recorded here; the
     * changes strip renders the active scope's entries. The log is thread-safe
     * and holds no listeners, so {@link #dispose()} has nothing to unbind.
     */
    private final SettingsChangeLog changeLog = new SettingsChangeLog();

    private final JPanel banner = new JPanel(new BorderLayout(8, 0));
    private final JSplitPane splitPane;

    private final JLabel changesToggle = new JLabel();
    private final DefaultListModel<SettingsChangeLog.ChangeEntry> changesListModel = new DefaultListModel<>();
    private final JList<SettingsChangeLog.ChangeEntry> changesList = new JList<>(changesListModel);
    private final JScrollPane changesScrollPane = new JScrollPane(changesList);
    private boolean changesExpanded;

    private SettingsTreeModel model;
    private SettingsScope activeScope = SettingsScope.APPLICATION;
    /** The selected section's scope-qualified identity; {@code null} when none. */
    private String selectedSectionKey;
    private JIPipeDesktopParameterFormPanel currentFormPanel;
    private JIPipeDesktopAppearanceSection currentSection;
    private JIPipeDesktopRawSettingsSection currentRawSection;
    private boolean updatingTree;
    /** Whether {@link #dispose()} already ran (it is idempotent). */
    private boolean disposed;

    /**
     * @param workbench the workbench
     */
    public JIPipeDesktopSettingsTab(JIPipeDesktopWorkbench workbench) {
        super(workbench);
        this.settingsService = JIPipe.getSettingsService();
        this.splitPane = new JIPipeDesktopSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                UIUtils.wrapInIslandPanelIfNeeded(new JScrollPane(tree)),
                UIUtils.wrapInIslandPanelIfNeeded(new JPanel()),
                JIPipeDesktopSplitPane.RATIO_1_TO_3);
        initialize();
        reloadSections();
        settingsService.getChangedEventEmitter().subscribe(changedListener);
    }

    /**
     * The application sheets and their scope: the scope of a sheet is the one
     * routed for its block ({@link SettingsScopes#scopeByBlock()}), defaulting
     * to {@link SettingsScope#APPLICATION} — everything not routed to the
     * runtime document is a user preference.
     *
     * @param sheets the registered application sheets
     * @param scopes the block → scope routing (see
     *               {@link JIPipeSettingsServiceComponent#effectiveScopeByBlock()})
     * @return one tree node per sheet
     */
    public static List<SettingsTreeModel.SheetNode> applicationNodes(Collection<JIPipeApplicationSettingsSheet> sheets,
                                                                     Map<String, SettingsScope> scopes) {
        List<SettingsTreeModel.SheetNode> result = new ArrayList<>();
        for (JIPipeApplicationSettingsSheet sheet : sheets) {
            if (isAbsorbed(sheet.getId())) {
                // Its parameters are shown by a merged section (spec: the merged
                // section owns these knobs), so the plain entry would duplicate them.
                continue;
            }
            SettingsScope scope = scopes.getOrDefault("sheet:" + sheet.getId(), SettingsScope.APPLICATION);
            result.add(new SettingsTreeModel.SheetNode(sheet.getId(),
                    sheet.getName(),
                    sheet.getDescription(),
                    sheet.getCategory(),
                    scope,
                    sheet.getId()));
        }
        return result;
    }

    /**
     * @param sheetId a sheet id
     * @return whether the sheet's tree entry is replaced by a merged page
     */
    private static boolean isAbsorbed(String sheetId) {
        return sheetId != null && ABSORBED_SHEET_IDS.contains(sheetId);
    }

    /**
     * The open project's sheets; they belong to {@link SettingsScope#PROJECT}.
     *
     * @param sheets the project's live sheets
     * @return one tree node per sheet
     */
    private static String sectionKey(SettingsTreeModel.SheetNode node) {
        return SettingsTreeModel.scopeQualifiedId(node.scope(), node.id());
    }

    public static List<SettingsTreeModel.SheetNode> projectNodes(Collection<JIPipeProjectSettingsSheet> sheets) {
        List<SettingsTreeModel.SheetNode> result = new ArrayList<>();
        for (JIPipeProjectSettingsSheet sheet : sheets) {
            result.add(new SettingsTreeModel.SheetNode(sheet.getId(),
                    sheet.getName(),
                    sheet.getDescription(),
                    sheet.getCategory(),
                    SettingsScope.PROJECT,
                    sheet.getId()));
        }
        return result;
    }

    private void initialize() {
        setLayout(new BorderLayout());
        tree.setCellRenderer(new SettingsTreeCellRenderer());
        splitPane.setBorder(UIUtils.createEmptyBorder(5));

        initializeBanner();

        // Top: search + scope switcher, with the (hidden by default) live-update
        // banner as a thin strip below it. Center: the tree and the section form.
        JPanel header = new JPanel(new BorderLayout());
        header.add(createTopBar(), BorderLayout.NORTH);
        header.add(banner, BorderLayout.SOUTH);

        JPanel content = new JPanel(new BorderLayout());
        content.add(header, BorderLayout.NORTH);
        content.add(splitPane, BorderLayout.CENTER);
        // Bottom: the collapsible since-open changes strip (collapsed by default).
        content.add(createChangesStrip(), BorderLayout.SOUTH);
        add(content, BorderLayout.CENTER);

        tree.addTreeSelectionListener(e -> onTreeSelectionChanged());
        searchField.getTextField().getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                rebuildTree();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                rebuildTree();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                rebuildTree();
            }
        });
    }

    private JComponent createTopBar() {
        JPanel topBar = new JPanel(new BorderLayout(8, 0));
        topBar.setBorder(UIUtils.createEmptyBorder(5));

        JPanel searchPanel = new JPanel(new BorderLayout(4, 0));
        searchField.setPreferredSize(new Dimension(280, 28));
        searchPanel.add(searchField, BorderLayout.CENTER);
        topBar.add(searchPanel, BorderLayout.WEST);

        for (SettingsScope scope : SCOPES) {
            scopeSwitcher.addTab(scopeToTitle(scope), null, new JPanel());
        }
        scopeSwitcher.addChangeListener(e -> onScopeChanged());
        topBar.add(scopeSwitcher, BorderLayout.EAST);
        return topBar;
    }

    private void initializeBanner() {
        banner.setVisible(false);
        banner.setBorder(UIUtils.createEmptyBorder(4));
        banner.setBackground(UIManager.getColor("InfoBackground"));
        banner.add(new JLabel("<html><strong>Settings were changed externally.</strong></html>"), BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        buttons.setOpaque(false);
        JButton reloadButton = new JButton("Reload section", JIPipe.RESOURCES.getIcon16("actions/edit-reset.png"));
        reloadButton.addActionListener(e -> {
            hideBanner();
            rebuildRightPane();
        });
        buttons.add(reloadButton);
        JButton dismissButton = new JButton("Dismiss", JIPipe.RESOURCES.getIcon16("actions/cancel.png"));
        dismissButton.addActionListener(e -> {
            hideBanner();
            // Acknowledging the banner also acknowledges this scope's foreign
            // changes: they leave the changes strip so a dismissed notice does
            // not linger as a stale row.
            changeLog.acknowledgeForeign(activeScope);
            refreshChangesStrip();
        });
        buttons.add(dismissButton);
        banner.add(buttons, BorderLayout.EAST);
    }

    /**
     * Builds the collapsible changes strip shown at the bottom of the tab: a
     * toggle label ("Changes (n)", n = the active scope's entries) and, when
     * expanded, one row per since-open change of the active scope with a
     * trailing per-entry reset button. Collapsed by default; the expanded state
     * is a field, not persisted.
     *
     * @return the strip
     */
    private JComponent createChangesStrip() {
        JPanel strip = new JPanel(new BorderLayout());
        strip.setBorder(UIUtils.createEmptyBorder(2));

        changesToggle.setIcon(JIPipe.RESOURCES.getIcon16("actions/chevron-right.png"));
        changesToggle.setBorder(UIUtils.createEmptyBorder(2));
        changesToggle.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        changesToggle.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                setChangesExpanded(!changesExpanded);
            }
        });
        strip.add(changesToggle, BorderLayout.NORTH);

        changesList.setCellRenderer(new ChangesCellRenderer());
        changesScrollPane.setPreferredSize(new Dimension(0, 150));
        changesScrollPane.setVisible(false);
        strip.add(changesScrollPane, BorderLayout.CENTER);

        updateChangesToggleText();
        return strip;
    }

    /**
     * Expands or collapses the changes strip.
     *
     * @param expanded whether the strip is expanded
     */
    private void setChangesExpanded(boolean expanded) {
        changesExpanded = expanded;
        changesScrollPane.setVisible(expanded);
        updateChangesToggleText();
        revalidate();
        repaint();
    }

    private void updateChangesToggleText() {
        int count = entriesOfActiveScope().size();
        changesToggle.setText("Changes (" + count + ")");
        changesToggle.setIcon(JIPipe.RESOURCES.getIcon16(changesExpanded
                ? "actions/chevron-down.png"
                : "actions/chevron-right.png"));
    }

    private List<SettingsChangeLog.ChangeEntry> entriesOfActiveScope() {
        return changeLog.entries().stream().filter(entry -> entry.scope() == activeScope).collect(Collectors.toList());
    }

    /**
     * Re-reads the log and re-renders the strip for the active scope. Must run
     * on the EDT.
     */
    private void refreshChangesStrip() {
        List<SettingsChangeLog.ChangeEntry> entries = entriesOfActiveScope();
        changesListModel.clear();
        for (SettingsChangeLog.ChangeEntry entry : entries) {
            changesListModel.addElement(entry);
        }
        if (entries.isEmpty()) {
            if (changesExpanded) {
                setChangesExpanded(false);
            }
        }
        updateChangesToggleText();
        revalidate();
        repaint();
    }

    /**
     * Resets one entry's location to its default through the raw editor's
     * model — the reviewed documented-vs-pass-through reset lives there and is
     * not re-implemented here. The model is a throwaway: its constructor only
     * captures the service and the scope's document and registers no listener,
     * and {@link RawSettingsTableModel#resetKey(String)} writes through the
     * engine, which emits the change event that records the reset itself.
     * <p>
     * The strip is refreshed on the EDT after the write, and the right pane is
     * rebuilt when the reset touched the active scope so the section reflects
     * the reset.
     *
     * @param entry the entry to reset
     */
    private void resetChangeEntry(SettingsChangeLog.ChangeEntry entry) {
        try {
            new RawSettingsTableModel(settingsService, entry.scope()).resetKey(entry.path());
        } catch (RuntimeException e) {
            JOptionPane.showMessageDialog(this,
                    "Cannot reset '" + entry.path() + "': " + e.getMessage(),
                    "Reset setting",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        boolean touchedActiveScope = entry.scope() == activeScope;
        SwingUtilities.invokeLater(() -> {
            // The reset emitted its own change event, whose later refresh would
            // suffice — refreshing here too keeps the strip honest even if the
            // scope was switched in between.
            if (touchedActiveScope) {
                rebuildRightPane();
            }
            refreshChangesStrip();
        });
    }

    /**
     * Reloads the section list from the sheet registries and the open project,
     * then rebuilds the tree.
     */
    public void reloadSections() {
        List<SettingsTreeModel.SheetNode> nodes = new ArrayList<>();
        Map<String, JIPipeParameterCollection> sheets = new LinkedHashMap<>();
        Map<String, Icon> categoryIcons = new LinkedHashMap<>();

        Map<String, JIPipeApplicationSettingsSheet> applicationSheets = JIPipe.getSettings().getRegisteredSheets();
        List<SettingsTreeModel.SheetNode> applicationNodes =
                applicationNodes(applicationSheets.values(), settingsService.effectiveScopeByBlock());
        nodes.addAll(applicationNodes);
        for (SettingsTreeModel.SheetNode node : applicationNodes) {
            sheets.put(sectionKey(node), applicationSheets.get(node.id()));
            categoryIcons.putIfAbsent(node.category(), applicationSheets.get(node.id()).getCategoryIcon());
        }

        if (getDesktopWorkbench().getProject() != null) {
            Map<String, JIPipeProjectSettingsSheet> projectSheets = getDesktopWorkbench().getProject().getSettingsSheets();
            List<SettingsTreeModel.SheetNode> projectNodes = projectNodes(projectSheets.values());
            nodes.addAll(projectNodes);
            for (SettingsTreeModel.SheetNode node : projectNodes) {
                sheets.put(sectionKey(node), projectSheets.get(node.id()));
                categoryIcons.putIfAbsent(node.category(), projectSheets.get(node.id()).getCategoryIcon());
            }
        }

        this.model = new SettingsTreeModel(nodes);
        this.nodeMap.clear();
        this.sheetById.clear();
        for (SettingsTreeModel.SheetNode node : model.allNodes()) {
            nodeMap.put(sectionKey(node), node);
            sheetById.put(sectionKey(node), sheets.get(sectionKey(node)));
        }
        this.categoryIconById.clear();
        this.categoryIconById.putAll(categoryIcons);

        if (selectedSectionKey != null && !nodeMap.containsKey(selectedSectionKey)) {
            selectedSectionKey = null;
        }
        rebuildTree();
    }

    private void rebuildTree() {
        Map<String, DefaultMutableTreeNode> nodePathMap = new HashMap<>();
        DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode();
        nodePathMap.put("/", rootNode);

        if (model != null) {
            String query = searchField.getText();
            boolean searching = query != null && !query.isBlank();
            if (searching) {
                // Flat result list grouped by the model's category order.
                Map<String, List<SettingsTreeModel.SheetNode>> byCategory = new LinkedHashMap<>();
                for (SettingsTreeModel.SheetNode sheet : model.search(query, activeScope)) {
                    byCategory.computeIfAbsent(sheet.category(), k -> new ArrayList<>()).add(sheet);
                }
                for (Map.Entry<String, List<SettingsTreeModel.SheetNode>> entry : byCategory.entrySet()) {
                    rootNode.add(createCategoryNode(entry.getKey(), entry.getValue(), nodePathMap));
                }
            } else {
                for (SettingsTreeModel.CategoryNode category : model.categories(activeScope)) {
                    rootNode.add(createCategoryNode(category.name(), category.sheets(), nodePathMap));
                }
            }
        }

        updatingTree = true;
        try {
            tree.setModel(new DefaultTreeModel(rootNode));
            UIUtils.expandAllTree(tree);

            DefaultMutableTreeNode target = selectedSectionKey != null ? findNode(rootNode, selectedSectionKey) : null;
            if (target == null && rootNode.getChildCount() > 0) {
                target = (DefaultMutableTreeNode) rootNode.getChildAt(0);
            }
            if (target != null) {
                tree.getSelectionModel().setSelectionPath(new TreePath(target.getPath()));
            }
        } finally {
            updatingTree = false;
        }
        onTreeSelectionChanged();
    }

    private DefaultMutableTreeNode createCategoryNode(String category, List<SettingsTreeModel.SheetNode> sheets,
                                                     Map<String, DefaultMutableTreeNode> nodePathMap) {
        DefaultMutableTreeNode categoryNode = new DefaultMutableTreeNode(
                new SettingsTreeNodeEntry(category, categoryIconById.get(category), null));
        nodePathMap.put("/" + category, categoryNode);
        for (SettingsTreeModel.SheetNode sheet : sheets) {
            DefaultMutableTreeNode sheetNode = new DefaultMutableTreeNode(new SettingsTreeNodeEntry(sheet.name(),
                    sheetIcon(sheet), sheet));
            categoryNode.add(sheetNode);
            nodePathMap.put("/" + category + "/" + sheet.name(), sheetNode);
        }
        return categoryNode;
    }

    private DefaultMutableTreeNode findNode(DefaultMutableTreeNode node, String sectionKey) {
        if (node.getUserObject() instanceof SettingsTreeNodeEntry entry && entry.sheet() != null
                && sectionKey(entry.sheet()).equals(sectionKey)) {
            return node;
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            DefaultMutableTreeNode result = findNode((DefaultMutableTreeNode) node.getChildAt(i), sectionKey);
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    private Icon sheetIcon(SettingsTreeModel.SheetNode node) {
        JIPipeParameterCollection sheet = sheetById.get(sectionKey(node));
        return sheet instanceof JIPipeSettingsSheet settingsSheet ? settingsSheet.getIcon() : null;
    }

    private void onScopeChanged() {
        // Note: no "same scope" early return — the switcher is initialised in
        // initialize(), before the section model exists, and that first event
        // must not be swallowed. Rebuilding an unchanged scope is cheap.
        SettingsScope scope = SCOPES[Math.max(0, scopeSwitcher.getSelectedIndex())];
        activeScope = scope;
        selectedSectionKey = null;
        rebuildTree();
        refreshChangesStrip(); // re-filter the strip to the new scope
    }

    private void onTreeSelectionChanged() {
        if (updatingTree) {
            return;
        }
        Object selected = tree.getLastSelectedPathComponent();
        if (!(selected instanceof DefaultMutableTreeNode node)) {
            selectedSectionKey = null;
            rebuildRightPane();
            return;
        }
        SettingsTreeNodeEntry entry = (SettingsTreeNodeEntry) node.getUserObject();
        String previous = selectedSectionKey;
        selectedSectionKey = entry.sheet() != null ? sectionKey(entry.sheet()) : null;
        if (!Objects.equals(previous, selectedSectionKey)) {
            hideBanner();
        }
        rebuildRightPane();
    }

    /**
     * (Re-)creates the right pane for the selected section. Called on section
     * selection, on scope switch, on banner "reload" and on reload requests.
     */
    public void rebuildRightPane() {
        disposeFormPanel();

        SettingsTreeModel.SheetNode sheetNode = selectedSectionKey != null ? nodeMap.get(selectedSectionKey) : null;
        if (sheetNode == null) {
            splitPane.setRightComponent(UIUtils.wrapInIslandPanelIfNeeded(UIUtils.createInfoLabel("No settings selected",
                    "Select a section in the tree on the left")));
            return;
        }

        JIPipeParameterCollection sheet = sheetById.get(sectionKey(sheetNode));
        boolean editable = sheet != null && model.isEditable(sheetNode, activeScope);

        JPanel wrapper = new JPanel(new BorderLayout());
        if (!editable) {
            wrapper.add(createScopeHintLabel(sheetNode.scope()), BorderLayout.NORTH);
        }

        String mergedPage = mergedPageOf(sheetNode.id());
        if (sheet == null) {
            wrapper.add(UIUtils.createInfoLabel("No settings available",
                    "The section '" + sheetNode.name() + "' is not available in this session"), BorderLayout.CENTER);
        } else if (editable && mergedPage != null) {
            if (JIPipeDesktopRawSettingsSection.PAGE_RAW.equals(mergedPage)) {
                // The raw settings editor lists the whole scope document; it hosts
                // its own scope selector and is not tied to this section's sheet.
                JIPipeDesktopRawSettingsSection section = new JIPipeDesktopRawSettingsSection();
                section.setScope(activeScope);
                currentRawSection = section;
                wrapper.add(section, BorderLayout.CENTER);
            } else {
                // A merged section: render the composed content instead of a form.
                JIPipeDesktopAppearanceSection section = new JIPipeDesktopAppearanceSection(getDesktopWorkbench(),
                        this,
                        mergedPage);
                currentSection = section;
                wrapper.add(section, BorderLayout.CENTER);
            }
        } else {
            JIPipeDesktopParameterFormPanel formPanel = createPlainFormPanel(sheet);
            if (!editable) {
                formPanel.setEnabled(false);
            }
            currentFormPanel = formPanel;
            wrapper.add(formPanel, BorderLayout.CENTER);
        }

        splitPane.setRightComponent(UIUtils.wrapInIslandPanelIfNeeded(wrapper));
    }

    private JIPipeDesktopParameterFormPanel createPlainFormPanel(JIPipeParameterCollection sheet) {
        return new JIPipeDesktopParameterFormPanel(getDesktopWorkbench(),
                sheet,
                MarkdownText.fromPluginResource("documentation/application-settings.md", new HashMap<>()),
                JIPipeDesktopParameterFormPanel.WITH_SCROLLING | JIPipeDesktopParameterFormPanel.WITH_DOCUMENTATION | JIPipeDesktopParameterFormPanel.WITH_SEARCH_BAR);
    }

    private JLabel createScopeHintLabel(SettingsScope scope) {
        return new JLabel("<html>This section belongs to the " + scopeToTitle(scope)
                + " settings &mdash; switch to the " + scopeToTitle(scope) + " tab to edit it</html>",
                JIPipe.RESOURCES.getIcon16("info.png"), SwingConstants.LEFT);
    }

    private void onSettingsChanged(JIPipeSettingsServiceComponent.SettingsChangedEvent event) {
        // The log is the audit of everything that changed since the tab opened:
        // every scope, every origin. It is thread-safe, so recording here — on
        // whatever thread mutated the document — is safe; the strip then
        // re-reads the log on the EDT.
        changeLog.record(event, RawSettingsTableModel.truncate(RawSettingsTableModel.render(event.getNewValue())));
        SwingUtilities.invokeLater(this::refreshChangesStrip);

        JIPipeSettingsServiceComponent.ChangeOrigin origin = event.getOrigin();
        boolean external = origin == JIPipeSettingsServiceComponent.ChangeOrigin.EXTERNAL_FILE
                || origin == JIPipeSettingsServiceComponent.ChangeOrigin.EXTERNAL_PROCESS
                || origin == JIPipeSettingsServiceComponent.ChangeOrigin.CARRY_OVER;
        if (!external || event.getScope() != activeScope) {
            return;
        }
        SwingUtilities.invokeLater(this::showBanner);
    }

    private void showBanner() {
        if (!isDisplayable()) {
            return;
        }
        banner.setVisible(true);
        revalidate();
        repaint();
    }

    private void hideBanner() {
        if (banner.isVisible()) {
            banner.setVisible(false);
            revalidate();
            repaint();
        }
    }

    private void disposeFormPanel() {
        if (currentSection != null) {
            currentSection.dispose();
            currentSection = null;
        }
        if (currentRawSection != null) {
            currentRawSection.dispose();
            currentRawSection = null;
        }
        if (currentFormPanel != null) {
            currentFormPanel.dispose();
            currentFormPanel = null;
        }
        splitPane.setRightComponent(UIUtils.wrapInIslandPanelIfNeeded(new JPanel()));
    }

    /**
     * Selects a section by its <b>scope-qualified identity</b> (scope + sheet
     * id) and switches the scope switcher to that scope. Sections share ids
     * across scopes ({@code org.hkijena.jipipe:default-environments} is both a
     * RUNTIME application sheet and a PROJECT sheet), so the scope is part of
     * what is being addressed.
     *
     * @param scope   the section's scope
     * @param sheetId the sheet's registration id
     * @return whether the section exists
     */
    public boolean selectSheet(SettingsScope scope, String sheetId) {
        if (scope == null || sheetId == null) {
            return false;
        }
        SettingsTreeModel.SheetNode node = nodeMap.get(SettingsTreeModel.scopeQualifiedId(scope, sheetId));
        if (node == null) {
            return false;
        }
        this.selectedSectionKey = sectionKey(node);
        // Clear a stale search so the target section is visible in the tree.
        if (!searchField.getText().isEmpty()) {
            searchField.setText(""); // triggers rebuildTree() via the document listener
        }
        if (node.scope() != activeScope) {
            setActiveScope(node.scope());
        } else {
            rebuildTree();
        }
        return true;
    }

    /**
     * @return the id of the selected section, or {@code null}
     */
    public String getSelectedSheetId() {
        SettingsTreeModel.SheetNode node = selectedSectionKey != null ? nodeMap.get(selectedSectionKey) : null;
        return node != null ? node.id() : null;
    }

    /**
     * @return the scope of the selected section, or {@code null} when none
     */
    public SettingsScope getSelectedSectionScope() {
        SettingsTreeModel.SheetNode node = selectedSectionKey != null ? nodeMap.get(selectedSectionKey) : null;
        return node != null ? node.scope() : null;
    }

    /**
     * @return the current section model (all scopes), or {@code null} before
     * {@link #reloadSections()} ran
     */
    public SettingsTreeModel getModel() {
        return model;
    }

    /**
     * Sets the section search text. A search narrows the tree to the matching
     * sections; an empty text restores the full tree.
     *
     * @param text the search text, or {@code null} to clear
     */
    public void setSectionSearch(String text) {
        String value = text == null ? "" : text;
        if (!Objects.equals(searchField.getText(), value)) {
            searchField.setText(value); // triggers rebuildTree() via the document listener
        }
        rebuildTree();
    }

    /**
     * @return the scope currently shown
     */
    public SettingsScope getActiveScope() {
        return activeScope;
    }

    /**
     * @param scope the scope to show
     */
    public void setActiveScope(SettingsScope scope) {
        if (scope == null || scope == activeScope) {
            return;
        }
        activeScope = scope;
        int index = Arrays.asList(SCOPES).indexOf(scope);
        if (scopeSwitcher.getSelectedIndex() != index) {
            // Triggers onScopeChanged() -> rebuildTree()
            scopeSwitcher.setSelectedIndex(index);
        } else {
            rebuildTree();
        }
    }

    private static String scopeToTitle(SettingsScope scope) {
        switch (scope) {
            case APPLICATION:
                return "Application";
            case PROJECT:
                return "Project";
            case RUNTIME:
                return "Runtime";
            default:
                return scope.name();
        }
    }

    @Override
    public void dispose() {
        // Idempotent: the pane disposes tab contents on window close, and a tab
        // the user closed earlier was already disposed by forceCloseTab.
        if (disposed) {
            return;
        }
        disposed = true;
        settingsService.getChangedEventEmitter().unsubscribe(changedListener);
        disposeFormPanel();
    }

    /**
     * Renders one changes-strip row: the change text (with a foreign marker)
     * and a trailing per-entry "Reset" button.
     */
    private class ChangesCellRenderer extends JPanel implements ListCellRenderer<SettingsChangeLog.ChangeEntry> {

        private final JLabel textLabel = new JLabel();
        private final JButton resetButton = new JButton("Reset", JIPipe.RESOURCES.getIcon16("actions/edit-reset.png"));
        private ActionListener resetListener;

        ChangesCellRenderer() {
            super(new BorderLayout(4, 0));
            setBorder(UIUtils.createEmptyBorder(2));
            add(textLabel, BorderLayout.CENTER);
            resetButton.setToolTipText("Reset this setting to its default");
            add(resetButton, BorderLayout.EAST);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends SettingsChangeLog.ChangeEntry> list,
                                                      SettingsChangeLog.ChangeEntry value,
                                                      int index,
                                                      boolean isSelected,
                                                      boolean cellHasFocus) {
            String text = value.displayText();
            if (value.isForeign()) {
                text += "  \u00b7external";
                textLabel.setIcon(JIPipe.RESOURCES.getIcon16("actions/gnumeric-link-external.png"));
            } else {
                textLabel.setIcon(null);
            }
            textLabel.setText(text);
            textLabel.setToolTipText(value.path() + " (" + value.origin() + ")");
            textLabel.setForeground(isSelected ? list.getSelectionForeground() : list.getForeground());
            setBackground(isSelected ? list.getSelectionBackground() : list.getBackground());
            setOpaque(true);
            resetButton.removeActionListener(resetListener);
            resetListener = e -> resetChangeEntry(value);
            resetButton.addActionListener(resetListener);
            return this;
        }
    }

    /**
     * One entry in the tree: a category (no sheet) or a section (with sheet).
     *
     * @param name  the display name
     * @param icon  the icon
     * @param sheet the section, or {@code null} for a category
     */
    private record SettingsTreeNodeEntry(String name, Icon icon, SettingsTreeModel.SheetNode sheet) {
    }

    private class SettingsTreeCellRenderer extends JLabel implements TreeCellRenderer {

        public SettingsTreeCellRenderer() {
            setOpaque(true);
            setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        }

        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
            if (value instanceof DefaultMutableTreeNode node && node.getUserObject() instanceof SettingsTreeNodeEntry entry) {
                boolean readOnly = entry.sheet() != null && model != null && !model.isEditable(entry.sheet(), activeScope);
                setText(entry.name());
                setIcon(entry.icon());
                if (readOnly) {
                    // Spec §3.4: sections of the other scope are shown dimmed.
                    setForeground(UIManager.getColor("Label.disabledForeground"));
                    setToolTipText("Belongs to the " + scopeToTitle(entry.sheet().scope()) + " settings");
                } else {
                    setForeground(selected ? UIManager.getColor("List.selectionForeground") : UIManager.getColor("List.foreground"));
                    setToolTipText(entry.sheet() != null ? entry.sheet().description() : null);
                }
            } else {
                setText("");
                setIcon(null);
                setForeground(UIManager.getColor("List.foreground"));
                setToolTipText(null);
            }
            setBackground(selected ? UIManager.getColor("List.selectionBackground") : UIManager.getColor("List.background"));
            return this;
        }
    }
}
