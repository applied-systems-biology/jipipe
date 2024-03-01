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

package org.hkijena.jipipe.ui.backups;

import ij.IJ;
import org.hkijena.jipipe.api.JIPipeProject;
import org.hkijena.jipipe.api.JIPipeRunnable;
import org.hkijena.jipipe.api.backups.CollectBackupsRun;
import org.hkijena.jipipe.api.backups.JIPipeProjectBackupItem;
import org.hkijena.jipipe.api.backups.JIPipeProjectBackupItemCollection;
import org.hkijena.jipipe.api.backups.PruneBackupsRun;
import org.hkijena.jipipe.extensions.settings.BackupSettings;
import org.hkijena.jipipe.extensions.settings.FileChooserSettings;
import org.hkijena.jipipe.ui.JIPipeProjectWindow;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbench;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.JIPipeWorkbenchPanel;
import org.hkijena.jipipe.ui.components.FormPanel;
import org.hkijena.jipipe.ui.components.search.SearchTextField;
import org.hkijena.jipipe.ui.running.JIPipeRunExecuterUI;
import org.hkijena.jipipe.ui.running.JIPipeRunnerQueue;
import org.hkijena.jipipe.ui.running.JIPipeRunnerQueueButton;
import org.hkijena.jipipe.utils.AutoResizeSplitPane;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

public class BackupManagerPanel extends JIPipeWorkbenchPanel implements JIPipeRunnable.FinishedEventListener {

    private static final JIPipeRunnerQueue BACKUP_QUEUE = new JIPipeRunnerQueue("Backups");
    private final SearchTextField searchTextField = new SearchTextField();
    private final JCheckBox limitToCurrentProjectFilter = new JCheckBox("Limit to current project");
    private final FormPanel propertiesPanel = new FormPanel(FormPanel.WITH_SCROLLING);
    private final JTree backupTree = new JTree();
    private List<JIPipeProjectBackupItemCollection> backupCollections = new ArrayList<>();

    public BackupManagerPanel(JIPipeWorkbench workbench) {
        super(workbench);
        BACKUP_QUEUE.getFinishedEventEmitter().subscribe(this);
        initialize();
        reloadBackups();
        refreshTree();
        refreshSidebar();
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
        toolBar.add(new JIPipeRunnerQueueButton(getWorkbench(), BACKUP_QUEUE));
        toolBar.add(UIUtils.createStandardButton("Reload", UIUtils.getIconFromResources("actions/reload.png"), this::reloadBackups));

        backupTree.setCellRenderer(new BackupManagerTreeCellRenderer());
        backupTree.addTreeSelectionListener(e -> {
            refreshSidebar();
        });
        backupTree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if(SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2) {
                    if(backupTree.getLastSelectedPathComponent() != null) {
                        DefaultMutableTreeNode node = (DefaultMutableTreeNode) backupTree.getLastSelectedPathComponent();
                        if(node.getUserObject() instanceof JIPipeProjectBackupItem) {
                            restoreBackup((JIPipeProjectBackupItem) node.getUserObject());
                        }
                    }
                }
            }
        });
        add(new AutoResizeSplitPane(AutoResizeSplitPane.LEFT_RIGHT, new JScrollPane(backupTree), propertiesPanel, new AutoResizeSplitPane.DynamicSidebarRatio(350, false)));

        limitToCurrentProjectFilter.addActionListener(e -> refreshTree());
        searchTextField.addActionListener(e -> refreshTree());
    }

    private void refreshSidebar() {
        propertiesPanel.clear();
        FormPanel.GroupHeaderPanel generalHeader = propertiesPanel.addGroupHeader("Backups", UIUtils.getIconFromResources("actions/save.png"));
        generalHeader.setDescription("Here you can manage the collection of backups automatically created by JIPipe. " +
                "You can enable/disable backups and change the interval in the application settings.");

        propertiesPanel.addWideToForm(UIUtils.createLeftAlignedButton("Open backup directory", UIUtils.getIconFromResources("actions/folder-open.png"), this::openBackupFolder));
        propertiesPanel.addWideToForm(UIUtils.createLeftAlignedButton("Remove backups without project file", UIUtils.getIconFromResources("actions/delete.png"), this::removeUnnamedBackups));
        propertiesPanel.addWideToForm(UIUtils.createLeftAlignedButton("Remove backups with project file", UIUtils.getIconFromResources("actions/delete.png"), this::removeNamedBackups));

        if(backupTree.getSelectionCount() > 0) {
            propertiesPanel.addGroupHeader("Selection (" + backupTree.getSelectionCount() + " items)", UIUtils.getIconFromResources("actions/edit-select-all.png"));
            propertiesPanel.addWideToForm(UIUtils.createLeftAlignedButton("Delete selection", UIUtils.getIconFromResources("actions/delete.png"), this::deleteSelection));
        }

        if(backupTree.getLastSelectedPathComponent() != null) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) backupTree.getLastSelectedPathComponent();
            if(node.getUserObject() instanceof JIPipeProjectBackupItemCollection) {
                propertiesPanel.addGroupHeader("Backup collection", UIUtils.getIconFromResources("mimetypes/application-jipipe.png"));
                JIPipeProjectBackupItemCollection itemCollection = (JIPipeProjectBackupItemCollection) node.getUserObject();
                propertiesPanel.addToForm(UIUtils.makeReadonlyTextField("" + itemCollection.getBackupItemList().size()), new JLabel("Backup count"));
                propertiesPanel.addToForm(UIUtils.makeReadonlyTextField(itemCollection.getSessionId()), new JLabel("Session ID"));
                propertiesPanel.addToForm(UIUtils.makeReadonlyTextField(itemCollection.getOriginalProjectPath()), new JLabel("Original project path"));

            }
            else if(node.getUserObject() instanceof JIPipeProjectBackupItem) {
                propertiesPanel.addGroupHeader("Backup", UIUtils.getIconFromResources("actions/clock.png"));
                JIPipeProjectBackupItem backupItem = (JIPipeProjectBackupItem) node.getUserObject();
                propertiesPanel.addToForm(UIUtils.makeReadonlyTextField(backupItem.getOriginalProjectPath()), new JLabel("Original project path"));
                propertiesPanel.addToForm(UIUtils.makeReadonlyTextField(backupItem.getBackupTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)), new JLabel("Modification time"));
                propertiesPanel.addWideToForm(UIUtils.createLeftAlignedButton("Restore", UIUtils.getIconFromResources("actions/fileopen.png"), () -> restoreBackup(backupItem)));
            }
        }

        propertiesPanel.addVerticalGlue();
    }

    private void restoreBackup(JIPipeProjectBackupItem backupItem) {
        Path saveDirectory = Paths.get("");
        if(!StringUtils.isNullOrEmpty(backupItem.getOriginalProjectPath())) {
            try {
                saveDirectory = Paths.get(backupItem.getOriginalProjectPath()).getParent();
            }
            catch (Throwable ignored) {

            }
        }

        FileChooserSettings.getInstance().setLastDirectoryBy(FileChooserSettings.LastDirectoryKey.Projects, saveDirectory);
        Path path = FileChooserSettings.saveFile(this, FileChooserSettings.LastDirectoryKey.Projects, "Restore backup as ...", UIUtils.EXTENSION_FILTER_JIP);
        if(path != null) {
            try {
                Files.copy(backupItem.getProjectPath(), path, StandardCopyOption.REPLACE_EXISTING);
                JIPipeProjectWindow projectWindow = JIPipeProjectWindow.newWindow(getWorkbench().getContext(), new JIPipeProject(), false, true);
                projectWindow.openProject(path, true);
            }
            catch (Exception e ){
                IJ.handleException(e);
            }
        }
    }

    private void deleteSelection() {
        Set<JIPipeProjectBackupItemCollection> collectionSet = new HashSet<>();
        Set<JIPipeProjectBackupItem> itemSet = new HashSet<>();

        TreePath[] selectionPaths = backupTree.getSelectionPaths();
        if(selectionPaths != null) {
            for (TreePath selectionPath : selectionPaths) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) selectionPath.getLastPathComponent();
                if(node.getUserObject() instanceof JIPipeProjectBackupItemCollection) {
                    collectionSet.add((JIPipeProjectBackupItemCollection) node.getUserObject());
                }
                else if(node.getUserObject() instanceof JIPipeProjectBackupItem) {
                    itemSet.add((JIPipeProjectBackupItem) node.getUserObject());
                }
            }
        }

        if(collectionSet.isEmpty() && itemSet.isEmpty()) {
            return;
        }

        if(JOptionPane.showConfirmDialog(this,
                "Dou you really want to delete " + collectionSet.size() + " collections and " + itemSet.size() + " items?",
                "Delete selection",
                JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            try {
                for (JIPipeProjectBackupItem backupItem : itemSet) {
                    if(Files.isRegularFile(backupItem.getProjectPath())) {
                        Files.delete(backupItem.getProjectPath());
                    }
                }
                for (JIPipeProjectBackupItemCollection collection : collectionSet) {
                    for (JIPipeProjectBackupItem backupItem : collection.getBackupItemList()) {
                        if(Files.isRegularFile(backupItem.getProjectPath())) {
                            Files.delete(backupItem.getProjectPath());
                        }
                    }
                }
            }
            catch (Exception e) {
                IJ.handleException(e);
            }
            reloadBackups();
        }
    }

    private void removeNamedBackups() {
        if(JOptionPane.showConfirmDialog(this, "Do you really want to remove all backups of projects that have a storage path?\n" +
                "Please note that the tool will NOT check if the files are still present at the path.", "Remove backups with project file", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            JIPipeRunExecuterUI.runInDialog(getWorkbench(), this, new PruneBackupsRun(false, true), BACKUP_QUEUE);
        }
    }

    private void removeUnnamedBackups() {
        if(JOptionPane.showConfirmDialog(this, "Do you really want to remove all backups of projects that have NO storage path because they were never saved?\n" +
                "It might be possible that some of the affected projects have been saved after the creation of the backup.", "Remove backups with project file", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            JIPipeRunExecuterUI.runInDialog(getWorkbench(), this, new PruneBackupsRun(true, false), BACKUP_QUEUE);
        }
    }

    private void openBackupFolder() {
        try {
            Desktop.getDesktop().open(BackupSettings.getInstance().getCurrentBackupPath().toFile());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void reloadBackups() {
        BACKUP_QUEUE.enqueue(new CollectBackupsRun());
    }

    private void refreshTree() {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode();

        for (JIPipeProjectBackupItemCollection backupCollection : backupCollections) {

            if(limitToCurrentProjectFilter.isSelected()) {
                if(getWorkbench() instanceof JIPipeProjectWorkbench) {
                    Path projectSavePath = ((JIPipeProjectWorkbench) getWorkbench()).getProjectWindow().getProjectSavePath();
                    if(projectSavePath == null) {
                        // Nothing found
                        root = new DefaultMutableTreeNode();
                        break;
                    }
                    else {
                        if(!Objects.equals(backupCollection.getOriginalProjectPath(), projectSavePath.toString())) {
                            continue;
                        }
                    }
                }
            }

            // Ensure that we allow child matching
            boolean parentMatches = searchTextField.test(backupCollection.renderName());
            boolean childMatches = false;
            for (JIPipeProjectBackupItem backupItem : backupCollection.getBackupItemList()) {
                if(searchTextField.test(backupItem.getBackupTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))) {
                    childMatches = true;
                    break;
                }
            }

            if(!childMatches && !parentMatches) {
                continue;
            }

            DefaultMutableTreeNode collectionNode = new DefaultMutableTreeNode(backupCollection);
            for (JIPipeProjectBackupItem backupItem : backupCollection.getBackupItemList()) {
                DefaultMutableTreeNode backupItemNode = new DefaultMutableTreeNode(backupItem);

                if(parentMatches) {
                    collectionNode.add(backupItemNode);
                }
                else {
                    boolean matches = searchTextField.test(backupItem.getBackupTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                    if(matches) {
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

    public static BackupManagerPanel openNewWindow(JIPipeWorkbench workbench) {
        JFrame frame = new JFrame();
        if(workbench.getWindow() instanceof JFrame) {
            frame.setTitle(((JFrame) workbench.getWindow()).getTitle() + " - Backups");
        }
        else {
            frame.setTitle("JIPipe - Backups");
        }
        frame.setIconImage(UIUtils.getJIPipeIcon128());
        BackupManagerPanel panel = new BackupManagerPanel(workbench);
        frame.setContentPane(panel);
        frame.pack();
        frame.setSize(1024,768);
        frame.setLocationRelativeTo(workbench.getWindow());
        frame.setVisible(true);
        return panel;
    }

    @Override
    public void onRunnableFinished(JIPipeRunnable.FinishedEvent event) {
        if(!isDisplayable()) {
            BACKUP_QUEUE.getFinishedEventEmitter().unsubscribe(this);
            return;
        }
        if(event.getRun() instanceof CollectBackupsRun) {
            this.backupCollections = ((CollectBackupsRun) event.getRun()).getOutput();
            refreshTree();
        }
        else {
            reloadBackups();
        }
    }
}
