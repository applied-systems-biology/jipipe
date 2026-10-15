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
import org.hkijena.jipipe.desktop.commons.components.parameters.JIPipeDesktopParameterFormPanel;
import org.hkijena.jipipe.plugins.nodetemplate.JIPipeNodeTemplateSimpleListCellRenderer;
import org.hkijena.jipipe.plugins.nodetemplate.NodeTemplatesRefreshedEvent;
import org.hkijena.jipipe.plugins.nodetemplate.NodeTemplatesRefreshedEventListener;
import org.hkijena.jipipe.plugins.nodetemplate.templatedownloader.NodeTemplateDownloaderRun;
import org.hkijena.jipipe.plugins.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.plugins.parameters.library.markup.MarkdownText;
import org.hkijena.jipipe.plugins.settings.application.JIPipeFileChooserApplicationSettings;
import org.hkijena.jipipe.utils.PathUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * The "Configure templates" modal (task 9, spec §8.3). A thin Swing wrapper
 * over the Swing-free {@link TemplateManagementController}: each scope tab is
 * a {@link JList} of that scope's templates plus a button row of the
 * operations that scope allows. The store logic (copy / delete / edit /
 * export / import and the R9d scope partition) all lives in the controller, so
 * this class only renders it and handles the file choosers, confirmations, and
 * the editor dialog.
 * <p>
 * Tabs:
 * <ul>
 * <li><b>System</b> — read-only list; "Copy to Global" (the escape hatch).</li>
 * <li><b>Global</b> — edit, copy to project, delete, export, import.</li>
 * <li><b>Project</b> — same, scoped to the current project's metadata (only
 *     present when the workbench is a project workbench).</li>
 * <li><b>Download</b> — the #1333 seam: a button that opens the existing
 *     {@link NodeTemplateDownloaderRun} (no repository logic built here).</li>
 * </ul>
 */
public class TemplateManagementDialog extends JDialog implements NodeTemplatesRefreshedEventListener {

    private final JIPipeDesktopWorkbench workbench;
    private final JIPipeProject project;
    private final TemplateManagementController controller = new TemplateManagementController();
    private final JTabbedPane tabs = new JTabbedPane();
    private final Map<TemplateScope, JList<JIPipeNodeTemplate>> scopeLists = new EnumMap<>(TemplateScope.class);

    public TemplateManagementDialog(JIPipeDesktopWorkbench workbench, TemplateScope initialScope) {
        super(workbench.getWindow());
        this.workbench = workbench;
        this.project = workbench instanceof JIPipeDesktopProjectWorkbench projectWorkbench ? projectWorkbench.getProject() : null;
        setTitle("Configure node templates");
        setModal(true);
        setIconImage(UIUtils.getJIPipeIcon128());
        initialize(initialScope);
        JIPipe.getNodeTemplates().getNodeTemplatesRefreshedEventEmitter().subscribeWeak(this);
    }

    private void initialize(TemplateScope initialScope) {
        getContentPane().setLayout(new BorderLayout());
        tabs.addTab("System", JIPipe.RESOURCES.getIcon24("actions/configure.png"), buildScopeTab(TemplateScope.System));
        tabs.addTab("Global", JIPipe.RESOURCES.getIcon24("actions/folder.png"), buildScopeTab(TemplateScope.Global));
        if (project != null) {
            tabs.addTab("Project", JIPipe.RESOURCES.getIcon24("actions/folder.png"), buildScopeTab(TemplateScope.Project));
        }
        tabs.addTab("Download", JIPipe.RESOURCES.getIcon24("actions/download.png"), buildDownloadTab());

        // Open directly on the entry's scope tab (the context-menu "Configure
        // templates" item passes the entry's scope). Fall back to Global.
        int start = tabs.indexOfTab(initialScope == null ? "Global" : scopeName(initialScope));
        if (start >= 0) {
            tabs.setSelectedIndex(start);
        }

        getContentPane().add(tabs, BorderLayout.CENTER);
        pack();
        setSize(560, 640);
        setLocationRelativeTo(workbench.getWindow());
        UIUtils.addEscapeListener(this);
    }

    private static String scopeName(TemplateScope scope) {
        return switch (scope) {
            case System -> "System";
            case Global -> "Global";
            case Project -> "Project";
        };
    }

    // ------------------------------------------------------------------
    // Tab building
    // ------------------------------------------------------------------

    /**
     * Builds a scope tab: a search-less {@link JList} of that scope's
     * templates (reloaded on every template refresh) plus a per-scope button
     * row. Factored out of {@link #initialize} so the dialog stays a thin
     * wrapper.
     */
    private JComponent buildScopeTab(TemplateScope scope) {
        JPanel panel = new JPanel(new BorderLayout(4, 4));
        panel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

        JList<JIPipeNodeTemplate> list = new JList<>(new DefaultListModel<>());
        list.setCellRenderer(new JIPipeNodeTemplateSimpleListCellRenderer());
        list.setOpaque(false);
        list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        JScrollPane scrollPane = new JScrollPane(list);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scopeLists.put(scope, list);
        panel.add(scrollPane, BorderLayout.CENTER);

        if (scope == TemplateScope.System) {
            JLabel readOnly = new JLabel(JIPipe.RESOURCES.getIcon16("emblems/emblem-important-blue.png"));
            JPanel notice = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
            notice.add(readOnly);
            notice.add(new JLabel("System templates are read-only. Use “Copy to Global” to make an editable copy."));
            panel.add(notice, BorderLayout.NORTH);
        }
        panel.add(buildButtonRow(scope), BorderLayout.SOUTH);

        reloadList(scope);
        return panel;
    }

    /** The per-scope button row (spec §8.3). */
    private JComponent buildButtonRow(TemplateScope scope) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        switch (scope) {
            case System -> row.add(makeButton("actions/edit-copy.png", "Copy to Global", this::copyToGlobal));
            case Global -> {
                row.add(makeButton("actions/document-edit.png", "Edit template", this::editSelected));
                if (project != null) {
                    row.add(makeButton("actions/edit-copy.png", "Copy to Project", this::copyToProject));
                }
                row.add(makeButton("actions/edit-delete.png", "Delete", this::deleteSelected));
                row.add(makeButton("actions/document-export.png", "Export", this::exportTemplates));
                row.add(makeButton("actions/document-import.png", "Import", this::importTemplates));
            }
            case Project -> {
                row.add(makeButton("actions/document-edit.png", "Edit template", this::editSelected));
                row.add(makeButton("actions/edit-copy.png", "Copy to Global", this::copyToGlobal));
                row.add(makeButton("actions/edit-delete.png", "Delete", this::deleteSelected));
                row.add(makeButton("actions/document-export.png", "Export", this::exportTemplates));
                row.add(makeButton("actions/document-import.png", "Import", this::importTemplates));
            }
        }
        return row;
    }

    /**
     * The Download tab: the #1333 seam. A single button that opens the
     * existing {@link NodeTemplateDownloaderRun} (which surfaces the
     * {@code NodeTemplateDownloaderDialog}). No repository logic is built here
     * — the future repository swaps this tab's source.
     */
    private JComponent buildDownloadTab() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        panel.add(new JLabel(JIPipe.RESOURCES.getIcon16("actions/download.png")), BorderLayout.NORTH);
        JButton button = makeButton("actions/download.png", "Download templates", () ->
            JIPipeDesktopRunExecuteUI.runInDialog(workbench, this, new NodeTemplateDownloaderRun(workbench)));
        JPanel center = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 8));
        center.add(button);
        panel.add(center, BorderLayout.CENTER);
        return panel;
    }

    private JButton makeButton(String iconName, String tooltip, Runnable action) {
        JButton button = new JButton(JIPipe.RESOURCES.getIcon16(iconName));
        button.setToolTipText(tooltip);
        UIUtils.makeButtonFlat25x25(button);
        button.addActionListener(e -> action.run());
        return button;
    }

    // ------------------------------------------------------------------
    // List (re)loading
    // ------------------------------------------------------------------

    private void reloadList(TemplateScope scope) {
        JList<JIPipeNodeTemplate> list = scopeLists.get(scope);
        if (list == null) {
            return;
        }
        DefaultListModel<JIPipeNodeTemplate> model = new DefaultListModel<>();
        for (JIPipeNodeTemplate template : controller.forScope(project, scope)) {
            model.addElement(template);
        }
        list.setModel(model);
    }

    @Override
    public void onJIPipeNodeTemplatesRefreshed(NodeTemplatesRefreshedEvent event) {
        for (TemplateScope scope : scopeLists.keySet()) {
            reloadList(scope);
        }
    }

    // ------------------------------------------------------------------
    // Operations (delegate to the controller)
    // ------------------------------------------------------------------

    private JList<JIPipeNodeTemplate> currentList() {
        for (Map.Entry<TemplateScope, JList<JIPipeNodeTemplate>> e : scopeLists.entrySet()) {
            // The list whose model is currently displayed is the active tab.
            if (isListInSelectedTab(e.getValue())) {
                return e.getValue();
            }
        }
        return null;
    }

    private boolean isListInSelectedTab(JList<JIPipeNodeTemplate> list) {
        Component selected = tabs.getSelectedComponent();
        if (selected instanceof JPanel panel) {
            for (Component c : panel.getComponents()) {
                if (c instanceof JScrollPane sp) {
                    return sp.getViewport().getView() == list;
                }
            }
        }
        return false;
    }

    private void requireSelection(JList<JIPipeNodeTemplate> list) {
        if (list.getSelectedValuesList().isEmpty()) {
            JOptionPane.showMessageDialog(this, "You did not select any templates!", "No selection", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void copyToGlobal() {
        JList<JIPipeNodeTemplate> list = currentList();
        if (list == null) {
            return;
        }
        requireSelection(list);
        TemplateScope source = scopeOfList(list);
        if (source == TemplateScope.Global) {
            return; // already global
        }
        int n = list.getSelectedValuesList().size();
        if (JOptionPane.showConfirmDialog(this, "Copy " + n + " template(s) into the global storage?",
                "Copy to global storage", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) != JOptionPane.YES_OPTION) {
            return;
        }
        for (JIPipeNodeTemplate t : ImmutableList.copyOf(list.getSelectedValuesList())) {
            controller.copyTo(project, source, TemplateScope.Global, t);
        }
    }

    private void copyToProject() {
        if (project == null) {
            return;
        }
        JList<JIPipeNodeTemplate> list = currentList();
        if (list == null) {
            return;
        }
        requireSelection(list);
        int n = list.getSelectedValuesList().size();
        if (JOptionPane.showConfirmDialog(this, "Copy " + n + " template(s) into the project storage?",
                "Copy to project", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) != JOptionPane.YES_OPTION) {
            return;
        }
        for (JIPipeNodeTemplate t : ImmutableList.copyOf(list.getSelectedValuesList())) {
            controller.copyTo(project, TemplateScope.Global, TemplateScope.Project, t);
        }
    }

    private void deleteSelected() {
        JList<JIPipeNodeTemplate> list = currentList();
        if (list == null) {
            return;
        }
        requireSelection(list);
        TemplateScope source = scopeOfList(list);
        int n = list.getSelectedValuesList().size();
        if (JOptionPane.showConfirmDialog(this, "Delete " + n + " template(s)?",
                "Delete templates", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) != JOptionPane.YES_OPTION) {
            return;
        }
        for (JIPipeNodeTemplate t : ImmutableList.copyOf(list.getSelectedValuesList())) {
            controller.delete(project, source, t);
        }
    }

    private void editSelected() {
        JList<JIPipeNodeTemplate> list = currentList();
        if (list == null) {
            return;
        }
        if (list.getSelectedValue() == null) {
            JOptionPane.showMessageDialog(this, "You did not select any templates!", "No selection", JOptionPane.ERROR_MESSAGE);
            return;
        }
        JIPipeNodeTemplate template = list.getSelectedValue();
        if (template.isFromExtension()) {
            JOptionPane.showMessageDialog(this, "System-provided templates cannot be edited!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        // Edit on a COPY (exactly as the old template box's edit action did);
        // because taxonomyFunction is a registered @JIPipeParameter, the form
        // shows the taxonomy-function picker automatically (spec §8.2).
        JIPipeNodeTemplate copy = new JIPipeNodeTemplate(template);
        if (JIPipeDesktopParameterFormPanel.showDialog(workbench, copy,
                new MarkdownText("# Node templates\n\nUse this user interface to modify node templates."),
                "Edit template",
                JIPipeDesktopParameterFormPanel.WITH_SCROLLING
                    | JIPipeDesktopParameterFormPanel.WITH_SEARCH_BAR
                    | JIPipeDesktopParameterFormPanel.WITH_DOCUMENTATION)) {
            controller.edit(project, scopeOfList(list), template, copy);
        }
    }

    private void exportTemplates() {
        JList<JIPipeNodeTemplate> list = currentList();
        if (list == null) {
            return;
        }
        requireSelection(list);
        Path path = JIPipeDesktop.saveFile(this, workbench, JIPipeFileChooserApplicationSettings.LastDirectoryKey.Projects,
            "Export templates", HTMLText.EMPTY, PathUtils.EXTENSION_FILTER_JSON);
        if (path != null) {
            if (!controller.exportTemplates(project, scopeOfList(list), list.getSelectedValuesList(), path)) {
                UIUtils.showErrorDialog(workbench, this, new IOException("Could not write template file " + path));
            }
        }
    }

    private void importTemplates() {
        JList<JIPipeNodeTemplate> list = currentList();
        if (list == null) {
            return;
        }
        TemplateScope target = scopeOfList(list);
        if (target == TemplateScope.System) {
            return; // System is read-only
        }
        Path path = JIPipeDesktop.openFile(this, workbench, JIPipeFileChooserApplicationSettings.LastDirectoryKey.Projects,
            "Import templates", HTMLText.EMPTY, PathUtils.EXTENSION_FILTER_JSON);
        if (path != null) {
            List<JIPipeNodeTemplate> imported = controller.importTemplates(project, target, path);
            if (imported.isEmpty()) {
                JOptionPane.showMessageDialog(this, "No templates were imported from " + path + ".", "Import", JOptionPane.INFORMATION_MESSAGE);
            }
        }
    }

    /** The scope a given list belongs to (the reverse of {@link #scopeLists}). */
    private TemplateScope scopeOfList(JList<JIPipeNodeTemplate> list) {
        for (Map.Entry<TemplateScope, JList<JIPipeNodeTemplate>> e : scopeLists.entrySet()) {
            if (e.getValue() == list) {
                return e.getKey();
            }
        }
        return null;
    }
}
