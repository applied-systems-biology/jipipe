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

import com.google.common.eventbus.Subscribe;
import net.imagej.updater.UpdateSite;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.JIPipeExtension;
import org.hkijena.jipipe.JIPipeImageJUpdateSiteDependency;
import org.hkijena.jipipe.api.registries.JIPipeExtensionRegistry;
import org.hkijena.jipipe.ui.ijupdater.ActivateUpdateSiteRun;
import org.hkijena.jipipe.ui.running.JIPipeRunExecuterUI;
import org.hkijena.jipipe.ui.running.JIPipeRunnerQueue;
import org.hkijena.jipipe.ui.running.RunWorkerFinishedEvent;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.util.*;

public class ExtensionItemActionButton extends JButton {

    private final JIPipeModernPluginManagerUI pluginManagerUI;
    private final JIPipeExtension extension;

    public ExtensionItemActionButton(JIPipeModernPluginManagerUI pluginManagerUI, JIPipeExtension extension) {
        this.pluginManagerUI = pluginManagerUI;
        this.extension = extension;
        addActionListener(e -> executeAction());
        updateDisplay();
        getExtensionRegistry().getEventBus().register(this);
        JIPipeRunnerQueue.getInstance().getEventBus().register(this);
    }

    private JIPipeExtensionRegistry getExtensionRegistry() {
        return JIPipe.getInstance().getExtensionRegistry();
    }

    private void executeAction() {
        if(extension.isActivated()) {
            if(extension.isScheduledForDeactivation()) {
                getExtensionRegistry().clearSchedule(extension.getDependencyId());
            }
            else {
                getExtensionRegistry().scheduleDeactivateExtension(extension.getDependencyId());
            }
        }
        else {
            if(extension.isScheduledForActivation()) {
                getExtensionRegistry().clearSchedule(extension.getDependencyId());
            }
            else {
                activateExtension();
            }
        }
    }

    private void activateExtension() {
        if(extension instanceof UpdateSiteExtension) {
            if(!pluginManagerUI.isUpdateSitesReady()) {
                JOptionPane.showMessageDialog(SwingUtilities.getWindowAncestor(this), "ImageJ updates sites are currently not ready/unavailable.",
                        "Activate ImageJ update site", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (!showUpdateSiteConfirmationDialog(Collections.singleton(((UpdateSiteExtension) extension).getUpdateSite().getName())))
                return;
        }
        else {
            if(extension.isActivated()) {
                getExtensionRegistry().clearSchedule(extension.getDependencyId());
                return;
            }
            if(extension.getImageJUpdateSiteDependencies().isEmpty()) {
                getExtensionRegistry().scheduleActivateExtension(extension.getDependencyId());
            }
            else {
                if(!pluginManagerUI.isUpdateSitesReady()) {
                    int response = JOptionPane.showOptionDialog(SwingUtilities.getWindowAncestor(this), "The selected extension requests various ImageJ update sites, but there is currently no connection to the update site system. You can ignore update sites or wait until the initialization is complete. If you click 'Wait' click the 'Activate' " +
                                    "button again after the update sites have been initialized.",
                            "Activate " + extension.getMetadata().getName(), JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, new Object[]{"Wait", "Ignore", "Cancel"},
                            "Wait");
                    if(response == JOptionPane.YES_OPTION || response == JOptionPane.CANCEL_OPTION)
                        return;
                    getExtensionRegistry().scheduleActivateExtension(extension.getDependencyId());
                }
                else {
                    // Check if there are missing update sites
                    Set<String> missing = new HashSet<>();
                    for (JIPipeImageJUpdateSiteDependency dependency : extension.getImageJUpdateSiteDependencies()) {
                        missing.add(dependency.getName());
                    }
                    if(pluginManagerUI.getUpdateSites() != null) {
                        for (UpdateSite updateSite : pluginManagerUI.getUpdateSites().getUpdateSites(false)) {
                            missing.remove(updateSite.getName());
                        }
                    }
                    if(missing.isEmpty()) {
                        // Can directly activate
                        getExtensionRegistry().scheduleActivateExtension(extension.getDependencyId());
                    }
                    else {
                        if (!showUpdateSiteConfirmationDialog(missing))
                            return;
                        getExtensionRegistry().scheduleActivateExtension(extension.getDependencyId());
                    }
                }
            }
        }
    }

    private boolean showUpdateSiteConfirmationDialog(Set<String> missing) {
        // Show confirm dialog
        InstallUpdateSitesConfirmationDialog dialog = new InstallUpdateSitesConfirmationDialog(this, pluginManagerUI, extension, missing);
        dialog.setModal(true);
        dialog.setSize(800,600);
        dialog.setLocationRelativeTo(SwingUtilities.getWindowAncestor(this));
        dialog.setVisible(true);
        if(dialog.isCancelled()) {
            return false;
        }
        // Trigger installation run
        List<UpdateSite> toActivate = new ArrayList<>();
        for (Map.Entry<JIPipeImageJUpdateSiteDependency, Boolean> entry : dialog.getSitesToInstall().entrySet()) {
            if(entry.getValue()) {
                UpdateSite updateSite = pluginManagerUI.getUpdateSites().getUpdateSite(entry.getKey().getName(), true);
                if(updateSite != null) {
                    toActivate.add(updateSite);
                }
                else {
                    updateSite = pluginManagerUI.getUpdateSites().addUpdateSite(entry.getKey().toUpdateSite());
                    toActivate.add(updateSite);
                }
            }
        }
        ActivateAndApplyUpdateSiteRun run = new ActivateAndApplyUpdateSiteRun(pluginManagerUI.getUpdateSites(), toActivate);
        JIPipeRunExecuterUI.runInDialog(SwingUtilities.getWindowAncestor(this), run);
        return true;
    }

    private void updateDisplay() {
        setEnabled(!extension.isCoreExtension());
        if(extension.isActivated()) {
            if(extension.isScheduledForDeactivation()) {
                setText("Undo deactivation");
                setIcon(UIUtils.getIconFromResources("actions/undo.png"));
            }
            else {
                setText("Deactivate");
                setIcon(UIUtils.getIconFromResources("emblems/vcs-conflicting.png"));
            }
        }
        else {
            if(extension.isScheduledForActivation()) {
                setText("Undo activation");
                setIcon(UIUtils.getIconFromResources("actions/undo.png"));
            }
            else {
                setText("Activate");
                setIcon(UIUtils.getIconFromResources("emblems/vcs-normal.png"));
            }
        }
    }

    @Subscribe
    public void onExtensionActivated(JIPipeExtensionRegistry.ScheduledActivateExtension event) {
        updateDisplay();
    }

    @Subscribe
    public void onExtensionDeactivated(JIPipeExtensionRegistry.ScheduledDeactivateExtension event) {
        updateDisplay();
    }

    @Subscribe
    public void onUpdateSiteActivated(RunWorkerFinishedEvent event) {
        if(event.getRun() instanceof ActivateAndApplyUpdateSiteRun) {
            updateDisplay();
        }
    }
}
