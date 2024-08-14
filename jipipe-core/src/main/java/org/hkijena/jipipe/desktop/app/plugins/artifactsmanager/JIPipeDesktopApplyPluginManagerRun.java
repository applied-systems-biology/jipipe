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

package org.hkijena.jipipe.desktop.app.plugins.artifactsmanager;

import net.imagej.updater.UpdateSite;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.JIPipePlugin;
import org.hkijena.jipipe.api.AbstractJIPipeRunnable;
import org.hkijena.jipipe.api.notifications.JIPipeNotification;
import org.hkijena.jipipe.desktop.app.plugins.pluginsmanager.JIPipeDesktopImageJUpdateSitesRepository;
import org.hkijena.jipipe.desktop.app.plugins.pluginsmanager.JIPipeDesktopPluginManagerUI;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class JIPipeDesktopApplyPluginManagerRun extends AbstractJIPipeRunnable {

    private final JIPipeDesktopPluginManagerUI pluginManagerUI;
    private final Set<JIPipePlugin> pluginsToActivate;
    private final Set<JIPipePlugin> pluginsToDeactivate;
    private final Set<UpdateSite> updateSitesToInstall;

    public JIPipeDesktopApplyPluginManagerRun(JIPipeDesktopPluginManagerUI pluginManagerUI, Set<JIPipePlugin> pluginsToActivate, Set<JIPipePlugin> pluginsToDeactivate, Set<UpdateSite> updateSitesToInstall) {
        this.pluginManagerUI = pluginManagerUI;
        this.pluginsToActivate = pluginsToActivate;
        this.pluginsToDeactivate = pluginsToDeactivate;
        this.updateSitesToInstall = updateSitesToInstall;
    }

    @Override
    public String getTaskLabel() {
        return "Apply plugin operations";
    }

    @Override
    public void run() {
        if (!pluginsToDeactivate.isEmpty()) {
            getProgressInfo().log("Deactivating plugins ...");
            for (JIPipePlugin plugin : pluginsToDeactivate) {
                getProgressInfo().log("-> " + plugin.getDependencyId());
                JIPipe.getInstance().getPluginRegistry().scheduleDeactivatePlugin(plugin.getDependencyId());
            }
        }

        if (!pluginsToActivate.isEmpty()) {
            getProgressInfo().log("Activating plugins ...");
            for (JIPipePlugin plugin : pluginsToActivate) {
                getProgressInfo().log("-> " + plugin.getDependencyId());
                JIPipe.getInstance().getPluginRegistry().scheduleActivatePlugin(plugin.getDependencyId());
            }
        }

        try {
            // Update sites
            if (!updateSitesToInstall.isEmpty()) {
                JIPipeDesktopImageJUpdateSitesRepository.ActivateDeactivateRun run =
                        new JIPipeDesktopImageJUpdateSitesRepository.ActivateDeactivateRun(pluginManagerUI.getUpdateSitesRepository(),
                                Collections.emptyList(),
                                new ArrayList<>(updateSitesToInstall));
                run.setProgressInfo(getProgressInfo().resolve("Update sites"));
                run.run();
            }
        }
        catch (Exception e) {
            getProgressInfo().getNotifications().push(new JIPipeNotification("jipipe:plugin-manager:install-update-sites:failed",
                    "Failed to install ImageJ update sites",
                    "The following ImageJ update sites could not be activated: " + updateSitesToInstall.stream().filter(Objects::nonNull).map(UpdateSite::getName).collect(Collectors.joining(", ")) +
                    "\n\nPlease install the sites manually via the ImageJ updater (Plugins > ImageJ plugins)"));
        }
    }
}
