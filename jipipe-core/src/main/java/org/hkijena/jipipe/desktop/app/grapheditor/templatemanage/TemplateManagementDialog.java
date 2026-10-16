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

package org.hkijena.jipipe.desktop.app.grapheditor.templatemanage;

import com.google.common.collect.ImmutableList;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeNodeTemplate;
import org.hkijena.jipipe.api.nodes.database.entries.TemplateScope;
import org.hkijena.jipipe.api.nodes.taxonomy.JIPipeNodeTaxonomyFunction;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.desktop.JIPipeDesktop;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.app.running.JIPipeDesktopRunExecuteUI;
import org.hkijena.jipipe.desktop.commons.components.parameters.JIPipeDesktopParameterFormPanel;
import org.hkijena.jipipe.desktop.commons.components.panels.JIPipeDesktopFormPanel;
import org.hkijena.jipipe.desktop.commons.components.search.JIPipeDesktopSearchTextField;
import org.hkijena.jipipe.desktop.commons.theme.JIPipeDesktopModernThemeStyle;
import org.hkijena.jipipe.plugins.nodetemplate.NodeTemplatesRefreshedEvent;
import org.hkijena.jipipe.plugins.nodetemplate.NodeTemplatesRefreshedEventListener;
import org.hkijena.jipipe.plugins.nodetemplate.templatedownloader.NodeTemplateDownloaderRun;
import org.hkijena.jipipe.plugins.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.plugins.parameters.library.markup.MarkdownText;
import org.hkijena.jipipe.plugins.settings.application.JIPipeFileChooserApplicationSettings;
import org.hkijena.jipipe.utils.CustomStateLessToggleButton;
import org.hkijena.jipipe.utils.JIPipeDesktopSplitPane;
import org.hkijena.jipipe.utils.PathUtils;
import org.hkijena.jipipe.utils.ThemeUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The "Configure node templates" manager (round-2 redesign, work-item 1324
 * design v2). A <b>non-modal, unique</b> feature-manager window (a
 * {@link JFrame}, opened via {@link #show(JIPipeDesktopWorkbench, TemplateScope)}
 * so at most one instance exists per owner window — opening it again brings the
 * existing one to the front) with two views:
 * <ul>
 * <li><b>Manage</b>: collapsible sections (Your templates / This project /
 * Downloaded [+ "Get more"] / System, last and collapsed by default). Each
 * section's templates are a wrapping {@link JList} of cards (standard
 * shift/ctrl selection, wheel scroll, right-click context menu for the
 * operations — no bottom toolbar).</li>
 * <li><b>Repository</b> ("Get more"): an islands split pane (category sidebar
 * + content) that notes the marketplace is a future feature, with the existing
 * "Download templates" action.</li>
 * </ul>
 * <p>
 * Section headers use the shared {@link JIPipeDesktopFormPanel.GroupHeaderPanel}
 * with the parameter-panel collapse button
 * ({@link CustomStateLessToggleButton}, caret-right/down, flat, non-opaque), so
 * the collapse look and the extra-button placement match the rest of the app.
 * The store logic (copy / delete / edit / export / import and the scope
 * partition) all lives in the Swing-free {@link TemplateManagementController}.
 */
public class TemplateManagementDialog extends JFrame implements NodeTemplatesRefreshedEventListener {

    /** Unique-enforcement registry: one manager per owner window (item 1). */
    private static final Map<Window, TemplateManagementDialog> INSTANCES = new HashMap<>();

    /** True once the window has been closed (Swing has no isDisposed()). */
    private boolean disposed;

    private static final int CARD_CELL_WIDTH = 210;
    private static final int CARD_CELL_HEIGHT = 66;
    // The painted card is inset by CARD_INSET within its cell: this inset is the
    // GUTTER, so adjacent cards never touch and every card has clear spacing
    // around it (round-3 "gutter" ask).
    private static final int CARD_INSET = 6;
    // A clearly visible corner radius (round-3 "rounded corners" ask).
    private static final int CARD_ARC = 12;

    private final JIPipeDesktopWorkbench workbench;
    private final JIPipeProject project;
    private final TemplateManagementController controller = new TemplateManagementController();

    // --- Views (built in initialize(); non-final so the two-card layout can
    // assign them) ---
    private final JPanel viewContainer = new JPanel(new CardLayout());
    private JPanel manageView;
    private JPanel repositoryView;

    // --- Manage view sections ---
    private final List<TemplateSection> sections = new ArrayList<>();

    public TemplateManagementDialog(JIPipeDesktopWorkbench workbench, TemplateScope initialScope) {
        this.workbench = workbench;
        this.project = workbench instanceof JIPipeDesktopProjectWorkbench projectWorkbench ? projectWorkbench.getProject() : null;
        setTitle(UIUtils.getWindowTitle("Configure node templates"));
        setIconImage(UIUtils.getJIPipeIcon128());
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        initialize();
        JIPipe.getNodeTemplates().getNodeTemplatesRefreshedEventEmitter().subscribeWeak(this);
    }

    /**
     * Opens (or brings to the front) the template manager for the given owner
     * window — the unique-enforcement entry point: at most one instance per
     * owner window; a second call focuses the existing one. Non-blocking (the
     * window is a non-modal {@link JFrame}).
     *
     * @param workbench  the workbench providing the owner window + project
     * @param initialScope the scope to focus (reserved; the manager shows all sections)
     * @return the shown (or already-existing) window
     */
    public static TemplateManagementDialog show(JIPipeDesktopWorkbench workbench, TemplateScope initialScope) {
        Window owner = workbench.getWindow();
        TemplateManagementDialog existing = owner == null ? null : INSTANCES.get(owner);
        if (existing != null && !existing.disposed) {
            existing.setVisible(true);
            existing.toFront();
            existing.requestFocus();
            return existing;
        }
        TemplateManagementDialog window = new TemplateManagementDialog(workbench, initialScope);
        if (owner != null) {
            INSTANCES.put(owner, window);
            window.addWindowListener(new java.awt.event.WindowAdapter() {
                @Override
                public void windowClosed(java.awt.event.WindowEvent e) {
                    window.disposed = true;
                    INSTANCES.remove(owner);
                }
            });
        }
        window.setVisible(true);
        return window;
    }

    private void initialize() {
        JPanel contentPane = new JPanel(new BorderLayout(8, 8));
        contentPane.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        contentPane.setBackground(ThemeUtils.getCurrentStyle().getWindowBackground());

        manageView = buildManageView();
        repositoryView = buildRepositoryView();
        viewContainer.add(manageView, "manage");
        viewContainer.add(repositoryView, "repository");
        contentPane.add(viewContainer, BorderLayout.CENTER);
        setContentPane(contentPane);

        showManageView();
        // Same size as the AI overview window (item 3).
        setSize(800, 600);
        setLocationRelativeTo(workbench.getWindow());
        reloadSections();
        addEscapeListener();
    }

    /** Escape closes the window (frame-equivalent of {@code UIUtils.addEscapeListener}). */
    private void addEscapeListener() {
        getRootPane().registerKeyboardAction(e -> setVisible(false),
            KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
            JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    private void showManageView() {
        ((CardLayout) viewContainer.getLayout()).show(viewContainer, "manage");
    }

    private void showRepositoryView() {
        ((CardLayout) viewContainer.getLayout()).show(viewContainer, "repository");
    }

    // ------------------------------------------------------------------
    // Manage view
    // ------------------------------------------------------------------

    private JPanel buildManageView() {
        // Build the manage view EXACTLY the way the AI monitor does (the design
        // reference the user keeps pointing at): a JIPipeDesktopFormPanel with
        // WITH_SCROLLING, wrapped in ONE island. The form panel IS the proven
        // machinery that my hand-rolled attempts kept getting wrong:
        //   - its content is a JXPanel with ScrollableSizeHint.FIT width, so the
        //     page width always tracks the scroll viewport (no right-edge
        //     overflow — fixes the "list expands to the right after resizing
        //     with System expanded" bug);
        //   - addGroupHeader / addWideToForm lay rows out with GridBag
        //     fill=HORIZONTAL + weightx=1, so every row is STRETCHED to the full
        //     page width. A VERTICAL_WRAP list reflows its column count to the
        //     width it is actually given, so the cards always fit (BoxLayout —
        //     what (76)/(77) used — only gives children their self-referential
        //     preferred width, which is why the list used to overflow);
        //   - the WITH_SCROLLING scroll handles all vertical scrolling; the
        //     opaque panel background is the same color as the island fill, so
        //     the whole thing reads as ONE rounded island (the "compare with
        //     the AI monitor" islands ask).
        JIPipeDesktopFormPanel formPanel = new JIPipeDesktopFormPanel(JIPipeDesktopFormPanel.WITH_SCROLLING);

        // Sections (the design's order; System last and collapsed by default).
        sections.clear();
        sections.add(new TemplateSection("Your templates", TemplateScope.Global, JIPipe.RESOURCES.getIcon16("actions/configure.png"), true));
        if (project != null) {
            sections.add(new TemplateSection("This project", TemplateScope.Project, JIPipe.RESOURCES.getIcon16("actions/graph-compartment.png"), true));
        }
        sections.add(new TemplateSection("Downloaded", TemplateScope.Downloaded, JIPipe.RESOURCES.getIcon16("actions/download.png"), true,
            this::buildGetMoreButton));
        sections.add(new TemplateSection("System", TemplateScope.System, JIPipe.RESOURCES.getIcon16("actions/dialog-templates.png"), false));
        for (TemplateSection section : sections) {
            section.buildSectionOn(formPanel);
        }
        formPanel.addVerticalGlue();

        // ONE island wraps the whole scrollable page (the window background
        // shows as the gutter around it), matching the AI monitor.
        JPanel view = new JPanel(new BorderLayout());
        view.setOpaque(false);
        view.add(UIUtils.wrapInIslandPanelIfNeeded(formPanel), BorderLayout.CENTER);
        return view;
    }

    // ------------------------------------------------------------------
    // Repository view ("Get more") — the marketplace seam (a future feature)
    // ------------------------------------------------------------------

    private JPanel buildRepositoryView() {
        JPanel view = new JPanel(new BorderLayout());
        view.setOpaque(false);

        // Header (the round-3 fix: this was dropped in the (76) rewrite — the
        // "Back" button and the search bar are back). [← Back] returns to the
        // manage view; the search field filters the (future) marketplace.
        JPanel header = new JPanel(new BorderLayout(8, 0));
        header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(0, 0, 6, 0));
        JButton backButton = UIUtils.createButton("Back", JIPipe.RESOURCES.getIcon16("actions/go-previous.png"), this::showManageView);
        UIUtils.setStandardButtonBorder(backButton);
        header.add(backButton, BorderLayout.WEST);
        JIPipeDesktopSearchTextField searchField = new JIPipeDesktopSearchTextField();
        searchField.setToolTipText("Search the marketplace (a future feature)");
        header.add(searchField, BorderLayout.CENTER);
        view.add(header, BorderLayout.NORTH);

        // Sidebar: Featured / Collections (a future feature) / function branches.
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setOpaque(false);
        sidebar.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        sidebar.add(sidebarItem("Featured", JIPipe.RESOURCES.getIcon16("actions/rating.png")));
        JButton collections = sidebarItem("Collections", JIPipe.RESOURCES.getIcon16("actions/box.png"));
        collections.setEnabled(false);
        collections.setToolTipText("A future feature of the template marketplace");
        sidebar.add(collections);
        sidebar.add(Box.createVerticalStrut(8));
        for (JIPipeNodeTaxonomyFunction top : JIPipe.getNodes().getFunctions().topLevel()) {
            sidebar.add(sidebarItem(top.getName(), top.getIcon() != null ? top.getIcon() : JIPipe.RESOURCES.getIcon16("actions/tag.png")));
        }
        sidebar.add(Box.createVerticalGlue());

        // Content: the marketplace is a future feature; the existing
        // "Download templates" action stays available now.
        JPanel content = new JPanel(new BorderLayout(8, 8));
        content.setOpaque(false);
        content.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        JLabel header2 = UIUtils.createJLabel("Template marketplace", JIPipe.RESOURCES.getIcon32("actions/download.png"), 20);
        content.add(header2, BorderLayout.NORTH);
        JLabel note = new JLabel("<html>The full template marketplace — searching, categories, and sharing your own "
            + "templates — is a future feature.<br/><br/>You can already download templates from the configured "
            + "repositories.</html>");
        note.setVerticalAlignment(SwingConstants.TOP);
        note.setForeground(ThemeUtils.getCurrentStyle().getTextMuted());
        content.add(note, BorderLayout.CENTER);
        JPanel actionRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        actionRow.setOpaque(false);
        actionRow.add(UIUtils.createButton("Download templates", JIPipe.RESOURCES.getIcon16("actions/download.png"),
            () -> JIPipeDesktopRunExecuteUI.runInDialog(workbench, this, new NodeTemplateDownloaderRun(workbench))));
        content.add(actionRow, BorderLayout.SOUTH);

        JIPipeDesktopSplitPane splitPane = new JIPipeDesktopSplitPane(JIPipeDesktopSplitPane.LEFT_RIGHT,
            UIUtils.wrapInIslandPanelIfNeeded(sidebar),
            UIUtils.wrapInIslandPanelIfNeeded(content),
            JIPipeDesktopSplitPane.RATIO_1_TO_3);
        view.add(splitPane, BorderLayout.CENTER);
        return view;
    }

    private JButton sidebarItem(String text, Icon icon) {
        JButton button = new JButton(text, icon);
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));
        button.setOpaque(false);
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setForeground(ThemeUtils.getCurrentStyle().getTextForeground());
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        return button;
    }

    private JButton buildGetMoreButton() {
        JButton getMore = UIUtils.createButton("Get more", JIPipe.RESOURCES.getIcon16("actions/download.png"), this::showRepositoryView);
        UIUtils.setStandardButtonBorder(getMore);
        return getMore;
    }

    // ------------------------------------------------------------------
    // One collapsible section (GroupHeaderPanel header + wrapping JList)
    // ------------------------------------------------------------------

    private class TemplateSection {
        private final String title;
        private final TemplateScope scope;
        private final boolean expanded;
        private final Icon icon;
        private final java.util.function.Supplier<JComponent> headerExtra;
        private JIPipeDesktopFormPanel formPanel;
        private JIPipeDesktopFormPanel.GroupHeaderPanel header;
        private final JList<JIPipeNodeTemplate> list;
        private final CardRenderer cardRenderer = new CardRenderer();

        TemplateSection(String title, TemplateScope scope, Icon icon, boolean expanded) {
            this(title, scope, icon, expanded, null);
        }

        TemplateSection(String title, TemplateScope scope, Icon icon, boolean expanded, java.util.function.Supplier<JComponent> headerExtra) {
            this.title = title;
            this.scope = scope;
            this.expanded = expanded;
            this.icon = icon;
            this.headerExtra = headerExtra;

            // The wrapping card list (items 6,7,8,9): standard shift/ctrl
            // selection, wheel scroll, clickthrough (renderer labels don't
            // block), virtualized for many items.
            //
            // The list lives in a JViewport (its own scroll pane, see
            // buildSectionOn) and forces its width to the viewport width, so it
            // is always given a STABLE width to wrap into — no layout manager
            // (BoxLayout, the form's GridBag) does that for a wrap list on its
            // own.
            this.list = new JList<>(new DefaultListModel<>()) {
                @Override
                public boolean getScrollableTracksViewportWidth() {
                    // Always track the viewport width so the wrapping grid
                    // reflows to the available width on every resize.
                    return true;
                }
            };
            // HORIZONTAL_WRAP — NOT VERTICAL_WRAP. This is what fixes the
            // "list expands to the right beyond the edge" bug. HORIZONTAL_WRAP
            // lays the cards out left-to-right and computes the number of
            // COLUMNS from the list's WIDTH (columnCount = width / cellWidth),
            // so the grid fills the available width and grows DOWNWARD as
            // needed, reflowing its column count on every resize. VERTICAL_WRAP
            // instead computes the column count from the HEIGHT and grows the
            // list RIGHTWARD to fit all of its columns — which is exactly the
            // right-edge overflow the user reported.
            list.setLayoutOrientation(JList.HORIZONTAL_WRAP);
            // CRITICAL: force visibleRowCount to 0. The default (8) makes
            // BasicListUI compute the column count from the VISIBLE ROW COUNT
            // (ignoring the actual width), which is wrong for a card grid. With
            // 0 it uses the WIDTH-derived branch (columnCount = width /
            // cellWidth), so the grid fills the available width, reflows its
            // column count on every resize, and never overflows the right edge.
            list.setVisibleRowCount(0);
            // Fixed cells give a deterministic wrap grid; the modern card (with
            // its gutter + rounded corners) is painted INSIDE each cell by the
            // renderer, so the cells act as the inter-card spacing.
            list.setFixedCellWidth(CARD_CELL_WIDTH);
            list.setFixedCellHeight(CARD_CELL_HEIGHT);
            list.setCellRenderer(cardRenderer);
            list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
            // The card renderer paints its OWN rounded selection; suppress the
            // list's default rectangular selection fill so it doesn't show
            // through the card's gutter or square off the corners.
            list.setSelectionBackground(ThemeUtils.getCurrentStyle().getPanelBackground());
            list.setBorder(BorderFactory.createEmptyBorder(2, 4, 6, 4));
            list.setOpaque(false); // the island/form background provides the surface
            list.addMouseListener(new MouseAdapter() {
                // Plain / shift / ctrl selection is handled by the JList itself
                // (the standard list UX the user asked for); only the popup and
                // the double-click-edit are wired here. The popup-trigger check
                // runs on BOTH press and release to cover all platforms (the
                // JIPipe convention, see the node browser's list/tree menus).
                @Override
                public void mousePressed(MouseEvent e) {
                    if (e.isPopupTrigger()) {
                        showTemplateMenu(e, list);
                    }
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    if (e.isPopupTrigger()) {
                        showTemplateMenu(e, list);
                    }
                }

                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2) {
                        JIPipeNodeTemplate t = list.getSelectedValue();
                        if (t != null && canEdit(t)) {
                            editTemplate(t, scopeOf(t));
                        }
                    }
                }
            });

            // The list lives in its own viewport (buildSectionOn). With both
            // scrollbars set to NEVER that scroll pane is not a nested
            // scroller — it is just what forces the width-tracking list to the
            // available width. The form panel's single outer scroll
            // (buildManageView) still scrolls the whole page.
            list.setMinimumSize(new Dimension(CARD_CELL_WIDTH, 0));
        }

        /**
         * Adds this section to the form panel: a full-width group header (with
         * the collapse caret + optional extra button) and the full-width card
         * list right below it — the same addGroupHeader + addWideToForm pattern
         * the AI monitor and the parameter forms use.
         */
        void buildSectionOn(JIPipeDesktopFormPanel formPanel) {
            this.formPanel = formPanel;

            // The shared group header (item 5), added full-width by the form:
            // title + icon, the parameter-panel collapse caret at the START of
            // the title, and any extra button (e.g. "Get more") at the END
            // (item 12 placement, fixes the ellipsis of item 4).
            this.header = formPanel.addGroupHeader(title, icon);

            // The card list in its own viewport. Both scrollbars are NEVER, so
            // this is NOT a nested scroller — it is the width-forcing wrapper
            // the wrap list needs (see the list's construction). The form
            // panel's single outer scroll scrolls the whole page.
            JScrollPane listScroll = new JScrollPane(list,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
            listScroll.setBorder(null);
            listScroll.setOpaque(false);
            listScroll.getViewport().setOpaque(false);
            listScroll.setVisible(expanded);

            CustomStateLessToggleButton collapseButton = new CustomStateLessToggleButton("",
                JIPipe.RESOURCES.getIcon16("actions/caret-right.png"),
                "",
                JIPipe.RESOURCES.getIcon16("actions/caret-down.png"),
                expanded);
            UIUtils.makeButtonFlat25x25(collapseButton);
            collapseButton.setOpaque(false); // no toggled/pressed background (item 10)
            collapseButton.setFocusPainted(false);
            collapseButton.setToolTipText(expanded ? "Collapse" : "Expand");
            header.addToStartOfTitlePanel(collapseButton);
            if (headerExtra != null) {
                header.addToEndOfTitlePanel(headerExtra.get());
            }
            header.setCollapsed(!expanded);
            collapseButton.getToggledEventEmitter().subscribeLambda((e, listener) -> {
                boolean nowExpanded = collapseButton.getState();
                header.setCollapsed(!nowExpanded);
                // The form's GridBag skips invisible components, so hiding the
                // list collapses the section to just its header (the proven
                // collapse mechanism of the parameter forms).
                listScroll.setVisible(nowExpanded);
                collapseButton.setToolTipText(nowExpanded ? "Collapse" : "Expand");
                formPanel.revalidate();
            });

            // Full-width row: the form's GridBag (fill=HORIZONTAL, weightx=1)
            // stretches the scroll pane to the page width; its viewport then
            // forces the width-tracking list to exactly that width, so the
            // HORIZONTAL_WRAP list reflows its column count (width-derived) on
            // every resize (the "list expands to the right beyond the edge"
            // fix).
            formPanel.addWideToForm(listScroll);
        }

        /** (Re)loads this section's card list from the store. */
        void reload() {
            DefaultListModel<JIPipeNodeTemplate> model = (DefaultListModel<JIPipeNodeTemplate>) list.getModel();
            model.removeAllElements();
            for (JIPipeNodeTemplate t : controller.forScope(project, scope)) {
                model.addElement(t);
            }
        }

        /** The templates currently in this section's list (test accessor support). */
        List<JIPipeNodeTemplate> templates() {
            DefaultListModel<JIPipeNodeTemplate> model = (DefaultListModel<JIPipeNodeTemplate>) list.getModel();
            List<JIPipeNodeTemplate> result = new ArrayList<>();
            for (int i = 0; i < model.getSize(); i++) {
                result.add(model.get(i));
            }
            return result;
        }
    }

    // ------------------------------------------------------------------
    // Selection + context menu (delegating to the controller)
    // ------------------------------------------------------------------

    private void showTemplateMenu(MouseEvent e, JList<JIPipeNodeTemplate> list) {
        List<JIPipeNodeTemplate> selected = list.getSelectedValuesList();
        JPopupMenu menu = buildTemplateMenu(selected);
        if (menu != null) {
            menu.show(list, e.getX(), e.getY());
        }
    }

    /**
     * The context menu for a set of selected templates (item 6). Items are
     * included only when applicable to the (possibly multi) selection — this
     * is the bulk right-click operation. Returns {@code null} when there is no
     * actionable selection.
     */
    private JPopupMenu buildTemplateMenu(List<JIPipeNodeTemplate> selected) {
        if (selected.isEmpty()) {
            return null;
        }
        JPopupMenu menu = new JPopupMenu();
        boolean anySystem = false, anyDownloaded = false, anyEditable = false, anyProject = false, anyGlobal = false;
        for (JIPipeNodeTemplate t : selected) {
            switch (scopeOf(t)) {
                case System -> anySystem = true;
                case Downloaded -> anyDownloaded = true;
                case Global -> anyGlobal = true;
                case Project -> anyProject = true;
            }
            if (canEdit(t)) {
                anyEditable = true;
            }
        }

        if (anyEditable && selected.size() == 1) {
            menu.add(UIUtils.createMenuItem("Edit", "Edit this template", JIPipe.RESOURCES.getIcon16("actions/document-edit.png"),
                () -> editTemplate(selected.get(0), scopeOf(selected.get(0)))));
        }
        if (anySystem || anyDownloaded || anyProject) {
            // The escape hatch: a bundled pattern / a download / a project
            // template becomes a local (editable) version in the user's own
            // templates. (Publishing it back is a future feature.)
            menu.add(UIUtils.createMenuItem(anySystem || anyDownloaded ? "Copy to Your templates" : "Copy to Global",
                "Copy into the user's own templates", JIPipe.RESOURCES.getIcon16("actions/edit-copy.png"),
                () -> copySelected(selected, TemplateScope.Global)));
        }
        if (project != null && (anyGlobal || anyDownloaded)) {
            menu.add(UIUtils.createMenuItem("Copy to Project", "Copy into the project storage", JIPipe.RESOURCES.getIcon16("actions/folder.png"),
                () -> copySelected(selected, TemplateScope.Project)));
        }
        if (!anySystem) {
            // Deletable: user-owned templates and downloads (removing a
            // download — re-obtainable via "Get more"). System patterns are
            // bundled and cannot be removed.
            menu.add(UIUtils.createMenuItem(anyDownloaded && !anyGlobal && !anyProject ? "Remove download" : "Delete",
                "Delete the selected template(s)", JIPipe.RESOURCES.getIcon16("actions/edit-delete.png"),
                () -> deleteSelected(selected)));
        }
        menu.addSeparator();
        menu.add(UIUtils.createMenuItem("Export…", "Export the selected template(s) to a file", JIPipe.RESOURCES.getIcon16("actions/document-export.png"),
            () -> exportSelected(selected)));
        menu.add(UIUtils.createMenuItem("Import…", "Import templates from a file into the user's own templates", JIPipe.RESOURCES.getIcon16("actions/document-import.png"),
            this::importTemplates));
        return menu;
    }

    private boolean canEdit(JIPipeNodeTemplate template) {
        // System (bundled) and Downloaded (repository artifact) templates are
        // read-only: the escape hatch for both is a copy into the user's own
        // templates.
        return !template.isFromExtension()
            && !JIPipe.getNodeTemplates().isDownloadedTemplate(template);
    }

    /**
     * The scope a template belongs to, from the live store (mirrors the
     * database builder's {@code determineTemplateScope}): plugin templates are
     * System, global-store templates are Downloaded (repository source) or
     * Global (user-authored), everything else is Project. Deriving from the
     * store (rather than "which section listed it") keeps the context menu
     * correct for a template that just changed scope mid-session.
     */
    private TemplateScope scopeOf(JIPipeNodeTemplate template) {
        if (JIPipe.getNodeTemplates().isPluginTemplate(template)) {
            return TemplateScope.System;
        }
        if (JIPipe.getNodeTemplates().isInGlobal(template)) {
            return JIPipe.getNodeTemplates().isDownloadedTemplate(template) ? TemplateScope.Downloaded : TemplateScope.Global;
        }
        return TemplateScope.Project;
    }

    private void editTemplate(JIPipeNodeTemplate template, TemplateScope scope) {
        // Edit on a COPY (exactly as the old template box's edit action did);
        // because taxonomyFunction is a registered @JIPipeParameter, the form
        // shows the taxonomy-function picker automatically.
        JIPipeNodeTemplate copy = new JIPipeNodeTemplate(template);
        if (JIPipeDesktopParameterFormPanel.showDialog(workbench, copy,
                new MarkdownText("# Node templates\n\nUse this user interface to modify node templates."),
                "Edit template",
                JIPipeDesktopParameterFormPanel.WITH_SCROLLING
                    | JIPipeDesktopParameterFormPanel.WITH_SEARCH_BAR
                    | JIPipeDesktopParameterFormPanel.WITH_DOCUMENTATION)) {
            controller.edit(project, scope, template, copy);
        }
    }

    private void copySelected(List<JIPipeNodeTemplate> selected, TemplateScope target) {
        if (project == null && target == TemplateScope.Project) {
            return;
        }
        int n = selected.size();
        String targetName = target == TemplateScope.Global ? "global" : "project";
        if (JOptionPane.showConfirmDialog(this, "Copy " + n + " template(s) into the " + targetName + " storage?",
                "Copy to " + targetName + " storage", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) != JOptionPane.YES_OPTION) {
            return;
        }
        for (JIPipeNodeTemplate t : ImmutableList.copyOf(selected)) {
            controller.copyTo(project, scopeOf(t), target, t);
        }
    }

    private void deleteSelected(List<JIPipeNodeTemplate> selected) {
        int n = selected.size();
        if (JOptionPane.showConfirmDialog(this, "Delete " + n + " template(s)?",
                "Delete templates", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) != JOptionPane.YES_OPTION) {
            return;
        }
        for (JIPipeNodeTemplate t : ImmutableList.copyOf(selected)) {
            controller.delete(project, scopeOf(t), t);
        }
    }

    private void exportSelected(List<JIPipeNodeTemplate> selected) {
        if (selected.isEmpty()) {
            return;
        }
        Path path = JIPipeDesktop.saveFile(this, workbench, JIPipeFileChooserApplicationSettings.LastDirectoryKey.Projects,
            "Export templates", HTMLText.EMPTY, PathUtils.EXTENSION_FILTER_JSON);
        if (path != null) {
            if (!controller.exportTemplates(project, scopeOf(selected.get(0)), selected, path)) {
                UIUtils.showErrorDialog(workbench, this, new IOException("Could not write template file " + path));
            }
        }
    }

    private void importTemplates() {
        Path path = JIPipeDesktop.openFile(this, workbench, JIPipeFileChooserApplicationSettings.LastDirectoryKey.Projects,
            "Import templates", HTMLText.EMPTY, PathUtils.EXTENSION_FILTER_JSON);
        if (path != null) {
            List<JIPipeNodeTemplate> imported = controller.importTemplates(project, TemplateScope.Global, path);
            if (imported.isEmpty()) {
                JOptionPane.showMessageDialog(this, "No templates were imported from " + path + ".", "Import", JOptionPane.INFORMATION_MESSAGE);
            }
        }
    }

    // ------------------------------------------------------------------
    // Refresh + test accessors
    // ------------------------------------------------------------------

    private void reloadSections() {
        for (TemplateSection section : sections) {
            section.reload();
        }
    }

    @Override
    public void onJIPipeNodeTemplatesRefreshed(NodeTemplatesRefreshedEvent event) {
        if (isDisplayable()) {
            SwingUtilities.invokeLater(this::reloadSections);
        }
    }

    // ------------------------------------------------------------------
    // Test accessors (headless UI tests; no production callers)
    // ------------------------------------------------------------------

    List<String> sectionTitlesForTest() {
        List<String> titles = new ArrayList<>();
        for (TemplateSection section : sections) {
            titles.add(section.title);
        }
        return titles;
    }

    boolean isSectionExpandedForTest(String title) {
        for (TemplateSection section : sections) {
            if (section.title.equals(title)) {
                return section.expanded;
            }
        }
        throw new IllegalArgumentException("no section: " + title);
    }

    List<JIPipeNodeTemplate> sectionTemplatesForTest(String title) {
        for (TemplateSection section : sections) {
            if (section.title.equals(title)) {
                return section.templates();
            }
        }
        throw new IllegalArgumentException("no section: " + title);
    }

    /** The context-menu item labels for the given selection (test access). */
    List<String> templateMenuLabelsForTest(List<JIPipeNodeTemplate> selected) {
        JPopupMenu menu = buildTemplateMenu(selected);
        if (menu == null) {
            return List.of();
        }
        List<String> labels = new ArrayList<>();
        for (Component c : menu.getComponents()) {
            if (c instanceof JMenuItem item) {
                labels.add(item.getText());
            }
        }
        return labels;
    }

    // ------------------------------------------------------------------
    // The modern card row renderer for the section lists
    // ------------------------------------------------------------------

    /**
     * A modern template card (round-3): a rounded-rectangle surface inset by a
     * GUTTER within its (fixed-size) list cell, with the template icon, name
     * and a one-line description. The cell is transparent; the card is painted
     * in {@link #paintComponent} INSET by {@link #CARD_INSET} so the corners
     * are rounded (a plain {@code LineBorder} is square) and the inter-card
     * spacing (the gutter) is a real gap of the list background between cards,
     * not just a border.
     */
    private class CardRenderer extends JPanel implements ListCellRenderer<JIPipeNodeTemplate> {
        private final JLabel icon = new JLabel();
        private final JLabel name = new JLabel();
        private final JLabel desc = new JLabel();
        private boolean cardSelected = false;

        CardRenderer() {
            setLayout(new BorderLayout(8, 2));
            setOpaque(false);
            // The CONTENT (icon + text) sits inside the painted card: the
            // CARD_INSET gutter plus a little inner padding.
            setBorder(BorderFactory.createEmptyBorder(CARD_INSET + 6, CARD_INSET + 10, CARD_INSET + 6, CARD_INSET + 10));

            icon.setVerticalAlignment(SwingConstants.CENTER);
            name.setOpaque(false);
            name.setFont(name.getFont().deriveFont(Font.BOLD));
            name.setVerticalAlignment(SwingConstants.CENTER);
            desc.setOpaque(false);
            desc.setForeground(ThemeUtils.getCurrentStyle().getTextMuted());
            desc.setVerticalAlignment(SwingConstants.CENTER);

            JPanel text = new JPanel();
            text.setOpaque(false);
            text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
            text.add(name);
            text.add(desc);
            add(icon, BorderLayout.WEST);
            add(text, BorderLayout.CENTER);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            Object oldAA = g2.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            try {
                // The card is INSET by the gutter within the cell, so adjacent
                // cards are separated by a 2*CARD_INSET gap of the list
                // background (the "gutter" the user asked for).
                int x = CARD_INSET;
                int y = CARD_INSET;
                int w = getWidth() - 2 * CARD_INSET - 1;
                int h = getHeight() - 2 * CARD_INSET - 1;
                JIPipeDesktopModernThemeStyle style = ThemeUtils.getCurrentStyle();
                Color fill = cardSelected ? style.getSelectionBackground() : style.getPanelBackground();
                Color stroke = cardSelected ? style.getSelectionHighlight() : style.getBorderColor();
                g2.setColor(fill);
                g2.fillRoundRect(x, y, w, h, CARD_ARC, CARD_ARC);
                g2.setColor(stroke);
                g2.drawRoundRect(x, y, w, h, CARD_ARC, CARD_ARC);
            } finally {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldAA);
            }
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends JIPipeNodeTemplate> list, JIPipeNodeTemplate value,
                                                      int index, boolean isSelected, boolean cellHasFocus) {
            setPreferredSize(new Dimension(CARD_CELL_WIDTH, CARD_CELL_HEIGHT));
            cardSelected = isSelected;
            if (value != null) {
                icon.setIcon(value.getIconImage() != null ? value.getIconImage() : JIPipe.RESOURCES.getIcon32("actions/tag.png"));
                name.setText(value.getName());
                name.setToolTipText(value.getName());
                desc.setText(shortDescription(value));
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
                icon.setForeground(fg);
            } else {
                setForeground(style.getTextForeground());
                name.setForeground(style.getTextForeground());
                desc.setForeground(style.getTextMuted());
                icon.setForeground(style.getTextForeground());
            }
            return this;
        }

        private String shortDescription(JIPipeNodeTemplate template) {
            if (template.getDescription() != null) {
                String body = template.getDescription().getBody().replace('\n', ' ').trim();
                if (!body.isEmpty()) {
                    return body.length() > 42 ? body.substring(0, 41) + "…" : body;
                }
            }
            return scopeOf(template).name();
        }
    }
}
