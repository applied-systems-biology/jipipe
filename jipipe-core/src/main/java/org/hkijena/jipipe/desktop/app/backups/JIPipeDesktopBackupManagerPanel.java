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
import org.hkijena.jipipe.api.backups.*;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.api.run.JIPipeRunnable;
import org.hkijena.jipipe.api.run.JIPipeRunnableQueue;
import org.hkijena.jipipe.desktop.JIPipeDesktop;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWindow;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbenchPanel;
import org.hkijena.jipipe.desktop.app.running.JIPipeDesktopRunExecuteUI;
import org.hkijena.jipipe.desktop.app.running.queue.JIPipeDesktopRunnableQueueButton;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopFormPanel;
import org.hkijena.jipipe.desktop.commons.components.search.JIPipeDesktopSearchTextField;
import org.hkijena.jipipe.plugins.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.plugins.settings.JIPipeBackupApplicationSettings;
import org.hkijena.jipipe.plugins.settings.JIPipeFileChooserApplicationSettings;
import org.hkijena.jipipe.utils.JIPipeDesktopSplitPane;
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
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

public class JIPipeDesktopBackupManagerPanel extends JIPipeDesktopWorkbenchPanel implements JIPipeRunnable.FinishedEventListener {

    private static final JIPipeRunnableQueue BACKUP_QUEUE = new JIPipeRunnableQueue("Backups");
    private final JIPipeDesktopSearchTextField searchTextField = new JIPipeDesktopSearchTextField();
    private final JCheckBox limitToCurrentProjectFilter = new JCheckBox("Limit to current project");
    private final JIPipeDesktopFormPanel propertiesPanel = new JIPipeDesktopFormPanel(JIPipeDesktopFormPanel.WITH_SCROLLING);
    private final JTree backupTree = new JTree();
    private List<JIPipeProjectBackupItemCollection> backupCollections = new ArrayList<>();

    public JIPipeDesktopBackupManagerPanel(JIPipeDesktopWorkbench workbench) {
        super(workbench);
        BACKUP_QUEUE.getFinishedEventEmitter().subscribe(this);
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
        toolBar.add(new JIPipeDesktopRunnableQueueButton(getDesktopWorkbench(), BACKUP_QUEUE));
        toolBar.add(UIUtils.createStandardButton("Reload", UIUtils.getIconFromResources("actions/reload.png"), this::reloadBackups));

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
        JIPipeDesktopFormPanel.GroupHeaderPanel generalHeader = propertiesPanel.addGroupHeader("Backups", UIUtils.getIconFromResources("actions/filesave.png"));
        generalHeader.addDescriptionRow("Here you can manage the collection of backups automatically created by JIPipe. " +
                "You can enable/disable backups and change the interval in the application settings.");

        propertiesPanel.addWideToForm(UIUtils.createLeftAlignedButton("Open backup directory", UIUtils.getIconFromResources("actions/folder-open.png"), this::openBackupFolder));
        propertiesPanel.addWideToForm(UIUtils.createLeftAlignedButton("Remove old backups", UIUtils.getIconFromResources("actions/clear-brush.png"), this::removeOldBackups));
        propertiesPanel.addWideToForm(UIUtils.createLeftAlignedButton("Remove backups without project file", UIUtils.getIconFromResources("actions/delete.png"), this::removeUnnamedBackups));
        propertiesPanel.addWideToForm(UIUtils.createLeftAlignedButton("Remove backups with project file", UIUtils.getIconFromResources("actions/delete.png"), this::removeNamedBackups));

        if (backupTree.getSelectionCount() > 0) {
            propertiesPanel.addGroupHeader("Selection (" + backupTree.getSelectionCount() + " items)", UIUtils.getIconFromResources("actions/edit-select-all.png"));
            propertiesPanel.addWideToForm(UIUtils.createLeftAlignedButton("Delete selection", UIUtils.getIconFromResources("actions/delete.png"), this::deleteSelection));
        }

        if (backupTree.getLastSelectedPathComponent() != null) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) backupTree.getLastSelectedPathComponent();
            if (node.getUserObject() instanceof JIPipeProjectBackupItemCollection) {
                propertiesPanel.addGroupHeader("Backup collection", UIUtils.getIconFromResources("mimetypes/application-jipipe.png"));
                JIPipeProjectBackupItemCollection itemCollection = (JIPipeProjectBackupItemCollection) node.getUserObject();
                propertiesPanel.addToForm(UIUtils.createReadonlyTextField("" + itemCollection.getBackupItemList().size()), new JLabel("Backup count"));
                propertiesPanel.addToForm(UIUtils.createReadonlyTextField(itemCollection.getSessionId()), new JLabel("Session ID"));
                propertiesPanel.addToForm(UIUtils.createReadonlyTextField(itemCollection.getOriginalProjectPath()), new JLabel("Original project path"));

            } else if (node.getUserObject() instanceof JIPipeProjectBackupItem) {
                propertiesPanel.addGroupHeader("Backup", UIUtils.getIconFromResources("actions/clock.png"));
                JIPipeProjectBackupItem backupItem = (JIPipeProjectBackupItem) node.getUserObject();
                propertiesPanel.addToForm(UIUtils.createReadonlyTextField(backupItem.getOriginalProjectPath()), new JLabel("Original project path"));
                propertiesPanel.addToForm(UIUtils.createReadonlyTextField(backupItem.getBackupTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)), new JLabel("Modification time"));
                propertiesPanel.addWideToForm(UIUtils.createLeftAlignedButton("Restore", UIUtils.getIconFromResources("actions/fileopen.png"), () -> restoreBackup(backupItem)));
            }
        }

        propertiesPanel.addVerticalGlue();
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
                DeleteOldBackupsRun run = new DeleteOldBackupsRun(Duration.ofDays(days));
                JIPipeDesktopRunExecuteUI.runInDialog(getDesktopWorkbench(), this, run, BACKUP_QUEUE);
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
        Path path = JIPipeDesktop.saveFile(this, getDesktopWorkbench(), JIPipeFileChooserApplicationSettings.LastDirectoryKey.Projects, "Restore backup as ...", HTMLText.EMPTY, UIUtils.EXTENSION_FILTER_JIP);
        if (path != null) {
            try {
                Files.copy(backupItem.getProjectPath(), path, StandardCopyOption.REPLACE_EXISTING);
                JIPipeDesktopProjectWindow projectWindow = JIPipeDesktopProjectWindow.newWindow(getDesktopWorkbench().getContext(), new JIPipeProject(), false, true);
                projectWindow.openProject(path, true);
            } catch (Exception e) {
                IJ.handleException(e);
            }
        }
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
                for (JIPipeProjectBackupItem backupItem : itemSet) {
                    if (Files.isRegularFile(backupItem.getProjectPath())) {
                        Files.delete(backupItem.getProjectPath());
                    }
                }
                for (JIPipeProjectBackupItemCollection collection : collectionSet) {
                    for (JIPipeProjectBackupItem backupItem : collection.getBackupItemList()) {
                        if (Files.isRegularFile(backupItem.getProjectPath())) {
                            Files.delete(backupItem.getProjectPath());
                        }
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
            JIPipeDesktopRunExecuteUI.runInDialog(getDesktopWorkbench(), this, new PruneBackupsRun(false, true), BACKUP_QUEUE);
        }
    }

    private void removeUnnamedBackups() {
        if (JOptionPane.showConfirmDialog(this, "Do you really want to remove all backups of projects that have NO storage path because they were never saved?\n" +
                "It might be possible that some of the affected projects have been saved after the creation of the backup.", "Remove backups with project file", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            JIPipeDesktopRunExecuteUI.runInDialog(getDesktopWorkbench(), this, new PruneBackupsRun(true, false), BACKUP_QUEUE);
        }
    }

    private void openBackupFolder() {
        try {
            Desktop.getDesktop().open(JIPipeBackupApplicationSettings.getInstance().getCurrentBackupPath().toFile());
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
            BACKUP_QUEUE.getFinishedEventEmitter().unsubscribe(this);
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
