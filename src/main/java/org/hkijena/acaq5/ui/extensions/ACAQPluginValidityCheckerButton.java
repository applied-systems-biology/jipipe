package org.hkijena.acaq5.ui.extensions;

import com.google.common.eventbus.Subscribe;
import org.hkijena.acaq5.ACAQDefaultRegistry;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.events.ExtensionRegisteredEvent;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;

/**
 * Button that checks the plugin validity and shows the report if clicked
 */
public class ACAQPluginValidityCheckerButton extends JButton {

    private ACAQValidityReport report = new ACAQValidityReport();

    /**
     * Creates new instance
     */
    public ACAQPluginValidityCheckerButton() {
        recheckValidity();
        ACAQDefaultRegistry.getInstance().getEventBus().register(this);
    }

    /**
     * Triggers a validity check
     */
    public void recheckValidity() {
        report.clear();
        ACAQDefaultRegistry.getInstance().reportValidity(report);

        if (report.isValid()) {
            setText("All plugins valid");
            setIcon(UIUtils.getIconFromResources("check-circle-green.png"));
        } else {
            setText("Some plugins could not be loaded");
            setIcon(UIUtils.getIconFromResources("error.png"));
        }
    }

    /**
     * Triggered when an extension is registered.
     * Rechecks validity.
     *
     * @param event Generated event
     */
    @Subscribe
    public void onExtensionRegistered(ExtensionRegisteredEvent event) {
        recheckValidity();
    }
}
