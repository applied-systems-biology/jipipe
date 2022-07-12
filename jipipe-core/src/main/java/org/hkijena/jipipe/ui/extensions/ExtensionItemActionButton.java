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
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.JIPipeExtension;
import org.hkijena.jipipe.api.registries.JIPipeExtensionRegistry;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;

public class ExtensionItemActionButton extends JButton {

    private final JIPipeModernPluginManagerUI pluginManagerUI;
    private final JIPipeExtension extension;

    public ExtensionItemActionButton(JIPipeModernPluginManagerUI pluginManagerUI, JIPipeExtension extension) {
        this.pluginManagerUI = pluginManagerUI;
        this.extension = extension;
        addActionListener(e -> executeAction());
        updateDisplay();
        getExtensionRegistry().getEventBus().register(this);
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
                    TODO
                }
            }
        }
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
}
