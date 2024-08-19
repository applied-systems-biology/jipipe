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
import org.hkijena.jipipe.JIPipeImageJUpdateSiteDependency;
import org.hkijena.jipipe.JIPipePlugin;
import org.hkijena.jipipe.api.AbstractJIPipeRunnable;

import java.util.Set;

public class JIPipeDesktopApplyPluginManagerRun extends AbstractJIPipeRunnable {

    private final Set<JIPipePlugin> pluginsToActivate;
    private final Set<JIPipePlugin> pluginsToDeactivate;
    private final Set<JIPipeImageJUpdateSiteDependency> updateSitesToInstall;

    public JIPipeDesktopApplyPluginManagerRun(Set<JIPipePlugin> pluginsToActivate, Set<JIPipePlugin> pluginsToDeactivate, Set<JIPipeImageJUpdateSiteDependency> updateSitesToInstall) {
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

        getProgressInfo().log("Any additional items that need to be manually resolved will be displayed here:");
        if(updateSitesToInstall.isEmpty()) {
            getProgressInfo().log("-> Nothing to do");
        }
        else {
            for (JIPipeImageJUpdateSiteDependency updateSite : updateSitesToInstall) {
                getProgressInfo().log("-> TODO: Check if update site " + updateSite.getName() + " is activated");
            }
        }

    }
}
