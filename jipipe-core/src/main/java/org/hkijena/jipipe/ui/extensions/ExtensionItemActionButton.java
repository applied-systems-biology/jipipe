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
    private final JIPipeExtension extension;

    public ExtensionItemActionButton(JIPipeExtension extension) {
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
                getExtensionRegistry().scheduleActivateExtension(extension.getDependencyId());
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
