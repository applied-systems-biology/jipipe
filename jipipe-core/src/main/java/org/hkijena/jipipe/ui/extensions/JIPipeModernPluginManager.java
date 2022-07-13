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
 *
 */

package org.hkijena.jipipe.ui.extensions;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import net.imagej.ui.swing.updater.ImageJUpdater;
import net.imagej.updater.Conflicts;
import net.imagej.updater.FilesCollection;
import net.imagej.updater.UpdateSite;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.registries.JIPipeExtensionRegistry;
import org.hkijena.jipipe.ui.JIPipeDummyWorkbench;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.components.MessagePanel;
import org.hkijena.jipipe.ui.ijupdater.ConflictDialog;
import org.hkijena.jipipe.ui.ijupdater.RefreshRepositoryRun;
import org.hkijena.jipipe.ui.running.JIPipeRunnerQueue;
import org.hkijena.jipipe.ui.running.RunWorkerFinishedEvent;
import org.hkijena.jipipe.ui.running.RunWorkerInterruptedEvent;
import org.hkijena.jipipe.utils.CoreImageJUtils;
import org.hkijena.jipipe.utils.NetworkUtils;

import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class JIPipeModernPluginManager {

    private final EventBus eventBus = new EventBus();

    private final MessagePanel messagePanel;

    private MessagePanel.Message updateSiteMessage;

    private MessagePanel.Message restartMessage;

    private RefreshRepositoryRun refreshRepositoryRun;

    private boolean updateSitesReady = false;

    private FilesCollection updateSites;

    private final List<UpdateSiteExtension> updateSiteWrapperExtensions = new ArrayList<>();

    private boolean updateSitesApplied = false;

    public JIPipeModernPluginManager(MessagePanel messagePanel) {
        this.messagePanel = messagePanel;
        JIPipe.getInstance().getExtensionRegistry().getEventBus().register(this);
        JIPipeRunnerQueue.getInstance().getEventBus().register(this);

        updateMessagePanel();
    }

    private JIPipeExtensionRegistry getExtensionRegistry() {
        return JIPipe.getInstance().getExtensionRegistry();
    }

    public void initializeUpdateSites() {
        updateSiteMessage = messagePanel.addMessage(MessagePanel.MessageType.Info, "ImageJ update sites are currently being loaded. Until this process is finished, ImageJ plugins cannot be managed.");
        if (ImageJUpdater.isDebian()) {
            messagePanel.addMessage(MessagePanel.MessageType.Error, "You are using the Debian packaged version of ImageJ. " +
                    "You should update ImageJ with your system's usual package manager instead.");
            onFailure();
            return;
        }
        if (!NetworkUtils.hasInternetConnection()) {
            messagePanel.addMessage(MessagePanel.MessageType.Error, "Cannot connect to the Internet. Do you have a network connection? " +
                    "Are your proxy settings correct? See also http://forum.imagej.net/t/5070");
            onFailure();
            return;
        }
        if (Files.exists(CoreImageJUtils.getImageJUpdaterRoot().resolve("update"))) {
            messagePanel.addMessage(MessagePanel.MessageType.Warning, "We recommend to restart ImageJ, as some updates were applied.");
        }
        refreshRepositoryRun = new RefreshRepositoryRun();
        JIPipeRunnerQueue.getInstance().enqueue(refreshRepositoryRun);
    }

    private void updateMessagePanel() {
        if(restartMessage != null) {
            messagePanel.removeMessage(restartMessage);
            restartMessage = null;
        }
        if(isUpdateSitesApplied()){
            JButton exitButton = new JButton("Close ImageJ");
            exitButton.addActionListener(e -> System.exit(0));
            restartMessage = messagePanel.addMessage(MessagePanel.MessageType.Info, "To apply the changes, please restart ImageJ.", exitButton);
        }
        else if(!getExtensionRegistry().getScheduledDeactivateExtensions().isEmpty() || !getExtensionRegistry().getScheduledActivateExtensions().isEmpty()) {
            JButton exitButton = new JButton("Close ImageJ");
            exitButton.addActionListener(e -> {
                if(JOptionPane.showConfirmDialog(SwingUtilities.getWindowAncestor(messagePanel), "Do you really want to close ImageJ? You will lose all unsaved changes.", "Close ImageJ", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                    System.exit(0);
                }
            });
            JButton restartJIPipeGUIButton = new JButton("Restart JIPipe");
            restartJIPipeGUIButton.addActionListener(e -> {
                if(JOptionPane.showConfirmDialog(SwingUtilities.getWindowAncestor(messagePanel), "Do you really want to restart JIPipe? You will lose all unsaved changes.", "Restart JIPipe", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                    JIPipe.restartGUI();
                }
            });
            restartMessage = messagePanel.addMessage(MessagePanel.MessageType.Info, "To apply the changes, please restart ImageJ or JIPipe.", exitButton, restartJIPipeGUIButton);
        }
    }

    public void updateConflictsMessage() {
        List<Conflicts.Conflict> conflicts = updateSites.getConflicts();
        if (updateSites != null && conflicts != null && !conflicts.isEmpty()) {
            JButton resolveConflictsButton = new JButton("Resolve conflicts ...");
            resolveConflictsButton.addActionListener(e -> resolveConflicts());
            messagePanel.addMessage(MessagePanel.MessageType.Warning,
                    "There are " + conflicts.size() + " ImageJ update site conflicts. Please click the following button to resolve them.",
                    resolveConflictsButton);
        }
    }

    public void resolveConflicts() {
        final List<Conflicts.Conflict> conflicts = updateSites.getConflicts();
        if (conflicts != null && conflicts.size() > 0) {
            ConflictDialog dialog = new ConflictDialog(SwingUtilities.getWindowAncestor(messagePanel), "Conflicting versions") {
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

    @Subscribe
    public void onExtensionActivated(JIPipeExtensionRegistry.ScheduledActivateExtension event) {
        updateMessagePanel();
    }

    @Subscribe
    public void onExtensionDeactivated(JIPipeExtensionRegistry.ScheduledDeactivateExtension event) {
        updateMessagePanel();
    }

    private void onFailure() {
        removeUpdateSiteMessage();
        eventBus.post(new UpdateSitesFailedEvent(this));
    }

    private void onSuccess() {
        updateSitesReady = true;
        removeUpdateSiteMessage();
        createUpdateSitesWrappers();
        resolveConflicts();
        eventBus.post(new UpdateSitesReadyEvent(this));
    }

    private void removeUpdateSiteMessage() {
        if(updateSiteMessage != null) {
            messagePanel.removeMessage(updateSiteMessage);
            updateSiteMessage = null;
        }
    }

    public EventBus getEventBus() {
        return eventBus;
    }

    public MessagePanel getMessagePanel() {
        return messagePanel;
    }

    public boolean isUpdateSitesReady() {
        return updateSitesReady;
    }

    public FilesCollection getUpdateSites() {
        return updateSites;
    }

    public List<UpdateSiteExtension> getUpdateSiteWrapperExtensions() {
        return Collections.unmodifiableList(updateSiteWrapperExtensions);
    }

    public boolean isUpdateSitesApplied() {
        return updateSitesApplied;
    }

    private void createUpdateSitesWrappers() {
        updateSiteWrapperExtensions.clear();
        if(updateSites != null) {
            for (UpdateSite updateSite : updateSites.getUpdateSites(true)) {
                UpdateSiteExtension extension = new UpdateSiteExtension(updateSite);
                updateSiteWrapperExtensions.add(extension);
            }
        }
    }

    @Subscribe
    public void onOperationInterrupted(RunWorkerInterruptedEvent event) {
        if (event.getRun() == refreshRepositoryRun) {
            messagePanel.addMessage(MessagePanel.MessageType.Error, "There was an error during the ImageJ update site update.");
            onFailure();
        }
    }

    @Subscribe
    public void onOperationFinished(RunWorkerFinishedEvent event) {
        if (event.getRun() == refreshRepositoryRun) {
            this.updateSites = refreshRepositoryRun.getFilesCollection();
            onSuccess();
        }
        else if(event.getRun() instanceof ActivateAndApplyUpdateSiteRun || event.getRun() instanceof DeactivateAndApplyUpdateSiteRun) {
            updateSitesApplied = true;
            updateMessagePanel();
        }
    }

    public static class UpdateSitesReadyEvent {
        private final JIPipeModernPluginManager pluginManager;

        public UpdateSitesReadyEvent(JIPipeModernPluginManager pluginManager) {
            this.pluginManager = pluginManager;
        }

        public JIPipeModernPluginManager getPluginManager() {
            return pluginManager;
        }
    }

    public static class UpdateSitesFailedEvent {
        private final JIPipeModernPluginManager pluginManager;

        public UpdateSitesFailedEvent(JIPipeModernPluginManager pluginManager) {
            this.pluginManager = pluginManager;
        }

        public JIPipeModernPluginManager getPluginManager() {
            return pluginManager;
        }
    }
}
