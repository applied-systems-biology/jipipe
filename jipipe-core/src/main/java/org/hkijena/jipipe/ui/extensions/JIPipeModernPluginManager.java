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

import com.google.common.collect.ImmutableList;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import net.imagej.ui.swing.updater.ImageJUpdater;
import net.imagej.updater.Conflicts;
import net.imagej.updater.FilesCollection;
import net.imagej.updater.UpdateSite;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.JIPipeDependency;
import org.hkijena.jipipe.JIPipeExtension;
import org.hkijena.jipipe.JIPipeImageJUpdateSiteDependency;
import org.hkijena.jipipe.api.registries.JIPipeExtensionRegistry;
import org.hkijena.jipipe.ui.components.MessagePanel;
import org.hkijena.jipipe.ui.ijupdater.ConflictDialog;
import org.hkijena.jipipe.ui.ijupdater.RefreshRepositoryRun;
import org.hkijena.jipipe.ui.running.JIPipeRunExecuterUI;
import org.hkijena.jipipe.ui.running.JIPipeRunnerQueue;
import org.hkijena.jipipe.ui.running.RunWorkerFinishedEvent;
import org.hkijena.jipipe.ui.running.RunWorkerInterruptedEvent;
import org.hkijena.jipipe.utils.CoreImageJUtils;
import org.hkijena.jipipe.utils.NetworkUtils;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Files;
import java.util.List;
import java.util.*;

public class JIPipeModernPluginManager {

    private final EventBus eventBus = new EventBus();

    private final Component parent;

    private final MessagePanel messagePanel;
    private final List<UpdateSiteExtension> updateSiteWrapperExtensions = new ArrayList<>();
    private MessagePanel.Message updateSiteMessage;
    private MessagePanel.Message restartMessage;
    private RefreshRepositoryRun refreshRepositoryRun;
    private boolean updateSitesReady = false;
    private FilesCollection updateSites;
    private boolean updateSitesApplied = false;

    public JIPipeModernPluginManager(Component parent, MessagePanel messagePanel) {
        this.parent = parent;
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
        if (restartMessage != null) {
            messagePanel.removeMessage(restartMessage);
            restartMessage = null;
        }
        if (isUpdateSitesApplied()) {
            JButton exitButton = new JButton("Close ImageJ");
            exitButton.addActionListener(e -> System.exit(0));
            restartMessage = messagePanel.addMessage(MessagePanel.MessageType.Info, "To apply the changes, please restart ImageJ.", exitButton);
        } else if (!getExtensionRegistry().getScheduledDeactivateExtensions().isEmpty() || !getExtensionRegistry().getScheduledActivateExtensions().isEmpty()) {
            JButton exitButton = new JButton("Close ImageJ");
            exitButton.addActionListener(e -> {
                if (JOptionPane.showConfirmDialog(SwingUtilities.getWindowAncestor(messagePanel), "Do you really want to close ImageJ? You will lose all unsaved changes.", "Close ImageJ", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                    System.exit(0);
                }
            });
            JButton restartJIPipeGUIButton = new JButton("Restart JIPipe");
            restartJIPipeGUIButton.addActionListener(e -> {
                if (JOptionPane.showConfirmDialog(SwingUtilities.getWindowAncestor(messagePanel), "Do you really want to restart JIPipe? You will lose all unsaved changes.", "Restart JIPipe", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
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
            ConflictDialog dialog = new ConflictDialog(SwingUtilities.getWindowAncestor(parent), "Conflicting versions") {
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
        if (updateSiteMessage != null) {
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
        if (updateSites != null) {
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
        } else if (event.getRun() instanceof ActivateAndApplyUpdateSiteRun || event.getRun() instanceof DeactivateAndApplyUpdateSiteRun) {
            updateSitesApplied = true;
            updateMessagePanel();
        }
    }

    public void deactivateExtension(JIPipeExtension extension) {
        if (extension instanceof UpdateSiteExtension) {
            deactivateUpdateSiteExtension((UpdateSiteExtension) extension);
        } else {
            deactivateJIPipeExtension(extension);
        }
    }

    private void deactivateJIPipeExtension(JIPipeExtension extension) {

        Set<String> dependents = getExtensionRegistry().getAllDependentsOf(extension.getDependencyId());
        for (String s : ImmutableList.copyOf(dependents)) {
            if (getExtensionRegistry().willBeDeactivatedOnNextStartup(s))
                dependents.remove(s);
        }
        if (!dependents.isEmpty()) {
            DeactivateDependentsConfirmationDialog dialog = new DeactivateDependentsConfirmationDialog(SwingUtilities.getWindowAncestor(parent), this, extension);
            dialog.setModal(true);
            dialog.setSize(800, 600);
            dialog.setLocationRelativeTo(SwingUtilities.getWindowAncestor(parent));
            dialog.setVisible(true);
            if (dialog.isCancelled()) {
                return;
            }
        }

        // Straight-forward
        getExtensionRegistry().scheduleDeactivateExtension(extension.getDependencyId());
    }

    private void deactivateUpdateSiteExtension(UpdateSiteExtension extension) {
        if (!isUpdateSitesReady()) {
            JOptionPane.showMessageDialog(SwingUtilities.getWindowAncestor(parent), "ImageJ updates sites are currently not ready/unavailable.",
                    "Deactivate ImageJ update site", JOptionPane.ERROR_MESSAGE);
            return;
        }
        // Is an update site that was not added
        if (extension.getUpdateSite(getUpdateSites()) == null)
            return;
        if (JOptionPane.showConfirmDialog(SwingUtilities.getWindowAncestor(parent), "Do you really want to deactivate the update site '"
                + extension.getUpdateSite(getUpdateSites()).getName() + "'? Please note that this will delete plugin files from the ImageJ directory.", "Deactivate update site", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            DeactivateAndApplyUpdateSiteRun run = new DeactivateAndApplyUpdateSiteRun(this, Collections.singletonList(extension.getUpdateSite(getUpdateSites())));
            JIPipeRunExecuterUI.runInDialog(SwingUtilities.getWindowAncestor(parent), run);
        }
    }

    public void activateExtension(JIPipeExtension extension) {
        if (extension instanceof UpdateSiteExtension) {
            activateUpdateSiteExtension((UpdateSiteExtension) extension);
        } else {
            activateJIPipeExtension(extension);
        }
    }

    private void activateJIPipeExtension(JIPipeExtension extension) {

        // Get all dependencies and attempt to resolve them against the known extensions
        Set<JIPipeDependency> allDependencies = getExtensionRegistry().tryResolveToKnownDependencies(extension.getAllDependencies());
        Set<JIPipeDependency> missingDependencies = new HashSet<>();
        for (JIPipeDependency dependency : allDependencies) {
            if (getExtensionRegistry().willBeDeactivatedOnNextStartup(dependency.getDependencyId())) {
                missingDependencies.add(dependency);
            }
        }

        // Collect all [missing] update sites
        Set<JIPipeImageJUpdateSiteDependency> missingUpdateSites = new HashSet<>();
        Set<JIPipeImageJUpdateSiteDependency> allUpdateSites = extension.getAllImageJUpdateSiteDependencies();

        if (!allUpdateSites.isEmpty()) {
            if (!isUpdateSitesReady()) {
                int response = JOptionPane.showOptionDialog(SwingUtilities.getWindowAncestor(parent), "The selected extension requests various ImageJ update sites, but there is currently no connection to the update site system.\n" +
                                "You can ignore update sites or wait until the initialization is complete. If you click 'Wait' click the 'Activate' " +
                                "button again after the update sites have been initialized.",
                        "Activate " + extension.getMetadata().getName(), JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, new Object[]{"Wait", "Ignore", "Cancel"},
                        "Wait");
                if (response == JOptionPane.YES_OPTION || response == JOptionPane.CANCEL_OPTION)
                    return;
                getExtensionRegistry().scheduleActivateExtension(extension.getDependencyId());
            } else {
                // Check if there are missing update sites
                for (JIPipeImageJUpdateSiteDependency dependency : allUpdateSites) {
                    missingUpdateSites.add(dependency);
                }
                if (getUpdateSites() != null) {
                    for (UpdateSite updateSite : getUpdateSites().getUpdateSites(false)) {
                        missingUpdateSites.remove(updateSite.getName());
                    }
                }
            }
        }

        // Show confirm dialog
        if (!missingUpdateSites.isEmpty() || !missingDependencies.isEmpty()) {
            ActivateDependenciesConfirmationDialog dialog = new ActivateDependenciesConfirmationDialog(SwingUtilities.getWindowAncestor(parent),
                    this,
                    extension,
                    missingDependencies,
                    missingUpdateSites);
            dialog.setModal(true);
            dialog.setSize(800, 600);
            dialog.setLocationRelativeTo(SwingUtilities.getWindowAncestor(parent));
            dialog.setVisible(true);
            if (dialog.isCancelled()) {
                return;
            }
            // Trigger installation run of update sites
            List<UpdateSite> toActivate = new ArrayList<>();
            for (Map.Entry<JIPipeImageJUpdateSiteDependency, Boolean> entry : dialog.getDependencySitesToInstall().entrySet()) {
                if (entry.getValue()) {
                    UpdateSite updateSite = getUpdateSites().getUpdateSite(entry.getKey().getName(), true);
                    if (updateSite != null) {
                        toActivate.add(updateSite);
                    } else {
                        updateSite = getUpdateSites().addUpdateSite(entry.getKey().toUpdateSite());
                        toActivate.add(updateSite);
                    }
                }
            }
            if (!toActivate.isEmpty()) {
                ActivateAndApplyUpdateSiteRun run = new ActivateAndApplyUpdateSiteRun(this, toActivate);
                JIPipeRunExecuterUI.runInDialog(SwingUtilities.getWindowAncestor(parent), run);
            }
        }

        // Schedule activation of extension
        getExtensionRegistry().scheduleActivateExtension(extension.getDependencyId());

    }

    private void activateUpdateSiteExtension(UpdateSiteExtension extension) {
        if (!isUpdateSitesReady()) {
            JOptionPane.showMessageDialog(SwingUtilities.getWindowAncestor(parent), "ImageJ updates sites are currently not ready/unavailable.",
                    "Activate ImageJ update site", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (JOptionPane.showConfirmDialog(SwingUtilities.getWindowAncestor(parent), "Do you really want to activate the update site '"
                + extension.getUpdateSite(getUpdateSites()).getName() + "'? Please note that you need an active internet connection.", "Activate update site", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            ActivateAndApplyUpdateSiteRun run = new ActivateAndApplyUpdateSiteRun(this, Collections.singletonList(extension.getUpdateSite(getUpdateSites())));
            JIPipeRunExecuterUI.runInDialog(SwingUtilities.getWindowAncestor(parent), run);
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
