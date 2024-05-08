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

import net.imagej.updater.FilesCollection;
import net.imagej.updater.Installer;
import net.imagej.updater.UpdateSite;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.run.JIPipeRunnable;
import org.hkijena.jipipe.desktop.app.extensions.JIPipeDesktopModernPluginManager;
import org.hkijena.jipipe.desktop.commons.ijupdater.JIPipeDesktopImageJUpdaterProgressAdapter2;
import org.xml.sax.SAXException;

import javax.swing.*;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

public class JIPipeDesktopActivateAndApplyUpdateSiteRun implements JIPipeRunnable {

    private final JIPipeDesktopModernPluginManager pluginManager;
    private final List<UpdateSite> updateSites;
    private JIPipeProgressInfo progressInfo = new JIPipeProgressInfo();

    public JIPipeDesktopActivateAndApplyUpdateSiteRun(JIPipeDesktopModernPluginManager pluginManager, List<UpdateSite> updateSites) {
        this.pluginManager = pluginManager;
        this.updateSites = updateSites;
    }

    @Override
    public void run() {

        FilesCollection filesCollection = pluginManager.getUpdateSites();

        // Activate
        try {
            for (UpdateSite updateSite : updateSites) {
                filesCollection.activateUpdateSite(updateSite, new JIPipeDesktopImageJUpdaterProgressAdapter2(progressInfo));
            }
        } catch (ParserConfigurationException | IOException | SAXException e) {
            throw new RuntimeException(e);
        }

        // Resolve conflicts
        try {
            SwingUtilities.invokeAndWait(pluginManager::resolveConflicts);
        } catch (InterruptedException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }

        // Install
        final Installer installer =
                new Installer(filesCollection, new JIPipeDesktopImageJUpdaterProgressAdapter2(progressInfo));
        try {
            installer.start();
            filesCollection.write();
            progressInfo.log("Updated successfully.  Please restart ImageJ!");
        } catch (final Exception e) {
            throw new RuntimeException(e);
        } finally {
            installer.done();
        }
    }

    public JIPipeDesktopModernPluginManager getPluginManager() {
        return pluginManager;
    }

    public List<UpdateSite> getUpdateSites() {
        return updateSites;
    }

    @Override
    public JIPipeProgressInfo getProgressInfo() {
        return progressInfo;
    }

    @Override
    public void setProgressInfo(JIPipeProgressInfo progressInfo) {
        this.progressInfo = progressInfo;
    }

    @Override
    public String getTaskLabel() {
        return "ImageJ updater: Activate site";
    }
}
