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
import org.hkijena.jipipe.api.JIPipeRunnable;
import org.hkijena.jipipe.api.registries.JIPipeExtensionRegistry;
import org.hkijena.jipipe.ui.running.JIPipeRunnerQueue;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;

public class ExtensionItemActionButton extends JButton {

    private final JIPipeModernPluginManager pluginManager;
    private final JIPipeExtension extension;

    public ExtensionItemActionButton(JIPipeModernPluginManager pluginManager, JIPipeExtension extension) {
        this.pluginManager = pluginManager;
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
        if (getExtensionRegistry().willBeActivatedOnNextStartup(extension.getDependencyId())) {
            pluginManager.deactivateExtension(extension);
        } else {
            pluginManager.activateExtension(extension);
        }
    }

    private void updateDisplay() {
        setEnabled(!extension.isCoreExtension());
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
    public void onExtensionActivated(JIPipeExtensionRegistry.ScheduledActivateExtensionEvent event) {
        updateDisplay();
    }

    @Override
    public void onExtensionDeactivated(JIPipeExtensionRegistry.ScheduledDeactivateExtensionEvent event) {
        updateDisplay();
    }

    @Override
    public void onUpdateSiteActivated(JIPipeRunnable.FinishedEvent event) {
        if (event.getRun() instanceof ActivateAndApplyUpdateSiteRun || event.getRun() instanceof DeactivateAndApplyUpdateSiteRun) {
            if (extension instanceof UpdateSiteExtension) {
                // Try to update it
                ((UpdateSiteExtension) extension).getUpdateSite(pluginManager.getUpdateSites());
            }
            updateDisplay();
        }
    }
}
