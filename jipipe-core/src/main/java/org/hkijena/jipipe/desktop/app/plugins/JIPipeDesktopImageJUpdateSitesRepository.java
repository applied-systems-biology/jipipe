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

package org.hkijena.jipipe.desktop.app.plugins;

import net.imagej.ui.swing.updater.SwingAuthenticator;
import net.imagej.updater.Conflicts;
import net.imagej.updater.FilesCollection;
import net.imagej.updater.Installer;
import net.imagej.updater.UpdateSite;
import net.imagej.updater.util.AvailableSites;
import net.imagej.updater.util.UpdaterUtil;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.hkijena.jipipe.api.AbstractJIPipeRunnable;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.notifications.JIPipeNotification;
import org.hkijena.jipipe.desktop.commons.ijupdater.JIPipeDesktopImageJUpdaterConflictDialog;
import org.hkijena.jipipe.desktop.commons.ijupdater.JIPipeDesktopImageJUpdaterProgressAdapter;
import org.hkijena.jipipe.desktop.commons.ijupdater.JIPipeDesktopImageJUpdaterProgressAdapter2;
import org.hkijena.jipipe.utils.CoreImageJUtils;
import org.hkijena.jipipe.utils.StringUtils;
import org.xml.sax.SAXException;

import javax.swing.*;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.Authenticator;
import java.util.List;

public class JIPipeDesktopImageJUpdateSitesRepository {
    private final JIPipeDesktopPluginManagerUI pluginManager;
    private FilesCollection updateSites;
    private RepositoryStatus status = RepositoryStatus.NotLoaded;
    private String conflictWarnings;

    public JIPipeDesktopImageJUpdateSitesRepository(JIPipeDesktopPluginManagerUI pluginManager) {
        this.pluginManager = pluginManager;
    }

    private void ensureUpdateSites(JIPipeProgressInfo progressInfo) {
        if(status == RepositoryStatus.NotLoaded) {
            try {
                progressInfo.log("Rebuilding update site index ...");
                rebuildUpdateSites(progressInfo);
                progressInfo.log("Checking for conflicts ...");
                resolveConflicts();
                status = RepositoryStatus.Loaded;
            }
            catch (Throwable e) {
                status = RepositoryStatus.Failed;
                progressInfo.log(ExceptionUtils.getStackTrace(e));
                progressInfo.getNotifications().push(new JIPipeNotification("ij-update-site:error", "Error while initializing ImageJ update sites!",
                        "An error happened while loading the ImageJ update sites service. " +
                                "Please refer to the log to learn more about this error."));
            }
        }
        else if(status == RepositoryStatus.Loaded) {
            progressInfo.log("ImageJ update sites are already loaded");
        }
        else {
            progressInfo.log("[!!!] IMAGEJ UPDATE SITES COULD NOT BE LOADED");
        }
    }

    private void rebuildUpdateSites(JIPipeProgressInfo progressInfo) {
        UpdaterUtil.useSystemProxies();
        Authenticator.setDefault(new SwingAuthenticator());

        updateSites = new FilesCollection(CoreImageJUtils.getImageJUpdaterRoot().toFile());
        AvailableSites.initializeAndAddSites(updateSites);

        // Look for conflicts
        try {
            conflictWarnings = updateSites.downloadIndexAndChecksum(new JIPipeDesktopImageJUpdaterProgressAdapter2(progressInfo));
        } catch (ParserConfigurationException | SAXException e) {
            throw new RuntimeException(e);
        }

        updateSites.markForUpdate(false);
    }

    public void resolveConflicts() {
        final List<Conflicts.Conflict> conflicts = updateSites.getConflicts();
        if (conflicts != null && !conflicts.isEmpty()) {
            if(JOptionPane.showConfirmDialog(pluginManager, "The ImageJ updater detected conflicts, which you are recommended to resolve.\n" +
                     "Continue with the conflict resolution?", "Conflicts detected", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION) {
                JIPipeDesktopImageJUpdaterConflictDialog dialog = new JIPipeDesktopImageJUpdaterConflictDialog(SwingUtilities.getWindowAncestor(pluginManager), "Conflicting versions") {
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected void updateConflictList() {
                        conflictList = conflicts;
                    }
                };
                try {
                    SwingUtilities.invokeAndWait(dialog::resolve);
                } catch (InterruptedException | InvocationTargetException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    public FilesCollection getUpdateSites() {
        return updateSites;
    }

    public RepositoryStatus getStatus() {
        return status;
    }

    public enum RepositoryStatus {
        NotLoaded,
        Loaded,
        Failed
    }

    public static class ActivateDeactivateRun extends AbstractJIPipeRunnable {
        private final JIPipeDesktopImageJUpdateSitesRepository repository;
        private final List<UpdateSite> toDeactivate;
        private final List<UpdateSite> toActivate;

        public ActivateDeactivateRun(JIPipeDesktopImageJUpdateSitesRepository repository, List<UpdateSite> toDeactivate, List<UpdateSite> toActivate) {
            this.repository = repository;
            this.toDeactivate = toDeactivate;
            this.toActivate = toActivate;
        }

        @Override
        public String getTaskLabel() {
            return "Apply update sites";
        }

        @Override
        public void run() {
            JIPipeProgressInfo progressInfo = getProgressInfo();
            repository.ensureUpdateSites(progressInfo);

            if(!toDeactivate.isEmpty()) {
                for (UpdateSite updateSite : toDeactivate) {
                    repository.updateSites.deactivateUpdateSite(updateSite);
                }
            }

            if(!toActivate.isEmpty()) {
                for (UpdateSite updateSite : toActivate) {
                    try {
                        repository.updateSites.activateUpdateSite(updateSite, new JIPipeDesktopImageJUpdaterProgressAdapter2(progressInfo));
                    } catch (ParserConfigurationException | IOException | SAXException e) {
                        throw new RuntimeException(e);
                    }
                }
            }

            repository.resolveConflicts();

            final Installer installer =
                    new Installer(repository.updateSites, new JIPipeDesktopImageJUpdaterProgressAdapter2(progressInfo));
            try {
                installer.start();
                repository.updateSites.write();
                progressInfo.log("Updated successfully. Please restart ImageJ!");
            } catch (final Exception e) {
                throw new RuntimeException(e);
            } finally {
                installer.done();
            }
        }
    }
}
