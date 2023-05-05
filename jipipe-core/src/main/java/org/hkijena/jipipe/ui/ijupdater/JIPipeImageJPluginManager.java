/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.ui.ijupdater;

import com.google.common.eventbus.Subscribe;
import net.imagej.ui.swing.updater.ImageJUpdater;
import net.imagej.updater.Conflicts;
import net.imagej.updater.FilesCollection;
import net.imagej.updater.UpdateSite;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeRunnable;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.JIPipeWorkbenchPanel;
import org.hkijena.jipipe.ui.components.MessagePanel;
import org.hkijena.jipipe.ui.running.JIPipeRunExecuterUI;
import org.hkijena.jipipe.ui.running.JIPipeRunnerQueue;
import org.hkijena.jipipe.utils.AutoResizeSplitPane;
import org.hkijena.jipipe.utils.CoreImageJUtils;
import org.hkijena.jipipe.utils.NetworkUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * JIPipe's own implementation of the ImageJ updater {@link net.imagej.ui.swing.updater.ImageJUpdater}
 */
public class JIPipeImageJPluginManager extends JIPipeWorkbenchPanel {

    private MessagePanel messagePanel;
    private UpdateSiteListUI updateSiteListUI;
    private ManagerUI managerUI;
    private RefreshRepositoryRun refreshRepositoryRun;
    private ActivateUpdateSiteRun activateUpdateSiteRun;
    private ApplyRun applyRun;
    private FilesCollection currentFilesCollection;
    private Set<String> updateSitesToActivate = new HashSet<>();
    private List<UpdateSite> updateSitesToAddAndActivate = new ArrayList<>();

    public JIPipeImageJPluginManager(JIPipeWorkbench workbench) {
        this(workbench, true);
    }

    /**
     * @param workbench the workbench
     */
    public JIPipeImageJPluginManager(JIPipeWorkbench workbench, boolean refresh) {
        super(workbench);
        initialize();
        JIPipeRunnerQueue.getInstance().getEventBus().register(this);
        if (refresh)
            refreshUpdater();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);

        toolBar.add(Box.createHorizontalGlue());

        JButton openLegacyUpdaterButton = new JButton("Open native updater", UIUtils.getIconFromResources("apps/imagej.png"));
        openLegacyUpdaterButton.setToolTipText("Opens the updater that comes with ImageJ.");
        openLegacyUpdaterButton.addActionListener(e -> openLegacyUpdater());
        toolBar.add(openLegacyUpdaterButton);

        JButton refreshButton = new JButton("Reset", UIUtils.getIconFromResources("actions/clear-brush.png"));
        refreshButton.addActionListener(e -> {
            if (isCurrentlyRunning()) {
                JOptionPane.showMessageDialog(this,
                        "There is already an operation running. Please wait until it is finished.",
                        "Reset", JOptionPane.ERROR_MESSAGE);
                return;
            }
            refreshUpdater();
        });
        toolBar.add(refreshButton);

        toolBar.addSeparator();

        JButton applyButton = new JButton("Apply changes", UIUtils.getIconFromResources("emblems/vcs-normal.png"));
        applyButton.addActionListener(e -> applyChanges());
        toolBar.add(applyButton);

        add(toolBar, BorderLayout.NORTH);

        JPanel upgraderPanel = new JPanel(new BorderLayout());
        messagePanel = new MessagePanel();
        messagePanel.setBorder(null);
        upgraderPanel.add(messagePanel, BorderLayout.NORTH);

        managerUI = new ManagerUI(getWorkbench(), this);
        upgraderPanel.add(managerUI, BorderLayout.CENTER);

        updateSiteListUI = new UpdateSiteListUI(this);

        JSplitPane splitPane = new AutoResizeSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                updateSiteListUI,
                upgraderPanel, AutoResizeSplitPane.RATIO_1_TO_3);
        add(splitPane, BorderLayout.CENTER);
    }

    private void applyChanges() {
        if (isCurrentlyRunning()) {
            JOptionPane.showMessageDialog(this,
                    "There is already an operation running. Please wait until it is finished.",
                    "Apply changes", JOptionPane.ERROR_MESSAGE);
            return;
        }
        final ResolveDependencies resolver = new ResolveDependencies(getWorkbench().getWindow(), currentFilesCollection);
        if (!resolver.resolve())
            return;
        applyRun = new ApplyRun(currentFilesCollection);
        messagePanel.clear();
        enqueueRun(applyRun);
    }

    private void enqueueRun(JIPipeRunnable runnable) {
        JIPipeRunExecuterUI ui = new JIPipeRunExecuterUI(runnable);
        managerUI.setOptionPanelContent(null);
        managerUI.setMainPanelContent(ui);
        ui.startRun();
    }

    private void openLegacyUpdater() {
        ImageJUpdater updater = new ImageJUpdater();
        getWorkbench().getContext().inject(updater);
        updater.run();
    }

    public void refreshUpdater() {
        updateSitesToActivate.clear();
        messagePanel.clear();
        if (ImageJUpdater.isDebian()) {
            messagePanel.addMessage(MessagePanel.MessageType.Error, "You are using the Debian packaged version of ImageJ. " +
                    "You should update ImageJ with your system's usual package manager instead.", true, true);
            return;
        }
        if (!NetworkUtils.hasInternetConnection()) {
            messagePanel.addMessage(MessagePanel.MessageType.Error, "Cannot connect to the Internet. Do you have a network connection? " +
                    "Are your proxy settings correct? See also http://forum.imagej.net/t/5070", true, true);
            return;
        }
        if (Files.exists(CoreImageJUtils.getImageJUpdaterRoot().resolve("update"))) {
            messagePanel.addMessage(MessagePanel.MessageType.Warning, "We recommend to restart ImageJ, as some updates were applied.", true, true);
        }

        refreshRepositoryRun = new RefreshRepositoryRun();
        updateSiteListUI.setFilesCollection(null);
        enqueueRun(refreshRepositoryRun);
    }

    private void resolveConflicts() {
        final List<Conflicts.Conflict> conflicts = currentFilesCollection.getConflicts();
        if (conflicts != null && conflicts.size() > 0) {
            ConflictDialog dialog = new ConflictDialog(getWorkbench().getWindow(), "Conflicting versions") {
                private static final long serialVersionUID = 1L;

                @Override
                protected void updateConflictList() {
                    conflictList = conflicts;
                }
            };
            if (!dialog.resolve()) {
                updateConflictsMessage();
            }
        }
    }

    public List<UpdateSite> getUpdateSitesToAddAndActivate() {
        return updateSitesToAddAndActivate;
    }

    public void setUpdateSitesToAddAndActivate(List<UpdateSite> updateSitesToAddAndActivate) {
        this.updateSitesToAddAndActivate = updateSitesToAddAndActivate;
    }

    public boolean isCurrentlyRunning() {
        return !JIPipeRunnerQueue.getInstance().isEmpty();
    }

    @Subscribe
    public void onOperationInterrupted(JIPipeRunnable.InterruptedEvent event) {
        if (event.getRun() == refreshRepositoryRun) {
            messagePanel.addMessage(MessagePanel.MessageType.Error, "There was an error during the update.", true, true);
            getWorkbench().sendStatusBarText("Could not refresh ImageJ plugin information from online resources");
        } else if (event.getRun() == activateUpdateSiteRun) {
            messagePanel.addMessage(MessagePanel.MessageType.Error, "There was an error during activation of update sites.", true, true);
            getWorkbench().sendStatusBarText("Could not activate update sites");
        } else if (event.getRun() == applyRun) {
            messagePanel.addMessage(MessagePanel.MessageType.Error, "There was an error during installation.", true, true);
            getWorkbench().sendStatusBarText("Could not apply changes");
        }
    }

    @Subscribe
    public void onOperationFinished(JIPipeRunnable.FinishedEvent event) {
        if (event.getRun() == refreshRepositoryRun) {
            getWorkbench().sendStatusBarText("Refreshed ImageJ plugin information from online resources");
            if (refreshRepositoryRun.getFilesCollection() != null) {
                this.currentFilesCollection = refreshRepositoryRun.getFilesCollection();
                showCurrentFilesCollection();
            } else {
                updateSiteListUI.setFilesCollection(null);
            }
            if (!updateSitesToAddAndActivate.isEmpty()) {
                if (currentFilesCollection != null) {
                    int activated = 0;
                    for (UpdateSite updateSite : updateSitesToAddAndActivate) {
                        // Find equivalent
                        UpdateSite existing = currentFilesCollection.getUpdateSite(updateSite.getName(), true);
                        if (existing != null) {
                            if (!existing.isActive()) {
                                updateSitesToActivate.add(existing.getName());
                                ++activated;
                            }
                        } else {
                            currentFilesCollection.addUpdateSite(updateSite);
                            updateSitesToActivate.add(updateSite.getName());
                            ++activated;
                        }
                        updateSiteListUI.refreshList();

                        // Trigger activation run
                        activateStagedUpdateSites();
                    }
                    updateSitesToAddAndActivate.clear();
                    messagePanel.addMessage(MessagePanel.MessageType.Info, "Activated " + activated + " update sites. Click 'Apply changes' to install the files.", true, true);
                } else {
                    messagePanel.addMessage(MessagePanel.MessageType.Error, "Could not activate update sites.", true, true);
                }
            }
            managerUI.setMainPanelContent(null);
        } else if (event.getRun() == activateUpdateSiteRun) {
            getWorkbench().sendStatusBarText("Activated update sites");
            showCurrentFilesCollection();
            managerUI.setMainPanelContent(null);
        } else if (event.getRun() == applyRun) {
            JButton exitButton = new JButton("Close ImageJ");
            exitButton.addActionListener(e -> {
                JIPipe.getSettings().save();
                System.exit(0);
            });
            messagePanel.addMessage(MessagePanel.MessageType.Info, "Changes were successfully applied. Please restart ImageJ.", true, true, exitButton);
            showCurrentFilesCollection();
            managerUI.setMainPanelContent(null);
        }
    }

    public void showCurrentFilesCollection() {
        updateSiteListUI.setFilesCollection(currentFilesCollection);
        managerUI.setFilesCollection(currentFilesCollection);

        // Offer conflict resolution
        updateConflictsMessage();
    }

    public void updateConflictsMessage() {
        List<Conflicts.Conflict> conflicts = currentFilesCollection.getConflicts();
        if (currentFilesCollection != null && conflicts != null && !conflicts.isEmpty()) {
            JButton resolveConflictsButton = new JButton("Resolve conflicts ...");
            resolveConflictsButton.addActionListener(e -> resolveConflicts());
            messagePanel.addMessage(MessagePanel.MessageType.Warning,
                    "There are " + conflicts.size() + " conflicts. Please resolve them.",
                    true, true, resolveConflictsButton);
        }
    }

    public void stageActivateUpdateSite(UpdateSite updateSite) {
        updateSitesToActivate.add(updateSite.getName());
        updateActivateUpdateSitesMessage();
    }

    private void updateActivateUpdateSitesMessage() {
        if (!updateSitesToActivate.isEmpty()) {
            JButton refreshButton = new JButton("Refresh", UIUtils.getIconFromResources("actions/view-refresh.png"));
            refreshButton.addActionListener(e -> activateStagedUpdateSites());
            messagePanel.addMessage(MessagePanel.MessageType.Info, "You changed the configuration of update sites. Click the following button to " +
                    "download the list of files that is needed.", true, true, refreshButton);
        }
    }

    private void activateStagedUpdateSites() {
        List<UpdateSite> toActivate = new ArrayList<>();
        for (UpdateSite updateSite : currentFilesCollection.getUpdateSites(true)) {
            if (!updateSite.isActive() && updateSitesToActivate.contains(updateSite.getName())) {
                toActivate.add(updateSite);
            }
        }
        activateUpdateSiteRun = new ActivateUpdateSiteRun(currentFilesCollection, toActivate);
        managerUI.setFilesCollection(null);
        enqueueRun(activateUpdateSiteRun);
    }

    public void removeUpdateSite(UpdateSite updateSite) {
        try {
            currentFilesCollection.removeUpdateSite(updateSite.getName());
            showCurrentFilesCollection();
        } catch (Exception e) {
            UIUtils.openErrorDialog(this, e);
        }
    }

    public void deactivateUpdateSite(UpdateSite updateSite) {
        updateSitesToActivate.remove(updateSite.getName());
        updateActivateUpdateSitesMessage();
        try {
            currentFilesCollection.deactivateUpdateSite(updateSite);
            showCurrentFilesCollection();
        } catch (Exception e) {
            UIUtils.openErrorDialog(this, e);
        }
    }

    public void addUpdateSite(UpdateSite updateSite) {
        try {
            currentFilesCollection.addUpdateSite(updateSite);
        } catch (Exception e) {
            UIUtils.openErrorDialog(this, e);
        }
    }
}
