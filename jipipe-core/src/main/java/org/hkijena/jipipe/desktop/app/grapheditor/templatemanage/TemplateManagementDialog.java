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

    private static final int CARD_WIDTH = 200;
    private static final int CARD_HEIGHT = 58;

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
        // Sections (the design's order; System last and collapsed by default).
        sections.clear();
        sections.add(new TemplateSection("Your templates", TemplateScope.Global, JIPipe.RESOURCES.getIcon16("actions/configure.png"), true));
        if (project != null) {
            sections.add(new TemplateSection("This project", TemplateScope.Project, JIPipe.RESOURCES.getIcon16("actions/graph-compartment.png"), true));
        }
        sections.add(new TemplateSection("Downloaded", TemplateScope.Downloaded, JIPipe.RESOURCES.getIcon16("actions/download.png"), true,
            this::buildGetMoreButton));
        sections.add(new TemplateSection("System", TemplateScope.System, JIPipe.RESOURCES.getIcon16("actions/dialog-templates.png"), false));

        JPanel sectionsPanel = new JPanel();
        sectionsPanel.setLayout(new BoxLayout(sectionsPanel, BoxLayout.Y_AXIS));
        sectionsPanel.setOpaque(false);
        sectionsPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        for (TemplateSection section : sections) {
            section.getSectionComponent().setMaximumSize(new Dimension(Integer.MAX_VALUE, 500));
            sectionsPanel.add(section.getSectionComponent());
            sectionsPanel.add(Box.createVerticalStrut(6));
        }

        // A single vertical-only scroll for the whole manage view (item 2: no
        // horizontal bar — the JList grid never overflows horizontally).
        JScrollPane scroll = new JScrollPane(sectionsPanel, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(null);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(16);

        JPanel view = new JPanel(new BorderLayout());
        view.setOpaque(false);
        view.add(scroll, BorderLayout.CENTER);
        return view;
    }

    // ------------------------------------------------------------------
    // Repository view ("Get more") — the marketplace seam (a future feature)
    // ------------------------------------------------------------------

    private JPanel buildRepositoryView() {
        JPanel view = new JPanel(new BorderLayout());
        view.setOpaque(false);

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
        JLabel header = UIUtils.createJLabel("Template marketplace", JIPipe.RESOURCES.getIcon32("actions/download.png"), 20);
        content.add(header, BorderLayout.NORTH);
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
        private final JPanel sectionPanel;
        private final JIPipeDesktopFormPanel.GroupHeaderPanel header;
        private final JScrollPane listScroll;
        private final JList<JIPipeNodeTemplate> list;
        private final CardRenderer cardRenderer = new CardRenderer();

        TemplateSection(String title, TemplateScope scope, Icon icon, boolean expanded) {
            this(title, scope, icon, expanded, null);
        }

        TemplateSection(String title, TemplateScope scope, Icon icon, boolean expanded, java.util.function.Supplier<JComponent> headerExtra) {
            this.title = title;
            this.scope = scope;
            this.expanded = expanded;

            // The wrapping card list (items 6,7,8,9): standard shift/ctrl
            // selection, wheel scroll, clickthrough (renderer labels don't
            // block), virtualized for many items. Created before the collapse
            // button so the collapse handler can reference the scroll pane.
            this.list = new JList<>(new DefaultListModel<>());
            list.setLayoutOrientation(JList.VERTICAL_WRAP);
            list.setFixedCellWidth(CARD_WIDTH);
            list.setFixedCellHeight(CARD_HEIGHT);
            list.setCellRenderer(cardRenderer);
            list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
            list.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
            list.setOpaque(false);
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

            this.listScroll = new JScrollPane(list, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
            listScroll.setBorder(BorderFactory.createEmptyBorder(2, 4, 4, 4));
            listScroll.getViewport().setOpaque(false);
            listScroll.setVisible(expanded);

            // The shared group header (item 5): title + icon, the parameter-panel
            // collapse caret at the START of the title, and any extra button
            // (e.g. "Get more") at the END (item 12 placement, fixes the
            // ellipsis of item 4).
            this.header = new JIPipeDesktopFormPanel.GroupHeaderPanel(title, icon, 8);

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
                listScroll.setVisible(nowExpanded);
                collapseButton.setToolTipText(nowExpanded ? "Collapse" : "Expand");
            });

            this.sectionPanel = new JPanel();
            sectionPanel.setLayout(new BoxLayout(sectionPanel, BoxLayout.Y_AXIS));
            sectionPanel.setOpaque(false);
            sectionPanel.add(header);
            sectionPanel.add(UIUtils.wrapInIslandPanelIfNeeded(listScroll)); // islands design (item 11)
        }

        JPanel getSectionComponent() {
            return sectionPanel;
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
    // The card row renderer for the section lists
    // ------------------------------------------------------------------

    private class CardRenderer extends JPanel implements ListCellRenderer<JIPipeNodeTemplate> {
        private final JLabel icon = new JLabel();
        private final JLabel name = new JLabel();
        private final JLabel desc = new JLabel();

        CardRenderer() {
            setLayout(new BorderLayout(8, 2));
            setOpaque(true);
            setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIUtils.getControlBorderColor(), 1, true),
                BorderFactory.createEmptyBorder(6, 8, 6, 8)));

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
        public Component getListCellRendererComponent(JList<? extends JIPipeNodeTemplate> list, JIPipeNodeTemplate value,
                                                      int index, boolean isSelected, boolean cellHasFocus) {
            setPreferredSize(new Dimension(CARD_WIDTH, CARD_HEIGHT));
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
            if (isSelected) {
                setBackground(UIManager.getColor("List.selectionBackground"));
                setForeground(UIManager.getColor("List.selectionForeground"));
                name.setForeground(UIManager.getColor("List.selectionForeground"));
                icon.setForeground(UIManager.getColor("List.selectionForeground"));
            } else {
                setBackground(ThemeUtils.getCurrentStyle().getPanelBackground());
                setForeground(ThemeUtils.getCurrentStyle().getTextForeground());
                name.setForeground(ThemeUtils.getCurrentStyle().getTextForeground());
                icon.setForeground(ThemeUtils.getCurrentStyle().getTextForeground());
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
