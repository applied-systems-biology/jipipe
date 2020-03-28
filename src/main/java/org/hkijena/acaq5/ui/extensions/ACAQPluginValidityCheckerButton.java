package org.hkijena.acaq5.ui.extensions;

import com.google.common.eventbus.Subscribe;
import org.hkijena.acaq5.ACAQDefaultRegistry;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.events.ExtensionRegisteredEvent;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;

public class ACAQPluginValidityCheckerButton extends JButton {

    private ACAQValidityReport report = new ACAQValidityReport();

    public ACAQPluginValidityCheckerButton() {
        recheckValidity();
        ACAQDefaultRegistry.getInstance().getEventBus().register(this);
    }

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

    @Subscribe
    public void onExtensionRegistered(ExtensionRegisteredEvent event) {
        recheckValidity();
    }
}
