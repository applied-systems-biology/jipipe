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

package org.hkijena.jipipe.desktop.app.extensions;

import com.google.common.collect.ImmutableList;
import net.imagej.ui.swing.updater.ImageJUpdater;
import net.imagej.updater.Conflicts;
import net.imagej.updater.FilesCollection;
import net.imagej.updater.UpdateSite;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.JIPipeDependency;
import org.hkijena.jipipe.JIPipeImageJUpdateSiteDependency;
import org.hkijena.jipipe.JIPipePlugin;
import org.hkijena.jipipe.api.JIPipeWorkbench;
import org.hkijena.jipipe.api.events.AbstractJIPipeEvent;
import org.hkijena.jipipe.api.events.JIPipeEventEmitter;
import org.hkijena.jipipe.api.registries.JIPipePluginRegistry;
import org.hkijena.jipipe.api.run.JIPipeRunnable;
import org.hkijena.jipipe.api.run.JIPipeRunnableQueue;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbenchAccess;
import org.hkijena.jipipe.desktop.app.plugins.JIPipeDesktopActivateAndApplyUpdateSiteRun;
import org.hkijena.jipipe.desktop.app.plugins.JIPipeDesktopDeactivateAndApplyUpdateSiteRun;
import org.hkijena.jipipe.desktop.app.running.JIPipeDesktopRunExecuterUI;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopMessagePanel;
import org.hkijena.jipipe.desktop.commons.ijupdater.JIPipeDesktopImageJUpdaterConflictDialog;
import org.hkijena.jipipe.desktop.commons.ijupdater.JIPipeDesktopImageJUpdaterRefreshRepositoryRun;
import org.hkijena.jipipe.utils.CoreImageJUtils;
import org.hkijena.jipipe.utils.NetworkUtils;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Files;
import java.util.List;
import java.util.*;

public class JIPipeDesktopModernPluginManager implements JIPipeDesktopWorkbenchAccess, JIPipePluginRegistry.ScheduledActivatePluginEventListener, JIPipePluginRegistry.ScheduledDeactivatePluginEventListener, JIPipeRunnable.FinishedEventListener, JIPipeRunnable.InterruptedEventListener {

    private final UpdateSitesReadyEventEmitter updateSitesReadyEventEmitter = new UpdateSitesReadyEventEmitter();
    private final UpdateSitesFailedEventEmitter updateSitesFailedEventEmitter = new UpdateSitesFailedEventEmitter();
    private final JIPipeDesktopWorkbench desktopWorkbench;
    private final Component parent;

    private final JIPipeDesktopMessagePanel messagePanel;
    private final List<JIPipeDesktopUpdateSitePlugin> updateSiteWrapperExtensions = new ArrayList<>();
    private JIPipeDesktopMessagePanel.Message updateSiteMessage;
    private JIPipeDesktopMessagePanel.Message restartMessage;
    private JIPipeDesktopImageJUpdaterRefreshRepositoryRun refreshRepositoryRun;
    private boolean updateSitesReady = false;
    private FilesCollection updateSites;
    private boolean updateSitesApplied = false;

    public JIPipeDesktopModernPluginManager(JIPipeDesktopWorkbench desktopWorkbench, Component parent, JIPipeDesktopMessagePanel messagePanel) {
        this.desktopWorkbench = desktopWorkbench;
        this.parent = parent;
        this.messagePanel = messagePanel;
        JIPipe.getInstance().getPluginRegistry().getScheduledActivatePluginEventEmitter().subscribeWeak(this);
        JIPipe.getInstance().getPluginRegistry().getScheduledDeactivatePluginEventEmitter().subscribeWeak(this);
        JIPipeRunnableQueue.getInstance().getFinishedEventEmitter().subscribeWeak(this);
        JIPipeRunnableQueue.getInstance().getInterruptedEventEmitter().subscribeWeak(this);

        updateMessagePanel();
    }

    @Override
    public JIPipeWorkbench getWorkbench() {
        return desktopWorkbench;
    }

    public UpdateSitesReadyEventEmitter getUpdateSitesReadyEventEmitter() {
        return updateSitesReadyEventEmitter;
    }

    public UpdateSitesFailedEventEmitter getUpdateSitesFailedEventEmitter() {
        return updateSitesFailedEventEmitter;
    }

    private JIPipePluginRegistry getExtensionRegistry() {
        return JIPipe.getInstance().getPluginRegistry();
    }

    public void initializeUpdateSites() {
        updateSiteMessage = messagePanel.addMessage(JIPipeDesktopMessagePanel.MessageType.Info, "ImageJ update sites are currently being loaded. Until this process is finished, ImageJ plugins cannot be managed.", true, true);
        if (ImageJUpdater.isDebian()) {
            messagePanel.addMessage(JIPipeDesktopMessagePanel.MessageType.Error, "You are using the Debian packaged version of ImageJ. " +
                    "You should update ImageJ with your system's usual package manager instead.", true, true);
            onFailure();
            return;
        }
        if (!NetworkUtils.hasInternetConnection()) {
            messagePanel.addMessage(JIPipeDesktopMessagePanel.MessageType.Error, "Cannot connect to the Internet. Do you have a network connection? " +
                    "Are your proxy settings correct? See also http://forum.imagej.net/t/5070", true, true);
            onFailure();
            return;
        }
        if (Files.exists(CoreImageJUtils.getImageJUpdaterRoot().resolve("update"))) {
            messagePanel.addMessage(JIPipeDesktopMessagePanel.MessageType.Warning, "We recommend to restart ImageJ, as some updates were applied.", true, true);
        }
        refreshRepositoryRun = new JIPipeDesktopImageJUpdaterRefreshRepositoryRun();
        JIPipeRunnableQueue.getInstance().enqueue(refreshRepositoryRun);
    }

    private void updateMessagePanel() {
        if (restartMessage != null) {
            messagePanel.removeMessage(restartMessage);
            restartMessage = null;
        }
        JButton exitButton = new JButton("Close ImageJ/JIPipe");
        exitButton.addActionListener(e -> {
            if (JOptionPane.showConfirmDialog(SwingUtilities.getWindowAncestor(messagePanel), "Do you really want to close ImageJ? You will lose all unsaved changes.", "Close ImageJ", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                JIPipe.exitLater(0);
            }
        });
        restartMessage = messagePanel.addMessage(JIPipeDesktopMessagePanel.MessageType.Info, "To apply the changes, please restart ImageJ or JIPipe.", true, true, exitButton);
    }

    public void updateConflictsMessage() {
        List<Conflicts.Conflict> conflicts = updateSites.getConflicts();
        if (updateSites != null && conflicts != null && !conflicts.isEmpty()) {
            JButton resolveConflictsButton = new JButton("Resolve conflicts ...");
            resolveConflictsButton.addActionListener(e -> resolveConflicts());
            messagePanel.addMessage(JIPipeDesktopMessagePanel.MessageType.Warning,
                    "There are " + conflicts.size() + " ImageJ update site conflicts. Please click the following button to resolve them.",
                    true, true, resolveConflictsButton);
        }
    }

    public void resolveConflicts() {
        final List<Conflicts.Conflict> conflicts = updateSites.getConflicts();
        if (conflicts != null && conflicts.size() > 0) {
            JIPipeDesktopImageJUpdaterConflictDialog dialog = new JIPipeDesktopImageJUpdaterConflictDialog(SwingUtilities.getWindowAncestor(parent), "Conflicting versions") {
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

    private void onFailure() {
        removeUpdateSiteMessage();
        updateSitesFailedEventEmitter.emit(new UpdateSitesFailedEvent(this));
    }

    private void onSuccess() {
        updateSitesReady = true;
        removeUpdateSiteMessage();
        createUpdateSitesWrappers();
        resolveConflicts();
        updateSitesReadyEventEmitter.emit(new UpdateSitesReadyEvent(this));
    }

    private void removeUpdateSiteMessage() {
        if (updateSiteMessage != null) {
            messagePanel.removeMessage(updateSiteMessage);
            updateSiteMessage = null;
        }
    }

    @Override
    public JIPipeDesktopWorkbench getDesktopWorkbench() {
        return desktopWorkbench;
    }

    public JIPipeDesktopMessagePanel getMessagePanel() {
        return messagePanel;
    }

    public boolean isUpdateSitesReady() {
        return updateSitesReady;
    }

    public FilesCollection getUpdateSites() {
        return updateSites;
    }

    public List<JIPipeDesktopUpdateSitePlugin> getUpdateSiteWrapperExtensions() {
        return Collections.unmodifiableList(updateSiteWrapperExtensions);
    }

    public boolean isUpdateSitesApplied() {
        return updateSitesApplied;
    }

    private void createUpdateSitesWrappers() {
        updateSiteWrapperExtensions.clear();
        if (updateSites != null) {
            for (UpdateSite updateSite : updateSites.getUpdateSites(true)) {
                JIPipeDesktopUpdateSitePlugin extension = new JIPipeDesktopUpdateSitePlugin(updateSite);
                updateSiteWrapperExtensions.add(extension);
            }
        }
    }

    public void deactivateExtension(JIPipePlugin extension) {
        if (extension instanceof JIPipeDesktopUpdateSitePlugin) {
            deactivateUpdateSiteExtension((JIPipeDesktopUpdateSitePlugin) extension);
        } else {
            deactivateJIPipeExtension(extension);
        }
    }

    private void deactivateJIPipeExtension(JIPipePlugin extension) {

        Set<String> dependents = getExtensionRegistry().getAllDependentsOf(extension.getDependencyId());
        for (String s : ImmutableList.copyOf(dependents)) {
            if (getExtensionRegistry().willBeDeactivatedOnNextStartup(s))
                dependents.remove(s);
        }
        if (!dependents.isEmpty()) {
            JIPipeDesktopDeactivateDependentsConfirmationDialog dialog = new JIPipeDesktopDeactivateDependentsConfirmationDialog(SwingUtilities.getWindowAncestor(parent), this, extension);
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

    private void deactivateUpdateSiteExtension(JIPipeDesktopUpdateSitePlugin extension) {
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
            JIPipeDesktopDeactivateAndApplyUpdateSiteRun run = new JIPipeDesktopDeactivateAndApplyUpdateSiteRun(this, Collections.singletonList(extension.getUpdateSite(getUpdateSites())));
            JIPipeDesktopRunExecuterUI.runInDialog(desktopWorkbench, SwingUtilities.getWindowAncestor(parent), run);
        }
    }

    public void activateExtension(JIPipePlugin extension) {
        if (extension instanceof JIPipeDesktopUpdateSitePlugin) {
            activateUpdateSiteExtension((JIPipeDesktopUpdateSitePlugin) extension);
        } else {
            activateJIPipeExtension(extension);
        }
    }

    private void activateJIPipeExtension(JIPipePlugin extension) {

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
            JIPipeDesktopActivateDependenciesConfirmationDialog dialog = new JIPipeDesktopActivateDependenciesConfirmationDialog(SwingUtilities.getWindowAncestor(parent),
                    this,
                    extension,
                    missingDependencies,
                    missingUpdateSites);
            dialog.setModal(true);
            dialog.pack();
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
                JIPipeDesktopActivateAndApplyUpdateSiteRun run = new JIPipeDesktopActivateAndApplyUpdateSiteRun(this, toActivate);
                JIPipeDesktopRunExecuterUI.runInDialog(desktopWorkbench, SwingUtilities.getWindowAncestor(parent), run);
            }
        }

        // Schedule activation of extension
        getExtensionRegistry().scheduleActivateExtension(extension.getDependencyId());

    }

    private void activateUpdateSiteExtension(JIPipeDesktopUpdateSitePlugin extension) {
        if (!isUpdateSitesReady()) {
            JOptionPane.showMessageDialog(SwingUtilities.getWindowAncestor(parent), "ImageJ updates sites are currently not ready/unavailable.",
                    "Activate ImageJ update site", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (JOptionPane.showConfirmDialog(SwingUtilities.getWindowAncestor(parent), "Do you really want to activate the update site '"
                + extension.getUpdateSite(getUpdateSites()).getName() + "'? Please note that you need an active internet connection.", "Activate update site", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            JIPipeDesktopActivateAndApplyUpdateSiteRun run = new JIPipeDesktopActivateAndApplyUpdateSiteRun(this, Collections.singletonList(extension.getUpdateSite(getUpdateSites())));
            JIPipeDesktopRunExecuterUI.runInDialog(desktopWorkbench, SwingUtilities.getWindowAncestor(parent), run);
        }
    }

    @Override
    public void onScheduledActivatePlugin(JIPipePluginRegistry.ScheduledActivatePluginEvent event) {
        updateMessagePanel();
    }

    @Override
    public void onScheduledDeactivatePlugin(JIPipePluginRegistry.ScheduledDeactivatePluginEvent event) {
        updateMessagePanel();
    }

    @Override
    public void onRunnableFinished(JIPipeRunnable.FinishedEvent event) {
        if (event.getRun() == refreshRepositoryRun) {
            this.updateSites = refreshRepositoryRun.getFilesCollection();
            onSuccess();
        } else if (event.getRun() instanceof JIPipeDesktopActivateAndApplyUpdateSiteRun || event.getRun() instanceof JIPipeDesktopDeactivateAndApplyUpdateSiteRun) {
            updateSitesApplied = true;
            updateMessagePanel();
        }
    }

    @Override
    public void onRunnableInterrupted(JIPipeRunnable.InterruptedEvent event) {
        if (event.getRun() == refreshRepositoryRun) {
            messagePanel.addMessage(JIPipeDesktopMessagePanel.MessageType.Error, "There was an error during the ImageJ update site update.", true, true);
            onFailure();
        }
    }

    public interface UpdateSitesReadyEventListener {
        void onPluginManagerUpdateSitesReady(UpdateSitesReadyEvent event);
    }

    public interface UpdateSitesFailedEventListener {
        void onPluginManagerUpdateSitesFailed(UpdateSitesFailedEvent event);
    }

    public static class UpdateSitesReadyEvent extends AbstractJIPipeEvent {
        private final JIPipeDesktopModernPluginManager pluginManager;

        public UpdateSitesReadyEvent(JIPipeDesktopModernPluginManager pluginManager) {
            super(pluginManager);
            this.pluginManager = pluginManager;
        }

        public JIPipeDesktopModernPluginManager getPluginManager() {
            return pluginManager;
        }
    }

    public static class UpdateSitesReadyEventEmitter extends JIPipeEventEmitter<UpdateSitesReadyEvent, UpdateSitesReadyEventListener> {
        @Override
        protected void call(UpdateSitesReadyEventListener updateSitesReadyEventListener, UpdateSitesReadyEvent event) {
            updateSitesReadyEventListener.onPluginManagerUpdateSitesReady(event);
        }
    }

    public static class UpdateSitesFailedEvent extends AbstractJIPipeEvent {
        private final JIPipeDesktopModernPluginManager pluginManager;

        public UpdateSitesFailedEvent(JIPipeDesktopModernPluginManager pluginManager) {
            super(pluginManager);
            this.pluginManager = pluginManager;
        }

        public JIPipeDesktopModernPluginManager getPluginManager() {
            return pluginManager;
        }
    }

    public static class UpdateSitesFailedEventEmitter extends JIPipeEventEmitter<UpdateSitesFailedEvent, UpdateSitesFailedEventListener> {

        @Override
        protected void call(UpdateSitesFailedEventListener updateSitesFailedEventListener, UpdateSitesFailedEvent event) {
            updateSitesFailedEventListener.onPluginManagerUpdateSitesFailed(event);
        }
    }
}
