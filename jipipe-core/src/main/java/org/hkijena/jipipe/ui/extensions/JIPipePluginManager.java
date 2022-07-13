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
import net.imagej.updater.FilesCollection;
import net.imagej.updater.UpdateSite;
import org.hkijena.jipipe.ui.components.MessagePanel;
import org.hkijena.jipipe.ui.ijupdater.RefreshRepositoryRun;
import org.hkijena.jipipe.ui.running.JIPipeRunnerQueue;
import org.hkijena.jipipe.ui.running.RunWorkerFinishedEvent;
import org.hkijena.jipipe.ui.running.RunWorkerInterruptedEvent;
import org.hkijena.jipipe.utils.CoreImageJUtils;
import org.hkijena.jipipe.utils.NetworkUtils;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class JIPipePluginManager {

    private final EventBus eventBus = new EventBus();

    private final MessagePanel messagePanel;

    private MessagePanel.Message updateSiteMessage;

    private RefreshRepositoryRun refreshRepositoryRun;

    private boolean updateSitesReady = false;

    private FilesCollection updateSites;

    private final List<UpdateSiteExtension> updateSiteWrapperExtensions = new ArrayList<>();

    private boolean updateSitesApplied = false;

    public JIPipePluginManager(MessagePanel messagePanel) {
        this.messagePanel = messagePanel;
        JIPipeRunnerQueue.getInstance().getEventBus().register(this);
    }

    public void initializeUpdateSites() {
        updateSiteMessage = messagePanel.addMessage(MessagePanel.MessageType.Info, "ImageJ update sites are currently being loaded. Until this process is finished, ImageJ plugins cannot be managed.", null);
        if (ImageJUpdater.isDebian()) {
            messagePanel.addMessage(MessagePanel.MessageType.Error, "You are using the Debian packaged version of ImageJ. " +
                    "You should update ImageJ with your system's usual package manager instead.", null);
            onFailure();
            return;
        }
        if (!NetworkUtils.hasInternetConnection()) {
            messagePanel.addMessage(MessagePanel.MessageType.Error, "Cannot connect to the Internet. Do you have a network connection? " +
                    "Are your proxy settings correct? See also http://forum.imagej.net/t/5070", null);
            onFailure();
            return;
        }
        if (Files.exists(CoreImageJUtils.getImageJUpdaterRoot().resolve("update"))) {
            messagePanel.addMessage(MessagePanel.MessageType.Warning, "We recommend to restart ImageJ, as some updates were applied.", null);
        }
        refreshRepositoryRun = new RefreshRepositoryRun();
        JIPipeRunnerQueue.getInstance().enqueue(refreshRepositoryRun);
    }

    private void onFailure() {
        removeUpdateSiteMessage();
        eventBus.post(new UpdateSitesFailedEvent(this));
    }

    private void onSuccess() {
        removeUpdateSiteMessage();
        createUpdateSitesWrappers();
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
            messagePanel.addMessage(MessagePanel.MessageType.Error, "There was an error during the ImageJ update site update.", null);
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
        }
    }

    public static class UpdateSitesReadyEvent {
        private final JIPipePluginManager pluginManager;

        public UpdateSitesReadyEvent(JIPipePluginManager pluginManager) {
            this.pluginManager = pluginManager;
        }

        public JIPipePluginManager getPluginManager() {
            return pluginManager;
        }
    }

    public static class UpdateSitesFailedEvent {
        private final JIPipePluginManager pluginManager;

        public UpdateSitesFailedEvent(JIPipePluginManager pluginManager) {
            this.pluginManager = pluginManager;
        }

        public JIPipePluginManager getPluginManager() {
            return pluginManager;
        }
    }
}
