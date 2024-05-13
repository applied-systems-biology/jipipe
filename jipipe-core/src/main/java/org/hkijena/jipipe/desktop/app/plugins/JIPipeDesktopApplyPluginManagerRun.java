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

import net.imagej.updater.UpdateSite;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.JIPipePlugin;
import org.hkijena.jipipe.api.AbstractJIPipeRunnable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;

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
        // Update sites
        if(!updateSitesToInstall.isEmpty()) {
            JIPipeDesktopImageJUpdateSitesRepository.ActivateDeactivateRun run =
                    new JIPipeDesktopImageJUpdateSitesRepository.ActivateDeactivateRun(pluginManagerUI.getUpdateSitesRepository(),
                            Collections.emptyList(),
                            new ArrayList<>(updateSitesToInstall));
            run.run();
        }

        if(!pluginsToDeactivate.isEmpty()) {
            getProgressInfo().log("Deactivating plugins ...");
            for (JIPipePlugin plugin : pluginsToDeactivate) {
                getProgressInfo().log("-> " + plugin.getDependencyId());
                JIPipe.getInstance().getPluginRegistry().scheduleDeactivatePlugin(plugin.getDependencyId());
            }
        }

        if(!pluginsToActivate.isEmpty()) {
            getProgressInfo().log("Activating plugins ...");
            for (JIPipePlugin plugin : pluginsToActivate) {
                getProgressInfo().log("-> " + plugin.getDependencyId());
                JIPipe.getInstance().getPluginRegistry().scheduleActivatePlugin(plugin.getDependencyId());
            }
        }
    }
}
