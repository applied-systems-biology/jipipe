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

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.JIPipePlugin;
import org.hkijena.jipipe.api.registries.JIPipePluginRegistry;
import org.hkijena.jipipe.api.run.JIPipeRunnable;
import org.hkijena.jipipe.api.run.JIPipeRunnableQueue;
import org.hkijena.jipipe.desktop.app.plugins.JIPipeDesktopActivateAndApplyUpdateSiteRun;
import org.hkijena.jipipe.desktop.app.plugins.JIPipeDesktopDeactivateAndApplyUpdateSiteRun;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;

public class JIPipeDesktopExtensionItemActionButton extends JButton implements JIPipePluginRegistry.ScheduledActivatePluginEventListener, JIPipePluginRegistry.ScheduledDeactivatePluginEventListener, JIPipeRunnable.FinishedEventListener {

    private final JIPipeDesktopModernPluginManager pluginManager;
    private final JIPipePlugin extension;

    public JIPipeDesktopExtensionItemActionButton(JIPipeDesktopModernPluginManager pluginManager, JIPipePlugin extension) {
        this.pluginManager = pluginManager;
        this.extension = extension;
        addActionListener(e -> executeAction());
        updateDisplay();
        getExtensionRegistry().getScheduledActivatePluginEventEmitter().subscribeWeak(this);
        getExtensionRegistry().getScheduledDeactivatePluginEventEmitter().subscribeWeak(this);
        JIPipeRunnableQueue.getInstance().getFinishedEventEmitter().subscribeWeak(this);
    }

    private JIPipePluginRegistry getExtensionRegistry() {
        return JIPipe.getInstance().getPluginRegistry();
    }

    private void executeAction() {
        if (getExtensionRegistry().willBeActivatedOnNextStartup(extension.getDependencyId())) {
            pluginManager.deactivateExtension(extension);
        } else {
            pluginManager.activateExtension(extension);
        }
    }

    private void updateDisplay() {
        setEnabled(!extension.isCorePlugin());
        if (extension.isActivated()) {
            if (extension.isScheduledForDeactivation()) {
                setText("Undo deactivation");
                setIcon(UIUtils.getIconFromResources("actions/undo.png"));
            } else {
                setText("Deactivate");
                setIcon(UIUtils.getIconFromResources("emblems/vcs-conflicting.png"));
            }
        } else {
            if (extension.isScheduledForActivation()) {
                setText("Undo activation");
                setIcon(UIUtils.getIconFromResources("actions/undo.png"));
            } else {
                setText("Activate");
                setIcon(UIUtils.getIconFromResources("emblems/vcs-normal.png"));
            }
        }
    }

    @Override
    public void onScheduledActivatePlugin(JIPipePluginRegistry.ScheduledActivatePluginEvent event) {
        updateDisplay();
    }

    @Override
    public void onScheduledDeactivatePlugin(JIPipePluginRegistry.ScheduledDeactivatePluginEvent event) {
        updateDisplay();
    }

    @Override
    public void onRunnableFinished(JIPipeRunnable.FinishedEvent event) {
        if (event.getRun() instanceof JIPipeDesktopActivateAndApplyUpdateSiteRun || event.getRun() instanceof JIPipeDesktopDeactivateAndApplyUpdateSiteRun) {
            if (extension instanceof JIPipeDesktopUpdateSitePlugin) {
                // Try to update it
                ((JIPipeDesktopUpdateSitePlugin) extension).getUpdateSite(pluginManager.getUpdateSites());
            }
            updateDisplay();
        }
    }
}
