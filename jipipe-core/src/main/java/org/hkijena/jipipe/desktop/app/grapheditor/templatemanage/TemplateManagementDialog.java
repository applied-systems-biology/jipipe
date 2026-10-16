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
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.desktop.JIPipeDesktop;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.app.running.JIPipeDesktopRunExecuteUI;
import org.hkijena.jipipe.desktop.commons.components.layouts.JIPipeDesktopWrapLayout;
import org.hkijena.jipipe.desktop.commons.components.parameters.JIPipeDesktopParameterFormPanel;
import org.hkijena.jipipe.plugins.nodetemplate.NodeTemplatesRefreshedEvent;
import org.hkijena.jipipe.plugins.nodetemplate.NodeTemplatesRefreshedEventListener;
import org.hkijena.jipipe.plugins.nodetemplate.templatedownloader.NodeTemplateDownloaderRun;
import org.hkijena.jipipe.plugins.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.plugins.parameters.library.markup.MarkdownText;
import org.hkijena.jipipe.plugins.settings.application.JIPipeFileChooserApplicationSettings;
import org.hkijena.jipipe.utils.PathUtils;
import org.hkijena.jipipe.utils.ThemeUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * The "Configure templates" manager (round-2 redesign, work-item 1324
 * design v2): a feature-manager style dialog with two views —
 * <ul>
 * <li><b>Manage</b>: collapsible sections "Your templates" (global),
 * "This project", "Downloaded" (a "Get more" button in its header), and
 * "System" (collapsed by default); each section shows its templates as
 * cards in a wrap layout. Scope operations (edit / copy / delete / export /
 * import) act on the current selection.</li>
 * <li><b>Repository</b> ("Get more"): a back button, a search field, a
 * sidebar (Featured / Collections (future) / function categories) and the
 * package list — the #1333 seam, prepared for the future upload feature.</li>
 * </ul>
 * <p>
 * The store logic (copy / delete / edit / export / import and the scope
 * partition) all lives in the Swing-free {@link TemplateManagementController}.
 */
public class TemplateManagementDialog extends JDialog implements NodeTemplatesRefreshedEventListener {

    private final JIPipeDesktopWorkbench workbench;
    private final JIPipeProject project;
    private final TemplateManagementController controller = new TemplateManagementController();

    // --- Views ---
    private final JPanel viewContainer = new JPanel(new CardLayout());
    private final JPanel manageView = new JPanel(new BorderLayout());
    private final JPanel repositoryView = new JPanel(new BorderLayout());

    // --- Manage view state ---
    private final List<TemplateSection> sections = new ArrayList<>();
    /** The current selection (cards are selectable across the sections of the Manage view). */
    private final java.util.List<JIPipeNodeTemplate> selection = new ArrayList<>();
    private final JLabel selectionLabel = new JLabel();
    private final JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));

    public TemplateManagementDialog(JIPipeDesktopWorkbench workbench, TemplateScope initialScope) {
        super(workbench.getWindow());
        this.workbench = workbench;
        this.project = workbench instanceof JIPipeDesktopProjectWorkbench projectWorkbench ? projectWorkbench.getProject() : null;
        setTitle("Configure node templates");
        setModal(true);
        setIconImage(UIUtils.getJIPipeIcon128());
        initialize();
        JIPipe.getNodeTemplates().getNodeTemplatesRefreshedEventEmitter().subscribeWeak(this);
    }

    private void initialize() {
        getContentPane().setLayout(new BorderLayout());
        viewContainer.add(manageView, "manage");
        viewContainer.add(repositoryView, "repository");
        getContentPane().add(viewContainer, BorderLayout.CENTER);
        buildManageView();
        buildRepositoryView();
        showManageView();
        pack();
        setSize(720, 560);
        setLocationRelativeTo(workbench.getWindow());
        UIUtils.addEscapeListener(this);
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

    private void buildManageView() {
        manageView.setBackground(ThemeUtils.getCurrentStyle().getWindowBackground());

        // Sections (the design's order; System last and collapsed by default)
        sections.clear();
        sections.add(new TemplateSection("Your templates", TemplateScope.Global, true));
        if (project != null) {
            sections.add(new TemplateSection("This project", TemplateScope.Project, true));
        }
        sections.add(new TemplateSection("Downloaded", TemplateScope.Global, true) {
            @Override
            protected boolean accepts(JIPipeNodeTemplate template) {
                // The "Downloaded" section: user-global templates that came
                // from a repository (a source URL), distinct from the ones
                // the user authored themselves.
                String source = template.getSource();
                return super.accepts(template) && source != null
                    && !JIPipeNodeTemplate.SOURCE_USER.equals(source)
                    && !JIPipeNodeTemplate.SOURCE_EXTENSION.equals(source);
            }

            @Override
            protected JComponent buildHeaderExtra() {
                JButton getMore = UIUtils.createButton("Get more", JIPipe.RESOURCES.getIcon16("actions/download.png"),
                    TemplateManagementDialog.this::openRepository);
                UIUtils.makeButtonFlat25x25(getMore);
                return getMore;
            }
        });
        sections.add(new TemplateSection("System", TemplateScope.System, false));

        JPanel sectionsPanel = new JPanel();
        sectionsPanel.setLayout(new BoxLayout(sectionsPanel, BoxLayout.Y_AXIS));
        sectionsPanel.setOpaque(false);
        for (TemplateSection section : sections) {
            sectionsPanel.add(section);
            sectionsPanel.add(Box.createVerticalStrut(4));
        }

        JScrollPane scroll = new JScrollPane(sectionsPanel);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        manageView.add(scroll, BorderLayout.CENTER);

        // Bottom bar: the live selection info + the scope operations
        JPanel bottom = new JPanel(new BorderLayout(8, 4));
        bottom.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UIUtils.getControlBorderColor()));
        selectionLabel.setForeground(ThemeUtils.getCurrentStyle().getSecondaryColor());
        bottom.add(selectionLabel, BorderLayout.WEST);
        bottom.add(buttonRow, BorderLayout.CENTER);
        manageView.add(bottom, BorderLayout.SOUTH);

        rebuildButtonRow();
        reloadSections();
    }

    /** One collapsible section of the Manage view. */
    private class TemplateSection extends JPanel {
        private final String title;
        private final TemplateScope scope;
        private final boolean expandedByDefault;
        private final JToggleButton collapseToggle = new JToggleButton();
        private final JLabel countLabel = new JLabel();
        private final JPanel cardPanel = new JPanel(new JIPipeDesktopWrapLayout(FlowLayout.LEFT, 6, 6));
        private boolean expanded;

        TemplateSection(String title, TemplateScope scope, boolean expandedByDefault) {
            this.title = title;
            this.scope = scope;
            this.expandedByDefault = expandedByDefault;
            this.expanded = expandedByDefault;
            setLayout(new BorderLayout());
            setOpaque(false);
            initialize();
        }

        /** Whether this section lists the given template. */
        protected boolean accepts(JIPipeNodeTemplate template) {
            List<JIPipeNodeTemplate> inScope = controller.forScope(project, scope);
            for (JIPipeNodeTemplate t : inScope) {
                if (t == template) {
                    return true;
                }
            }
            return false;
        }

        /** An extra control in the section header, right of the title (e.g. "Get more"). */
        protected JComponent buildHeaderExtra() {
            return null;
        }

        private void initialize() {
            JPanel header = new JPanel(new BorderLayout(4, 0));
            header.setOpaque(false);
            header.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));

            JPanel headerLeft = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
            headerLeft.setOpaque(false);
            JLabel titleLabel = new JLabel(title);
            titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, titleLabel.getFont().getSize() + 1f));
            headerLeft.add(titleLabel);
            countLabel.setForeground(ThemeUtils.getCurrentStyle().getSecondaryColor());
            headerLeft.add(countLabel);
            header.add(headerLeft, BorderLayout.WEST);

            JPanel headerRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
            headerRight.setOpaque(false);
            JComponent extra = buildHeaderExtra();
            if (extra != null) {
                headerRight.add(extra);
            }
            collapseToggle.setIcon(JIPipe.RESOURCES.getIcon16("actions/expand-all.png"));
            collapseToggle.setSelectedIcon(JIPipe.RESOURCES.getIcon16("actions/collapse-all.png"));
            UIUtils.makeButtonFlat25x25(collapseToggle);
            collapseToggle.setToolTipText("Collapse / expand the section");
            collapseToggle.addActionListener(e -> {
                expanded = !expanded;
                cardPanel.setVisible(expanded);
                revalidate();
                repaint();
            });
            collapseToggle.setSelected(!expanded);
            headerRight.add(collapseToggle);
            header.add(headerRight, BorderLayout.EAST);

            add(header, BorderLayout.NORTH);

            cardPanel.setOpaque(false);
            cardPanel.setBorder(BorderFactory.createEmptyBorder(2, 8, 4, 8));
            cardPanel.setVisible(expanded);
            JScrollPane cards = new JScrollPane(cardPanel);
            cards.setBorder(null);
            cards.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
            add(cards, BorderLayout.CENTER);
        }

        /** (Re)loads this section's cards from the store. */
        void reload() {
            List<JIPipeNodeTemplate> templates = controller.forScope(project, scope).stream()
                .filter(this::accepts)
                .toList();
            countLabel.setText(templates.isEmpty() ? "" : "(" + templates.size() + ")");
            cardPanel.removeAll();
            for (JIPipeNodeTemplate template : templates) {
                cardPanel.add(new TemplateCard(template));
            }
            cardPanel.revalidate();
            cardPanel.repaint();
        }
    }

    /** One template card (icon + name; click to select, double-click to edit). */
    private class TemplateCard extends JPanel {
        private final JIPipeNodeTemplate template;
        private boolean selected;

        TemplateCard(JIPipeNodeTemplate template) {
            this.template = template;
            setLayout(new BorderLayout(4, 2));
            setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIUtils.getControlBorderColor(), 1, true),
                BorderFactory.createEmptyBorder(6, 6, 6, 6)));
            setPreferredSize(new Dimension(180, 64));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setOpaque(true);
            setBackground(ThemeUtils.getCurrentStyle().getPanelBackground());

            JLabel icon = new JLabel(template.getIconImage() != null ? template.getIconImage()
                : JIPipe.RESOURCES.getIcon32("actions/tag.png"));
            add(icon, BorderLayout.WEST);

            JPanel text = new JPanel();
            text.setOpaque(false);
            text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
            JLabel name = new JLabel(template.getName());
            name.setToolTipText(template.getName());
            text.add(name);
            JLabel scopeInfo = new JLabel(locationText(template));
            scopeInfo.setFont(scopeInfo.getFont().deriveFont(Font.PLAIN, scopeInfo.getFont().getSize() - 2f));
            scopeInfo.setForeground(ThemeUtils.getCurrentStyle().getSecondaryColor());
            scopeInfo.setToolTipText(locationText(template));
            text.add(scopeInfo);
            add(text, BorderLayout.CENTER);

            addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mousePressed(java.awt.event.MouseEvent e) {
                    setCardSelected(TemplateCard.this, !selected);
                }

                @Override
                public void mouseEntered(java.awt.event.MouseEvent e) {
                    if (!selected) {
                        setBackground(ThemeUtils.getCurrentStyle().getSelectionHighlight());
                    }
                }

                @Override
                public void mouseExited(java.awt.event.MouseEvent e) {
                    if (!selected) {
                        setBackground(ThemeUtils.getCurrentStyle().getPanelBackground());
                    }
                }

                @Override
                public void mouseClicked(java.awt.event.MouseEvent e) {
                    if (e.getClickCount() == 2 && canEdit(template)) {
                        editTemplate(template, scopeOf(template));
                    }
                }
            });
        }
    }

    // ------------------------------------------------------------------
    // Selection + operations (delegating to the controller)
    // ------------------------------------------------------------------

    private void setCardSelected(TemplateCard card, boolean selected) {
        if (selected) {
            selection.add(card.template);
            card.setBackground(UIManager.getColor("List.selectionBackground"));
        } else {
            selection.remove(card.template);
            card.setBackground(ThemeUtils.getCurrentStyle().getPanelBackground());
        }
        card.selected = selected;
        card.repaint();
        rebuildButtonRow();
    }

    private void reloadSections() {
        selection.clear();
        for (TemplateSection section : sections) {
            section.reload();
        }
        rebuildButtonRow();
    }

    private void rebuildButtonRow() {
        buttonRow.removeAll();
        // The primary action depends on where the selection came from
        boolean allSystem = !selection.isEmpty();
        boolean anyEditable = false;
        for (JIPipeNodeTemplate t : selection) {
            if (!controller.forScope(project, TemplateScope.System).contains(t)) {
                allSystem = false;
            }
            if (canEdit(t)) {
                anyEditable = true;
            }
        }
        if (selection.isEmpty()) {
            selectionLabel.setText("Select templates to manage them");
        } else {
            selectionLabel.setText(selection.size() + " selected");
        }
        if (!selection.isEmpty()) {
            if (allSystem) {
                buttonRow.add(makeButton("actions/edit-copy.png", "Copy to Global", this::copySelectedToGlobal));
            } else {
                if (anyEditable && selection.size() == 1) {
                    buttonRow.add(makeButton("actions/document-edit.png", "Edit", this::editSelected));
                }
                buttonRow.add(makeButton("actions/edit-copy.png", "Copy to Global", this::copySelectedToGlobal));
                if (project != null) {
                    buttonRow.add(makeButton("actions/folder.png", "Copy to Project", this::copySelectedToProject));
                }
                buttonRow.add(makeButton("actions/edit-delete.png", "Delete", this::deleteSelected));
                buttonRow.add(makeButton("actions/document-export.png", "Export", this::exportSelected));
            }
        }
        buttonRow.add(makeButton("actions/document-import.png", "Import", this::importTemplates));
        buttonRow.revalidate();
        buttonRow.repaint();
    }

    private boolean canEdit(JIPipeNodeTemplate template) {
        return !template.isFromExtension();
    }

    private TemplateScope scopeOf(JIPipeNodeTemplate template) {
        for (TemplateSection section : sections) {
            if (section.accepts(template)) {
                return section.scope;
            }
        }
        return TemplateScope.Global;
    }

    private String locationText(JIPipeNodeTemplate template) {
        if (template.getDescription() != null && !template.getDescription().getBody().isEmpty()) {
            String body = template.getDescription().getBody().replace('\n', ' ').trim();
            return body.length() > 60 ? body.substring(0, 57) + "..." : body;
        }
        return scopeOf(template).name();
    }

    private void editSelected() {
        if (selection.size() != 1) {
            return;
        }
        JIPipeNodeTemplate template = selection.get(0);
        if (!canEdit(template)) {
            JOptionPane.showMessageDialog(this, "System-provided templates cannot be edited!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        editTemplate(template, scopeOf(template));
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

    private void copySelectedToGlobal() {
        if (selection.isEmpty()) {
            return;
        }
        int n = selection.size();
        if (JOptionPane.showConfirmDialog(this, "Copy " + n + " template(s) into the global storage?",
                "Copy to global storage", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) != JOptionPane.YES_OPTION) {
            return;
        }
        for (JIPipeNodeTemplate t : ImmutableList.copyOf(selection)) {
            controller.copyTo(project, scopeOf(t), TemplateScope.Global, t);
        }
    }

    private void copySelectedToProject() {
        if (project == null || selection.isEmpty()) {
            return;
        }
        int n = selection.size();
        if (JOptionPane.showConfirmDialog(this, "Copy " + n + " template(s) into the project storage?",
                "Copy to project storage", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) != JOptionPane.YES_OPTION) {
            return;
        }
        for (JIPipeNodeTemplate t : ImmutableList.copyOf(selection)) {
            controller.copyTo(project, scopeOf(t), TemplateScope.Project, t);
        }
    }

    private void deleteSelected() {
        if (selection.isEmpty()) {
            return;
        }
        int n = selection.size();
        if (JOptionPane.showConfirmDialog(this, "Delete " + n + " template(s)?",
                "Delete templates", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) != JOptionPane.YES_OPTION) {
            return;
        }
        for (JIPipeNodeTemplate t : ImmutableList.copyOf(selection)) {
            controller.delete(project, scopeOf(t), t);
        }
    }

    private void exportSelected() {
        if (selection.isEmpty()) {
            return;
        }
        Path path = JIPipeDesktop.saveFile(this, workbench, JIPipeFileChooserApplicationSettings.LastDirectoryKey.Projects,
            "Export templates", HTMLText.EMPTY, PathUtils.EXTENSION_FILTER_JSON);
        if (path != null) {
            if (!controller.exportTemplates(project, scopeOf(selection.get(0)), selection, path)) {
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
    // Repository view ("Get more") — the #1333 seam
    // ------------------------------------------------------------------

    private void openRepository() {
        showRepositoryView();
    }

    private void buildRepositoryView() {
        repositoryView.setBackground(ThemeUtils.getCurrentStyle().getWindowBackground());

        // Header: [← Manage] ... [Search ...]
        JPanel header = new JPanel(new BorderLayout(8, 0));
        header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        JButton manageButton = UIUtils.createButton("Manage", JIPipe.RESOURCES.getIcon16("actions/go-previous.png"),
            this::showManageView);
        UIUtils.makeButtonFlat25x25(manageButton);
        header.add(manageButton, BorderLayout.WEST);
        JTextField searchField = new JTextField();
        searchField.setToolTipText("Search templates");
        header.add(searchField, BorderLayout.CENTER);
        repositoryView.add(header, BorderLayout.NORTH);

        // Body: sidebar | content
        JPanel body = new JPanel(new BorderLayout());
        body.setOpaque(false);

        // Sidebar (the design): Featured / Collections (future) / <function categories>
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, UIUtils.getControlBorderColor()));
        sidebar.setBackground(ThemeUtils.getCurrentStyle().getPanelBackground());
        sidebar.add(sidebarItem("Featured", JIPipe.RESOURCES.getIcon16("actions/rating.png")));
        JLabel collections = new JLabel("  Collections");
        collections.setToolTipText("Coming with the node template repository (#1333)");
        collections.setEnabled(false);
        collections.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        sidebar.add(collections);
        sidebar.add(Box.createVerticalStrut(8));
        for (org.hkijena.jipipe.api.nodes.taxonomy.JIPipeNodeTaxonomyFunction top
            : JIPipe.getNodes().getFunctions().topLevel()) {
            sidebar.add(sidebarItem(top.getName(), top.getIcon()));
        }
        body.add(new JScrollPane(sidebar), BorderLayout.WEST);

        // Content: the existing downloader run embedded (repository URL +
        // package list); the future repository swaps this content area.
        JPanel content = new JPanel(new BorderLayout());
        content.setOpaque(false);
        content.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        JLabel placeholder = new JLabel(
            "<html><b>Repository</b><br/>Download templates from the configured repositories.<br/><br/>"
                + "This area is prepared for the node template repository (#1333):<br/>"
                + "search, categories, and sharing/upload come with it.</html>");
        placeholder.setVerticalAlignment(SwingConstants.TOP);
        content.add(placeholder, BorderLayout.NORTH);
        JPanel downloadRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 4));
        downloadRow.setOpaque(false);
        downloadRow.add(UIUtils.createButton("Download templates", JIPipe.RESOURCES.getIcon16("actions/download.png"),
            () -> JIPipeDesktopRunExecuteUI.runInDialog(workbench, this, new NodeTemplateDownloaderRun(workbench))));
        content.add(downloadRow, BorderLayout.SOUTH);
        body.add(content, BorderLayout.CENTER);

        repositoryView.add(body, BorderLayout.CENTER);
    }

    private JComponent sidebarItem(String text, Icon icon) {
        JButton button = new JButton(text, icon);
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        button.setOpaque(false);
        button.setContentAreaFilled(false);
        return button;
    }

    // ------------------------------------------------------------------
    // Shared helpers
    // ------------------------------------------------------------------

    private JButton makeButton(String iconName, String tooltip, Runnable action) {
        JButton button = new JButton(JIPipe.RESOURCES.getIcon16(iconName));
        button.setToolTipText(tooltip);
        UIUtils.makeButtonFlat25x25(button);
        button.addActionListener(e -> action.run());
        return button;
    }

    @Override
    public void onJIPipeNodeTemplatesRefreshed(NodeTemplatesRefreshedEvent event) {
        reloadSections();
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
                return controller.forScope(project, section.scope).stream()
                    .filter(section::accepts)
                    .toList();
            }
        }
        throw new IllegalArgumentException("no section: " + title);
    }
}
