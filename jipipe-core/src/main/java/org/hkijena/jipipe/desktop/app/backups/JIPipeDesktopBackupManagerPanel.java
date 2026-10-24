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

package org.hkijena.jipipe.desktop.app.backups;

import ij.IJ;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.DefaultJIPipeRunnable;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.backups.*;
import org.hkijena.jipipe.api.backups.store.JIPipeBackupObjectStore;
import org.hkijena.jipipe.api.backups.store.JIPipeBackupSnapshot;
import org.hkijena.jipipe.api.backups.store.JIPipeBackupStoreOperations;
import org.hkijena.jipipe.api.backups.store.JIPipeBackupVerificationReport;
import org.hkijena.jipipe.api.notifications.JIPipeNotification;
import org.hkijena.jipipe.api.notifications.JIPipeNotificationInbox;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.api.run.JIPipeRunnable;
import org.hkijena.jipipe.api.run.JIPipeQueuedRunnableExecutor;
import org.hkijena.jipipe.desktop.JIPipeDesktop;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWindow;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbenchPanel;
import org.hkijena.jipipe.desktop.app.running.JIPipeDesktopRunExecuteUI;
import org.hkijena.jipipe.desktop.app.running.logs.JIPipeDesktopRunnableLogsCollection;
import org.hkijena.jipipe.desktop.app.running.queue.JIPipeDesktopRunnableQueueButton;
import org.hkijena.jipipe.desktop.commons.components.panels.JIPipeDesktopFormPanel;
import org.hkijena.jipipe.desktop.commons.components.search.JIPipeDesktopSearchTextField;
import org.hkijena.jipipe.plugins.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.plugins.settings.application.JIPipeFileChooserApplicationSettings;
import org.hkijena.jipipe.utils.JIPipeDesktopSplitPane;
import org.hkijena.jipipe.utils.PathUtils;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.json.JsonUtils;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

public class JIPipeDesktopBackupManagerPanel extends JIPipeDesktopWorkbenchPanel implements JIPipeRunnable.FinishedEventListener {

    private final JIPipeDesktopSearchTextField searchTextField = new JIPipeDesktopSearchTextField();
    private final JCheckBox limitToCurrentProjectFilter = new JCheckBox("Limit to current project");
    private final JIPipeDesktopFormPanel propertiesPanel = new JIPipeDesktopFormPanel(JIPipeDesktopFormPanel.WITH_SCROLLING);
    private final JTree backupTree = new JTree();
    private List<JIPipeProjectBackupItemCollection> backupCollections = new ArrayList<>();

    public JIPipeDesktopBackupManagerPanel(JIPipeDesktopWorkbench workbench) {
        super(workbench);
        JIPipe.getInstance().getProjectBackup().getQueue().getFinishedEventEmitter().subscribe(this);
        initialize();
        reloadBackups();
        refreshTree();
        refreshSidebar();
    }

    public static JIPipeDesktopBackupManagerPanel openNewWindow(JIPipeDesktopWorkbench workbench) {
        JFrame frame = new JFrame();
        if (workbench.getWindow() instanceof JFrame) {
            frame.setTitle(((JFrame) workbench.getWindow()).getTitle() + " - Backups");
        } else {
            frame.setTitle("JIPipe - Backups");
        }
        frame.setIconImage(UIUtils.getJIPipeIcon128());
        JIPipeDesktopBackupManagerPanel panel = new JIPipeDesktopBackupManagerPanel(workbench);
        frame.setContentPane(panel);
        frame.pack();
        frame.setSize(1024, 768);
        frame.setLocationRelativeTo(workbench.getWindow());
        frame.setVisible(true);
        return panel;
    }

    private void initialize() {
        setLayout(new BorderLayout());

        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        add(toolBar, BorderLayout.NORTH);
        toolBar.addSeparator();
        toolBar.add(searchTextField);
        toolBar.add(limitToCurrentProjectFilter);
        toolBar.addSeparator();
        toolBar.add(new JIPipeDesktopRunnableQueueButton(getDesktopWorkbench(), JIPipe.getInstance().getProjectBackup().getQueue()));
        toolBar.add(UIUtils.createStandardButton("Reload", JIPipe.RESOURCES.getIcon16("actions/reload.png"), this::reloadBackups));

        backupTree.setCellRenderer(new JIPipeDesktopBackupManagerTreeCellRenderer());
        backupTree.addTreeSelectionListener(e -> {
            refreshSidebar();
        });
        backupTree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2) {
                    if (backupTree.getLastSelectedPathComponent() != null) {
                        DefaultMutableTreeNode node = (DefaultMutableTreeNode) backupTree.getLastSelectedPathComponent();
                        if (node.getUserObject() instanceof JIPipeProjectBackupItem) {
                            restoreBackup((JIPipeProjectBackupItem) node.getUserObject());
                        }
                    }
                }
            }
        });
        add(new JIPipeDesktopSplitPane(JIPipeDesktopSplitPane.LEFT_RIGHT, new JScrollPane(backupTree), propertiesPanel, new JIPipeDesktopSplitPane.DynamicSidebarRatio(350, false)));

        limitToCurrentProjectFilter.addActionListener(e -> refreshTree());
        searchTextField.addActionListener(e -> refreshTree());
    }

    private void refreshSidebar() {
        propertiesPanel.clear();
        JIPipeDesktopFormPanel.GroupHeaderPanel generalHeader = propertiesPanel.addGroupHeader("Backups", JIPipe.RESOURCES.getIcon16("actions/filesave.png"));
        generalHeader.addDescriptionRow("Here you can manage the collection of backups automatically created by JIPipe. " +
                "You can enable/disable backups and change the interval in the application settings.");

        propertiesPanel.addWideToForm(UIUtils.createLeftAlignedButton("Open backup directory", JIPipe.RESOURCES.getIcon16("actions/folder-open.png"), this::openBackupFolder));
        JButton cleanupButton = UIUtils.createLeftAlignedButton("Remove old backups", JIPipe.RESOURCES.getIcon16("actions/clear-brush.png"), () -> { });
        JPopupMenu cleanupMenu = UIUtils.addPopupMenuToButton(cleanupButton);
        cleanupMenu.add(UIUtils.createMenuItem("Cleanup backups", "Applies the standard backup cleanup operation", JIPipe.RESOURCES.getIcon16("actions/clear-brush.png"), this::thinBackups));
        cleanupMenu.add(UIUtils.createMenuItem("Remove old backups (days)", "Removes backups older than a given number of days", JIPipe.RESOURCES.getIcon16("actions/clear-brush.png"), this::removeOldBackups));
        propertiesPanel.addWideToForm(cleanupButton);
        propertiesPanel.addWideToForm(UIUtils.createLeftAlignedButton("Remove backups without project file", JIPipe.RESOURCES.getIcon16("actions/delete.png"), this::removeUnnamedBackups));
        propertiesPanel.addWideToForm(UIUtils.createLeftAlignedButton("Remove backups with project file", JIPipe.RESOURCES.getIcon16("actions/delete.png"), this::removeNamedBackups));
        JButton verifyButton = UIUtils.createLeftAlignedButton("Verify all backups", JIPipe.RESOURCES.getIcon16("actions/circle-check.png"), this::verifyAllBackups);
        verifyButton.setToolTipText("Checks all backups for damage. Runs in the background.");
        propertiesPanel.addWideToForm(verifyButton);
        propertiesPanel.addWideToForm(statsFooterLabel());

        if (backupTree.getSelectionCount() > 0) {
            propertiesPanel.addGroupHeader("Selection (" + backupTree.getSelectionCount() + " items)", JIPipe.RESOURCES.getIcon16("actions/edit-select-all.png"));
            propertiesPanel.addWideToForm(UIUtils.createLeftAlignedButton("Delete selection", JIPipe.RESOURCES.getIcon16("actions/delete.png"), this::deleteSelection));
        }

        if (backupTree.getLastSelectedPathComponent() != null) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) backupTree.getLastSelectedPathComponent();
            if (node.getUserObject() instanceof JIPipeProjectBackupItemCollection itemCollection) {
                propertiesPanel.addGroupHeader("Backup collection", JIPipe.RESOURCES.getIcon16("mimetypes/application-jipipe.png"));
                propertiesPanel.addToForm(UIUtils.createReadonlyTextField("" + itemCollection.getBackupItemList().size()), new JLabel("Backup count"));
                propertiesPanel.addToForm(UIUtils.createReadonlyTextField(itemCollection.getSessionId()), new JLabel("Session ID"));
                propertiesPanel.addToForm(UIUtils.createReadonlyTextField(itemCollection.getOriginalProjectPath()), new JLabel("Original project path"));

            } else if (node.getUserObject() instanceof JIPipeProjectBackupItem backupItem) {
                propertiesPanel.addGroupHeader("Backup", JIPipe.RESOURCES.getIcon16("actions/clock.png"));
                propertiesPanel.addToForm(UIUtils.createReadonlyTextField(backupItem.getOriginalProjectPath()), new JLabel("Original project path"));
                propertiesPanel.addToForm(UIUtils.createReadonlyTextField(backupItem.getBackupTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)), new JLabel("Modification time"));
                if (backupItem.isSnapshot()) {
                    String format = backupItem.getProjectFormat();
                    propertiesPanel.addToForm(UIUtils.createReadonlyTextField(
                            "jipx".equals(format) ? "JIPipe project crate" : "Legacy JIPipe project"), new JLabel("Format"));
                    propertiesPanel.addToForm(UIUtils.createReadonlyTextField(StringUtils.formatSize(backupItem.getTotalSizeBytes())), new JLabel("Size"));
                    propertiesPanel.addToForm(UIUtils.createReadonlyTextField(snapshotEntriesLabel(backupItem)), new JLabel("Entries"));
                }
                propertiesPanel.addWideToForm(UIUtils.createLeftAlignedButton("Restore", JIPipe.RESOURCES.getIcon16("actions/fileopen.png"), () -> restoreBackup(backupItem)));
            }
        }

        propertiesPanel.addVerticalGlue();
    }

    /**
     * The entry count of a snapshot item, read from its manifest on demand
     * (the item only caches the size, not the entry list). Returns
     * "unknown" if the manifest cannot be read.
     */
    private String snapshotEntriesLabel(JIPipeProjectBackupItem backupItem) {
        try {
            JIPipeBackupSnapshot snapshot = JsonUtils.readFromFile(backupItem.getSnapshotManifestPath(), JIPipeBackupSnapshot.class);
            return "" + snapshot.getEntries().size();
        } catch (Exception e) {
            return "unknown";
        }
    }

    /**
     * The stats footer label below the action buttons: "Backup data: X
     * (unique data: Y)". X is the sum of the sizes of all loaded backup
     * items; Y is the total size of the unique files held by the
     * content-addressed store (shared between backups is counted once).
     * Computed synchronously — it is a directory walk and a size pass over
     * the store, and the panel is opened on demand.
     */
    private JLabel statsFooterLabel() {
        long total = 0;
        for (JIPipeProjectBackupItemCollection backupCollection : backupCollections) {
            for (JIPipeProjectBackupItem backupItem : backupCollection.getBackupItemList()) {
                total += backupItem.getTotalSizeBytes();
            }
        }
        long uniqueBytes;
        try {
            uniqueBytes = JIPipe.getInstance().getProjectBackup().getStore().totalObjectBytes();
        } catch (Exception e) {
            return new JLabel("Backup data: unknown");
        }
        return new JLabel("Backup data: " + StringUtils.formatSize(total) + " (unique data: " + StringUtils.formatSize(uniqueBytes) + ")");
    }

    private void thinBackups() {
        if(JOptionPane.showConfirmDialog(this, "Do you really want to cleanup the backups?", "Cleanup backups", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            ThinBackupsRun run = new ThinBackupsRun(true);
            JIPipeDesktopRunExecuteUI.runInDialog(getDesktopWorkbench(), this, run, JIPipe.getInstance().getProjectBackup().getQueue(), JIPipeDesktopRunExecuteUI.GlobalLogMode.Everything);
        }
    }

    private void removeOldBackups() {
        JSpinner daySpinner = new JSpinner(new SpinnerNumberModel(60, 1, Short.MAX_VALUE, 1));
        if (JOptionPane.showConfirmDialog(this, UIUtils.boxHorizontal(new JLabel("Remove backups older than "),
                        daySpinner,
                        new JLabel(" days")),
                "Remove old backups",
                JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
            int days = ((Number) daySpinner.getModel().getValue()).intValue();
            if (days > 0) {
                DeleteOldBackupsByAgeRun run = new DeleteOldBackupsByAgeRun(Duration.ofDays(days));
                JIPipeDesktopRunExecuteUI.runInDialog(getDesktopWorkbench(), this, run, JIPipe.getInstance().getProjectBackup().getQueue(), JIPipeDesktopRunExecuteUI.GlobalLogMode.Everything);
            }
        }
    }

    private void restoreBackup(JIPipeProjectBackupItem backupItem) {
        Path saveDirectory = Paths.get("");
        if (!StringUtils.isNullOrEmpty(backupItem.getOriginalProjectPath())) {
            try {
                saveDirectory = Paths.get(backupItem.getOriginalProjectPath()).getParent();
            } catch (Throwable ignored) {

            }
        }

        JIPipeFileChooserApplicationSettings.getInstance().setLastDirectoryBy(JIPipeFileChooserApplicationSettings.LastDirectoryKey.Projects, saveDirectory);
        Path path = JIPipeDesktop.saveFile(this, getDesktopWorkbench(), JIPipeFileChooserApplicationSettings.LastDirectoryKey.Projects, "Restore backup as ...", HTMLText.EMPTY, PathUtils.EXTENSION_FILTER_JIP, PathUtils.EXTENSION_FILTER_JIPX);
        if (path == null) {
            return;
        }
        if (backupItem.isSnapshot()) {
            // The content-addressed snapshot is assembled into the target
            // by the store (verify-on-read); the extension is aligned
            // with the snapshot format so the loader recognizes it
            path = targetForSnapshot(backupItem, path);
            JIPipeBackupSnapshot snapshot;
            try {
                snapshot = JsonUtils.readFromFile(backupItem.getSnapshotManifestPath(), JIPipeBackupSnapshot.class);
            } catch (Exception e) {
                IJ.handleException(e);
                return;
            }
            RestoreSnapshotRun run = new RestoreSnapshotRun(JIPipe.getInstance().getProjectBackup().getStore(), snapshot, path);
            // Modal progress dialog — the restore is explicitly initiated by
            // the user and may take a while for large snapshots. The dialog
            // stays open until the user closes it, so the project is only
            // opened after a successful restore.
            JIPipeDesktopRunExecuteUI.runInDialog(getDesktopWorkbench(), this, run, JIPipe.getInstance().getProjectBackup().getQueue(), JIPipeDesktopRunExecuteUI.GlobalLogMode.Everything);
            if (!run.restored()) {
                // The failure is documented in the dialog log (and the run
                // log); the target file was not written or left untouched
                return;
            }
        } else {
            try {
                Files.copy(backupItem.getProjectPath(), path, StandardCopyOption.REPLACE_EXISTING);
            } catch (Exception e) {
                IJ.handleException(e);
                return;
            }
        }
        JIPipeDesktopProjectWindow projectWindow = JIPipeDesktopProjectWindow.newWindow(getDesktopWorkbench().getContext(), new JIPipeProject(), false, true);
        projectWindow.openProject(path, true);
    }

    /**
     * A queued run that restores one snapshot to the chosen target file
     * (verify-on-read; executed in the modal restore dialog).
     */
    private static class RestoreSnapshotRun extends DefaultJIPipeRunnable {
        private final JIPipeBackupObjectStore store;
        private final JIPipeBackupSnapshot snapshot;
        private final Path targetFile;
        private volatile boolean restored;

        private RestoreSnapshotRun(JIPipeBackupObjectStore store, JIPipeBackupSnapshot snapshot, Path targetFile) {
            this.store = store;
            this.snapshot = snapshot;
            this.targetFile = targetFile;
        }

        /**
         * @return true if the restore finished and the target file was written
         */
        public boolean restored() {
            return restored;
        }

        @Override
        public String getTaskLabel() {
            return "Restore backup";
        }

        @Override
        public void run() {
            try {
                JIPipeBackupStoreOperations.restoreSnapshot(store, snapshot, targetFile, getProgressInfo());
                restored = true;
            } catch (java.io.IOException e) {
                // The run cannot declare the checked exception (JIPipeRunnable
                // extends Runnable); the worker appends the full stack trace
                // to the progress log, and restored() stays false so the
                // project is NOT opened
                throw new RuntimeException("Backup restore failed", e);
            }
        }
    }

    /**
     * Aligns the extension of the user-chosen restore target with the
     * snapshot's project format (a .jipx snapshot restored to a .jip name
     * would not be recognized by the project loader)
     */
    private Path targetForSnapshot(JIPipeProjectBackupItem backupItem, Path chosen) {
        String format = backupItem.getProjectFormat();
        if (format == null) {
            return chosen;
        }
        String name = chosen.getFileName().toString();
        int dot = name.lastIndexOf('.');
        if (dot >= 0 && name.substring(dot + 1).equalsIgnoreCase(format)) {
            return chosen;
        }
        String base = dot >= 0 ? name.substring(0, dot) : name;
        return chosen.resolveSibling(base + "." + format);
    }

    private void deleteSelection() {
        Set<JIPipeProjectBackupItemCollection> collectionSet = new HashSet<>();
        Set<JIPipeProjectBackupItem> itemSet = new HashSet<>();

        TreePath[] selectionPaths = backupTree.getSelectionPaths();
        if (selectionPaths != null) {
            for (TreePath selectionPath : selectionPaths) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) selectionPath.getLastPathComponent();
                if (node.getUserObject() instanceof JIPipeProjectBackupItemCollection) {
                    collectionSet.add((JIPipeProjectBackupItemCollection) node.getUserObject());
                } else if (node.getUserObject() instanceof JIPipeProjectBackupItem) {
                    itemSet.add((JIPipeProjectBackupItem) node.getUserObject());
                }
            }
        }

        if (collectionSet.isEmpty() && itemSet.isEmpty()) {
            return;
        }

        if (JOptionPane.showConfirmDialog(this,
                "Dou you really want to delete " + collectionSet.size() + " collections and " + itemSet.size() + " items?",
                "Delete selection",
                JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            try {
                JIPipeProgressInfo progressInfo = JIPipeProgressInfo.SILENT;
                for (JIPipeProjectBackupItem backupItem : itemSet) {
                    backupItem.delete(progressInfo);
                }
                for (JIPipeProjectBackupItemCollection collection : collectionSet) {
                    for (JIPipeProjectBackupItem backupItem : collection.getBackupItemList()) {
                        backupItem.delete(progressInfo);
                    }
                }
            } catch (Exception e) {
                IJ.handleException(e);
            }
            reloadBackups();
        }
    }

    private void removeNamedBackups() {
        if (JOptionPane.showConfirmDialog(this, "Do you really want to remove all backups of projects that have a storage path?\n" +
                "Please note that the tool will NOT check if the files are still present at the path.", "Remove backups with project file", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            JIPipeDesktopRunExecuteUI.runInDialog(getDesktopWorkbench(), this, new PruneBackupsRun(false, true), JIPipe.getInstance().getProjectBackup().getQueue(), JIPipeDesktopRunExecuteUI.GlobalLogMode.Everything);
        }
    }

    private void removeUnnamedBackups() {
        if (JOptionPane.showConfirmDialog(this, "Do you really want to remove all backups of projects that have NO storage path because they were never saved?\n" +
                "It might be possible that some of the affected projects have been saved after the creation of the backup.", "Remove backups with project file", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            JIPipeDesktopRunExecuteUI.runInDialog(getDesktopWorkbench(), this, new PruneBackupsRun(true, false), JIPipe.getInstance().getProjectBackup().getQueue(), JIPipeDesktopRunExecuteUI.GlobalLogMode.Everything);
        }
    }

    /**
     * Starts a backup verification in the background (NON-MODAL — the user
     * keeps working; the result arrives in the status bar and, on problems,
     * in the notification inbox). The run is enqueued on the backup queue;
     * its log entry is pushed manually and marked read (the #753 pattern —
     * the run-logs collection only auto-subscribes to the global queue, and
     * {@code isLogSilent()} auto-marks the entry as read).
     */
    private void verifyAllBackups() {
        final VerifyBackupsRun run = new VerifyBackupsRun();
        JIPipeQueuedRunnableExecutor queue = JIPipe.getInstance().getProjectBackup().getQueue();

        // Self-unsubscribing listeners (the #753 pattern): the backup queue
        // is long-lived and also used by the periodic backup timer, so
        // subscribeLambdaOnce would be consumed by whichever run finishes
        // first — the listeners must stay until THIS run completes.
        final JIPipeRunnable.FinishedEventListener[] finishHolder = new JIPipeRunnable.FinishedEventListener[1];
        final JIPipeRunnable.InterruptedEventListener[] interruptHolder = new JIPipeRunnable.InterruptedEventListener[1];
        JIPipeRunnable.FinishedEventListener finishListener = event -> {
            if (event.getRun() != run) {
                return;
            }
            queue.getFinishedEventEmitter().unsubscribe(finishHolder[0]);
            queue.getInterruptedEventEmitter().unsubscribe(interruptHolder[0]);
            SwingUtilities.invokeLater(() -> onVerifyFinished(run, true));
        };
        JIPipeRunnable.InterruptedEventListener interruptListener = event -> {
            if (event.getRun() != run) {
                return;
            }
            queue.getFinishedEventEmitter().unsubscribe(finishHolder[0]);
            queue.getInterruptedEventEmitter().unsubscribe(interruptHolder[0]);
            SwingUtilities.invokeLater(() -> onVerifyFinished(run, false));
        };
        finishHolder[0] = finishListener;
        interruptHolder[0] = interruptListener;
        queue.getFinishedEventEmitter().subscribe(finishListener);
        queue.getInterruptedEventEmitter().subscribe(interruptListener);

        getDesktopWorkbench().sendStatusBarText("Checking backups in the background...");
        queue.enqueue(run);
    }

    /**
     * EDT bookkeeping for a finished (or cancelled) verification run:
     * status bar summary + notification on problems + the run-log entry
     * (marked read by {@code VerifyBackupsRun.isLogSilent()}).
     */
    private void onVerifyFinished(VerifyBackupsRun run, boolean success) {
        if (success) {
            JIPipeBackupVerificationReport report = run.getReport();
            if (report.isHealthy()) {
                getDesktopWorkbench().sendStatusBarText("Backups verified: " + report.snapshotsChecked
                        + " snapshots, " + report.objectsChecked + " files (" + StringUtils.formatSize(report.bytesChecked) + "). All good.");
            } else {
                getDesktopWorkbench().sendStatusBarText("Backup verification found " + report.problems.size()
                        + " problem(s) — see the notification inbox");
                JIPipeNotificationInbox.getInstance().push(new JIPipeNotification(
                        "Backup verification found " + report.problems.size() + " problem(s)",
                        String.join("\n", report.problems)));
            }
        } else {
            // Cancellation or an I/O failure — the reason is in the run log
            getDesktopWorkbench().sendStatusBarText("Backup verification could not be completed — see the run log");
        }
        // The backup queue is NOT the global queue the run-logs collection
        // auto-subscribes to, so the entry is pushed manually here (the #753
        // pattern); isLogSilent() auto-marks it as read.
        JIPipeDesktopRunnableLogsCollection.getInstance().pushToLog(run, success);
    }

    private void openBackupFolder() {
        UIUtils.desktopOpenFile(JIPipe.getInstance().getProjectBackup().getCurrentBackupPath());
    }

    private void reloadBackups() {
        JIPipe.getInstance().getProjectBackup().getQueue().enqueue(new CollectBackupsRun());
    }

    private void refreshTree() {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode();

        for (JIPipeProjectBackupItemCollection backupCollection : backupCollections) {

            if (limitToCurrentProjectFilter.isSelected()) {
                if (getDesktopWorkbench() instanceof JIPipeDesktopProjectWorkbench) {
                    Path projectSavePath = ((JIPipeDesktopProjectWorkbench) getDesktopWorkbench()).getProjectWindow().getProjectSavePath();
                    if (projectSavePath == null) {
                        // Nothing found
                        root = new DefaultMutableTreeNode();
                        break;
                    } else {
                        if (!Objects.equals(backupCollection.getOriginalProjectPath(), projectSavePath.toString())) {
                            continue;
                        }
                    }
                }
            }

            // Ensure that we allow child matching
            boolean parentMatches = searchTextField.test(backupCollection.renderName());
            boolean childMatches = false;
            for (JIPipeProjectBackupItem backupItem : backupCollection.getBackupItemList()) {
                if (searchTextField.test(backupItem.getBackupTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))) {
                    childMatches = true;
                    break;
                }
            }

            if (!childMatches && !parentMatches) {
                continue;
            }

            DefaultMutableTreeNode collectionNode = new DefaultMutableTreeNode(backupCollection);
            for (JIPipeProjectBackupItem backupItem : backupCollection.getBackupItemList()) {
                DefaultMutableTreeNode backupItemNode = new DefaultMutableTreeNode(backupItem);

                if (parentMatches) {
                    collectionNode.add(backupItemNode);
                } else {
                    boolean matches = searchTextField.test(backupItem.getBackupTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                    if (matches) {
                        collectionNode.add(backupItemNode);
                    }
                }

            }
            root.add(collectionNode);
        }

        backupTree.setModel(new DefaultTreeModel(root));
        UIUtils.expandAllTree(backupTree);
        refreshSidebar();
    }

    @Override
    public void onRunnableFinished(JIPipeRunnable.FinishedEvent event) {
        if (!isDisplayable()) {
            JIPipe.getInstance().getProjectBackup().getQueue().getFinishedEventEmitter().unsubscribe(this);
            return;
        }
        if (event.getRun() instanceof CollectBackupsRun) {
            this.backupCollections = ((CollectBackupsRun) event.getRun()).getOutput();
            refreshTree();
        } else {
            reloadBackups();
        }
    }
}
